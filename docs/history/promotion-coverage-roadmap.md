# Promotion Coverage Roadmap

## Member Points - Phase 2 Plan

### Business Needs

- Accumulate member points from fuel and non-fuel purchases.
- Redeem points for products or cash discount.
- Manage point expiration.
- Keep an auditable point transaction ledger.

### Suggested Architecture

- Add an independent `points` module; do not place points accrual logic inside `ruleengine`.
- Points accrual should be triggered after checkout confirmation.
- Points redemption can later be added as a dedicated `BenefitCalculator` type named `POINTS_REDEEM`, after member point balance lookup is available.
- Product exchange by points should use a separate page and workflow, not the checkout promotion calculation path.

### Dependencies

- Member system API for point balance query.
- Member system API for point deduction.
- Audit log records for every point balance change.
