-- An officer creates an exercise from a scenario and runs it. The exercise records who created it
-- and when it started and ended, and keeps the name its scenario had, as it keeps the positions
-- and the prepared language (ADR-0002): editing the scenario changes no existing exercise.

alter table exercise
    add column scenario_name text,
    -- Exercises created before anyone was recorded were seeded for development.
    add column created_by    text        not null default 'development seeder',
    add column created_at    timestamptz not null default now(),
    add column started_at    timestamptz,
    add column ended_at      timestamptz;

update exercise
set scenario_name = scenario.name
from scenario
where scenario.id = exercise.scenario_id;

-- When earlier exercises started or ended was never recorded; their creation is the best guess.
update exercise set started_at = created_at where state in ('running', 'ended');
update exercise set ended_at = created_at where state = 'ended';

alter table exercise
    alter column scenario_name set not null,
    alter column created_by drop default;
