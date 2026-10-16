---
name: angular-frontend
description: Use when writing or modifying Angular frontend code in Open Cabinet. Ensures standalone component patterns, design-token styling, service conventions, glossary-aligned naming, and front-end Construction Rules kept in step with the server.
user-invocable: false
---

## Open Cabinet Angular Frontend Coding Style

Follow these patterns exactly when writing Angular code.

> **Open decisions** (marked `TODO` below) must be settled before they are relied on:
> - **TODO(location)**: where the Angular app lives (e.g. inside the Spring Boot module or a separate `frontend/` folder).
> - **TODO(auth)**: authentication provider and route guard.
> - **TODO(design)**: visual design system — token prefix, palette, light/dark theme. Placeholder token prefix used here: `--oc-*`.
> - **TODO(3d)**: 3D rendering library (e.g. Three.js).

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
  standalone: true,
  imports: [NgFor, NgIf, FormsModule],
  styleUrls: ["./<component-name>.component.css"],
  templateUrl: "./<component-name>.component.html",
})
```

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

**Always use the design-system CSS custom properties** — never hardcode colors, spacing, fonts, or radii. The token set is **TODO(design)**; until it exists, use the placeholder `--oc-*` names below and define them in one global stylesheet:

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

**Bootstrap integration**: Use Bootstrap for layout grid, tables, pagination, modals, and badges. Override Bootstrap colors from the design tokens in one global overrides stylesheet. Component-level styles use the tokens.

### Routing

All routes except sign-in require an authenticated Member (**TODO(auth)**: guard name and provider):
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
