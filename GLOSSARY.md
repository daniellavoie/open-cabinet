# Open Cabinet

A 3D cabinet designer for cabinet Shops: design a job in 3D, then get the materials to build it and a quote to sell it.

## People

**Shop**:
A cabinet-making business, from a solo professional to a multi-person shop, that designs, prices and builds cabinets for its Clients.
_Avoid_: Builder, company, account, organisation

**Member**:
A person who works in a Shop and uses Open Cabinet on its behalf.
_Avoid_: User, employee, builder

**Owner**:
A Member who manages the Shop: its Members, Construction Standard, Material Catalog, prices, Labour Methods, taxes and currency.
_Avoid_: Admin, manager, boss

**Designer**:
A Member who creates Projects and Quotes using the Shop's settings, without changing prices or standards.
_Avoid_: Salesperson, drafter, user

**Client**:
The person a Shop designs and builds cabinets for, typically a homeowner.
_Avoid_: Customer, end user, homeowner

## Design

**Project**:
One job for a Client: a Room containing all the Cabinets to be designed, quoted and built together.
_Avoid_: Design, job, order

**Room**:
The space a Project's Cabinets are placed in, made of Walls, Openings and Appliances.
_Avoid_: Floor plan, space, layout

**Wall**:
A straight wall segment of a Room, with a length, height and angle to the next Wall.
_Avoid_: Side, partition

**Opening**:
A window or doorway in a Wall that Cabinets must avoid.
_Avoid_: Gap, hole, cutout

**Appliance**:
A placeholder for a fridge, range, dishwasher, sink or similar, which Cabinets are fitted around but which the Shop does not build.
_Avoid_: Fixture, equipment

**Cabinet**:
A single box-like unit within a Project, such as a base, wall or tall cabinet, made of Parts.
_Avoid_: Unit, box, module

**Cabinet Template**:
A reusable starting point for a type of Cabinet, such as a sink base or oven tower. Open Cabinet provides a starter library; each Shop can save its own.
_Avoid_: Preset, model, cabinet type, catalog item

**Part**:
One physical piece of a Cabinet, such as a side, bottom, shelf, door or drawer front, made of a real material.
_Avoid_: Panel, board, piece, component

**Front**:
A door or drawer front. Each Front is either made by the Shop (it appears on the Cut List) or ordered from a supplier (it appears on the Bill of Materials).
_Avoid_: Door, facade, face

**Trim**:
Parts that finish a Project rather than form a Cabinet: fillers, finished end panels, toe kicks and crown moulding.
_Avoid_: Accessories, finishing, mouldings

**Construction Standard**:
A Shop's chosen way of building cabinets: construction method, material thicknesses, joinery, edge banding and gaps.
_Avoid_: Shop settings, build preferences

**Construction Rules**:
The constraints a Construction Standard places on every Part, so that whatever is designed can actually be built.
_Avoid_: Validation, constraints

**Frameless**:
A construction method where doors mount directly on the cabinet box with no face frame, using the 32 mm hole system.
_Avoid_: European, Euro-style, full-access

## Pricing

**Material Catalog**:
A Shop's list of the sheet goods, edge banding and hardware it uses, with prices and sheet sizes.
_Avoid_: Price list, inventory, materials database

**Labour Method**:
How labour is priced on a Project: by operation (cuts, edge banding, assembly, hardware), by linear foot, or by estimated hours. Each Shop has a default; each Project can use another.
_Avoid_: Labour model, pricing mode

**Manual Line**:
A Quote line a Member enters by hand for work Open Cabinet does not model, such as countertops, installation or delivery.
_Avoid_: Extra, custom item, add-on

## Output

**Bill of Materials**:
Everything that must be bought to build a Project: sheet goods, edge banding and hardware, with quantities.
_Avoid_: BOM, materials list, shopping list

**Cut List**:
Every panel to be cut for a Project, with its dimensions, material and grain direction.
_Avoid_: Parts list, panel list

**Nesting**:
Laying out a Project's panels on full sheets of material to minimise waste and work out how many sheets to buy.
_Avoid_: Sheet optimisation, layout

**Quote**:
The price a Shop offers a Client for building a Project. A Quote is frozen once sent: later changes to the design or prices never alter it.
_Avoid_: Estimate, bid, price

**Quote Revision**:
A new, numbered version of a Quote, issued when a sent Quote needs to change.
_Avoid_: Update, amendment, new quote

**Elevation**:
A straight-on drawing of one Wall showing its Cabinets, Fronts and overall dimensions.
_Avoid_: Wall view, front view

**Shop Drawings**:
The drawings a Shop uses to build and install a Project: plan, Elevations and per-Cabinet details.
_Avoid_: Plans, blueprints, technical drawings
