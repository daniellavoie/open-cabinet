# Every piece of geometry is a buildable Part

Members design in free 3D, but everything they can draw is a Part made of a real material from the Material Catalog and governed by the Shop's Construction Rules; there is no free-form geometry. We chose this over pure free-form modelling (SketchUp-style) because the Cut List, Bill of Materials, Nesting and Quote must be derived automatically and reliably from the design, which is impossible if the app has to guess what an arbitrary shape is.

## Considered Options

- **Pure parametric** (pick a cabinet type, set dimensions): reliable outputs, but too rigid for the custom work Shops actually sell.
- **Pure free-form 3D**: maximum flexibility, but outputs become guesses and can describe things that cannot be built.
- **Free 3D editing of constrained Parts** (chosen): the flexibility of direct manipulation with the guarantee that every design can be built and priced.
