package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static net.pkhapps.roihu.TestExercises.startAndEnd;
import static net.pkhapps.roihu.TestOfficers.ANNA;
import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/** The training officer's side of exercises. The crew's side is in {@link CrewJoiningTest}. */
@IntegrationTest
class ExercisesTest {

    @Autowired
    Scenarios scenarios;

    @Autowired
    Exercises exercises;

    @Test
    void aNewExerciseIsInSetupWithItsScenariosNameAJoinCodeAndWhoCreatedIt() {
        var scenario = scenarios.create(new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH,
                Optional.empty(), List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")))), ANNA);
        var before = Instant.now();

        var created = assertThat(exercises.createFrom(scenario, ANNA))
                .asInstanceOf(type(CreateResult.Created.class)).actual();

        var exercise = exercises.get(created.id()).orElseThrow();
        assertThat(exercise.scenarioName()).isEqualTo("Warehouse fire");
        assertThat(exercise.state()).isEqualTo(ExerciseState.SETUP);
        assertThat(exercise.joinCode()).isEqualTo(created.joinCode());
        assertThat(exercise.created().by()).isEqualTo(ANNA);
        assertThat(exercise.created().at()).isBetween(before.minusSeconds(5), Instant.now());
    }

    @Test
    void aScenarioWithoutPositionsGivesNoExercise() {
        var scenario = scenarios.create(new ScenarioContent("Empty drill", PreparedLanguage.FINNISH,
                Optional.empty(), List.of()), ANNA);

        assertThat(exercises.createFrom(scenario, ANNA)).isInstanceOf(CreateResult.NoPositions.class);

        assertThat(exercises.list()).noneMatch(exercise -> exercise.scenarioName().equals("Empty drill"));
    }

    @Test
    void aDeletedScenarioGivesNoExercise() {
        var scenario = scenarios.create(warehouseFire(), ANNA);
        scenarios.delete(scenario);

        assertThat(exercises.createFrom(scenario, ANNA)).isInstanceOf(CreateResult.ScenarioGone.class);
    }

    @Test
    void anExerciseKeepsTheNameItsScenarioHadWhenItWasCreated() {
        var scenario = scenarios.create(warehouseFire(), ANNA);
        var created = (CreateResult.Created) exercises.createFrom(scenario, ANNA);

        var current = scenarios.get(scenario).orElseThrow();
        scenarios.save(scenario, current.version(), new ScenarioContent("Renamed", PreparedLanguage.FINNISH,
                Optional.empty(), current.content().positions()), ANNA);

        assertThat(exercises.get(created.id()).orElseThrow().scenarioName()).isEqualTo("Warehouse fire");
    }

    @Test
    void aCrewMemberJoinsAnExerciseCreatedThisWayWithItsCode(@Autowired CrewJoining crewJoining) {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);

        var joinable = crewJoining.findExercise(created.joinCode().toString()).orElseThrow();

        assertThat(joinable.positions()).extracting(ExercisePosition::name).containsExactly("Officer");
    }

    @Test
    void theListPutsExercisesNotYetEndedFirstThenTheEndedOnesMostRecentlyEndedFirst(
            @Autowired CrewJoining crewJoining) {
        var scenario = scenarios.create(new ScenarioContent("Listed fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911")),
                        new ScenarioPosition("Pump operator", Optional.empty()))), ANNA);
        var endedFirst = (CreateResult.Created) exercises.createFrom(scenario, ANNA);
        var inSetup = (CreateResult.Created) exercises.createFrom(scenario, BERTIL);
        var endedLast = (CreateResult.Created) exercises.createFrom(scenario, ANNA);
        startAndEnd(exercises, endedFirst);
        startAndEnd(exercises, endedLast);
        var officer = crewJoining.findExercise(inSetup.joinCode().toString()).orElseThrow().positions().getFirst();
        crewJoining.take(inSetup.joinCode().toString(), officer.id());

        var listed = exercises.list().stream()
                .filter(exercise -> List.of(endedFirst.id(), inSetup.id(), endedLast.id()).contains(exercise.id()))
                .toList();

        assertThat(listed).extracting(ExerciseSummary::id)
                .containsExactly(inSetup.id(), endedLast.id(), endedFirst.id());
        var setup = listed.getFirst();
        assertThat(setup.scenarioName()).isEqualTo("Listed fire");
        assertThat(setup.state()).isEqualTo(ExerciseState.SETUP);
        assertThat(setup.created().by()).isEqualTo(BERTIL);
        assertThat(setup.positionsTaken()).isEqualTo(1);
        assertThat(setup.positionCount()).isEqualTo(2);
        assertThat(setup.ended()).isEmpty();
        assertThat(listed.get(1).state()).isEqualTo(ExerciseState.ENDED);
        assertThat(listed.get(1).ended()).get().matches(ended -> !ended.isBefore(listed.get(2).ended().orElseThrow()));
    }

    @Test
    void startingMovesAnExerciseFromSetupToRunningWithEveryPositionFree() {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        var before = Instant.now();

        assertThat(exercises.start(created.id())).isInstanceOf(LifecycleResult.Done.class);

        var exercise = exercises.get(created.id()).orElseThrow();
        assertThat(exercise.state()).isEqualTo(ExerciseState.RUNNING);
        assertThat(exercise.started()).get().matches(started -> !started.isBefore(before.minusSeconds(5)));
        assertThat(exercise.ended()).isEmpty();
    }

    @Test
    void aRunningExerciseCannotBeStartedAgain() {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        exercises.start(created.id());
        var started = exercises.get(created.id()).orElseThrow().started();

        assertThat(exercises.start(created.id())).isEqualTo(new LifecycleResult.Refused(ExerciseState.RUNNING));

        assertThat(exercises.get(created.id()).orElseThrow().started()).isEqualTo(started);
    }

    @Test
    void endingMovesARunningExerciseToEndedForGood() {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        exercises.start(created.id());
        var before = Instant.now();

        assertThat(exercises.end(created.id())).isInstanceOf(LifecycleResult.Done.class);

        var exercise = exercises.get(created.id()).orElseThrow();
        assertThat(exercise.state()).isEqualTo(ExerciseState.ENDED);
        assertThat(exercise.ended()).get().matches(ended -> !ended.isBefore(before.minusSeconds(5)));
        assertThat(exercises.end(created.id())).isEqualTo(new LifecycleResult.Refused(ExerciseState.ENDED));
        assertThat(exercises.start(created.id())).isEqualTo(new LifecycleResult.Refused(ExerciseState.ENDED));
        assertThat(exercises.get(created.id()).orElseThrow().ended()).isEqualTo(exercise.ended());
    }

    @Test
    void anExerciseInSetupCannotBeEndedWithoutStarting() {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);

        assertThat(exercises.end(created.id())).isEqualTo(new LifecycleResult.Refused(ExerciseState.SETUP));

        assertThat(exercises.get(created.id()).orElseThrow().state()).isEqualTo(ExerciseState.SETUP);
    }

    @Test
    void anExerciseInSetupCanBeDeletedWithItsPositions(@Autowired CrewJoining crewJoining) {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        var officer = crewJoining.findExercise(created.joinCode().toString()).orElseThrow().positions().getFirst();
        var holder = ((TakeResult.Taken) crewJoining.take(created.joinCode().toString(), officer.id())).token();

        assertThat(exercises.delete(created.id())).isInstanceOf(LifecycleResult.Done.class);

        assertThat(exercises.get(created.id())).isEmpty();
        assertThat(crewJoining.findExercise(created.joinCode().toString())).isEmpty();
        assertThat(crewJoining.findHolding(holder)).isEmpty();
    }

    @Test
    void anExerciseThatHasStartedCannotBeDeleted() {
        var running = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        exercises.start(running.id());
        var ended = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        startAndEnd(exercises, ended);

        assertThat(exercises.delete(running.id())).isEqualTo(new LifecycleResult.Refused(ExerciseState.RUNNING));
        assertThat(exercises.delete(ended.id())).isEqualTo(new LifecycleResult.Refused(ExerciseState.ENDED));

        assertThat(exercises.get(running.id())).isPresent();
        assertThat(exercises.get(ended.id())).isPresent();
    }

    @Test
    void nothingCanBeDoneToADeletedExercise() {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        exercises.delete(created.id());

        assertThat(exercises.start(created.id())).isInstanceOf(LifecycleResult.Gone.class);
        assertThat(exercises.end(created.id())).isInstanceOf(LifecycleResult.Gone.class);
        assertThat(exercises.delete(created.id())).isInstanceOf(LifecycleResult.Gone.class);
    }

    @Test
    void ofOfficersStartingOrDeletingAnExerciseAtOnceExactlyOneGetsThrough() throws Exception {
        var created = (CreateResult.Created) exercises.createFrom(scenarios.create(warehouseFire(), ANNA), ANNA);
        List<Callable<LifecycleResult>> actions = List.of(
                () -> exercises.start(created.id()), () -> exercises.start(created.id()),
                () -> exercises.delete(created.id()), () -> exercises.delete(created.id()));
        var together = new CyclicBarrier(actions.size());

        var results = new ArrayList<LifecycleResult>();
        try (var executor = Executors.newFixedThreadPool(actions.size())) {
            for (var acting : executor.invokeAll(actions.stream().<Callable<LifecycleResult>>map(action -> () -> {
                together.await();
                return action.call();
            }).toList())) {
                results.add(acting.get());
            }
        }

        assertThat(results).filteredOn(LifecycleResult.Done.class::isInstance).hasSize(1);
        var started = results.subList(0, 2).contains(new LifecycleResult.Done());
        assertThat(results).filteredOn(result -> !(result instanceof LifecycleResult.Done)).allMatch(started
                ? new LifecycleResult.Refused(ExerciseState.RUNNING)::equals
                : new LifecycleResult.Gone()::equals);
    }

    private static ScenarioContent warehouseFire() {
        return new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911"))));
    }
}
