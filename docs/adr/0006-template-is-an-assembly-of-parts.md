# A Cabinet Template is an assembly of Parts held by Attachments

A Cabinet Template is no longer a set of numbers fed to one fixed carcass recipe in code: it is an assembly of Parts (panels, Fronts, mouldings) and hardware that Members draw in a 3D designer, each Part held by Attachments (a face fixed to a face of the Cabinet or another Part, at an offset) so it follows when a Cabinet is resized. Assembly runs in Java and TypeScript, kept in step by shared cases as the Construction Rules are (ADR 0002), and a template can only be published if, at its minimum, default and maximum size, every Part has a positive size, fits its sheet, overlaps nothing and touches another Part. We chose this so Shops can build what they actually sell, not only variations of one box, while ADR 0001 still holds: everything drawn is a Part of a real material, so the Cut List, Bill of Materials, drawings and Quote are still derived exactly.

## Considered Options

- **Construction options on the fixed recipe** (back or none, top or stretchers, legs or toe kick…): planned first for Milestone 5; cheaper, but every new kind of Cabinet waits for code, and it would be thrown away once free assembly exists.
- **Stretch zones** (each Part fixed or stretching along an axis): easy to draw, but cannot say "the shelf is the inside width".
- **Fixed-size templates**: simplest, but one template per size is useless for custom work.

## Consequences

- The fixed recipe stays, frozen, to build Cabinets still on version 1 of the starter templates (ADR 0005); the starter library is redrawn as version 2 assemblies, tested to produce the same Parts.
- Joinery is not modelled: Parts meet as butt joints where they are drawn.
