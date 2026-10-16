# Keycloak realm setup

Open Cabinet signs Members in through the shared arendal Keycloak at
`https://auth.arendal.ca`, deployed from the `daniellavoie/arendal` repository
(`infra/environments/arendal-keycloak-production`). Realms and clients there are
not managed as code, so the Open Cabinet realm is created once by hand with the
commands below.

Keycloak is identity only (see `docs/adr/0003-keycloak-for-identity-only.md`):
the realm holds people and their credentials. Shops, Members and the
Owner/Designer roles live in Open Cabinet's own database, so the realm needs no
groups, roles or custom claims.

| Setting | Value |
|---|---|
| Realm | `open-cabinet` |
| Issuer | `https://auth.arendal.ca/realms/open-cabinet` |
| Client | `open-cabinet-web` (public, standard flow with PKCE S256) |
| App URLs | `https://cabinet.arendal.ca` (production), `http://localhost:4200` (local development) |

## Prerequisites

- `kubectl` access to the cluster (the `paasas-prod` context).
- The Keycloak admin password. On a fresh install it is the bootstrap secret;
  if it has since been changed in the admin console, use the current one.

## 1. Open an admin session

The commands run `kcadm.sh` inside the Keycloak pod against its local HTTP
port, so nothing goes through the ingress.

```sh
NS=arendal-keycloak-production
KCADM="kubectl exec -n $NS keycloak-0 -- /opt/keycloak/bin/kcadm.sh"
KCCONFIG="--config /tmp/kcadm.config"

# Prints the bootstrap admin password, to paste at the prompt below.
kubectl get secret keycloak-bootstrap-admin -n $NS -o jsonpath='{.data.password}' | base64 -d; echo

# Prompts for the password so it never lands in shell history or the pod's process list.
kubectl exec -it -n $NS keycloak-0 -- /opt/keycloak/bin/kcadm.sh config credentials \
  --config /tmp/kcadm.config \
  --server http://localhost:8080 \
  --realm master \
  --user admin
```

The session is stored in `/tmp/kcadm.config` inside the pod and is lost when
the pod restarts; rerun this step if a later command reports it is not logged in.

## 2. Create the realm

French is the default language, matching the app.

```sh
$KCADM create realms $KCCONFIG \
  -s realm=open-cabinet \
  -s displayName="Open Cabinet" \
  -s enabled=true \
  -s sslRequired=external \
  -s loginWithEmailAllowed=true \
  -s duplicateEmailsAllowed=false \
  -s registrationAllowed=false \
  -s resetPasswordAllowed=false \
  -s internationalizationEnabled=true \
  -s 'supportedLocales=["fr","en"]' \
  -s defaultLocale=fr
```

`registrationAllowed` stays `false`: Members join a Shop by invitation from
inside Open Cabinet. `resetPasswordAllowed` stays `false` until the realm has
an SMTP server configured (see [Later](#later)).

## 3. Create the Angular client

A public client (the browser cannot keep a secret) using the authorization code
flow with PKCE, which is what `keycloak-js` does in
`backend/src/main/webapp/src/app/security/auth-service.ts`.

```sh
$KCADM create clients $KCCONFIG -r open-cabinet \
  -s clientId=open-cabinet-web \
  -s name="Open Cabinet" \
  -s enabled=true \
  -s publicClient=true \
  -s standardFlowEnabled=true \
  -s implicitFlowEnabled=false \
  -s directAccessGrantsEnabled=false \
  -s serviceAccountsEnabled=false \
  -s rootUrl=https://cabinet.arendal.ca \
  -s 'redirectUris=["https://cabinet.arendal.ca/*","http://localhost:4200/*"]' \
  -s 'webOrigins=["https://cabinet.arendal.ca","http://localhost:4200"]' \
  -s 'attributes."pkce.code.challenge.method"=S256' \
  -s 'attributes."post.logout.redirect.uris"=+'
```

`post.logout.redirect.uris=+` reuses the redirect URIs, so signing out returns
to the app.

## 4. Create a first account (optional)

For testing sign-in before invitations exist. `--temporary` forces a password
change on first sign-in.

```sh
$KCADM create users $KCCONFIG -r open-cabinet \
  -s username=owner@example.ca \
  -s email=owner@example.ca \
  -s emailVerified=true \
  -s firstName=Prénom \
  -s lastName=Nom \
  -s enabled=true

kubectl exec -it -n $NS keycloak-0 -- /opt/keycloak/bin/kcadm.sh set-password \
  --config /tmp/kcadm.config -r open-cabinet \
  --username owner@example.ca \
  --temporary
```

## 5. Verify

```sh
# The issuer the app is configured with must match exactly.
curl -s https://auth.arendal.ca/realms/open-cabinet/.well-known/openid-configuration | jq -r .issuer
# -> https://auth.arendal.ca/realms/open-cabinet

$KCADM get clients $KCCONFIG -r open-cabinet -q clientId=open-cabinet-web \
  --fields clientId,publicClient,redirectUris,webOrigins
```

Then open `https://cabinet.arendal.ca`: it should redirect to the
`open-cabinet` login page and come back to the home page once signed in.

## 6. Clean up

```sh
kubectl exec -n $NS keycloak-0 -- rm -f /tmp/kcadm.config
```

## Later

- **SMTP**: configure the realm's email settings (Realm settings → Email), then
  set `resetPasswordAllowed=true` and `verifyEmail=true`:
  `$KCADM update realms/open-cabinet $KCCONFIG -s resetPasswordAllowed=true -s verifyEmail=true`
- **Login theme**: a branded `open-cabinet` theme once the design system exists.
- **Additional environments**: add their URLs to the client's `redirectUris`
  and `webOrigins` with `$KCADM update clients/<id> ...`.
