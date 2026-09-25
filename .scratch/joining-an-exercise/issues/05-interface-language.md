# Interface language

Status: ready-for-agent

## Parent

[PRD](../PRD.md)

## What to build

The interface is available in Finnish, Swedish and English. The default is the browser's preferred language if it is one of the three, and otherwise a deployment default set by a new configuration property (Finnish unless configured). A compact fi/sv/en switcher on the join screen and the position screen changes the language, and the choice is kept in a cookie. Every string built so far is translated into all three languages. Position names, call signs and other scenario content are never translated (ADR-0006). The same mechanism serves officers once their screens exist.

## Acceptance criteria

- [ ] The browser's preferred language is used when it is Finnish, Swedish or English
- [ ] Otherwise the configurable deployment default is used, and it defaults to Finnish
- [ ] The switcher appears on the join screen and the position screen
- [ ] The choice survives a reload
- [ ] All existing strings have Finnish, Swedish and English translations
- [ ] Position names and call signs are shown exactly as written in every interface language
- [ ] View tests cover the defaults, the switcher and the remembered choice

## Blocked by

- [Take a free position and keep it](02-take-a-free-position-and-keep-it.md)
