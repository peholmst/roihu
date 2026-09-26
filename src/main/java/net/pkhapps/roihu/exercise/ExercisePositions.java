package net.pkhapps.roihu.exercise;

import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.pkhapps.roihu.db.generated.Tables.EXERCISE_POSITION;
import static net.pkhapps.roihu.db.generated.Tables.HOLDING;

/** An exercise's positions in scenario order, each free or taken, as the crew and officers see them. */
final class ExercisePositions {

    private ExercisePositions() {
    }

    static List<ExercisePosition> of(DSLContext db, UUID exercise) {
        return db.select(EXERCISE_POSITION.ID, EXERCISE_POSITION.NAME, EXERCISE_POSITION.CALL_SIGN,
                        HOLDING.EXERCISE_POSITION_ID.isNotNull())
                .from(EXERCISE_POSITION)
                .leftJoin(HOLDING).on(HOLDING.EXERCISE_POSITION_ID.eq(EXERCISE_POSITION.ID))
                .where(EXERCISE_POSITION.EXERCISE_ID.eq(exercise))
                .orderBy(EXERCISE_POSITION.ORDINAL)
                .fetch(position -> new ExercisePosition(new PositionId(position.value1()),
                        position.value2(), Optional.ofNullable(position.value3()), position.value4()));
    }
}
