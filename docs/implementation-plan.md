# Implementation plan

How Open Cabinet gets from today to v1, in milestones. Each milestone ships to production on its own. Terms follow [`GLOSSARY.md`](../GLOSSARY.md); decisions are in [`docs/adr/`](adr).

## Current step

**Milestones 0–3 are in production** at `https://cabinet.arendal.ca`. The production environment, the `open-cabinet` Keycloak realm and its `open-cabinet-web` client are set up, and every merge into `main` deploys. Checked after the first deploy: pods and database healthy, DNS and TLS in place, the app served under `/fr/` and `/en/`, the API refusing unsigned requests. To finish:

1. Smoke-test signed in: create an account in the `open-cabinet` realm, sign in, create a Shop, design a Project, download a Quote PDF.
2. Milestone 6 is under way: the foundation, the Conversation, Design Proposals, attachments and the scenario suite are done; email with Resend is next. The suite has not met the real model yet: once `ANTHROPIC_API_KEY` is set, run it on Sonnet and Opus to choose the builder model and record their replies. The assistant needs an `ANTHROPIC_API_KEY` secret in the `open-cabinet-production` GitHub environment before it can be turned on.

## Status

| Milestone | Status |
|---|---|
| 0. Foundation and delivery | Done (PR #1), in production |
| 1. First end-to-end slice | Done (PR #2), in production |
| 2. Shop-ready | Done (PR #3), in production |
| 3. Design depth | Done (PR #4), in production |
| 4. Outputs for the shop floor and the Client | Done, in production |
| 5. Template Designer | Done, in production |
| 6. Design Assistant | In progress |
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

## 4. Outputs for the shop floor and the Client: done

0. **Template Versions** (first, ahead of the outputs; ADR 0005). Done. A Cabinet used to keep only its template's code, so editing `cabinet-templates.json` silently changed every old Project. Each entry of `cabinet-templates.json` is now one Template Version, only ever added, never edited: `cabinet-templates.published` lists each with a hash of its content, and `CabinetTemplateVersionsTest` fails when a published one changes (one file rather than a file per version, because both test suites read it directly). Every Cabinet records its template and version; existing Cabinets are migrated to version 1. Outdated Cabinets get a badge in 3D and in the side panel, and the Project a notice ("3 Cabinets can be updated") with Update all and each version's change note. Updating keeps the Cabinet's own values (size, shelves, dividers, Fronts, finished ends, crown, blind side) and is refused, showing the broken rules, when they don't fit the new version; Revert goes back to any published version. Sent Quotes are untouched; draft Quotes get the existing "priced before a change" notice.
1. **Shop Drawings**: done. A PDF from the Project editor (saved Project, in the Member's language and the Shop's units): a plan view with Walls, Openings, Appliances and every Cabinet labelled (wall Cabinets dashed); an Elevation per Wall with Fronts, widths chained along the floor and over the wall Cabinets, the Wall's height and the wall Cabinets' height from the floor; a sheet per Cabinet with its front (height of each front row), its side and its own Parts. Each sheet at the largest standard scale that fits (architectural for imperial Shops), with a title block.
2. **Quote PDF with pictures**: done. Each new Quote captures pictures of the design with its price (V12, a column of their own so the app never loads them): the Room in 3D under the Client and Project, then an Elevation of each Wall with Cabinets after the totals, with only the Wall's length and height and no labels. They are vector drawings made on the server from the same data as the Shop Drawings (an axonometric view from the side most Cabinets face), so a sent Quote keeps them unchanged. Quotes created before print as before; recreate a draft to add them.
3. **Printable and exportable outputs**: done. PDF and CSV buttons on the Cut List, Nesting and Materials tabs, for the saved Project in the Member's language. PDFs on landscape pages with the Shop Drawings' title block: the Cut List then the Fronts to order; a page per nested sheet at a standard scale, Parts labelled; the priced Bill of Materials with its total. CSVs like the Material Catalog export (commas, decimal points, fixed column names, lengths in mm), the Cut List also with lengths as the Shop measures them, and a byte order mark so spreadsheets read accents.
4. **Better Nesting**: done. Sheets are cut into strips, strips into columns, and shorter Parts stack in a column (three-stage guillotine, still cut on a panel saw). Each layout, rip first or crosscut first (grain still along the sheet), is packed with several orderings, first fit and best fit, keeping the fewest sheets then the fewest cuts; over 200 random cabinet mixes it never uses more sheets than before and saves one in about one Project in nine. Each Project chooses Automatic (fewest sheets per material), rip first or crosscut first (V13); the Nesting tab compares both layouts per material. Cuts are numbered in cutting order (1, 1.1, 1.1.1…) on the Nesting PDF. A new Construction Standard length, the minimum offcut (1" to start), keeps every strip wide enough for the saw to hold.

Follow-ups found while building it:
- An Elevation shows only its own Wall's Cabinets: a corner Cabinet from the neighbouring Wall is not drawn in section, and dimensions of very narrow items (a 1" filler) crowd their text.
- Shelves and drawer boxes are not drawn in the Cabinet sheets; the shelf count is in the notes.
- Nesting packs each Project's Parts on new sheets only; a Shop's offcut stock from earlier jobs is not tracked. Cut numbers are on the PDF, not in the app's Nesting view.
- Bill of Materials quantities have no unit (sheets, metres, m², pieces), in the app and in its exports.
- The 3D view in the Quote draws boxes farthest first, which can misdraw Cabinets that cross in depth (deep Appliances beside a corner); counters are not drawn.

## 5. Template Designer: done

Members design their own Cabinet Templates in a separate 3D designer, assembling Parts and hardware from the Shop's catalog, and the starter library is redrawn the same way (ADR 0006). Templates stay versioned (ADR 0005), so Projects never change under them; what a Project places is still a Cabinet.

What a template is:
- An assembly of **Parts** (panels, Fronts, mouldings) and **hardware**, held by **Attachments**: a Part's face fixed to a face of the Cabinet or of another Part, with an offset that is a number, a Construction Standard value (toe kick height, door gap) or a material thickness. Resizing a Cabinet moves every Part with it.
- Materials are Construction Standard roles by default (carcass, front, the Shop's hinge…), a specific catalog material when chosen. Each panel edge picks its banding and each panel its grain, with defaults (front edges get the front banding, grain vertical).
- Each Part has a role (side, shelf, door… or "other" with a typed name), which labels it on the Cut List and drawings.
- **Front stacks** size Fronts like today's front rows (fixed heights and shares, the Standard's door gap). **Repeated** Parts (shelves) take their count per Cabinet; a Front can split into two leaves above a width; a template's divider Part is placed per Cabinet.
- Hardware sits on a Part, counted fixed or by length (one hinge per 20" of door, at least 2).
- Butt joints only. Templates keep their type (base, wall, tall) and size ranges.
- Template rules, live in the designer and enforced on publish at the minimum, default and maximum size: every Part has a positive size, nothing overlaps, every Part fits its sheet, every Part touches another, and there is at least one Part or front stack. Assembly runs in Java and TypeScript, kept in step by a shared `template-assembly/cases.json`.

1. **The assembly model.** Done. A Cabinet Template may carry an `assembly` (Parts held by Attachments, front stacks, repeated Parts split into sections by dividers, a divider Part, hardware counted fixed or by length, mouldings by the metre); templates without one keep the frozen recipe. `Assembly.java` and `assembly.ts` build it at a Cabinet's size, `TemplateRules.java` and `template-rules.ts` check a template at its minimum, default and maximum size, kept in step by `template-assembly/cases.json` and `rules.json`. The Cut List, Bill of Materials, Shop Drawings, Quote pictures and the Project's 3D view use an assembled template's real Parts. No published template is assembled yet: they exist as test data until step 3.
2. **Starter library, version 2.** Done, ahead of the designer so new templates can start from real assemblies. Every starter template is published as version 2, an assembly; `StarterTemplateVersionsTest` builds versions 1 and 2 over every sampled size, shelf count, divider, made or ordered Fronts, finished ends and blind side, and both cut the same Parts (lengths within the 0.1 mm rounding step) and need the same hardware; every version 2 passes the template rules. The assembly model gained what the recipe does: Parts marked as Fronts (the blind panel), hardware counted by steps (hinges by door height, legs by width), a clearance at dividers, and mirroring for blind sides on the left. The version 1 recipe stays, frozen, for Cabinets still on version 1; their Cabinets now show the update.
3. **The designer, for Project templates.** Done (V14: `project_template` and its versions). A separate screen opened from the Project editor: a palette of the Shop's materials and Standard roles, a 3D view where snapping creates Attachments, a properties panel (Attachments, offsets, banding, grain, repetition, hardware), a minimum / default / maximum preview and the rules listed live. Start from an existing template or from a Cabinet. One draft and published Template Versions with change notes; a template belongs to its Project until shared; on first publish the editor offers to switch the original Cabinet to it. Names are typed once, not translated, unique within the Project and the Shop library. As built: "Design a template from this one" (from the template chosen to add) and "Make a template from this Cabinet" (saved Cabinets, its size becoming the default) start a draft from an assembled template, starter version 2 for version 1 Cabinets; the designer has a template panel (type, size ranges, shelves, blind width), a palette (carcass, back and front panels, front stack, moulding, dividers), Parts listed with their problems, a 3D view at minimum, default or maximum size where a face's "3D" button then a click on another Part or the Cabinet's outline creates the Attachment, panels for a Part (role, "other" with a name, material, position, grain, Front, repetition, banding, faces with offsets, fixed sizes), a front stack and hardware, and the template rules listed live. Publishing saves, then the server checks the template rules with the Shop's own materials (refusing with the rules broken), and the first version offers itself to the Cabinet it was made from, set in the Project's draft for the Member to save. The "other" Part role carries its name to the Cut List and every printed output.
4. **Shop library and Mirror.** Sharing moves a Project template with its history into the Shop library; it can't be un-shared. Owners and Designers create, publish and share templates. Templates are archived, never deleted, once a version is in use. Milestone 3's Shop Cabinet Template presets become Shop templates and the preset feature goes away. Blind side becomes **Mirror**, for any Cabinet. Done (V15). As built: the designer's "Share with the shop…" asks once, then the template keeps its Project but every Project of the Shop sees it, grouped as "Shop library" when adding a Cabinet. The **Templates** page lists the library: open a template in the designer, start a new one from a starter or library template (it has no Project), archive or restore a published one (archived templates are hidden when adding Cabinets; Cabinets using them keep them), delete one never published. A library template can also start from scratch: base, wall or tall, an empty assembly with that type's usual sizes (from the plainest starter of the type), which is the default on the Templates page; the empty designer says where to begin. A name is unique within its Project and within the library. At startup, each preset becomes a library template: the newest assembled version of its starter, with the preset's size, elevation and shelves as defaults (ranges widened to hold them) and published as version 1, renamed "Name (2)" if the name is taken. A Cabinet's **Mirror** builds it left for right; a blind corner's blind side moves to the left, and V15 turns saved left blind sides into Mirror.

In Projects, made or ordered Fronts applies to Parts marked as Fronts; finished ends and crown moulding stay Trim.

Found while building: a preset's per-Cabinet choices (made or ordered Fronts, finished ends, crown, dividers) are not carried into its library template; the emptied `shop_cabinet_template` table can be dropped once every Shop has started on V15; the designer draws Fronts see-through to show the inside, which may read as glass; picking a target under another Part means turning the view or using the face's list.

Follow-ups: joinery (dados, rabbets, dowels), which changes Part sizes and matters most for CNC export; moving version 1 Cabinets to version 2 without a Member, which would need its own decision against ADR 0005.

## 6. Design Assistant: in progress

Members describe changes in a Conversation, or forward a Client's email thread, and the Design Assistant answers with a Design Proposal they review in 3D and accept or discard (ADR 0004). Built with Spring AI on Claude: Sonnet builds Proposals, Haiku sorts the Inbox, both set in configuration.

Ground rules for every step:
- Off until an Owner opts the Shop in; Settings say what is sent to the model provider and to Resend (which keeps received mail 30 days). A monthly budget per Shop, in dollars, stops the assistant clearly when spent.
- Owners and Designers use it. It reads the current design, published Cabinet Templates (starter, Shop and the Project's own), the Construction Standard, material and Front style names and the Client's name; never prices, Quotes or other Projects. It places Cabinets from published templates but never creates, publishes or updates templates, nor moves Cabinets to a newer Template Version.
- It answers in the Member's language. Email content is untrusted: it is quoted to the model, never followed as instructions.

1. **Foundation.** The `assistant` module (hexagonal: Spring AI and Resend in adapters only), the model port, the Shop opt-in and budget, the glossary terms and ADR 0004. Done (V16: `assistant_settings`, `assistant_usage`). As built: Spring AI (2.0 since the move to Spring Boot 4.1), the Anthropic module without its starter, so the model exists only when `ANTHROPIC_API_KEY` is set; the deploy workflow copies that GitHub secret into the `open-cabinet-server` Secret the chart already loads. The builder and sorter models and their prices (US dollars per million tokens) are configuration; a configured model without prices stops the server at startup, so no call goes uncounted. Every call goes through one gate that refuses it unless the Shop opted in and has budget left this calendar month, then records its tokens and cost, rounded up. The Settings page has an Owner-only "Design Assistant" section: what is sent to Anthropic and to Resend, the opt-in (impossible without a key), the monthly budget, this month's spending, who turned it on, the models, and "Test the assistant", a tiny request to the sorter model, counted like any other.
2. **Conversation → Design Proposal.** Tools for every editor action, each checked by the Construction Rules, with a test that fails when an editor action has no tool. One open Proposal per Project; it must pass the rules and lists its open questions and assumptions. Each request runs as a background job, one at a time per Conversation, streaming progress to the page. A Live / Proposal toggle in the editor marks what is added, changed or removed. Accepting takes the editing lock and replays the Proposal's steps onto the current design, stopping at the first step that breaks a rule; Undo accept restores the earlier design if nobody has edited since. Chatting needs no lock; Proposals change only through the Conversation. Conversations start on an existing Project and are kept with it. Done (V17: `assistant_conversation`, `assistant_message`, `design_proposal`). As built: the server's own copy of the editor's actions (`DesignEdits.java`, kept in step with `design-edits.ts` by `design-edits/cases.json`); one tool per action, plus `view_design` and `note_proposal` (summary, open questions, assumptions), and `DesignToolsTest` fails when `design-edits.ts` exports an action no tool performs. The domain runs the model ↔ tool rounds itself (Spring AI only declares the tools), up to 25 per request, each through the assistant's gate, so the opt-in and budget hold mid-request; a step that would break a Construction Rule the design did not already break is refused, and the model is told which. A request continues the open Proposal, or starts one from the Project's design; what was taken is kept when a request stops early (budget, model error, too many steps, restart). The page polls the Conversation every second while a request runs rather than holding a stream open (the Keycloak token travels in a header, which a browser `EventSource` cannot send); progress lives in memory, so a restart loses it and closes the request with a note. The editor's **Assistant** tab shows the Conversation beside the 3D view, which switches between the Project and the Proposal, the Proposal's additions, changes and removals outlined in their own colours. Accept needs the editor saved and the lock free, replays the steps (each recreating the ids it created), stops at the first refused step and says how many applied; Undo accept restores the design while the Project's last save is still the accept's; Discard closes the Proposal.
3. **Attachments.** Photos, sketches and PDFs in the Conversation, stored in PostgreSQL behind a port (25 MB per message). Measurements read from a photo are always assumptions; only typed numbers or a dimensioned plan count as given. Done (V18: `assistant_attachment`, behind an `AttachmentStore` port). As built: JPEG, PNG, WebP, GIF and PDF, the type read from the bytes; at most 10 files and 25 MB per message, 5 MB per image and 100 pages per PDF (the model provider's limits, checked on upload; pages counted with OpenPDF). The browser shrinks photos larger than the model needs to a 2048-pixel JPEG before sending. Attachments travel with their message to the model, as images and PDF documents, while the message is in the 30 the assistant rereads; the Conversation shows them as thumbnails and opens them full size; another Shop gets nothing. The rule on measurements is in the assistant's instructions and under the message box. Settings now say that attachments are sent to Anthropic and kept with the Project.
4. **Scenario suite.** Real-model scenarios ("galley kitchen from this sketch": passes the rules, expected Cabinets and Openings) as shared test data next to `construction-rules/cases.json`, run on demand, not on every pull request; use it to choose between Sonnet and Opus. Unit tests use recorded model replies. Done. As built: scenarios in `backend/src/test/resources/assistant-scenarios/` (a starting design, the Member's messages with their photos, sketches or PDFs, and checks on the result: rules passed, steps, walls, cabinets by template and type, openings, appliances, a template under an opening, open questions, assumptions), six to start: a galley kitchen from a dimensioned sketch, an L-shaped kitchen in words, wall cabinets above a run (in English), swapping drawers for a sink, a request too vague to act on, and a photo without dimensions. They run through the real Conversation domain, the Project and Shop in memory, with ids numbered in order so recordings replay. `ANTHROPIC_API_KEY=… ./mvnw -pl backend test -Pscenarios -Dscenario.models=claude-sonnet-5-5,claude-opus-5-5` runs each against each model and writes `target/assistant-scenarios/report.md` (checks, steps, rounds, tokens, cost, time, the Proposal's summary, questions and assumptions); `-Dscenario.record=true` also saves the replies to the test resources, and `RecordedScenariosTest` replays every saved recording on every build. Until the first real run, one hand-written recording proves the replay path.
5. **Email with Resend.** Sending first: Invitation emails (closes the Milestone 2 follow-up) and notices to Members. Then receiving: one address per Shop at `in.cabinet.arendal.ca` (MX record from the Helm chart's DNSEndpoint), the signed `email.received` webhook, body, headers and attachments copied on arrival (Resend's download links expire after an hour). Only a Member's own address is accepted, with DMARC passing, or DKIM when the domain has no DMARC; everything else is dropped silently. Free plan to start, with a warning at 80% of the daily limit.
6. **Inbox.** Forwarded emails land in the Shop's Inbox, seen by every Member with who forwarded each. Haiku summarises each one and suggests the Client (from addresses in the thread) and Project; a Member confirms, or creates the Client and Project from the email, and only then is a Proposal built. Later emails in the same thread join the same Conversation and refine the open Proposal. The forwarding Member is told in the app and by email when the Proposal is ready; Clients are never emailed. Unassigned Inbox items are deleted after 30 days.

Found while building: attachments are resent on every round of a request, so a long PDF makes each round dearer; Anthropic's prompt caching would cut that and needs its cache tokens counted against the budget; one Conversation per Project for now (later email threads may need more); the Proposal's outlines are thin, like the editor's rule highlights, and could be stronger; the budget is checked before each call, so calls running at the same time can overshoot it slightly. The configured prices for `claude-sonnet-5-5` and `claude-opus-5-5` are to check against Anthropic's price list.

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
