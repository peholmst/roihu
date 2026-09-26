-- Two officers may edit one scenario at once. Each save names the version it was made from, and
-- a save from a version someone has since changed is refused rather than silently overwriting
-- their work.

alter table scenario add column version integer not null default 0;
