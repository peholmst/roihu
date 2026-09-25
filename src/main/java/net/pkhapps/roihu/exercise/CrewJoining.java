package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.scenario.PreparedLanguage;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static net.pkhapps.roihu.db.generated.Tables.*;

/** Everything a crew member does to get from a join code to a position. */
@Service
public class CrewJoining {

    private final DSLContext db;

    CrewJoining(DSLContext db) {
        this.db = db;
    }

    /**
     * Finds the exercise a typed code admits to. Unknown codes, malformed codes and the codes of
     * ended exercises all find nothing, so the answer never reveals whether a code ever existed.
     */
    @Transactional(readOnly = true)
    public Optional<JoinableExercise> findExercise(String typedCode) {
        return JoinCode.parse(typedCode).flatMap(joinCode -> db
                .select(EXERCISE.ID, EXERCISE.STATE, SCENARIO.PREPARED_LANGUAGE)
                .from(EXERCISE).join(SCENARIO).on(SCENARIO.ID.eq(EXERCISE.SCENARIO_ID))
                .where(EXERCISE.JOIN_CODE.eq(joinCode.value()))
                .and(EXERCISE.STATE.ne(net.pkhapps.roihu.db.generated.enums.ExerciseState.ended))
                .fetchOptional(record -> new JoinableExercise(
                        toExerciseState(record.get(EXERCISE.STATE)),
                        PreparedLanguage.fromCode(record.get(SCENARIO.PREPARED_LANGUAGE).getLiteral()),
                        db.select(EXERCISE_POSITION.NAME, EXERCISE_POSITION.CALL_SIGN)
                                .from(EXERCISE_POSITION)
                                .where(EXERCISE_POSITION.EXERCISE_ID.eq(record.get(EXERCISE.ID)))
                                .orderBy(EXERCISE_POSITION.ORDINAL)
                                .fetch(position -> new ExercisePosition(position.value1(),
                                        Optional.ofNullable(position.value2()))))));
    }

    private static ExerciseState toExerciseState(net.pkhapps.roihu.db.generated.enums.ExerciseState stored) {
        return switch (stored) {
            case setup -> ExerciseState.SETUP;
            case running -> ExerciseState.RUNNING;
            case ended -> ExerciseState.ENDED;
        };
    }
}
