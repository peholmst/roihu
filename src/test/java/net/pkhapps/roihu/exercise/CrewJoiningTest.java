package net.pkhapps.roihu.exercise;

import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioId;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

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
        var scenario = scenarios.create("Warehouse fire", PreparedLanguage.FINNISH, List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Safety officer", Optional.empty())));
        var joinCode = exercises.createFrom(scenario);

        var exercise = crewJoining.findExercise(joinCode.toString()).orElseThrow();

        assertThat(exercise.state()).isEqualTo(ExerciseState.SETUP);
        assertThat(exercise.preparedLanguage()).isEqualTo(PreparedLanguage.FINNISH);
        assertThat(exercise.positions()).containsExactly(
                new ExercisePosition("Officer", Optional.of("RVSP911")),
                new ExercisePosition("Pump operator", Optional.of("RVS911K")),
                new ExercisePosition("Safety officer", Optional.empty()));
    }

    @Test
    void theJoinCodeIsFoundHoweverItIsTyped() {
        var joinCode = exercises.createFrom(aScenario()).toString();

        assertThat(crewJoining.findExercise(joinCode.toLowerCase())).isPresent();
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
    void aJoinCodeAlreadyInUseIsDrawnAgain(@Autowired DSLContext db) {
        var scenario = aScenario();
        var sameSeedEveryTime = new Exercises(db, () -> new Random(347));

        var first = sameSeedEveryTime.createFrom(scenario);
        var second = sameSeedEveryTime.createFrom(scenario);

        assertThat(second).isNotEqualTo(first);
        assertThat(crewJoining.findExercise(first.toString())).isPresent();
        assertThat(crewJoining.findExercise(second.toString())).isPresent();
    }

    private ScenarioId aScenario() {
        return scenarios.create("Warehouse fire", PreparedLanguage.FINNISH,
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911"))));
    }
}
