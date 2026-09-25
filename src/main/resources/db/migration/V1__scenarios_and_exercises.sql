-- A scenario is the reusable preparation; an exercise is one run of it. The exercise owns a copy
-- of the scenario's positions rather than referencing them (ADR-0002), so that editing the
-- scenario never changes an exercise that already exists.

create type prepared_language as enum ('fi', 'sv', 'en');

create table scenario (
    id                uuid primary key default gen_random_uuid(),
    name              text              not null,
    prepared_language prepared_language not null
);

create table scenario_position (
    scenario_id uuid    not null references scenario (id) on delete cascade,
    ordinal     integer not null,
    name        text    not null,
    call_sign   text,
    primary key (scenario_id, ordinal)
);

create type exercise_state as enum ('setup', 'running', 'ended');

create table exercise (
    id          uuid primary key default gen_random_uuid(),
    -- Restricting: a scenario that has been run cannot be deleted.
    scenario_id uuid           not null references scenario (id),
    state       exercise_state not null default 'setup',
    join_code   char(8)        not null unique
);

create table exercise_position (
    id          uuid primary key default gen_random_uuid(),
    exercise_id uuid    not null references exercise (id) on delete cascade,
    ordinal     integer not null,
    name        text    not null,
    call_sign   text,
    unique (exercise_id, ordinal)
);
