package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.scenario.ScenarioId;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import static net.pkhapps.roihu.db.generated.Tables.*;

/** Runs of scenarios. For now only what the development seeder and the tests need. */
@Service
public class Exercises {

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
     * Creates an exercise that owns a copy of the scenario's positions and prepared language, and
     * returns its join code.
     */
    @Transactional
    public JoinCode createFrom(ScenarioId scenario) {
        var preparedLanguage = db.select(SCENARIO.PREPARED_LANGUAGE)
                .from(SCENARIO)
                .where(SCENARIO.ID.eq(scenario.value()))
                .fetchOptional(SCENARIO.PREPARED_LANGUAGE)
                .orElseThrow(() -> new IllegalArgumentException("No scenario " + scenario.value()));
        var random = randomness.get();
        JoinCode joinCode;
        Optional<UUID> exerciseId;
        do {
            joinCode = JoinCode.random(random);
            exerciseId = db.insertInto(EXERCISE)
                    .set(EXERCISE.SCENARIO_ID, scenario.value())
                    .set(EXERCISE.JOIN_CODE, joinCode.value())
                    .set(EXERCISE.PREPARED_LANGUAGE, preparedLanguage)
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
        return joinCode;
    }

    /**
     * Ends the exercise, after which it admits nobody. Unguarded for now: the lifecycle's rules
     * arrive with the officer's controls.
     */
    @Transactional
    public void end(JoinCode joinCode) {
        db.update(EXERCISE)
                .set(EXERCISE.STATE, net.pkhapps.roihu.db.generated.enums.ExerciseState.ended)
                .where(EXERCISE.JOIN_CODE.eq(joinCode.value()))
                .execute();
        changes.publish(joinCode);
    }
}
