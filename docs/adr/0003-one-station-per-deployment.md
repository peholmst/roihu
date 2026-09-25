# One station per deployment

A deployment serves a single fire station or brigade. Every training officer there
shares one scenario library and may run anyone's scenario, and there is no
organisation or tenant concept anywhere in the model.

Multi-tenancy was considered and rejected: it would put a tenant filter in every
query, screen and test from day one, for a product aimed at small-scale tabletop
exercises within one station. Serving a second station means running a second
deployment.

## Consequences

Reversing this later means adding a tenant to every aggregate root and backfilling
existing data — cheap to design in now, expensive to retrofit. Treat the absence of
an organisation entity as deliberate rather than as an omission to be corrected.
