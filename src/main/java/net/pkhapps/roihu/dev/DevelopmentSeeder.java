package net.pkhapps.roihu.dev;

import net.pkhapps.roihu.base.security.Officer;
import net.pkhapps.roihu.exercise.Exercises;
import net.pkhapps.roihu.exercise.JoinCode;
import net.pkhapps.roihu.scenario.PreparedLanguage;
import net.pkhapps.roihu.scenario.ScenarioContent;
import net.pkhapps.roihu.scenario.ScenarioPosition;
import net.pkhapps.roihu.scenario.Scenarios;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static net.pkhapps.roihu.db.generated.Tables.EXERCISE;
import static net.pkhapps.roihu.db.generated.Tables.SCENARIO;

/**
 * Puts an exercise in place for local development, until training officers can create one
 * themselves, and logs how to join it. Reuses the exercise across restarts so that the logged
 * link stays valid. Never active in production, which runs under the {@code prod} profile.
 */
@Component
@Profile("dev")
class DevelopmentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentSeeder.class);
    private static final String SCENARIO_NAME = "RVS911 and RVS903 (seeded for development)";
    /** Who the library says created the seeded scenario. Matches what the schema backfilled. */
    private static final Officer SEEDER = new Officer("development seeder");

    private final DSLContext db;
    private final Scenarios scenarios;
    private final Exercises exercises;
    private final int port;

    DevelopmentSeeder(DSLContext db, Scenarios scenarios, Exercises exercises,
                      @Value("${server.port:8080}") int port) {
        this.db = db;
        this.scenarios = scenarios;
        this.exercises = exercises;
        this.port = port;
    }

    @Override
    public void run(ApplicationArguments args) {
        var joinCode = seededExercise().orElseGet(() -> exercises.createFrom(scenarios.create(new ScenarioContent(
                SCENARIO_NAME, PreparedLanguage.FINNISH, Optional.empty(), List.of(
                        new ScenarioPosition("Yksikönjohtaja", Optional.of("RVSP911")),
                        new ScenarioPosition("Kuljettaja", Optional.of("RVS911K")),
                        new ScenarioPosition("Savusukeltaja 1", Optional.of("RVS911S1")),
                        new ScenarioPosition("Savusukeltaja 2", Optional.of("RVS911S2")),
                        new ScenarioPosition("Savusukeltaja 3", Optional.of("RVS911S3")),
                        new ScenarioPosition("Savusukeltaja 4", Optional.of("RVS911S4")),
                        new ScenarioPosition("Säiliöauton kuljettaja", Optional.of("RVS903")))), SEEDER)));
        log.info("Seeded exercise: join code {}, join link http://localhost:{}/join/{}", joinCode, port, joinCode);
    }

    private Optional<JoinCode> seededExercise() {
        return db.select(EXERCISE.JOIN_CODE)
                .from(EXERCISE).join(SCENARIO).on(SCENARIO.ID.eq(EXERCISE.SCENARIO_ID))
                .where(SCENARIO.NAME.eq(SCENARIO_NAME))
                .and(EXERCISE.STATE.ne(net.pkhapps.roihu.db.generated.enums.ExerciseState.ended))
                .limit(1)
                .fetchOptional(EXERCISE.JOIN_CODE)
                .flatMap(JoinCode::parse);
    }
}
