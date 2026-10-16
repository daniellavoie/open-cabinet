---
name: java-backend
description: Use when writing or modifying Java backend code in the Open Cabinet Spring Boot server. Ensures hexagonal architecture, Lombok conventions, glossary-aligned naming, and Shop-scoped multi-tenancy.
user-invocable: false
---

## Open Cabinet Java Backend Coding Style

Follow these patterns exactly when writing Java code in the Spring Boot back end.

> **Open decisions** (marked `TODO` below) must be settled before they are relied on:
> - **TODO(package)**: root package. Placeholder used here: `dev.daniellavoie.opencabinet`.
> - **TODO(reactive)**: blocking Spring MVC or reactive WebFlux (`Mono`/`Flux`). Until decided, write blocking code and do not introduce reactive types.

### Domain Language

Name classes, fields and endpoints with the terms in `GLOSSARY.md` (Shop, Member, Client, Project, Room, Cabinet, Part, Front, Quote, Cut List…), never with the words listed under `_Avoid_`. If a concept has no glossary term yet, raise it before inventing a name.

### Architecture: Hexagonal (Ports & Adapters)

New features MUST follow this directory structure:

```
<feature>/
├── domain/
│   ├── model/              # Domain models (records, @Value, @Builder)
│   ├── port/
│   │   ├── api/            # Business interfaces (<Feature>Domain)
│   │   └── backend/        # Repository/storage interfaces (<Feature>Repository)
│   └── Default<Feature>Domain.java   # @Service implementation
└── module/
    ├── adapter/
    │   └── database/
    │       ├── entity/     # JPA entities (<Feature>Entity) with .to() conversion
    │       ├── <Feature>JpaRepository.java        # Spring Data JPA interface
    │       └── <Feature>DatabaseRepository.java   # @Repository implementing port/backend interface
    ├── job/                # Background jobs (if needed)
    └── web/
        └── <Feature>Controller.java   # @RestController
```

Domain logic (Construction Rules, Cut List, Bill of Materials, Nesting, Quote calculation) lives in `domain/` and never depends on `module/`.

### Construction Rules Are Implemented Twice

Per `docs/adr/0002-construction-rules-run-in-browser-and-server.md`, Construction Rules also exist in the Angular front end. The server is the authority. Any change to a Construction Rule must:
1. be made in both the Java and TypeScript implementations, and
2. be covered by the shared, language-neutral test cases that both test suites run.

Everything priced or printed (Cut List, Bill of Materials, Nesting, Quote) is computed on the server only.

### Class Annotations (exact order matters)

**Domain Service:**
```java
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultProjectDomain implements ProjectDomain {
```

**Repository Adapter:**
```java
@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDatabaseRepository implements ProjectRepository {
```

**REST Controller:**
```java
@RestController
@AllArgsConstructor
@RequestMapping("/api/<resource>")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {
```

**JPA Entity:**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Project")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectEntity {
```

Note: Entities use `@FieldDefaults(level = AccessLevel.PRIVATE)` WITHOUT `makeFinal = true`.

**Configuration:**
```java
@Data
@Configuration
@ConfigurationProperties("open-cabinet")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OpenCabinetConfiguration {
```

**Immutable Domain Model (value object):**
```java
@Value
@Jacksonized
@Builder(toBuilder = true)
public class PartDimensions {
	BigDecimal length;
	BigDecimal width;
	BigDecimal thickness;
}
```

### Key Patterns

**Constructor injection** - Always via `@AllArgsConstructor` + `@FieldDefaults`, never `@Autowired`:
```java
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultQuoteDomain implements QuoteDomain {
	QuoteRepository repository;
	ProjectDomain projectDomain;
	MaterialCatalogDomain materialCatalogDomain;
```

**Entity-to-domain conversion** - Entities have a `to()` method:
```java
public Project to() {
	return Project.builder()
			.id(id)
			.name(name)
			.clientId(client.getId())
			.build();
}
```

**Multi-tenancy** - Every piece of data belongs to a Shop. `Principal` is passed through all domain and repository methods, and every query is scoped to the Principal's Shop:
```java
Project findById(UUID projectId, Principal principal);
Page<Project> findByClient(UUID clientId, Principal principal, Pageable pageable);
```

**Roles** - Owner-only operations (Members, Construction Standard, Material Catalog, prices, Labour Methods, taxes, currency) are rejected for Designers in the domain layer, not only in the controller.

**Frozen Quotes** - A sent Quote is never modified. Changes produce a new Quote Revision; never update a sent Quote's lines or totals in place.

**Validation** - Use `Optional` + `orElseThrow` for null checks, `IllegalArgumentException` for input errors, `IllegalStateException` for state violations:
```java
Optional.ofNullable(request.getClientId())
		.orElseThrow(() -> new IllegalArgumentException("a client is required"));
```

**Controller methods** - Thin delegation to domain, always pass `Principal`:
```java
@PostMapping
public Project create(@RequestBody CreateProjectRequest request, Principal principal) {
	return domain.create(request, principal);
}
```

**Pagination** - Use Spring Data `Page<T>` and `Pageable`:
```java
@GetMapping
public Page<Project> findAll(Principal principal, Pageable pageable) {
	return domain.findByPrincipal(principal, pageable);
}
```

**Logging** - Use `@Slf4j` with structured parameters:
```java
log.info("Sending quote {}/{} revision {} with: { total = {}, currency = {} }",
		shopId, quoteId, revision, total, currency);
```

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Domain interface | `<Feature>Domain` | `ProjectDomain` |
| Domain impl | `Default<Feature>Domain` | `DefaultProjectDomain` |
| Backend port | `<Feature>Repository` | `ProjectRepository` |
| DB adapter | `<Feature>DatabaseRepository` | `ProjectDatabaseRepository` |
| JPA interface | `<Feature>JpaRepository` | `ProjectJpaRepository` |
| Entity | `<Feature>Entity` | `ProjectEntity` |
| Controller | `<Feature>Controller` | `ProjectController` |
| Config | `<Feature>Configuration` or `<Feature>Config` | `OpenCabinetConfiguration` |
| Util | `<Feature>Util` | `NestingUtil` |
| Domain model | Plain glossary name | `Project`, `Cabinet`, `Quote`, `PartDimensions` |

### Package Structure

- Root: `dev.daniellavoie.opencabinet` (**TODO(package)**: confirm)
- Features: `dev.daniellavoie.opencabinet.<feature>.*`, one feature per glossary area (e.g. `shop`, `client`, `project`, `catalog`, `quote`)

### Formatting

- Tab indentation (not spaces) in Java files
- Lombok `@Builder(toBuilder = true)` for mutability when needed
- `@Transactional` on repository adapter methods that need it (not on domain)
- Use `toBuilder()` pattern for updating entities:
  ```java
  repository.save(project.toBuilder()
  		.name(request.getName())
  		.updateTimestamp(LocalDateTime.now())
  		.build());
  ```
