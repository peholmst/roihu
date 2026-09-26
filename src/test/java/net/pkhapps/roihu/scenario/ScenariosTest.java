package net.pkhapps.roihu.scenario;

import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.ExercisePosition;
import net.pkhapps.roihu.exercise.Exercises;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.roihu.TestOfficers.ANNA;
import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class ScenariosTest {

    @Autowired
    Scenarios scenarios;

    @Test
    void aCreatedScenarioIsFoundWithEverythingItWasGivenAndItsPositionsInOrder() {
        var content = new ScenarioContent("Warehouse fire", PreparedLanguage.SWEDISH,
                Optional.of("Night shift, one engine, water supply from a hydrant"), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Safety officer", Optional.empty())));

        var id = scenarios.create(content, ANNA);

        assertThat(scenarios.get(id)).get().extracting(Scenario::content).isEqualTo(content);
    }

    @Test
    void theOfficerWhoCreatesAScenarioIsRecordedAsItsCreatorAndLastChanger() {
        var before = Instant.now();

        var scenario = scenarios.get(scenarios.create(warehouseFire(), ANNA)).orElseThrow();

        assertThat(scenario.created().by()).isEqualTo(ANNA);
        assertThat(scenario.created().at()).isBetween(before.minusSeconds(5), Instant.now());
        assertThat(scenario.lastChanged()).isEqualTo(scenario.created());
    }

    @Test
    void savingReplacesTheWholeScenarioAndRecordsWhoChangedItButNotWhoCreatedIt() {
        var id = scenarios.create(new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH,
                Optional.of("First draft"), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Safety officer", Optional.empty()))), ANNA);
        var created = scenarios.get(id).orElseThrow().created();
        var edited = new ScenarioContent("Warehouse fire at night", PreparedLanguage.ENGLISH, Optional.empty(), List.of(
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")),
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Water supply", Optional.of("RVS903"))));

        scenarios.save(id, edited, BERTIL);

        var saved = scenarios.get(id).orElseThrow();
        assertThat(saved.content()).isEqualTo(edited);
        assertThat(saved.created()).isEqualTo(created);
        assertThat(saved.lastChanged().by()).isEqualTo(BERTIL);
        assertThat(saved.lastChanged().at()).isAfterOrEqualTo(created.at());
    }

    @Test
    void theLibraryListsEveryScenarioWithItsLanguagePositionCountAndLastChangeMostRecentlyChangedFirst() {
        var earlier = scenarios.create(warehouseFire(), ANNA);
        var later = scenarios.create(new ScenarioContent("Chimney fire", PreparedLanguage.SWEDISH, Optional.empty(),
                List.of()), ANNA);
        scenarios.save(earlier, new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.of("RVS911K")))), BERTIL);

        var library = scenarios.list().stream()
                .filter(summary -> summary.id().equals(earlier) || summary.id().equals(later))
                .toList();

        assertThat(library)
                .extracting(ScenarioSummary::name, ScenarioSummary::preparedLanguage, ScenarioSummary::positionCount,
                        summary -> summary.lastChanged().by())
                .containsExactly(
                        tuple("Warehouse fire", PreparedLanguage.FINNISH, 2, BERTIL),
                        tuple("Chimney fire", PreparedLanguage.SWEDISH, 0, ANNA));
        assertThat(library.getFirst().lastChanged()).isEqualTo(scenarios.get(earlier).orElseThrow().lastChanged());
    }

    @Test
    void namesAndCallSignsMayRepeatAndCallSignsMayBeMissingOrWrittenAnyWay() {
        var content = new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(), List.of(
                new ScenarioPosition("Smoke diver", Optional.of("RVS911")),
                new ScenarioPosition("Smoke diver", Optional.of("RVS911")),
                new ScenarioPosition("Observer", Optional.empty()),
                new ScenarioPosition("Kuljettaja", Optional.of("rvs 911 / k"))));

        var first = scenarios.create(content, ANNA);
        var second = scenarios.create(content, BERTIL);

        assertThat(scenarios.get(first)).get().extracting(Scenario::content).isEqualTo(content);
        assertThat(scenarios.get(second)).get().extracting(Scenario::content).isEqualTo(content);
    }

    @Test
    void aPositionHasANameThoughItMayLackACallSign() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ScenarioPosition("  ", Optional.of("RVS911")));
    }

    @Test
    void editingAScenarioLeavesItsExistingExercisesAsTheyWere(@Autowired Exercises exercises,
                                                              @Autowired CrewJoining crewJoining) {
        var id = scenarios.create(warehouseFire(), ANNA);
        var joinCode = exercises.createFrom(id);

        scenarios.save(id, new ScenarioContent("Warehouse fire", PreparedLanguage.SWEDISH, Optional.empty(), List.of(
                new ScenarioPosition("Incident commander", Optional.of("RVS91")))), BERTIL);

        var exercise = crewJoining.findExercise(joinCode.toString()).orElseThrow();
        assertThat(exercise.preparedLanguage()).isEqualTo(PreparedLanguage.FINNISH);
        assertThat(exercise.positions())
                .extracting(ExercisePosition::name, ExercisePosition::callSign)
                .containsExactly(tuple("Officer", Optional.of("RVSP911")));
    }

    private static ScenarioContent warehouseFire() {
        return new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911"))));
    }
}
