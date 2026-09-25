-- A crew member holds a position through a token their browser keeps. A position has at most
-- one holder; only a hash of the token is stored, so the table alone lets nobody take a
-- position over from someone else.

create table holding (
    exercise_position_id uuid primary key references exercise_position (id) on delete cascade,
    token_hash           bytea not null unique
);
