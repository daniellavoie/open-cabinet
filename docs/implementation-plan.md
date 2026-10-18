# Implementation plan

How Open Cabinet gets from today to v1, in milestones. Each milestone ships to production on its own. Terms follow [`GLOSSARY.md`](../GLOSSARY.md); decisions are in [`docs/adr/`](adr).

## Current step

**Milestones 0–3 are in production** at `https://cabinet.arendal.ca`. The production environment, the `open-cabinet` Keycloak realm and its `open-cabinet-web` client are set up, and every merge into `main` deploys. Checked after the first deploy: pods and database healthy, DNS and TLS in place, the app served under `/fr/` and `/en/`, the API refusing unsigned requests. To finish:

1. Smoke-test signed in: create an account in the `open-cabinet` realm, sign in, create a Shop, design a Project, download a Quote PDF.
2. Milestone 4 is under way: Template Versions and Shop Drawings are done; the Quote PDF with pictures is next.

## Status

| Milestone | Status |
|---|---|
| 0. Foundation and delivery | Done (PR #1), in production |
| 1. First end-to-end slice | Done (PR #2), in production |
| 2. Shop-ready | Done (PR #3), in production |
| 3. Design depth | Done (PR #4), in production |
| 4. Outputs for the shop floor and the Client | In progress |
| 5. Template Designer | Planned |
| 6. Design Assistant | Planned |
| 7. After v1 | Not scheduled |

## 0. Foundation and delivery: done

- Spring Boot + Angular skeleton, Keycloak sign-in (identity only, ADR 0003), PostgreSQL with Flyway.
- Helm chart for the arendal cluster; CI on every pull request; every push to `main` tested, published with the commit SHA and deployed.
- Glossary, ADRs 0001–0003, `java-backend` and `angular-frontend` skills.

## 1. First end-to-end slice: done

Proves the core pipeline, design → Cut List → Nesting → Quote.

- Shop onboarding (creator becomes Owner), Clients, Material Catalog seeded with Quebec Frameless defaults, Construction Standard.
- Projects: a Room of straight Walls with base and wall Cabinets from the starter Cabinet Templates, edited in 3D (select, drag along a Wall with snapping) and in a side panel.
- Construction Rules in Java and TypeScript, sharing `construction-rules/cases.json` (ADR 0002).
- Cut List with edge banding per edge, rule-based hardware, guillotine Nesting, Bill of Materials.
- Quotes: three Labour Methods, markup, Manual Lines, Shop taxes; frozen when sent; Client PDF in French.

## 2. Shop-ready: done

What a real Shop needs before using it every day.

1. **Member invitations.** Done. An Owner creates an Invitation link (optional email, role, valid 14 days, shown once); the invitee signs in and joins with that role. Owners manage roles and remove Members; a Shop keeps at least one Owner. Links are passed on by hand: sending them by email needs a mail provider (follow-up).
2. **One editor at a time.** Done. Opening the editor takes a two-minute lock, renewed while open; others see the Project read-only with the holder's name.
3. **Quote Revisions.** Done. A sent Quote is revised into S-0001-R1, R2…; sending a revision supersedes the earlier one.
4. **Material Catalog CSV import.** Done, with export. Updates by SKU, adds the rest, all or nothing with line errors; reads French-locale spreadsheet files.
5. **English.** Done. Built per language under `/fr/` and `/en/`, language switch in the top bar, missing translations fail the build.
6. **Settings polish.** Done. After a settings or catalog change, Owners see the draft Quotes still carrying old prices.

Follow-ups found while building it:
- Server error messages (e.g. CSV line errors) and the Quote PDF are only in English and French respectively; localize both by the Member's and the Client's language.
- Email delivery for Invitations.
- The Keycloak login page follows the realm's default language, not the app's.

## 3. Design depth: done

The design freedom ADR 0001 allows: free 3D editing, constrained to buildable Parts.

1. **More starter templates.** Done. Fronts are described as rows (door, drawer, false front, appliance opening); new tall pantry, oven tower, blind corner, sink with false front and drawer bank templates.
2. **Openings and Appliances.** Done. Windows and doorways, fridge, range, dishwasher and hood placeholders, placed and dragged like Cabinets. Cabinets must not block Openings or overlap Appliances; new Cabinets are placed around them. The sink stays a Cabinet Template (sink base).
3. **Construction Rules at corners.** Done. Plan-view checks across Walls: Cabinets must not run into what stands on the neighbouring Wall, nor have it against their Fronts, except over a blind corner's blind part.
4. **Part-level editing in 3D.** Done for dividers: Cabinets with door-only Fronts take dividers, set in the panel or dragged in a see-through 3D view; each section gets its own shelves; sections are at least 6" wide.
5. **Fronts: made or ordered.** Done. Per Cabinet; ordered Fronts come in a Front style (new catalog kind, priced per m²), leave the Cut List and are listed for ordering and bought on the Bill of Materials by area.
6. **Trim.** Done. Fillers on Walls (Front material), finished end panels per Cabinet side, toe kicks per run of floor Cabinets, crown moulding per run of wall and tall Cabinets (new catalog kind, priced per metre).
7. **Shop Cabinet Templates.** Done. Owners save a designed Cabinet as a named preset of its starter template; Cabinets keep the starter code, so rules and Parts are unchanged.

Follow-ups found while building it:
- Shelves are spaced evenly; moving single shelves and resizing other Parts in 3D is not built.
- Fillers are always made in the Shop and are not checked across Walls; ordered Fronts have one price per m² per style, no size tiers.
- Toe kicks are cut from the carcass material; a Shop may want its own toe kick material in the Construction Standard.

## 4. Outputs for the shop floor and the Client: in progress

0. **Template Versions** (first, ahead of the outputs; ADR 0005). Done. A Cabinet used to keep only its template's code, so editing `cabinet-templates.json` silently changed every old Project. Each entry of `cabinet-templates.json` is now one Template Version, only ever added, never edited: `cabinet-templates.published` lists each with a hash of its content, and `CabinetTemplateVersionsTest` fails when a published one changes (one file rather than a file per version, because both test suites read it directly). Every Cabinet records its template and version; existing Cabinets are migrated to version 1. Outdated Cabinets get a badge in 3D and in the side panel, and the Project a notice ("3 Cabinets can be updated") with Update all and each version's change note. Updating keeps the Cabinet's own values (size, shelves, dividers, Fronts, finished ends, crown, blind side) and is refused, showing the broken rules, when they don't fit the new version; Revert goes back to any published version. Sent Quotes are untouched; draft Quotes get the existing "priced before a change" notice.
1. **Shop Drawings**: done. A PDF from the Project editor (saved Project, in the Member's language and the Shop's units): a plan view with Walls, Openings, Appliances and every Cabinet labelled (wall Cabinets dashed); an Elevation per Wall with Fronts, widths chained along the floor and over the wall Cabinets, the Wall's height and the wall Cabinets' height from the floor; a sheet per Cabinet with its front (height of each front row), its side and its own Parts. Each sheet at the largest standard scale that fits (architectural for imperial Shops), with a title block.
2. **Quote PDF with pictures**: a 3D render and Elevations alongside the price.
3. **Printable and exportable outputs**: Cut List, Nesting layouts and Bill of Materials as PDF and CSV.
4. **Better Nesting**: fill partly used sheets before opening new ones, compare layouts, keep grain and saw constraints.

Follow-ups found while building it:
- An Elevation shows only its own Wall's Cabinets: a corner Cabinet from the neighbouring Wall is not drawn in section, and dimensions of very narrow items (a 1" filler) crowd their text.
- Shelves and drawer boxes are not drawn in the Cabinet sheets; the shelf count is in the notes.

## 5. Template Designer: planned

Members design their own Cabinet Templates in a separate 3D designer, assembling Parts and hardware from the Shop's catalog, and the starter library is redrawn the same way (ADR 0006). Templates stay versioned (ADR 0005), so Projects never change under them; what a Project places is still a Cabinet.

What a template is:
- An assembly of **Parts** (panels, Fronts, mouldings) and **hardware**, held by **Attachments**: a Part's face fixed to a face of the Cabinet or of another Part, with an offset that is a number, a Construction Standard value (toe kick height, door gap) or a material thickness. Resizing a Cabinet moves every Part with it.
- Materials are Construction Standard roles by default (carcass, front, the Shop's hinge…), a specific catalog material when chosen. Each panel edge picks its banding and each panel its grain, with defaults (front edges get the front banding, grain vertical).
- Each Part has a role (side, shelf, door… or "other" with a typed name), which labels it on the Cut List and drawings.
- **Front stacks** size Fronts like today's front rows (fixed heights and shares, the Standard's door gap). **Repeated** Parts (shelves) take their count per Cabinet; a Front can split into two leaves above a width; a template's divider Part is placed per Cabinet.
- Hardware sits on a Part, counted fixed or by length (one hinge per 20" of door, at least 2).
- Butt joints only. Templates keep their type (base, wall, tall) and size ranges.
- Template rules, live in the designer and enforced on publish at the minimum, default and maximum size: every Part has a positive size, nothing overlaps, every Part fits its sheet, every Part touches another. Assembly runs in Java and TypeScript, kept in step by a shared `template-assembly/cases.json`.

1. **The assembly model.** Parts, Attachments, Front stacks, repetition, hardware; assembly in both languages with shared cases; the Cut List, Bill of Materials, Shop Drawings and the 3D view built from an assembled template's real Parts; the template rules. Templates written as JSON, no designer yet.
2. **The designer, for Project templates.** A separate screen opened from the Project editor: a palette of the Shop's materials and Standard roles, a 3D view where snapping creates Attachments, a properties panel (Attachments, offsets, banding, grain, repetition, hardware), a minimum / default / maximum preview and the rules listed live. Start from an existing template or from a Cabinet. One draft and published Template Versions with change notes; a template belongs to its Project until shared; on first publish the editor offers to switch the original Cabinet to it. Names are typed once, not translated, unique within the Project and the Shop library.
3. **Starter library, version 2.** Every starter template redrawn as an assembly, with tests showing it produces the same Parts as version 1 at sampled sizes. The version 1 recipe (`PartGenerator`, `FrontLayout` and their TypeScript side) stays, frozen, for Cabinets still on version 1.
4. **Shop library and Mirror.** Sharing moves a Project template with its history into the Shop library; it can't be un-shared. Owners and Designers create, publish and share templates. Templates are archived, never deleted, once a version is in use. Milestone 3's Shop Cabinet Template presets become Shop templates and the preset feature goes away. Blind side becomes **Mirror**, for any Cabinet.

In Projects, made or ordered Fronts applies to Parts marked as Fronts; finished ends and crown moulding stay Trim.

Follow-ups: joinery (dados, rabbets, dowels), which changes Part sizes and matters most for CNC export; moving version 1 Cabinets to version 2 without a Member, which would need its own decision against ADR 0005.

## 6. Design Assistant: planned

Members describe changes in a Conversation, or forward a Client's email thread, and the Design Assistant answers with a Design Proposal they review in 3D and accept or discard (ADR 0004). Built with Spring AI on Claude: Sonnet builds Proposals, Haiku sorts the Inbox, both set in configuration.

Ground rules for every step:
- Off until an Owner opts the Shop in; Settings say what is sent to the model provider and to Resend (which keeps received mail 30 days). A monthly budget per Shop, in dollars, stops the assistant clearly when spent.
- Owners and Designers use it. It reads the current design, published Cabinet Templates (starter, Shop and the Project's own), the Construction Standard, material and Front style names and the Client's name; never prices, Quotes or other Projects. It places Cabinets from published templates but never creates, publishes or updates templates, nor moves Cabinets to a newer Template Version.
- It answers in the Member's language. Email content is untrusted: it is quoted to the model, never followed as instructions.

1. **Foundation.** The `assistant` module (hexagonal: Spring AI and Resend in adapters only), the model port, the Shop opt-in and budget, the glossary terms and ADR 0004.
2. **Conversation → Design Proposal.** Tools for every editor action, each checked by the Construction Rules, with a test that fails when an editor action has no tool. One open Proposal per Project; it must pass the rules and lists its open questions and assumptions. Each request runs as a background job, one at a time per Conversation, streaming progress to the page. A Live / Proposal toggle in the editor marks what is added, changed or removed. Accepting takes the editing lock and replays the Proposal's steps onto the current design, stopping at the first step that breaks a rule; Undo accept restores the earlier design if nobody has edited since. Chatting needs no lock; Proposals change only through the Conversation. Conversations start on an existing Project and are kept with it.
3. **Attachments.** Photos, sketches and PDFs in the Conversation, stored in PostgreSQL behind a port (25 MB per message). Measurements read from a photo are always assumptions; only typed numbers or a dimensioned plan count as given.
4. **Scenario suite.** Real-model scenarios ("galley kitchen from this sketch": passes the rules, expected Cabinets and Openings) as shared test data next to `construction-rules/cases.json`, run on demand, not on every pull request; use it to choose between Sonnet and Opus. Unit tests use recorded model replies.
5. **Email with Resend.** Sending first: Invitation emails (closes the Milestone 2 follow-up) and notices to Members. Then receiving: one address per Shop at `in.cabinet.arendal.ca` (MX record from the Helm chart's DNSEndpoint), the signed `email.received` webhook, body, headers and attachments copied on arrival (Resend's download links expire after an hour). Only a Member's own address is accepted, with DMARC passing, or DKIM when the domain has no DMARC; everything else is dropped silently. Free plan to start, with a warning at 80% of the daily limit.
6. **Inbox.** Forwarded emails land in the Shop's Inbox, seen by every Member with who forwarded each. Haiku summarises each one and suggests the Client (from addresses in the thread) and Project; a Member confirms, or creates the Client and Project from the email, and only then is a Proposal built. Later emails in the same thread join the same Conversation and refine the open Proposal. The forwarding Member is told in the app and by email when the Proposal is ready; Clients are never emailed. Unassigned Inbox items are deleted after 30 days.

To confirm when building step 5: Resend's maximum message size, and whether `In-Reply-To` and `References` come as named headers or only in the raw message.

## 7. After v1: not scheduled

- Client portal: Clients design in the Shop's space and send a Quote Request.
- CNC export (DXF or machine files, with drilling).
- Face-frame Construction Standard.
- Supplier integrations (e.g. live catalog prices).
- Business model: open source, SaaS or open core (left open on purpose).

## Housekeeping

- Delete the merged branches `claude/build-and-deploy`, `claude/first-milestone`, `claude/milestone-2` and `claude/milestone-3`, and the old `claude/add-grill-skills`, from GitHub.
- Keep this file's **Current step** and **Status** up to date as milestones land.
