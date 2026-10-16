# Implementation plan

How Open Cabinet gets from today to v1, in milestones. Each milestone ships to production on its own. Terms follow [`GLOSSARY.md`](../GLOSSARY.md); decisions are in [`docs/adr/`](adr).

## Current step

**Milestone 1 → production.** Milestones 0 and 1 are merged into `main` (PRs #1 and #2), and the `Build and Deploy` workflow builds, tests and publishes the image. The deploy job fails at its first cluster call because the production environment isn't configured yet. To finish:

1. Add `KUBECONFIG_BASE64` and `GHCR_PAT` to the `open-cabinet-production` GitHub environment.
2. Create the `open-cabinet` realm and `open-cabinet-web` client: [`docs/operations/keycloak-realm.md`](operations/keycloak-realm.md).
3. Re-run the latest `Build and Deploy` run, then smoke-test `https://cabinet.arendal.ca`: sign in, create a Shop, design a Project, download a Quote PDF.

Then start Milestone 2.

## Status

| Milestone | Status |
|---|---|
| 0. Foundation and delivery | Done (PR #1); deploy pending, see Current step |
| 1. First end-to-end slice | Done (PR #2); deploy pending, see Current step |
| 2. Shop-ready | Next |
| 3. Design depth | Planned |
| 4. Outputs for the shop floor and the Client | Planned |
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

## 2. Shop-ready: next

What a real Shop needs before using it every day.

1. **Member invitations.** An Owner invites Designers and Owners by email; the invitee signs in through Keycloak and joins the Shop. Requires realm SMTP (see the runbook's *Later* section).
2. **One editor at a time.** A Project is locked to the Member editing it; others see it read-only and who holds the lock.
3. **Quote Revisions.** Revising a sent Quote creates Rev 1, Rev 2… of the same number instead of a new Quote.
4. **Material Catalog CSV import.** Bulk create and update prices from supplier price lists.
5. **English.** Extract the marked strings, add the English translation and a language switch; French stays the default.
6. **Settings polish.** Show Owners what a settings change does to open drafts.

## 3. Design depth: planned

The design freedom ADR 0001 allows: free 3D editing, constrained to buildable Parts.

1. **Openings and Appliances** in the Room: windows and doorways, plus fridge, range, dishwasher and sink placeholders that Cabinets fit around.
2. **Construction Rules at corners**: Cabinets on neighbouring Walls must not collide; blind corners.
3. **Trim**: fillers, finished end panels, toe kicks, crown moulding.
4. **Fronts: made or ordered.** Each Front chooses in-house (Cut List) or ordered from a supplier (Bill of Materials, priced by size and style).
5. **Part-level editing in 3D**: add dividers, move shelves, resize Parts, all checked by Construction Rules.
6. **Shop Cabinet Templates**: Owners save their own templates from a designed Cabinet.
7. **More starter templates**: tall pantry, oven tower, corner, sink with false front.

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

- Delete the merged branches `claude/build-and-deploy` and `claude/first-milestone`, and the old `claude/add-grill-skills`, from GitHub.
- Keep this file's **Current step** and **Status** up to date as milestones land.
