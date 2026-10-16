# The server computes all domain rules

All domain logic (Construction Rules, Cut List, Bill of Materials, Nesting and Quote calculation) lives only in the Spring Boot back end; the Angular front end renders the design and sends edits, but never applies the rules itself. We chose a single implementation over duplicating the Construction Rules in the browser for instant feedback, accepting a server round-trip per edit, so that what a Designer sees can never disagree with what is quoted.

## Considered Options

- **Server computes everything** (chosen): one implementation and one source of truth; every edit costs a network round-trip.
- **Construction Rules duplicated in the browser**: instant feedback while dragging, but two implementations in two languages that must be kept in step by a shared test suite.
- **Rules expressed as data, interpreted on both sides**: one rule definition, but it requires designing a rule language before the rules are known.

## Consequences

- 3D editing must stay usable with server latency, which shapes how dragging and snapping work in the UI.
- Revisit if editing feels sluggish in practice; the browser-side duplication option is the fallback.
