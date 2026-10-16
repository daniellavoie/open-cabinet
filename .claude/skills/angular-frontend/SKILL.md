---
name: angular-frontend
description: Use when writing or modifying Angular frontend code in server/src/main/webapp/. Ensures standalone component patterns, in-house design system usage (`--sd-*` tokens), service conventions, and consistent styling.
user-invocable: false
---

## Mineshaft Angular Frontend Coding Style

Follow these patterns exactly when writing Angular code in `server/src/main/webapp/`.

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
  selector: "server-logs",
  standalone: true,
  imports: [NgFor, NgIf, FormsModule],
  styleUrls: ["./<component-name>.component.css"],
  templateUrl: "./<component-name>.component.html",
})
```

- Selectors use element style: `selector: "server-logs"` (no `app-` prefix)
- Always use `standalone: true`
- Import Angular directives explicitly: `NgFor`, `NgIf`, `NgClass`, `DatePipe`, etc.
- Use `styleUrls` and `templateUrl` (external files, not inline)

### Component Types

**Page Component** - top-level route target:
```typescript
export class ServersComponent {
  page!: Page<Server>;
  currentPage = 0;
  pageSize = 10;

  constructor(private serverService: ServerService) {
    this.loadPage(0);
  }

  loadPage(page: number) {
    this.serverService.findAllPage(page, this.pageSize)
      .pipe(tap(page => this.page = page))
      .subscribe();
  }
}
```

**Sub-Component** - child with @Input/@Output:
```typescript
export class ServerLogsComponent implements OnDestroy, OnInit {
  @Input()
  server!: Server;

  constructor(
    private cdr: ChangeDetectorRef,
    private serverService: ServerService,
  ) {}

  ngOnInit() { /* initialize */ }
  ngOnDestroy() { /* cleanup subscriptions */ }
}
```

**Modal Component** - uses NgbActiveModal:
```typescript
export class RestartServerModalComponent {
  @Input()
  server!: Server;

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

**HTTP Service** extending EventService for auto-refresh:
```typescript
@Injectable()
export class ServerService extends EventService<Server[]> {
  constructor(
    private httpClient: HttpClient,
    private sseService: SseService,
  ) {
    super(true);  // enable 5s auto-refresh
  }

  public findAll(): Observable<Server[]> {
    return this.httpClient.get<Page<Server>>(`/api/game-server?sort=key.name,ASC&size=1000`)
      .pipe(map(page => page.content));
  }

  refresh(): Observable<Server[]> {
    return this.findAll();
  }
}
```

**EventService** - abstract base for pub-sub with auto-refresh:
- Manages subscriber list
- Auto-refreshes every 5 seconds when `refreshWithTimer: true`
- Subclasses implement `refresh(): Observable<T>`

**EventSubscriber** - `@Directive()` base class for components that consume events:
- Handles subscription lifecycle (auto-unsubscribe on `ngOnDestroy`)
- Components extend this and call `this.subscribe()`

### Model Classes

Use TypeScript classes with `!` definite assignment:
```typescript
export class Server {
  config!: GameServerConfig;
  creationTimestamp!: Date;
  hostname!: string;
  name!: string;
  ownerId!: string;
  port!: number;
  status!: string;
}
```

- Use `!` (non-null assertion) for all fields
- Use `| null` for nullable fields: `deletionTimestamp!: Date | null;`
- No constructors, no decorators - plain class shape

### RxJS Patterns

Use `pipe()` with operators. Common patterns:
```typescript
// Simple data fetch
this.serverService.findAllPage(page, this.pageSize)
  .pipe(tap(page => this.page = page))
  .subscribe();

// Chained operations
activatedRoute.paramMap
  .pipe(switchMap(paramMap => this.getServerNameFromParam(paramMap)))
  .pipe(switchMap(serverName => this.findByName(serverName)));

// SSE streaming
this.sseService.getEventSource(`/api/game-server/${name}/logs`)
  .pipe(switchMap(eventSource => this.handleLogsEventSource(eventSource)));
```

- Each `.pipe()` call is on its own line
- Use `tap` for side effects, `map` for transforms, `switchMap` for chained async
- Always `finalize()` for cleanup

### HTML Template Patterns

**Structural directives:**
```html
<tr *ngFor="let server of page?.content">
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
<input (keydown.enter)="addTag($event)" (blur)="addTag($event)">
```

**Two-way binding:**
```html
<input [(ngModel)]="inputValue">
```

**Component inputs:**
```html
<server-status [status]="server.status"></server-status>
<snapshot-selector [sourceServer]="server" (snapshotChange)="onSnapshotChange($event)"></snapshot-selector>
```

### CSS / Design System

**Always use the design-system CSS custom properties** (prefix `--sd-*`) — never hardcode colors, spacing, fonts, or radii:

```css
/* Colors */
background-color: var(--sd-bg-raise);
color: var(--sd-text-default);
border: 1px solid var(--sd-border-default);

/* Spacing (4px grid) */
padding: var(--sd-space-4);
gap: var(--sd-space-3);
margin-bottom: var(--sd-space-4);

/* Typography */
font-family: var(--sd-font-mono);
font-size: var(--sd-font-size-sm);

/* Radii */
border-radius: var(--sd-radius-md);

/* Transitions */
transition: all var(--sd-transition-base);
```

**Key design-system tokens:**

| Category | Tokens |
|----------|--------|
| Backgrounds | `--sd-bg-default` (darkest), `--sd-bg-raise`, `--sd-bg-secondary`, `--sd-bg-tertiary`, `--sd-bg-quaternary` |
| Text | `--sd-text-default`, `--sd-text-secondary`, `--sd-text-tertiary`, `--sd-text-emphasis`, `--sd-text-inverse` |
| Borders | `--sd-border-default`, `--sd-border-secondary` |
| Accent | `--sd-accent-default` (amber), `--sd-accent-hover`, `--sd-accent-muted` |
| Semantic | `--sd-success`, `--sd-destructive`, `--sd-warning`, `--sd-info` + `-muted` variants |
| Spacing | `--sd-space-1` through `--sd-space-12` (4px grid) |
| Font sizes | `--sd-font-size-xs` (11px) through `--sd-font-size-2xl` (24px) |
| Radii | `--sd-radius-sm` (2px), `--sd-radius-md` (4px), `--sd-radius-lg` (6px) |
| Transitions | `--sd-transition-fast` (120ms), `--sd-transition-base` (200ms) |

**Theme**: Dark theme with deep slate backgrounds and amber accent. Always `data-bs-theme="dark"`.

**Bootstrap integration**: Use Bootstrap for layout grid, tables, pagination, modals, and badges. Override Bootstrap colors via `system-bootstrap-overrides.css`. Component-level styles use the `--sd-*` tokens.

**Page layout wrapper:**
```css
.content-window {
  height: 100vh;
  overflow: auto;
  background-color: var(--sd-bg-default);
  padding: var(--sd-space-8) var(--sd-space-6);
}
```

### Routing

All routes use Auth0 guard:
```typescript
{ path: 'servers/:serverName', component: ServerDetailsComponent, canActivate: [authGuardFn] }
```

### Directory Organization

```
src/app/
├── <feature>/
│   ├── model/           # TypeScript model classes
│   ├── <feature>-service.ts
│   ├── <sub-feature>/   # Component directories
│   └── ...
├── util/
│   ├── event/           # EventService, EventSubscriber
│   ├── sse/             # SSE service
│   ├── model/           # Shared models (Page)
│   └── <component>/     # Reusable components
└── environments/
```