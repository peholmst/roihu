-- Officers share one scenario library, so a scenario records who created it and who last changed
-- it, by the email the officer allowlist knows them by. The description is for other officers
-- and never reaches the crew.

alter table scenario
    add column description     text,
    -- Scenarios put in place before anyone was recorded were seeded for development.
    add column created_by      text        not null default 'development seeder',
    add column created_at      timestamptz not null default now(),
    add column last_changed_by text        not null default 'development seeder',
    add column last_changed_at timestamptz not null default now();

alter table scenario
    alter column created_by drop default,
    alter column last_changed_by drop default;
