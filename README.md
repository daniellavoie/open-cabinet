# Open Cabinet

A 3D cabinet designer for cabinet Shops: design a Project in 3D, then get its Cut List, Nesting, Bill of Materials and a Quote to send the Client.

- Domain language: [`GLOSSARY.md`](GLOSSARY.md)
- Decisions: [`docs/adr/`](docs/adr)
- Keycloak realm setup: [`docs/operations/keycloak-realm.md`](docs/operations/keycloak-realm.md)

## Layout

| Path | What |
|---|---|
| `backend/` | Spring Boot (Java 21) API, PostgreSQL via Flyway |
| `backend/src/main/webapp/` | Angular 22 app, packaged into the backend jar by the `frontend` Maven profile |
| `backend/src/main/resources/cabinet-templates.json` | Starter Cabinet Templates, served to the Angular app |
| `backend/src/test/resources/construction-rules/cases.json` | Construction Rules cases shared by the Java and TypeScript test suites |
| `helm/open-cabinet/` | Helm chart; production values in `.github/infra/open-cabinet-production/` |

## Build and test

```sh
./mvnw verify -Pfrontend      # backend + Angular tests, then the packaged jar
```

The full-application test uses Testcontainers and is skipped when Docker is unavailable.

## Run locally

1. Start PostgreSQL: `docker compose -f backend/docker-compose.yml up -d` (or run the app with the `dev` profile, which starts it for you).
2. Make sure the `open-cabinet` realm and `open-cabinet-web` client exist (see the runbook), with `http://localhost:4200/*` as a redirect URI.
3. Run the API: `./mvnw -pl backend spring-boot:run -Dspring-boot.run.profiles=dev`
4. Run the Angular dev server, which proxies `/api` to port 8080:
   ```sh
   cd backend/src/main/webapp
   npm install
   npm start
   ```
5. Open http://localhost:4200.

## Deployment

Every push to `main` is tested, published as an image tagged with the commit SHA and deployed to `open-cabinet-production` by `.github/workflows/build-and-deploy.yml`. Pull requests run `.github/workflows/ci.yml`.
