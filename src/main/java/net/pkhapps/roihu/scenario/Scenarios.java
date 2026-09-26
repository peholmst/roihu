package net.pkhapps.roihu.scenario;

import net.pkhapps.roihu.base.security.Officer;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static net.pkhapps.roihu.db.generated.Tables.SCENARIO;
import static net.pkhapps.roihu.db.generated.Tables.SCENARIO_POSITION;

/**
 * The deployment's scenario library (ADR-0003), shared by all its officers: any officer may
 * change any scenario.
 */
@Service
public class Scenarios {

    private final DSLContext db;

    Scenarios(DSLContext db) {
        this.db = db;
    }

    /** The whole library, the scenarios changed most recently first. */
    @Transactional(readOnly = true)
    public List<ScenarioSummary> list() {
        var positionCount = DSL.field(DSL.selectCount().from(SCENARIO_POSITION)
                .where(SCENARIO_POSITION.SCENARIO_ID.eq(SCENARIO.ID)));
        return db.select(SCENARIO.ID, SCENARIO.NAME, SCENARIO.PREPARED_LANGUAGE, positionCount,
                        SCENARIO.LAST_CHANGED_BY, SCENARIO.LAST_CHANGED_AT)
                .from(SCENARIO)
                .orderBy(SCENARIO.LAST_CHANGED_AT.desc(), SCENARIO.ID)
                .fetch(scenario -> new ScenarioSummary(new ScenarioId(scenario.value1()), scenario.value2(),
                        PreparedLanguage.fromCode(scenario.value3().getLiteral()), scenario.value4(),
                        new Change(new Officer(scenario.value5()), scenario.value6().toInstant())));
    }

    /**
     * Reads the scenario and its positions from one snapshot, so that a save committed between the
     * two reads never shows as a mixture of two versions.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Optional<Scenario> get(ScenarioId id) {
        return db.selectFrom(SCENARIO)
                .where(SCENARIO.ID.eq(id.value()))
                .fetchOptional(scenario -> new Scenario(id, new ScenarioContent(
                        scenario.getName(),
                        PreparedLanguage.fromCode(scenario.getPreparedLanguage().getLiteral()),
                        Optional.ofNullable(scenario.getDescription()),
                        db.select(SCENARIO_POSITION.NAME, SCENARIO_POSITION.CALL_SIGN)
                                .from(SCENARIO_POSITION)
                                .where(SCENARIO_POSITION.SCENARIO_ID.eq(id.value()))
                                .orderBy(SCENARIO_POSITION.ORDINAL)
                                .fetch(position -> new ScenarioPosition(position.value1(),
                                        Optional.ofNullable(position.value2())))),
                        new Change(new Officer(scenario.getCreatedBy()), scenario.getCreatedAt().toInstant()),
                        new Change(new Officer(scenario.getLastChangedBy()), scenario.getLastChangedAt().toInstant()),
                        scenario.getVersion()));
    }

    @Transactional
    public ScenarioId create(ScenarioContent content, Officer officer) {
        var id = Objects.requireNonNull(db.insertInto(SCENARIO)
                .set(SCENARIO.NAME, content.name())
                .set(SCENARIO.PREPARED_LANGUAGE, stored(content.preparedLanguage()))
                .set(SCENARIO.DESCRIPTION, content.description().orElse(null))
                .set(SCENARIO.CREATED_BY, officer.email())
                .set(SCENARIO.LAST_CHANGED_BY, officer.email())
                .returning(SCENARIO.ID)
                .fetchSingle(SCENARIO.ID), "An inserted scenario always has an id");
        insertPositions(id, content);
        return new ScenarioId(id);
    }

    /**
     * Replaces everything the scenario holds with {@code content}, unless someone has changed it
     * since {@code version}. Exercises hold their own copy of the positions (ADR-0002), so nothing
     * depends on the positions being replaced.
     */
    @Transactional
    public SaveResult save(ScenarioId id, int version, ScenarioContent content, Officer officer) {
        var saved = db.update(SCENARIO)
                .set(SCENARIO.NAME, content.name())
                .set(SCENARIO.PREPARED_LANGUAGE, stored(content.preparedLanguage()))
                .set(SCENARIO.DESCRIPTION, content.description().orElse(null))
                .set(SCENARIO.LAST_CHANGED_BY, officer.email())
                .set(SCENARIO.LAST_CHANGED_AT, DSL.currentOffsetDateTime())
                .set(SCENARIO.VERSION, SCENARIO.VERSION.plus(1))
                .where(SCENARIO.ID.eq(id.value()))
                .and(SCENARIO.VERSION.eq(version))
                .execute();
        if (saved == 0) {
            return new SaveResult.Conflict(lastChanged(id));
        }
        db.deleteFrom(SCENARIO_POSITION).where(SCENARIO_POSITION.SCENARIO_ID.eq(id.value())).execute();
        insertPositions(id.value(), content);
        return new SaveResult.Saved();
    }

    private Change lastChanged(ScenarioId id) {
        return db.select(SCENARIO.LAST_CHANGED_BY, SCENARIO.LAST_CHANGED_AT)
                .from(SCENARIO)
                .where(SCENARIO.ID.eq(id.value()))
                .fetchOptional(scenario -> new Change(new Officer(scenario.value1()), scenario.value2().toInstant()))
                .orElseThrow(() -> new IllegalArgumentException("No scenario " + id.value()));
    }

    private void insertPositions(UUID scenario, ScenarioContent content) {
        var positions = content.positions();
        for (var ordinal = 0; ordinal < positions.size(); ordinal++) {
            var position = positions.get(ordinal);
            db.insertInto(SCENARIO_POSITION)
                    .set(SCENARIO_POSITION.SCENARIO_ID, scenario)
                    .set(SCENARIO_POSITION.ORDINAL, ordinal)
                    .set(SCENARIO_POSITION.NAME, position.name())
                    .set(SCENARIO_POSITION.CALL_SIGN, position.callSign().orElse(null))
                    .execute();
        }
    }

    private static net.pkhapps.roihu.db.generated.enums.PreparedLanguage stored(PreparedLanguage language) {
        return Objects.requireNonNull(
                net.pkhapps.roihu.db.generated.enums.PreparedLanguage.lookupLiteral(language.code()),
                "Every prepared language is stored");
    }
}
