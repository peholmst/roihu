package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.scenario.PreparedLanguage;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import static net.pkhapps.roihu.db.generated.Tables.*;

/** Everything a crew member does to get from a join code to a position. */
@Service
public class CrewJoining {

    /** The stored state, whose generated name clashes with {@link ExerciseState}. */
    private static final net.pkhapps.roihu.db.generated.enums.ExerciseState STORED_ENDED =
            net.pkhapps.roihu.db.generated.enums.ExerciseState.ended;

    private final DSLContext db;
    private final ExerciseChanges changes;
    private final SecureRandom random = new SecureRandom();

    CrewJoining(DSLContext db, ExerciseChanges changes) {
        this.db = db;
        this.changes = changes;
    }

    /**
     * Calls {@code onChange} whenever a position of the exercise is taken, taken over or
     * released, or the exercise changes state. It may be called from any thread, and only says
     * that something changed: read the exercise again to find out what.
     */
    public Subscription subscribe(JoinCode joinCode, Runnable onChange) {
        return changes.subscribe(joinCode, onChange);
    }

    /**
     * Finds the exercise a typed code admits to. Unknown codes, malformed codes and the codes of
     * ended exercises all find nothing, so the answer never reveals whether a code ever existed.
     */
    @Transactional(readOnly = true)
    public Optional<JoinableExercise> findExercise(String typedCode) {
        return JoinCode.parse(typedCode).flatMap(joinCode -> db
                .select(EXERCISE.ID, EXERCISE.STATE, EXERCISE.PREPARED_LANGUAGE)
                .from(EXERCISE)
                .where(EXERCISE.JOIN_CODE.eq(joinCode.value()))
                .and(EXERCISE.STATE.ne(STORED_ENDED))
                .fetchOptional(record -> new JoinableExercise(
                        ExerciseStates.of(record.get(EXERCISE.STATE)),
                        PreparedLanguage.fromCode(record.get(EXERCISE.PREPARED_LANGUAGE).getLiteral()),
                        db.select(EXERCISE_POSITION.ID, EXERCISE_POSITION.NAME, EXERCISE_POSITION.CALL_SIGN,
                                        HOLDING.EXERCISE_POSITION_ID.isNotNull())
                                .from(EXERCISE_POSITION)
                                .leftJoin(HOLDING).on(HOLDING.EXERCISE_POSITION_ID.eq(EXERCISE_POSITION.ID))
                                .where(EXERCISE_POSITION.EXERCISE_ID.eq(record.get(EXERCISE.ID)))
                                .orderBy(EXERCISE_POSITION.ORDINAL)
                                .fetch(position -> new ExercisePosition(new PositionId(position.value1()),
                                        position.value2(), Optional.ofNullable(position.value3()),
                                        position.value4())))));
    }

    /**
     * Takes a free position in the exercise that {@code typedCode} admits to. The database decides
     * between two crew members taking the same position at once: exactly one of them gets it.
     */
    @Transactional
    public TakeResult take(String typedCode, PositionId position) {
        var joinCode = JoinCode.parse(typedCode);
        if (joinCode.isEmpty()) {
            return new TakeResult.NotJoinable();
        }
        var joinable = joinablePosition(joinCode.get(), position);
        var token = HolderToken.random(random);
        var inserted = db.insertInto(HOLDING, HOLDING.EXERCISE_POSITION_ID, HOLDING.TOKEN_HASH)
                .select(db.select(EXERCISE_POSITION.ID, DSL.val(token.hash()))
                        .from(EXERCISE_POSITION)
                        .where(joinable))
                .onConflict(HOLDING.EXERCISE_POSITION_ID).doNothing()
                .execute();
        if (inserted == 1) {
            changes.publish(joinCode.get());
            return new TakeResult.Taken(token);
        }
        return db.fetchExists(EXERCISE_POSITION, joinable)
                ? new TakeResult.AlreadyTaken()
                : new TakeResult.NotJoinable();
    }

    /**
     * Takes a position whoever holds it, in the exercise that {@code typedCode} admits to. The
     * previous holder's token stops holding anything, which is how a crew member gets their
     * position back on a new device. Refused once the exercise has ended.
     */
    @Transactional
    public TakeResult takeOver(String typedCode, PositionId position) {
        var joinCode = JoinCode.parse(typedCode);
        if (joinCode.isEmpty()) {
            return new TakeResult.NotJoinable();
        }
        var token = HolderToken.random(random);
        var taken = db.insertInto(HOLDING, HOLDING.EXERCISE_POSITION_ID, HOLDING.TOKEN_HASH)
                .select(db.select(EXERCISE_POSITION.ID, DSL.val(token.hash()))
                        .from(EXERCISE_POSITION)
                        .where(joinablePosition(joinCode.get(), position)))
                .onConflict(HOLDING.EXERCISE_POSITION_ID).doUpdate()
                .set(HOLDING.TOKEN_HASH, token.hash())
                .execute();
        if (taken == 0) {
            return new TakeResult.NotJoinable();
        }
        changes.publish(joinCode.get());
        return new TakeResult.Taken(token);
    }

    private static Condition joinablePosition(JoinCode joinCode, PositionId position) {
        return EXERCISE_POSITION.ID.eq(position.value())
                .and(EXERCISE_POSITION.EXERCISE_ID.in(DSL.select(EXERCISE.ID).from(EXERCISE)
                        .where(EXERCISE.JOIN_CODE.eq(joinCode.value()))
                        .and(EXERCISE.STATE.ne(STORED_ENDED))));
    }

    /**
     * Releases the position a token holds, so that its holder can choose again and someone else
     * can take it. Refused once the exercise has ended, when no position changes hands any more.
     * Returns whether a position was released.
     */
    @Transactional
    public boolean changePosition(HolderToken token) {
        var released = db.deleteFrom(HOLDING)
                .where(HOLDING.TOKEN_HASH.eq(token.hash()))
                .and(HOLDING.EXERCISE_POSITION_ID.in(DSL.select(EXERCISE_POSITION.ID)
                        .from(EXERCISE_POSITION).join(EXERCISE).on(EXERCISE.ID.eq(EXERCISE_POSITION.EXERCISE_ID))
                        .where(EXERCISE.STATE.ne(STORED_ENDED))))
                .returning(HOLDING.EXERCISE_POSITION_ID)
                .fetchOptional(HOLDING.EXERCISE_POSITION_ID);
        released.ifPresent(position -> changes.publish(joinCodeOf(position)));
        return released.isPresent();
    }

    private JoinCode joinCodeOf(UUID position) {
        return db.select(EXERCISE.JOIN_CODE)
                .from(EXERCISE).join(EXERCISE_POSITION).on(EXERCISE_POSITION.EXERCISE_ID.eq(EXERCISE.ID))
                .where(EXERCISE_POSITION.ID.eq(position))
                .fetchOptional(EXERCISE.JOIN_CODE)
                .flatMap(JoinCode::parse)
                .orElseThrow();
    }

    /** Finds the position a token holds. A holding outlives the exercise ending. */
    @Transactional(readOnly = true)
    public Optional<Holding> findHolding(HolderToken token) {
        return db.select(EXERCISE.JOIN_CODE, EXERCISE.STATE,
                        EXERCISE_POSITION.ID, EXERCISE_POSITION.NAME, EXERCISE_POSITION.CALL_SIGN)
                .from(HOLDING)
                .join(EXERCISE_POSITION).on(EXERCISE_POSITION.ID.eq(HOLDING.EXERCISE_POSITION_ID))
                .join(EXERCISE).on(EXERCISE.ID.eq(EXERCISE_POSITION.EXERCISE_ID))
                .where(HOLDING.TOKEN_HASH.eq(token.hash()))
                .fetchOptional(record -> new Holding(
                        JoinCode.parse(record.get(EXERCISE.JOIN_CODE)).orElseThrow(),
                        new ExercisePosition(new PositionId(record.get(EXERCISE_POSITION.ID)),
                                record.get(EXERCISE_POSITION.NAME),
                                Optional.ofNullable(record.get(EXERCISE_POSITION.CALL_SIGN)), true),
                        ExerciseStates.of(record.get(EXERCISE.STATE))));
    }
}
