# Promotion Retail Backend

This backend is the Java 21 / Spring Boot 3 foundation for the gas-station promotion intelligent retail system.

Phase 1 priority:

1. Keep promotion calculation inside `ruleengine`.
2. Import Excel data into structured product, price, inventory, and promotion models.
3. Expose checkout calculation through backend APIs only.
4. Return available promotions, blocked promotions, explanations, rule versions, and original-price fallback.

Current skeleton:

- `ruleengine`: pure promotion calculation domain and first calculators.
- `checkout`: API orchestration boundary, no promotion logic in controllers.
- `importcenter`: reserved for EasyExcel import endpoints.
- `product`, `price`, `inventory`, `promotion`: structured data modules.
- `audit`, `auth`, `replenishment`, `poster`: later phase modules.

Dependency note:

- The default Maven dependency set is kept small so rule-engine tests can run in restricted/offline environments.
- Enable `-Pintegration-deps` when network or a complete Maven cache is available to add PostgreSQL driver, Flyway PostgreSQL support, EasyExcel, Springdoc OpenAPI, and PostgreSQL Testcontainers.
