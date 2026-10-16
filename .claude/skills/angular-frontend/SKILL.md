---
name: angular-frontend
description: Use when writing or modifying Angular frontend code in backend/src/main/webapp/. Ensures standalone component patterns, design-token styling, service conventions, glossary-aligned naming, and front-end Construction Rules kept in step with the server.
user-invocable: false
---

## Open Cabinet Angular Frontend Coding Style

Follow these patterns exactly when writing Angular code in `backend/src/main/webapp/`.

### Stack

- The app lives in `backend/src/main/webapp/`, is built by the backend build, and is served by Spring Boot as one deployable unit.
- **Authentication**: the existing Keycloak server, via OpenID Connect. API calls carry the Keycloak access token. Keycloak is identity only: the current Member's Shop and role (Owner or Designer) come from the Open Cabinet API, never from token claims.
- **3D**: plain Three.js wrapped in Angular components (see *3D Rendering*). No Angular-specific 3D wrapper libraries.
- **Change detection**: zone.js (`provideZoneChangeDetection`), with `HttpClient` on the XHR backend (`withXhr()`). Angular 22's default fetch backend resolves outside zone.js, so views would not update after responses.

### Domain Language

Name components, services, models and routes with the terms in `GLOSSARY.md` (Shop, Member, Client, Project, Room, Cabinet, Part, Front, Quote, Cut List…), never with the words listed under `_Avoid_`. If a concept has no glossary term yet, raise it before inventing a name.

### Languages

All user-facing text is available in French and English; French is the default. Never hardcode user-facing strings in templates or components.

### Construction Rules Are Implemented Twice

Per `docs/adr/0002-construction-rules-run-in-browser-and-server.md`, the front end runs the Construction Rules for live feedback while editing in 3D; the server is the authority. Any change to a Construction Rule must:
1. be made in both the TypeScript and Java implementations, and
2. be covered by the shared, language-neutral test cases that both test suites run.

When the server's result for a saved edit differs from the front end's, the server's result replaces it. The front end never computes the Cut List, Bill of Materials, Nesting or Quote; it displays what the server returns.

### Component Structure

Every component lives in its own directory with 3 files:
```
<component-name>/
├── <component-name>.component.ts
├── <component-name>.component.html
└── <component-name>.component.css
```

### Standalone Components (no NgModule)

All components are standalone. Use this exact decorator pattern:

```typescript
@Component({
  selector: "project-list",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [NgFor, NgIf, FormsModule],
  styleUrls: ["./<component-name>.component.css"],
  templateUrl: "./<component-name>.component.html",
})
```

- Always set `changeDetection: ChangeDetectionStrategy.Eager`: Angular 22 defaults components to OnPush, and the patterns below assign plain fields in `subscribe()`, which OnPush would never render
- Selectors use element style: `selector: "project-list"` (no `app-` prefix)
- Always use `standalone: true`
- Import Angular directives explicitly: `NgFor`, `NgIf`, `NgClass`, `DatePipe`, etc.
- Use `styleUrls` and `templateUrl` (external files, not inline)

### Component Types

**Page Component** - top-level route target:
```typescript
export class ProjectsComponent {
  page!: Page<Project>;
  currentPage = 0;
  pageSize = 10;

  constructor(private projectService: ProjectService) {
    this.loadPage(0);
  }

  loadPage(page: number) {
    this.projectService.findAllPage(page, this.pageSize)
      .pipe(tap(page => this.page = page))
      .subscribe();
  }
}
```

**Sub-Component** - child with @Input/@Output:
```typescript
export class CutListComponent implements OnDestroy, OnInit {
  @Input()
  project!: Project;

  constructor(
    private cdr: ChangeDetectorRef,
    private projectService: ProjectService,
  ) {}

  ngOnInit() { /* initialize */ }
  ngOnDestroy() { /* cleanup subscriptions */ }
}
```

**Modal Component** - uses NgbActiveModal:
```typescript
export class SendQuoteModalComponent {
  @Input()
  quote!: Quote;

  constructor(public modal: NgbActiveModal) {}

  dismiss() { this.modal.dismiss(); }
  confirm() { this.modal.close(result); }
}
```

**Reusable Utility Component** - pure @Input/@Output:
```typescript
export class LoadingButtonComponent {
  @Input() disabled = false;
  @Input() loading = false;
  @Input() text = "";
  @Output() load = new EventEmitter<void>();
}
```

### Service Patterns

**HTTP Service** - one per feature, wrapping the REST API:
```typescript
@Injectable()
export class ProjectService {
  constructor(private httpClient: HttpClient) {}

  public findAllPage(page: number, size: number): Observable<Page<Project>> {
    return this.httpClient.get<Page<Project>>(`/api/project?page=${page}&size=${size}`);
  }

  public findById(projectId: string): Observable<Project> {
    return this.httpClient.get<Project>(`/api/project/${projectId}`);
  }
}
```

### Model Classes

Use TypeScript classes with `!` definite assignment:
```typescript
export class Project {
  id!: string;
  clientId!: string;
  name!: string;
  creationTimestamp!: Date;
  updateTimestamp!: Date;
}
```

- Use `!` (non-null assertion) for all fields
- Use `| null` for nullable fields: `sentTimestamp!: Date | null;`
- No constructors, no decorators - plain class shape

### RxJS Patterns

Use `pipe()` with operators. Common patterns:
```typescript
// Simple data fetch
this.projectService.findAllPage(page, this.pageSize)
  .pipe(tap(page => this.page = page))
  .subscribe();

// Chained operations
activatedRoute.paramMap
  .pipe(map(paramMap => paramMap.get("projectId")!))
  .pipe(switchMap(projectId => this.projectService.findById(projectId)));
```

- Each `.pipe()` call is on its own line
- Use `tap` for side effects, `map` for transforms, `switchMap` for chained async
- Always `finalize()` for cleanup

### HTML Template Patterns

**Structural directives:**
```html
<tr *ngFor="let project of page?.content">
<div *ngIf="!page">Loading ...</div>
```

**Class bindings:**
```html
<li [class.active]="p === currentPage">
<li [class.disabled]="page.first">
```

**Event bindings:**
```html
<button (click)="loadPage(p)">
<input (keydown.enter)="search($event)" (blur)="search($event)">
```

**Two-way binding:**
```html
<input [(ngModel)]="inputValue">
```

**Component inputs:**
```html
<quote-status [status]="quote.status"></quote-status>
<material-selector [catalog]="catalog" (materialChange)="onMaterialChange($event)"></material-selector>
```

### CSS / Design Tokens

The current look is a **temporary light theme**: neutral palette, one accent color. A proper design system will replace it later, so styling must be swappable by editing token files only.

**Three token layers, all in `src/styles/`:**
1. `tokens.palette.css` — raw values (`--oc-palette-gray-100`, `--oc-palette-blue-600`…). Nothing outside the token files may reference these.
2. `tokens.semantic.css` — meaning-based tokens mapped onto the palette (`--oc-bg-default`, `--oc-bg-raise`, `--oc-text-default`, `--oc-text-secondary`, `--oc-border-default`, `--oc-accent-default`, `--oc-success`, `--oc-destructive`, `--oc-warning`, spacing `--oc-space-1`…`--oc-space-12` on a 4px grid, `--oc-font-size-*`, `--oc-radius-*`, `--oc-transition-*`).
3. `bootstrap-overrides.css` — Bootstrap variables set from semantic tokens only.

**Components use semantic tokens only** — never hardcode colors, spacing, fonts or radii, and never reference palette tokens:

```css
background-color: var(--oc-bg-raise);
color: var(--oc-text-default);
border: 1px solid var(--oc-border-default);
padding: var(--oc-space-4);
gap: var(--oc-space-3);
font-size: var(--oc-font-size-sm);
border-radius: var(--oc-radius-md);
transition: all var(--oc-transition-base);
```

If a component needs a value no semantic token expresses, add a semantic token rather than a literal. The 3D scene takes its colors from the same semantic tokens (read via `getComputedStyle`), not from literals in TypeScript.

**Bootstrap integration**: Use Bootstrap for layout grid, tables, pagination, modals, and badges. Component-level styles use the semantic tokens.

### 3D Rendering

- Each 3D view is an Angular component that owns one Three.js renderer, scene and camera, attached to a `<canvas>` in its template.
- Create the renderer in `ngAfterViewInit` and run the render loop with `NgZone.runOutsideAngular` so animation frames don't trigger change detection; re-enter the zone only to emit events (`@Output`).
- In `ngOnDestroy`, stop the loop and dispose every geometry, material, texture and the renderer.
- Scene objects map one-to-one to domain objects (a Cabinet, a Part, a Wall); keep the mapping in a dedicated service rather than in the component.

### Routing

All routes except sign-in require an authenticated Member, signed in through Keycloak:
```typescript
{ path: 'projects/:projectId', component: ProjectDetailsComponent, canActivate: [authGuardFn] }
```

Owner-only screens (Members, Construction Standard, Material Catalog, prices, Labour Methods, taxes, currency) are also hidden from Designers; the server still enforces the role.

### Directory Organization

```
src/app/
├── <feature>/
│   ├── model/           # TypeScript model classes
│   ├── <feature>-service.ts
│   ├── <sub-feature>/   # Component directories
│   └── ...
├── util/
│   ├── model/           # Shared models (Page)
│   └── <component>/     # Reusable components
└── environments/
```
