# Interface language

Status: done

## Parent

[PRD](../PRD.md)

## What to build

The interface is available in Finnish, Swedish and English. The default is the browser's preferred language if it is one of the three, and otherwise a deployment default set by a new configuration property (Finnish unless configured). A compact fi/sv/en switcher on the join screen and the position screen changes the language, and the choice is kept in a cookie. Every string built so far is translated into all three languages. Position names, call signs and other scenario content are never translated (ADR-0006). The same mechanism serves officers once their screens exist.

## Acceptance criteria

- [x] The browser's preferred language is used when it is Finnish, Swedish or English
- [x] Otherwise the configurable deployment default is used, and it defaults to Finnish
- [x] The switcher appears on the join screen and the position screen
- [x] The choice survives a reload
- [x] All existing strings have Finnish, Swedish and English translations
- [x] Position names and call signs are shown exactly as written in every interface language
- [x] View tests cover the defaults, the switcher and the remembered choice

## Blocked by

- [Take a free position and keep it](02-take-a-free-position-and-keep-it.md)

## Comments

Done in 6c62908. Found during implementation and review:

- The language is chosen when a browser window opens: the `roihu-language` cookie, then the
  browser's preference, then `roihu.default-language`. The switcher and the choosing live in
  `base/i18n`, ready for the officer's screens.
- Translation choices to confirm with the crews: position is *tehtävä* / *befattning*, inject
  is *syöte* / *inspel*.
- A browserless user's request is shared by its windows, so the tests set the browser's
  language and cookies through a window opened and closed before the one under test.
- Checked by hand: an English browser opens in English, and choosing Finnish survives a reload.
- Not covered by tests: a `roihu.default-language` other than Finnish.
- Follow-up: `<html lang>` stays `en` whatever the interface speaks, so screen readers read
  Finnish and Swedish with an English voice.
- Codex review found nothing to change.
