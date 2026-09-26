package net.pkhapps.roihu.scenario;

import net.pkhapps.roihu.IntegrationTest;
import net.pkhapps.roihu.exercise.CrewJoining;
import net.pkhapps.roihu.exercise.ExercisePosition;
import net.pkhapps.roihu.exercise.Exercises;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.pkhapps.roihu.TestOfficers.ANNA;
import static net.pkhapps.roihu.TestOfficers.BERTIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

        scenarios.save(id, versionOf(id), edited, BERTIL);

        var saved = scenarios.get(id).orElseThrow();
        assertThat(saved.content()).isEqualTo(edited);
        assertThat(saved.created()).isEqualTo(created);
        assertThat(saved.lastChanged().by()).isEqualTo(BERTIL);
        assertThat(saved.lastChanged().at()).isAfterOrEqualTo(created.at());
    }

    @Test
    void aSaveFromAVersionSomeoneHasSinceChangedIsRefusedNamingWhoChangedIt() {
        var id = scenarios.create(warehouseFire(), ANNA);
        var openedByAnna = scenarios.get(id).orElseThrow();
        var openedByBertil = scenarios.get(id).orElseThrow();
        var bertilsEdit = new ScenarioContent("Warehouse fire, revised", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911"))));
        assertThat(scenarios.save(id, openedByBertil.version(), bertilsEdit, BERTIL))
                .isInstanceOf(SaveResult.Saved.class);

        var result = scenarios.save(id, openedByAnna.version(), new ScenarioContent("Warehouse fire at night",
                PreparedLanguage.FINNISH, Optional.empty(), List.of()), ANNA);

        var current = scenarios.get(id).orElseThrow();
        assertThat(result).isEqualTo(new SaveResult.Conflict(current.lastChanged()));
        assertThat(current.lastChanged().by()).isEqualTo(BERTIL);
        assertThat(current.content()).isEqualTo(bertilsEdit);
    }

    @Test
    void ofOfficersSavingFromOneVersionAtOnceExactlyOneSavesAndTheOthersAreRefused() throws Exception {
        var id = scenarios.create(warehouseFire(), ANNA);
        var version = versionOf(id);
        var officers = 8;
        var start = new CyclicBarrier(officers);

        var results = new ArrayList<SaveResult>();
        try (var executor = Executors.newFixedThreadPool(officers)) {
            var saves = new ArrayList<Future<SaveResult>>();
            for (var officer = 0; officer < officers; officer++) {
                var content = new ScenarioContent("Warehouse fire " + officer, PreparedLanguage.FINNISH,
                        Optional.empty(), List.of(new ScenarioPosition("Officer " + officer, Optional.empty())));
                saves.add(executor.submit(() -> {
                    start.await();
                    return scenarios.save(id, version, content, BERTIL);
                }));
            }
            for (var save : saves) {
                results.add(save.get());
            }
        }

        assertThat(results).filteredOn(SaveResult.Saved.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(SaveResult.Conflict.class::isInstance).hasSize(officers - 1);
        var saved = scenarios.get(id).orElseThrow().content();
        assertThat(saved.positions()).extracting(ScenarioPosition::name)
                .containsExactly(saved.name().replace("Warehouse fire", "Officer"));
    }

    @Test
    void aDuplicateIsANewScenarioWithTheSameContentCreatedByTheDuplicatingOfficer() {
        var content = new ScenarioContent("Warehouse fire", PreparedLanguage.SWEDISH, Optional.of("Night shift"), List.of(
                new ScenarioPosition("Officer", Optional.of("RVSP911")),
                new ScenarioPosition("Pump operator", Optional.empty())));
        var original = scenarios.create(content, ANNA);

        var duplicate = scenarios.duplicate(original, BERTIL).orElseThrow();

        assertThat(duplicate).isNotEqualTo(original);
        var copy = scenarios.get(duplicate).orElseThrow();
        assertThat(copy.content()).isEqualTo(content);
        assertThat(copy.created().by()).isEqualTo(BERTIL);
        assertThat(copy.lastChanged().by()).isEqualTo(BERTIL);
    }

    @Test
    void changingADuplicateOrItsOriginalLeavesTheOtherAsItWas() {
        var content = warehouseFire();
        var original = scenarios.create(content, ANNA);
        var duplicate = scenarios.duplicate(original, BERTIL).orElseThrow();

        scenarios.save(original, versionOf(original), new ScenarioContent("Original", PreparedLanguage.FINNISH,
                Optional.empty(), List.of()), ANNA);
        scenarios.save(duplicate, versionOf(duplicate), new ScenarioContent("Duplicate", PreparedLanguage.ENGLISH,
                Optional.empty(), List.of(new ScenarioPosition("Observer", Optional.empty()))), BERTIL);

        assertThat(scenarios.get(original).orElseThrow().content()).isEqualTo(new ScenarioContent("Original",
                PreparedLanguage.FINNISH, Optional.empty(), List.of()));
        assertThat(scenarios.get(duplicate).orElseThrow().content()).isEqualTo(new ScenarioContent("Duplicate",
                PreparedLanguage.ENGLISH, Optional.empty(), List.of(new ScenarioPosition("Observer", Optional.empty()))));
    }

    @Test
    void aScenarioThatWasNeverRunIsDeletedFromTheLibrary() {
        var id = scenarios.create(warehouseFire(), ANNA);

        assertThat(scenarios.delete(id)).isInstanceOf(DeleteResult.Deleted.class);

        assertThat(scenarios.get(id)).isEmpty();
        assertThat(scenarios.list()).noneMatch(summary -> summary.id().equals(id));
    }

    @Test
    void aScenarioThatHasAnExerciseIsNotDeleted(@Autowired Exercises exercises) {
        var id = scenarios.create(warehouseFire(), ANNA);
        exercises.createFrom(id);

        assertThat(scenarios.delete(id)).isInstanceOf(DeleteResult.HasExercises.class);

        assertThat(scenarios.get(id)).get().extracting(Scenario::content).isEqualTo(warehouseFire());
    }

    @Test
    void aDeleteIsItsOwnTransactionSinceARefusalWouldLeaveAnEnclosingOneUnusable(
            @Autowired PlatformTransactionManager transactions) {
        var id = scenarios.create(warehouseFire(), ANNA);

        assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
                new TransactionTemplate(transactions).executeWithoutResult(status -> scenarios.delete(id)));
        assertThat(scenarios.get(id)).isPresent();
    }

    @Test
    void aScenarioAlreadyDeletedIsReportedGoneToWhoeverDeletesSavesOrDuplicatesItNext() {
        var id = scenarios.create(warehouseFire(), ANNA);
        var version = versionOf(id);
        scenarios.delete(id);

        assertThat(scenarios.delete(id)).isInstanceOf(DeleteResult.Gone.class);
        assertThat(scenarios.save(id, version, warehouseFire(), BERTIL)).isInstanceOf(SaveResult.Gone.class);
        assertThat(scenarios.duplicate(id, BERTIL)).isEmpty();
    }

    @Test
    void theLibraryListsEveryScenarioWithItsLanguagePositionCountAndLastChangeMostRecentlyChangedFirst() {
        var earlier = scenarios.create(warehouseFire(), ANNA);
        var later = scenarios.create(new ScenarioContent("Chimney fire", PreparedLanguage.SWEDISH, Optional.empty(),
                List.of()), ANNA);
        scenarios.save(earlier, versionOf(earlier), new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
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

        scenarios.save(id, versionOf(id), new ScenarioContent("Warehouse fire", PreparedLanguage.SWEDISH, Optional.empty(), List.of(
                new ScenarioPosition("Incident commander", Optional.of("RVS91")))), BERTIL);

        var exercise = crewJoining.findExercise(joinCode.toString()).orElseThrow();
        assertThat(exercise.preparedLanguage()).isEqualTo(PreparedLanguage.FINNISH);
        assertThat(exercise.positions())
                .extracting(ExercisePosition::name, ExercisePosition::callSign)
                .containsExactly(tuple("Officer", Optional.of("RVSP911")));
    }

    private int versionOf(ScenarioId id) {
        return scenarios.get(id).orElseThrow().version();
    }

    private static ScenarioContent warehouseFire() {
        return new ScenarioContent("Warehouse fire", PreparedLanguage.FINNISH, Optional.empty(),
                List.of(new ScenarioPosition("Officer", Optional.of("RVSP911"))));
    }
}
