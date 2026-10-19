# The Design Assistant proposes, a Member accepts

The Design Assistant (an LLM, Claude, behind a model port) never edits a Project's design directly: it builds a separate Design Proposal by calling tools that are the editor's own operations, each checked by the Construction Rules, and a Member accepts it, which replays those steps onto the current design under the editing lock. Its tools reach only that one Project's design and the Shop's design data (Cabinet Templates, Construction Standard, material names); never prices, Quotes, other Projects or email. We chose this so ADR 0001 still holds (whatever it proposes can be built), so prices and Quotes stay deterministic and set by people, and so text in a forwarded email can at worst produce a bad Proposal a Member declines.

## Considered Options

- **Model writes the whole design as JSON**, validated afterwards: simpler, but fragile and able to describe geometry no rule covers.
- **Model edits the live design**, with undo as the safety net: faster to use, but a Member is no longer the author of what gets quoted, and a colleague's edits can be overwritten.
- **Proposal replaces the design wholesale on accept**: simpler than replaying, but silently discards edits made since the Proposal was started.

## Consequences

- Every editor action needs a matching tool; a test fails when one is missing, or the assistant will propose what it cannot build.
- The feature is off until an Owner opts the Shop in, because Client emails and photos are sent to the model provider (Quebec Law 25), and each Shop has a monthly budget.
