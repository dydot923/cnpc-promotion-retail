# Database Migration Notes

## Current Environment

Date checked: 2026-07-07

Docker Desktop is available and the local PostgreSQL dev container has been validated:

```text
Docker client/server: 29.6.1
Docker Desktop: 4.80.0
PostgreSQL container: cnpc-promotion-postgres, healthy
```

Flyway V1-V4 have been validated with Testcontainers. V1-V3 were also validated against the live dev PostgreSQL database.

Note: Maven Surefire sets `TESTCONTAINERS_RYUK_DISABLED=true` for tests because this workstation cannot reliably pull `testcontainers/ryuk` from Docker Hub. Testcontainers still cleans up containers during normal JVM shutdown.

## Dev DB Verification Path

From the repository root:

```powershell
docker compose up -d postgres
docker ps
```

Confirm the `cnpc-promotion-postgres` container is healthy, then start the backend with the `dev-db` profile:

```powershell
cd backend
$env:DB_URL="jdbc:postgresql://localhost:5432/cnpc_promotion"
$env:DB_USERNAME="cnpc"
$env:DB_PASSWORD="cnpc"
mvn -DskipTests spring-boot:run "-Dspring-boot.run.profiles=dev-db"
```

Flyway should apply:

```text
V1__init_core_tables.sql
V2__promotion_rule_governance.sql
V3__audit_checkout_confirmation_replenishment.sql
V4__coupon_redeem.sql
```

## V3 Structure Checks

Use `psql` inside the container:

```powershell
docker exec -it cnpc-promotion-postgres psql -U cnpc -d cnpc_promotion
```

Check table definitions:

```sql
\d+ audit_log
\d+ checkout_confirmation
\d+ replenishment_list
```

Expected JSONB fields:

```text
audit_log.before_snapshot jsonb
audit_log.after_snapshot jsonb
checkout_confirmation.selected_candidate_snapshot jsonb
replenishment_list.items jsonb
coupon.applicable_categories jsonb
coupon.excluded_categories jsonb
coupon.applicable_product_codes jsonb
coupon.excluded_product_codes jsonb
```

Expected uniqueness:

```text
checkout_confirmation.confirmation_id unique
checkout_confirmation.calculation_id unique
```

## JSONB Read/Write Smoke SQL

```sql
insert into audit_log (
    audit_id, action, target_type, target_id,
    action_type, entity_type, entity_id,
    before_snapshot, after_snapshot,
    operator_id, operator_name, operated_at, reason, created_at
) values (
    'audit-smoke-1', 'CHECKOUT_CONFIRM', 'CHECKOUT_CONFIRMATION', 'confirm-smoke-1',
    'CHECKOUT_CONFIRM', 'CHECKOUT_CONFIRMATION', 'confirm-smoke-1',
    '{"status":"before"}'::jsonb, '{"status":"after"}'::jsonb,
    'migration-smoke', 'Migration Smoke', now(), 'jsonb smoke', now()
);

insert into checkout_confirmation (
    confirmation_id, calculation_id, selected_candidate_id,
    selected_candidate_snapshot,
    operator_id, operator_name, skipped, confirmed_at, created_at, updated_at
) values (
    'confirm-smoke-1', 'calc-smoke-1', 'original-price',
    '{"candidateId":"original-price","payableAmount":12.00,"discountAmount":0.00}'::jsonb,
    'migration-smoke', 'Migration Smoke', true, now(), now(), now()
);

insert into replenishment_list (
    list_id, list_name, status, items, total_items,
    created_by, created_at, updated_by, updated_at
) values (
    'repl-smoke-1', 'replenishment_smoke', 'DRAFT',
    '[{"productCode":"sku-1","suggestedQuantity":10}]'::jsonb,
    1, 'migration-smoke', now(), 'migration-smoke', now()
);

select after_snapshot ->> 'status' as audit_after_status
from audit_log
where audit_id = 'audit-smoke-1';

select selected_candidate_snapshot ->> 'candidateId' as candidate_id
from checkout_confirmation
where confirmation_id = 'confirm-smoke-1';

select items -> 0 ->> 'productCode' as first_product_code
from replenishment_list
where list_id = 'repl-smoke-1';
```

The expected results are:

```text
audit_after_status = after
candidate_id = original-price
first_product_code = sku-1
```

## API Smoke After Migration

After the backend starts on port `18082`, calculate and confirm once through the API or frontend:

```powershell
Invoke-RestMethod http://localhost:18082/actuator/health
```

Then verify rows:

```sql
select confirmation_id, calculation_id, selected_candidate_id, skipped
from checkout_confirmation
order by created_at desc
limit 5;

select action_type, entity_type, entity_id
from audit_log
order by created_at desc
limit 10;
```

## 2026-07-07 Docker Recheck

Docker was checked again after Docker Desktop started.

Result:

```text
Docker client version: 29.6.1
Docker server version: 29.6.1
Context: desktop-linux / docker-desktop
cnpc-promotion-postgres: healthy
Backend health on dev-db profile: UP
Flyway history: V1, V2, V3 success
JSONB smoke: audit_log, checkout_confirmation, replenishment_list read/write success
FlywayMigrationTest with Testcontainers: 1 test, 0 failures, 0 skipped
```

## 2026-07-07 Coupon V4 Recheck

Result:

```text
Flyway history: V1, V2, V3, V4 success
V4 creates: coupon_template, coupon
coupon.coupon_id: unique
coupon.status: indexed
coupon JSONB scopes: applicable/excluded categories and product codes
FlywayMigrationTest with Testcontainers: 1 test, 0 failures, 0 skipped
Backend regression after V4: 79 tests, 0 failures, 0 skipped
```

Decision:

- Keep the dev PostgreSQL container available for local manual verification.
- Stop the Spring Boot validation process after health checks.
- Use `mvn test` for backend regression; Flyway migration tests now run when Docker is available.
