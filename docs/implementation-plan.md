# Implementation plan

How Open Cabinet gets from today to v1, in milestones. Each milestone ships to production on its own. Terms follow [`GLOSSARY.md`](../GLOSSARY.md); decisions are in [`docs/adr/`](adr).

## Current step

**Milestone 3 review, then production.** Milestone 3 (Design depth) is done on `claude/milestone-3`, waiting for review and merge. Milestones 0–2 are merged into `main` (PRs #1, #2 and #3), and the `Build and Deploy` workflow builds, tests and publishes the image. The deploy job fails at its first cluster call because the production environment isn't configured yet. To finish:

1. Add `KUBECONFIG_BASE64` and `GHCR_PAT` to the `open-cabinet-production` GitHub environment.
2. Create the `open-cabinet` realm and `open-cabinet-web` client: [`docs/operations/keycloak-realm.md`](operations/keycloak-realm.md).
3. Re-run the latest `Build and Deploy` run, then smoke-test `https://cabinet.arendal.ca`: sign in, create a Shop, design a Project, download a Quote PDF.

The next successful deploy ships everything merged into `main`. After Milestone 3 is merged, start Milestone 4.

## Status

| Milestone | Status |
|---|---|
| 0. Foundation and delivery | Done (PR #1); deploy pending, see Current step |
| 1. First end-to-end slice | Done (PR #2); deploy pending, see Current step |
| 2. Shop-ready | Done (PR #3); deploy pending, see Current step |
| 3. Design depth | Done (`claude/milestone-3`), in review |
| 4. Outputs for the shop floor and the Client | Next |
| 5. After v1 | Not scheduled |

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

## 3. Design depth: done, in review

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

## 4. Outputs for the shop floor and the Client: planned

1. **Shop Drawings**: plan view, an Elevation per Wall and per-Cabinet details, printable.
2. **Quote PDF with pictures**: a 3D render and Elevations alongside the price.
3. **Printable and exportable outputs**: Cut List, Nesting layouts and Bill of Materials as PDF and CSV.
4. **Better Nesting**: fill partly used sheets before opening new ones, compare layouts, keep grain and saw constraints.

## 5. After v1: not scheduled

- Client portal: Clients design in the Shop's space and send a Quote Request.
- CNC export (DXF or machine files, with drilling).
- Face-frame Construction Standard.
- Supplier integrations (e.g. live catalog prices).
- Business model: open source, SaaS or open core (left open on purpose).

## Housekeeping

- Delete the merged branches `claude/build-and-deploy`, `claude/first-milestone` and `claude/milestone-2`, and the old `claude/add-grill-skills`, from GitHub.
- Keep this file's **Current step** and **Status** up to date as milestones land.
