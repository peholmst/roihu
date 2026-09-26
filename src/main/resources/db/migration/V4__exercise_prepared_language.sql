-- An exercise keeps the prepared language its scenario had when the exercise was created, as it
-- keeps the positions (ADR-0002): editing the scenario must not change what an existing
-- exercise tells its crew.

alter table exercise add column prepared_language prepared_language;

update exercise
set prepared_language = scenario.prepared_language
from scenario
where scenario.id = exercise.scenario_id;

alter table exercise alter column prepared_language set not null;
