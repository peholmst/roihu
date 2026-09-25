# The UI is translated, inject content is not

The application's own text is translated into Finnish, Swedish and English, and each
viewer chooses which they see. The injects an officer writes are not translated at
all: they exist in the one language they were typed in, and a crew member reads them
in that language whatever their interface is set to.

Translating content was the alternative, and it fails on who would do the work. There
is no automatic translation in the loop, so a translated inject means an officer
writing each one two or three times — including the ones improvised mid-exercise,
under time pressure, while running the simulation by hand. An officer who genuinely
needs a scenario in two languages prepares it twice, as two scenarios.

## Consequences

An officer can be reading a Finnish inject through a Swedish interface, and that is
the intended behaviour rather than a defect. A scenario carries its prepared language
as a hint so the mismatch is visible before anyone joins, but the hint describes the
prepared material only — an improvised inject may be in any language, which is why
the hint is not a guarantee.

Every user-facing string in the application must be externalised for translation;
inject text, labels and images must never be.
