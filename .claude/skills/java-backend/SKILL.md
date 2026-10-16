---
name: java-backend
description: Use when writing or modifying Java backend code in the server, client, common, or model modules. Ensures hexagonal architecture, Lombok conventions, reactive patterns, and naming consistency.
user-invocable: false
---

## Mineshaft Java Backend Coding Style

Follow these patterns exactly when writing Java code in the server-side modules.

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

### Class Annotations (exact order matters)

**Domain Service:**
```java
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultGameServerDomain implements GameServerDomain {
```

**Repository Adapter:**
```java
@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameServerDatabaseRepository implements GameServerRepository {
```

**REST Controller:**
```java
@RestController
@AllArgsConstructor
@RequestMapping("/api/<resource>")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameServerController {
```

**JPA Entity:**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Server")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerEntity {
```

Note: Entities use `@FieldDefaults(level = AccessLevel.PRIVATE)` WITHOUT `makeFinal = true`.

**Configuration:**
```java
@Data
@Configuration
@ConfigurationProperties("minecraft-operator-api")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MineshaftServerConfiguration {
```

**Immutable Domain Model (value object):**
```java
@Value
@Jacksonized
@Builder(toBuilder = true)
public class NodePortAllocation {
	int serverNodePort;
	int rconNodePort;
	int metricsNodePort;
}
```

### Key Patterns

**Constructor injection** - Always via `@AllArgsConstructor` + `@FieldDefaults`, never `@Autowired`:
```java
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultGameServerDomain implements GameServerDomain {
	KubernetesClient kubernetesClient;
	GameServerRepository repository;
	UserDomain userDomain;
```

**Entity-to-domain conversion** - Entities have a `to()` method:
```java
public GameServer to() {
	return GameServer.builder()
			.name(key.getName())
			.ownerId(key.getOwner().getId())
			.status(status)
			.build();
}
```

**Multi-tenancy** - `Principal` is passed through all domain and repository methods:
```java
GameServer findByName(String serverName, Principal principal);
Page<GameServer> findByPrincipal(Principal principal, Pageable pageable);
```

**Reactive types** - Use `Mono<T>` and `Flux<T>` for async operations:
```java
public Flux<String> logs(String serverName, Principal principal) {
	return reactiveRepository.findByName(serverName, principal)
			.flatMapMany(gameServer -> serverClient.logs(...));
}
```

**Validation** - Use `Optional` + `orElseThrow` for null checks, `IllegalArgumentException` for input errors, `IllegalStateException` for state violations:
```java
Optional.ofNullable(request.getVersion())
		.orElseThrow(() -> new IllegalArgumentException("a version is required"));
```

**Controller methods** - Thin delegation to domain, always pass `Principal`:
```java
@PostMapping
public GameServerEvent create(@RequestBody CreateServerRequest request, Principal principal) {
	return domain.create(request, principal);
}
```

**Pagination** - Use Spring Data `Page<T>` and `Pageable`:
```java
@GetMapping
public Page<GameServer> findAll(Principal principal, Pageable pageable) {
	return domain.findByPrincipal(principal, pageable);
}
```

**Logging** - Use `@Slf4j` with structured parameters:
```java
log.info("Updating server status {}/{} with: { status = {}, hostname = {}, port = {} }",
		ownerId, serverName, status, hostname, port);
```

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Domain interface | `<Feature>Domain` | `GameServerDomain` |
| Domain impl | `Default<Feature>Domain` | `DefaultGameServerDomain` |
| Backend port | `<Feature>Repository` | `GameServerRepository` |
| DB adapter | `<Feature>DatabaseRepository` | `GameServerDatabaseRepository` |
| JPA interface | `<Feature>JpaRepository` | `GameServerJpaRepository` |
| Entity | `<Feature>Entity` | `ServerEntity` |
| Controller | `<Feature>Controller` | `GameServerController` |
| Config | `<Feature>Configuration` or `<Feature>Config` | `MineshaftServerConfiguration` |
| Util | `<Feature>Util` | `StatefulSetUtil` |
| Domain model | Plain name | `GameServer`, `Snapshot`, `NodePortAllocation` |

### Package Structure

- Server module: `dev.daniellavoie.mineshaft.server.<feature>.*`
- Operator module: `dev.daniellavoie.minecraft.operator.server.*`
- Model module: `dev.daniellavoie.minecraft.operator.api.model.*`
- Operator model: `dev.daniellavoie.minecraft.operator.model.*`

### Formatting

- Tab indentation (not spaces) in Java files
- Lombok `@Builder(toBuilder = true)` for mutability when needed
- `@Transactional` on repository adapter methods that need it (not on domain)
- Use `toBuilder()` pattern for updating entities:
  ```java
  repository.save(server.toBuilder()
  		.status(ServerStatus.UPDATING)
  		.updateTimestamp(LocalDateTime.now())
  		.build());
  ```