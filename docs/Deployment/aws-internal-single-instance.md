# Internal AWS deployment — single instance

This is the deployment shape for the first release: **4 users, internal-facing
only, self-hosted PostgreSQL, nightly logical backups to S3.** It is deliberately
small. Everything runs as containers on one EC2 instance, managed with Docker
Compose.

Supporting files live in [`deploy/`](../../deploy):

| File                      | Purpose                                           |
| ------------------------- | ------------------------------------------------- |
| `docker-compose.prod.yml` | app + PostgreSQL + Keycloak + Caddy reverse proxy |
| `Caddyfile`               | TLS termination and routing                       |
| `env.example`             | template for `deploy/.env` (all credentials)      |
| `postgres-init/`          | creates the Keycloak database on first start      |
| `backup-to-s3.sh`         | nightly `pg_dump` of both databases to S3         |
| `restore-from-s3.sh`      | restores both databases from a backup pair        |
| `backup.env.example`      | template for `/etc/ombuto-backup.env`             |
| `ombuto-backup.service`   | systemd unit invoking the backup                  |
| `ombuto-backup.timer`     | nightly schedule                                  |

## Read this before the first deployment

**Liquibase must move to incremental changelogs first.** The project is still
using JHipster's generator-authored changelogs, which are rewritten in place
whenever `ombuto.jdl` is regenerated. That is fine while the database can be
wiped; it is not fine once there is data. A rewritten changelog whose checksum no
longer matches will either fail the startup validation or, worse, be applied
against a schema it does not describe. Before the first deployment that holds
real data, freeze the current changelogs as a baseline and add every subsequent
change as a new, append-only changelog file. This is the one blocking prerequisite
in this document.

**One instance only, by design.** Live collaboration uses Spring's in-memory
STOMP broker. Two application instances would not share subscriptions, so users
on different instances would silently stop seeing each other's edits. Scaling out
needs an external broker (Redis or RabbitMQ relay) first. With 4 users this is
not a constraint worth spending on — but it must not be worked around by simply
raising the desired count on an autoscaling group.

## What to ask the company's AWS team for

Give them this list. Everything is standard; nothing needs a networking exception.

**Compute**

- One EC2 instance, `t3.medium` (2 vCPU, 4 GB), Amazon Linux 2023, 50 GB gp3 root
  volume. The app runs with a 1 GB heap; PostgreSQL, Keycloak and the proxy fit
  comfortably in the rest at this volume.
- Placed in a **private subnet** — no public IP, no internet-facing load balancer.
- Outbound internet through a NAT gateway (or VPC endpoints for S3, ECR, SSM and
  CloudWatch if they prefer no NAT).

**Access**

- **SSM Session Manager** for shell access, not SSH. No bastion, no key pairs, no
  port 22 in any security group.
- Reachability for users: whatever they already use for internal apps — a private
  Route 53 record pointing at the instance if users are on the corporate network
  or VPN, or an internal ALB in front of it. The application itself does not care;
  only `PUBLIC_HOST` and the certificate change.

**Security group**

- Inbound 443 from the corporate CIDR ranges (or the internal ALB's security
  group) only.
- Inbound 80 from the same source, used only to redirect to 443.
- No other inbound rules. PostgreSQL and Keycloak are never exposed to the host,
  let alone the network.

**TLS**

- A server certificate for the chosen hostname from the company's internal CA,
  with its key. It is placed at `deploy/tls/server.crt` and `server.key`. Public
  ACME cannot be used — the host is not reachable from the internet.
- If an internal ALB terminates TLS instead, use ACM there and simplify the
  `Caddyfile` to plain HTTP on the instance.

**Storage and backups**

- One S3 bucket for backups: versioning on, public access blocked, SSE-KMS with a
  customer-managed key, and a lifecycle rule (suggested: keep daily for 30 days,
  transition to Glacier Instant Retrieval at 30 days, expire at 365).
- Weekly **EBS snapshots** of the root volume via Data Lifecycle Manager, retained
  4 weeks. These cover instance loss; the S3 dumps cover data loss and are the
  ones you actually restore from.

**IAM instance profile** — the instance needs exactly:

- `AmazonSSMManagedInstanceCore` (managed policy, for Session Manager)
- `s3:PutObject`, `s3:GetObject`, `s3:ListBucket` on the backup bucket and its
  contents only
- `kms:GenerateDataKey`, `kms:Decrypt` on the backup key
- `cloudwatch:PutMetricData` on namespace `Ombuto/Backup`
- `ecr:GetAuthorizationToken` plus pull permissions, if the image is served from ECR

**Monitoring**

- A CloudWatch alarm on `Ombuto/Backup` → `BackupSucceeded`, treating **missing
  data as breaching**, over a 26-hour period. This is the important one: a backup
  job that dies silently looks identical to one that had nothing to do.
- Alarms on instance `StatusCheckFailed` and root volume free space below 20%.

**Costs**, roughly, for `eu-west-1` at this size: the instance is the bulk of it
(around $30/month on demand, roughly half that on a one-year savings plan), the
NAT gateway is the next largest line if they use one, and S3 storage for a few
hundred megabytes of compressed dumps is negligible.

## Environment and secrets

Every credential comes from `deploy/.env`, created from `deploy/env.example` and
never committed. Generate each secret with `openssl rand -base64 32`.

The variable names in the compose file are the ones Spring Boot and Keycloak read
directly — `SPRING_DATASOURCE_*`, `SPRING_SECURITY_OAUTH2_CLIENT_*`, `KC_DB_*` —
so there is no indirection to trace when something does not connect.

Two settings are load-bearing and easy to miss:

- `SERVER_FORWARD_HEADERS_STRATEGY=framework` on the app. Without it, Spring
  builds OIDC redirect URIs from the container's own address and login bounces to
  `http://app:8080`.
- `KC_PROXY_HEADERS=xforwarded` and `KC_HOSTNAME=${PUBLIC_URL}` on Keycloak, so
  issued tokens carry the public issuer. The app validates the issuer, so a
  mismatch here fails every login with a token that looks superficially fine.

**MCP transport at the proxy.** The MCP server uses the Streamable HTTP transport
on `POST /mcp` (short JSON-RPC request/response) and `GET /mcp` (optional
server-to-client listening stream, framed as SSE). The `Caddyfile` keeps
`flush_interval -1` and zeroed read/write timeouts on the app upstream so the
listening stream is not buffered or terminated by the proxy; short JSON-RPC
POSTs are unaffected by those settings. There is no separate message endpoint —
the earlier HTTP+SSE transport (`/mcp` + `/mcp/message`) is gone.

If the company would rather not keep secrets in a file on disk, move `.env` to
AWS Secrets Manager and render it at boot from the instance profile. The compose
file does not need to change.

## First deployment

1. **Build the image** on a workstation: `npm run java:docker:prod`. This runs
   jib and produces `opportunitysolutiontree:<version>` on
   `eclipse-temurin:21-jre-noble`, also tagged `latest`. Either push it to ECR and
   set `APP_IMAGE` to that repository, or `docker save | ssh`-equivalent it onto
   the instance through SSM.

   **Set `APP_IMAGE` to the version tag, never `latest`.** A deployment you cannot
   name is one you cannot reason about afterwards: `latest` refers to whatever was
   built most recently, so it cannot answer which build is running, and pulling it
   again can silently change the application. The compose file refuses to start
   without `APP_IMAGE` set for this reason.
2. **Install Docker and the Compose plugin** on the instance, and copy the repo's
   `deploy/` directory to `/opt/ombuto-ost/deploy`.
3. **Place the TLS certificate** at `deploy/tls/server.crt` and `server.key`.
4. **Create `deploy/.env`** from `env.example` and fill in every value. `chmod 600`.
5. **Start the data tier first**, so the Keycloak database is created before
   anything tries to use it:
   `docker compose -f docker-compose.prod.yml up -d postgresql`
   Wait for it to report healthy.
6. **Start Keycloak**, then configure the realm in its admin console at
   `https://<host>/admin/master/`: create the realm, create the `web_app`
   confidential client with the redirect URI `https://<host>/login/oauth2/code/oidc`,
   create the `ROLE_USER` and `ROLE_ADMIN` roles, and add them to the token as a
   `groups` claim. The development realm export under
   `src/main/docker/realm-config/` is the reference for what the app expects; do
   not import it as-is, as it carries development-only clients and users.

   Also create the **`mcp_client`** public client used by LLM agent tools. The MCP
   filter chain (`application.mcp.audience`, defaults to `mcp-server`) refuses any
   token whose `aud` claim does not include one of the configured MCP audiences —
   `web_app`'s session tokens do not carry it, so they are rejected at `/mcp`. In
   the admin console:
   - **Clients → Create client** → Client type OpenID Connect, Client ID `mcp_client`.
   - **Capability config**: enable _Direct access grants_ (`password` grant) so the
     "Connect an agent" page's one-shot `curl` works, disable _Client authentication_
     (public client), enable _Standard flow_ (Authorization Code + PKCE) for
     interactive clients.
   - **Login settings → Valid redirect URIs**: `http://127.0.0.1:*`,
     `http://localhost:*` (and any HTTPS loopback variants your clients need).
   - **Advanced → Proof Key for Code Exchange**: `S256`.
   - **Client scopes → mcp_client-dedicated → Add mapper → By configuration →
     Audience**: name `audience-mcp-server`, leave _Included Client Audience_ empty,
     set _Included Custom Audience_ to `mcp-server`, _Add to access token_ on,
     _Add to ID token_ off. This is the audience the app validates at `/mcp`; do
     NOT add the same mapper to `web_app`.

   The dev realm export at `src/main/docker/realm-config/jhipster-realm.json`
   (client `mcp_client`, mapper `audience-mcp-server`) is the reference — replicate
   its shape, not its secret or redirect URIs.

7. **Start the rest**: `docker compose -f docker-compose.prod.yml up -d`.
8. **Verify** `https://<host>/management/health` returns `UP`, then sign in.
   `https://<host>/management/info` names exactly what is running — the build
   version and the git commit it was built from. That pair is the only reliable
   answer to "is this change live?", so record it with each deployment.
9. **Install the backup job**:
   ```bash
   sudo install -m 0755 backup-to-s3.sh    /usr/local/bin/ombuto-backup
   sudo install -m 0755 restore-from-s3.sh /usr/local/bin/ombuto-restore
   sudo install -m 0600 backup.env         /etc/ombuto-backup.env   # edit first
   sudo install -m 0644 ombuto-backup.service ombuto-backup.timer /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable --now ombuto-backup.timer
   sudo systemctl start ombuto-backup      # don't wait until tomorrow to find out
   ```
10. **Remove the bootstrap admin**: once a named Keycloak admin account exists,
    delete `KC_BOOTSTRAP_ADMIN_USERNAME`/`PASSWORD` from the compose environment
    and restart the keycloak service.

### Adding users

Users are created in the Keycloak admin console, not in the application. Two
things must be done for each one, and missing either produces a login that
appears to succeed and then lands nowhere:

- Assign the `ROLE_USER` role. The application authorises entirely from the
  `groups` claim.
- Set the user's **email as verified**, or have them verify it. `UserService`
  copies `email_verified` into the account's activated flag, so an unverified
  user is created deactivated.

## Backup and restore

`ombuto-backup` runs nightly and dumps **both** databases — the application's and
Keycloak's. Both are required. The application's own admin backup endpoint
deliberately excludes user rows because identities live in Keycloak, so an
application archive on its own cannot rebuild a working system.

Each dump is `pg_dump --format=custom --clean --if-exists`, gzipped, and uploaded
under `s3://<bucket>/<prefix>/app/` and `.../keycloak/`. The script refuses to
upload a dump smaller than 1 KB, takes a `flock` so overlapping runs cannot
corrupt each other, and publishes the `BackupSucceeded` metric on success.

To restore:

```bash
sudo ombuto-restore latest              # or a specific 20260921T020000Z stamp
```

It stops the app and Keycloak, restores both databases, and starts them again. It
requires typing `restore` to confirm, because it replaces the contents of both.

**Run a restore drill on a throwaway instance once a quarter.** Restore `latest`,
sign in, open a tree. An untested backup is a hypothesis.

## Recovery expectations

| Failure               | Recovery                                                       | Data loss          |
| --------------------- | -------------------------------------------------------------- | ------------------ |
| Container crash       | `restart: unless-stopped` brings it back                       | none               |
| Instance loss         | New instance from the weekly EBS snapshot, then restore latest | up to 24 hours     |
| Bad data / bad deploy | `ombuto-restore <stamp>`                                       | back to that stamp |

At 4 users a 24-hour recovery point is a reasonable trade. If it stops being
acceptable, the next step is not a more elaborate script — it is moving the
database to RDS, where point-in-time recovery is a checkbox.

## When this shape runs out

In rough order of when each would start to matter:

- **More than one application instance** — needs an external STOMP broker relay
  before it is safe (see above).
- **RDS instead of self-hosted PostgreSQL** — buys automated backups, PITR and
  patching. The only change here is `SPRING_DATASOURCE_URL`; drop the `postgresql`
  service and point the backup script elsewhere.
- **A managed identity provider** — if the company standardises on one, the app's
  OIDC configuration points at it instead and Keycloak disappears.
- **Centralised logs** — the CloudWatch agent shipping the container logs, rather
  than reading them over SSM.
