package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
        exercises.end(endedFirst.joinCode());
        exercises.end(endedLast.joinCode());
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

    private static ScenarioContent warehouseFire() {
        return new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911"))));
    }
}
