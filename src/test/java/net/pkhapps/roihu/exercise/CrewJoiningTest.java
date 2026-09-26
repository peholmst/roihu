package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioId;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static net.pkhapps.roihu.TestOfficers.ANNA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

@IntegrationTest
class CrewJoiningTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Autowired
    CrewJoining crewJoining;

    @Test
    void theJoinCodeFindsTheExerciseWithItsPositionsInScenarioOrder() {
        var scenario = scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Safety officer", Optional.empty()))), ANNA);
        var joinCode = exercises.createFrom(scenario);

        var exercise = crewJoining.findExercise(joinCode.toString()).orElseThrow();

        assertThat(exercise.state()).isEqualTo(ExerciseState.SETUP);
        assertThat(exercise.preparedLanguage()).isEqualTo(PreparedLanguage.FINNISH);
        assertThat(exercise.positions())
                .extracting(ExercisePosition::name, ExercisePosition::callSign)
                .containsExactly(
                        tuple("Officer", Optional.of("RVSP911")),
                        tuple("Pump operator", Optional.of("RVS911K")),
                        tuple("Safety officer", Optional.empty()));
    }

    @Test
    void theJoinCodeIsFoundHoweverItIsTyped() {
        var joinCode = exercises.createFrom(aScenario()).toString();

        assertThat(crewJoining.findExercise(joinCode.toLowerCase(Locale.ROOT))).isPresent();
        assertThat(crewJoining.findExercise(joinCode.replace("-", ""))).isPresent();
        assertThat(crewJoining.findExercise(" " + joinCode.replace("-", " ") + " ")).isPresent();
    }

    @Test
    void unknownAndMalformedCodesFindNothing() {
        exercises.createFrom(aScenario());

        assertThat(crewJoining.findExercise("ZZZZ-ZZZZ")).isEmpty();
        assertThat(crewJoining.findExercise("K7QX")).isEmpty();
        assertThat(crewJoining.findExercise("K7QX-M2P9-Z")).isEmpty();
        assertThat(crewJoining.findExercise("K7QX-M2PU")).isEmpty();
        assertThat(crewJoining.findExercise("")).isEmpty();
    }

    @Test
    void anEndedExerciseAdmitsNobody() {
        var joinCode = exercises.createFrom(aScenario());

        exercises.end(joinCode);

        assertThat(crewJoining.findExercise(joinCode.toString())).isEmpty();
    }

    @Test
    void aJoinCodeIsEightCodeCharactersInTwoGroups() {
        var joinCode = exercises.createFrom(aScenario());

        assertThat(joinCode.toString()).matches("[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}");
        assertThat(JoinCode.parse(joinCode.toString())).contains(joinCode);
    }

    @Test
    void aJoinCodeAlreadyInUseIsDrawnAgain(@Autowired DSLContext db, @Autowired ExerciseChanges changes) {
        var scenario = aScenario();
        var sameSeedEveryTime = new Exercises(db, changes, () -> new Random(347));

        var first = sameSeedEveryTime.createFrom(scenario);
        var second = sameSeedEveryTime.createFrom(scenario);

        assertThat(second).isNotEqualTo(first);
        assertThat(crewJoining.findExercise(first.toString())).isPresent();
        assertThat(crewJoining.findExercise(second.toString())).isPresent();
    }

    @Test
    void takingAFreePositionGivesATokenThatFindsItAgain() {
        var joinCode = exercises.createFrom(aScenario()).toString();
        var officer = crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst();

        var result = crewJoining.take(joinCode, officer.id());

        var token = assertThat(result).asInstanceOf(type(TakeResult.Taken.class)).actual().token();
        var holding = crewJoining.findHolding(token).orElseThrow();
        assertThat(holding.position().id()).isEqualTo(officer.id());
        assertThat(holding.position().name()).isEqualTo("Officer");
        assertThat(holding.state()).isEqualTo(ExerciseState.SETUP);
    }

    @Test
    void theExerciseShowsWhichPositionsAreTaken() {
        var joinCode = exercises.createFrom(twoPositions()).toString();
        var officer = crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst();

        crewJoining.take(joinCode, officer.id());

        assertThat(crewJoining.findExercise(joinCode).orElseThrow().positions())
                .extracting(ExercisePosition::name, ExercisePosition::taken)
                .containsExactly(tuple("Officer", true), tuple("Pump operator", false));
    }

    @Test
    void takingATakenPositionReportsItAndLeavesTheHolderInPlace() {
        var joinCode = exercises.createFrom(aScenario()).toString();
        var officer = crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst();
        var holder = ((TakeResult.Taken) crewJoining.take(joinCode, officer.id())).token();

        var result = crewJoining.take(joinCode, officer.id());

        assertThat(result).isInstanceOf(TakeResult.AlreadyTaken.class);
        assertThat(crewJoining.findHolding(holder)).isPresent();
    }

    @Test
    void crewMembersTakingOnePositionAtOnceLeaveExactlyOneHolderAndTheOthersAreOfferedATakeOver()
            throws Exception {
        var joinCode = exercises.createFrom(aScenario()).toString();
        var officer = crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst();
        var crewMembers = 8;
        var start = new CyclicBarrier(crewMembers);

        List<TakeResult> results;
        try (var executor = Executors.newFixedThreadPool(crewMembers)) {
            Callable<TakeResult> take = () -> {
                start.await();
                return crewJoining.take(joinCode, officer.id());
            };
            var takes = executor.invokeAll(Collections.nCopies(crewMembers, take));
            results = new ArrayList<>();
            for (var taking : takes) {
                results.add(taking.get());
            }
        }

        assertThat(results).filteredOn(TakeResult.Taken.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(TakeResult.AlreadyTaken.class::isInstance).hasSize(crewMembers - 1);
    }

    @Test
    void takingOverAHeldPositionGivesANewTokenAndThePreviousHolderNoLongerHoldsIt() {
        var joinCode = exercises.createFrom(aScenario()).toString();
        var officer = crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst();
        var previous = ((TakeResult.Taken) crewJoining.take(joinCode, officer.id())).token();

        var result = crewJoining.takeOver(joinCode, officer.id());

        var token = assertThat(result).asInstanceOf(type(TakeResult.Taken.class)).actual().token();
        assertThat(token).isNotEqualTo(previous);
        assertThat(crewJoining.findHolding(previous)).isEmpty();
        assertThat(crewJoining.findHolding(token)).get()
                .extracting(holding -> holding.position().id()).isEqualTo(officer.id());
        assertThat(crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst().taken()).isTrue();
    }

    @Test
    void changingPositionFreesItAndTheTokenNoLongerHoldsAnything() {
        var joinCode = exercises.createFrom(aScenario()).toString();
        var officer = crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst();
        var holder = ((TakeResult.Taken) crewJoining.take(joinCode, officer.id())).token();

        assertThat(crewJoining.changePosition(holder)).isTrue();

        assertThat(crewJoining.findHolding(holder)).isEmpty();
        assertThat(crewJoining.findExercise(joinCode).orElseThrow().positions().getFirst().taken()).isFalse();
    }

    @Test
    void onceTheExerciseHasEndedNoPositionChangesHandsButHoldersKeepTheirs() {
        var joinCode = exercises.createFrom(twoPositions());
        var positions = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions();
        var holder = ((TakeResult.Taken) crewJoining.take(joinCode.toString(), positions.get(0).id())).token();

        exercises.end(joinCode);

        assertThat(crewJoining.take(joinCode.toString(), positions.get(1).id()))
                .isInstanceOf(TakeResult.NotJoinable.class);
        assertThat(crewJoining.takeOver(joinCode.toString(), positions.get(0).id()))
                .isInstanceOf(TakeResult.NotJoinable.class);
        assertThat(crewJoining.changePosition(holder)).isFalse();
        assertThat(crewJoining.findHolding(holder)).get()
                .extracting(Holding::state).isEqualTo(ExerciseState.ENDED);
    }

    @Test
    void aPositionCanOnlyBeTakenWithTheCodeOfItsOwnExercise() {
        var ours = exercises.createFrom(aScenario()).toString();
        var theirs = exercises.createFrom(aScenario()).toString();
        var theirOfficer = crewJoining.findExercise(theirs).orElseThrow().positions().getFirst();

        assertThat(crewJoining.take(ours, theirOfficer.id())).isInstanceOf(TakeResult.NotJoinable.class);
        assertThat(crewJoining.take("ZZZZ-ZZZZ", theirOfficer.id())).isInstanceOf(TakeResult.NotJoinable.class);
        assertThat(crewJoining.takeOver(ours, theirOfficer.id())).isInstanceOf(TakeResult.NotJoinable.class);
        assertThat(crewJoining.findExercise(theirs).orElseThrow().positions().getFirst().taken()).isFalse();
    }

    @Test
    void followersOfAnExerciseHearOfEveryChangeToItAndOnlyToIt() {
        var joinCode = exercises.createFrom(twoPositions());
        var other = exercises.createFrom(aScenario());
        var positions = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions();
        var heard = new AtomicInteger();
        var heardOfOther = new AtomicInteger();
        crewJoining.subscribe(joinCode, heard::incrementAndGet);
        crewJoining.subscribe(other, heardOfOther::incrementAndGet);

        crewJoining.take(joinCode.toString(), positions.get(0).id());
        var holder = ((TakeResult.Taken) crewJoining.takeOver(joinCode.toString(), positions.get(0).id())).token();
        crewJoining.take(joinCode.toString(), positions.get(1).id());
        crewJoining.changePosition(holder);
        exercises.end(joinCode);

        assertThat(heard).hasValue(5);
        assertThat(heardOfOther).hasValue(0);
    }

    @Test
    void aFollowerThatFailsNeitherUndoesTheChangeNorKeepsItFromTheOthers() {
        var joinCode = exercises.createFrom(aScenario());
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        var heard = new AtomicInteger();
        crewJoining.subscribe(joinCode, () -> {
            throw new IllegalStateException("A browser that has gone away");
        });
        crewJoining.subscribe(joinCode, heard::incrementAndGet);

        var result = crewJoining.take(joinCode.toString(), officer.id());

        assertThat(result).isInstanceOf(TakeResult.Taken.class);
        assertThat(heard).hasValue(1);
    }

    @Test
    void aCancelledSubscriptionHearsNothingMore() {
        var joinCode = exercises.createFrom(aScenario());
        var officer = crewJoining.findExercise(joinCode.toString()).orElseThrow().positions().getFirst();
        var heard = new AtomicInteger();
        crewJoining.subscribe(joinCode, heard::incrementAndGet).cancel();

        crewJoining.take(joinCode.toString(), officer.id());

        assertThat(heard).hasValue(0);
    }

    private ScenarioId twoPositions() {
        return scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), ANNA);
    }

    private ScenarioId aScenario() {
        return scenarios.create(new ScenarioContent("Warehouse fire",
                PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA);
    }
}
