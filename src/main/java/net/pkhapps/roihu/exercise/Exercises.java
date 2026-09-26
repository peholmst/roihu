package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.base.security.Officer;
import net.pkhapps.roihu.scenario.Change;
import net.pkhapps.roihu.scenario.ScenarioId;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import static net.pkhapps.roihu.db.generated.Tables.*;

/** Runs of scenarios, as the training officers who run them see them. */
@Service
public class Exercises {

    private static final net.pkhapps.roihu.db.generated.enums.ExerciseState STORED_ENDED =
            net.pkhapps.roihu.db.generated.enums.ExerciseState.ended;

    private final DSLContext db;
    private final ExerciseChanges changes;
    private final Supplier<RandomGenerator> randomness;

    @Autowired
    Exercises(DSLContext db, ExerciseChanges changes) {
        this(db, changes, SecureRandom::new);
    }

    /** Takes one generator from {@code randomness} for each exercise it creates. */
    Exercises(DSLContext db, ExerciseChanges changes, Supplier<RandomGenerator> randomness) {
        this.db = db;
        this.changes = changes;
        this.randomness = randomness;
    }

    /**
     * Every exercise of the deployment, whoever created it: those not yet ended first, most
     * recently created first, then the ended ones, most recently ended first.
     */
    @Transactional(readOnly = true)
    public List<ExerciseSummary> list() {
        var positionCount = DSL.field(DSL.selectCount().from(EXERCISE_POSITION)
                .where(EXERCISE_POSITION.EXERCISE_ID.eq(EXERCISE.ID)));
        var positionsTaken = DSL.field(DSL.selectCount()
                .from(EXERCISE_POSITION).join(HOLDING).on(HOLDING.EXERCISE_POSITION_ID.eq(EXERCISE_POSITION.ID))
                .where(EXERCISE_POSITION.EXERCISE_ID.eq(EXERCISE.ID)));
        var ended = EXERCISE.STATE.eq(STORED_ENDED);
        return db.select(EXERCISE.ID, EXERCISE.SCENARIO_NAME, EXERCISE.STATE, EXERCISE.CREATED_BY,
                        EXERCISE.CREATED_AT, EXERCISE.STARTED_AT, EXERCISE.ENDED_AT, positionsTaken, positionCount)
                .from(EXERCISE)
                .orderBy(DSL.when(ended, 1).otherwise(0), EXERCISE.ENDED_AT.desc().nullsLast(),
                        EXERCISE.CREATED_AT.desc(), EXERCISE.ID)
                .fetch(exercise -> new ExerciseSummary(new ExerciseId(exercise.value1()), exercise.value2(),
                        ExerciseStates.of(exercise.value3()),
                        new Change(new Officer(exercise.value4()), exercise.value5().toInstant()),
                        Optional.ofNullable(exercise.value6()).map(OffsetDateTime::toInstant),
                        Optional.ofNullable(exercise.value7()).map(OffsetDateTime::toInstant),
                        exercise.value8(), exercise.value9()));
    }

    @Transactional(readOnly = true)
    public Optional<Exercise> get(ExerciseId id) {
        return db.selectFrom(EXERCISE)
                .where(EXERCISE.ID.eq(id.value()))
                .fetchOptional(exercise -> new Exercise(id, exercise.getScenarioName(),
                        ExerciseStates.of(exercise.getState()),
                        JoinCode.parse(exercise.getJoinCode()).orElseThrow(),
                        new Change(new Officer(exercise.getCreatedBy()), exercise.getCreatedAt().toInstant())));
    }

    /**
     * Creates an exercise in setup, created by {@code officer}. It owns a copy of the scenario's
     * name, prepared language and positions (ADR-0002), so that editing the scenario changes
     * nothing about it. Refused for a scenario without positions, which nobody could join.
     */
    @Transactional
    public CreateResult createFrom(ScenarioId scenario, Officer officer) {
        // Locks the scenario, so that no save can change what is copied from it until this commits.
        var source = db.select(SCENARIO.NAME, SCENARIO.PREPARED_LANGUAGE)
                .from(SCENARIO)
                .where(SCENARIO.ID.eq(scenario.value()))
                .forShare()
                .fetchOptional();
        if (source.isEmpty()) {
            return new CreateResult.ScenarioGone();
        }
        if (!db.fetchExists(SCENARIO_POSITION, SCENARIO_POSITION.SCENARIO_ID.eq(scenario.value()))) {
            return new CreateResult.NoPositions();
        }
        var random = randomness.get();
        JoinCode joinCode;
        Optional<UUID> exerciseId;
        do {
            joinCode = JoinCode.random(random);
            exerciseId = db.insertInto(EXERCISE)
                    .set(EXERCISE.SCENARIO_ID, scenario.value())
                    .set(EXERCISE.SCENARIO_NAME, source.get().value1())
                    .set(EXERCISE.PREPARED_LANGUAGE, source.get().value2())
                    .set(EXERCISE.JOIN_CODE, joinCode.value())
                    .set(EXERCISE.CREATED_BY, officer.email())
                    .onConflict(EXERCISE.JOIN_CODE).doNothing()
                    .returning(EXERCISE.ID)
                    .fetchOptional(EXERCISE.ID);
        } while (exerciseId.isEmpty());
        db.insertInto(EXERCISE_POSITION,
                        EXERCISE_POSITION.EXERCISE_ID, EXERCISE_POSITION.ORDINAL,
                        EXERCISE_POSITION.NAME, EXERCISE_POSITION.CALL_SIGN)
                .select(db.select(DSL.val(exerciseId.get()), SCENARIO_POSITION.ORDINAL,
                                SCENARIO_POSITION.NAME, SCENARIO_POSITION.CALL_SIGN)
                        .from(SCENARIO_POSITION)
                        .where(SCENARIO_POSITION.SCENARIO_ID.eq(scenario.value())))
                .execute();
        return new CreateResult.Created(new ExerciseId(exerciseId.get()), joinCode);
    }

    /**
     * Ends the exercise, after which it admits nobody. Unguarded for now: the lifecycle's rules
     * arrive with the officer's controls.
     */
    @Transactional
    public void end(JoinCode joinCode) {
        db.update(EXERCISE)
                .set(EXERCISE.STATE, STORED_ENDED)
                .set(EXERCISE.ENDED_AT, DSL.currentOffsetDateTime())
                .where(EXERCISE.JOIN_CODE.eq(joinCode.value()))
                .execute();
        changes.publish(joinCode);
    }
}
