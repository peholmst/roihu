package net.pkhapps.roihu.scenario;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static net.pkhapps.roihu.db.generated.Tables.SCENARIO;
import static net.pkhapps.roihu.db.generated.Tables.SCENARIO_POSITION;

/**
 * The deployment's scenario library (ADR-0003). For now only what the development seeder and
 * the tests need to put a scenario in place.
 */
@Service
public class Scenarios {

    private final DSLContext db;

    Scenarios(DSLContext db) {
        this.db = db;
    }

    @Transactional
    public ScenarioId create(String name, PreparedLanguage preparedLanguage, List<ScenarioPosition> positions) {
        var id = Objects.requireNonNull(db.insertInto(SCENARIO)
                .set(SCENARIO.NAME, name)
                .set(SCENARIO.PREPARED_LANGUAGE,
                        net.pkhapps.roihu.db.generated.enums.PreparedLanguage.lookupLiteral(preparedLanguage.code()))
                .returning(SCENARIO.ID)
                .fetchSingle(SCENARIO.ID), "An inserted scenario always has an id");
        for (var ordinal = 0; ordinal < positions.size(); ordinal++) {
            var position = positions.get(ordinal);
            db.insertInto(SCENARIO_POSITION)
                    .set(SCENARIO_POSITION.SCENARIO_ID, id)
                    .set(SCENARIO_POSITION.ORDINAL, ordinal)
                    .set(SCENARIO_POSITION.NAME, position.name())
                    .set(SCENARIO_POSITION.CALL_SIGN, position.callSign().orElse(null))
                    .execute();
        }
        return new ScenarioId(id);
    }
}
