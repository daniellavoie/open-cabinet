# Construction Rules run in the browser and on the server

The Construction Rules are implemented twice: in the Angular front end, so Designers get instant feedback while editing in 3D, and in the Spring Boot back end, which is the authority and re-checks every saved edit. Everything that is priced or printed (Cut List, Bill of Materials, Nesting and Quote calculation) is computed only on the server. The two rule implementations are kept in step by one shared set of language-neutral test cases (an edit plus its expected outcome) that both test suites must pass.

## Considered Options

- **Server computes everything**: one implementation, but a network round-trip on every edit makes 3D editing feel sluggish.
- **Construction Rules duplicated in the browser** (chosen): instant feedback where it matters, at the cost of two rule implementations in two languages.
- **Rules expressed as data, interpreted on both sides**: one rule definition, but it requires designing a rule language before the rules are known.

## Consequences

- A rule change is not done until it is made in both implementations and covered by the shared test cases.
- When the two disagree, the server wins: a saved edit is corrected to the server's result.
