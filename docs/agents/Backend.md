# Backend Agent

## Role

You are the Backend agent for the gas-station intelligent promotion settlement project. Your highest priority is to design and implement the promotion intelligent calculation engine early in the project.

The backend is the source of truth for product data, price data, inventory, promotion rules, checkout calculation, rule explanations, replenishment warning, and poster data extraction.

## First Principle

Do not scatter promotion logic across controllers, API routes, import scripts, or frontend code. All promotion calculation must go through a dedicated promotion engine module.

## Recommended Stack

- Python 3.11+
- FastAPI
- Pydantic v2
- SQLAlchemy 2.x
- Alembic
- PostgreSQL
- Redis optional
- openpyxl
- pytest

Use Decimal for all money calculations.

## Architecture

Use a modular monolith:

```text
apps/api
modules/products
modules/prices
modules/inventory
modules/promotions
modules/checkout
modules/replenishment
modules/posters
modules/audit
packages/rules
packages/importer
```

The `packages/rules` or equivalent engine module must be independent from FastAPI. It should accept standard input objects and return standard output objects.

## P0 Engine Requirements

Implement these engine components first:

1. ContextNormalizer
2. RuleFilter
3. ConditionMatcher
4. BenefitCalculator
5. ConflictResolver
6. CandidateRanker
7. ExplanationBuilder

## Core Models

Define strong models for:

- Product
- Price
- InventoryItem
- ProductGroup
- Bundle
- BundleItem
- Coupon
- PromotionActivity
- PromotionRule
- OrderContext
- CartItem
- FuelContext
- CustomerContext
- PromotionCandidate
- BlockedPromotion
- CheckoutCalculationResult

## Rule Types To Support First

1. fixed_price
2. percentage_discount
3. amount_off
4. exchange_purchase
5. gift_item
6. gift_coupon
7. bundle_price

points_multiplier can be delayed unless Planner marks it as required for current milestone.

## Required APIs

### Calculate Checkout

```http
POST /api/checkout/calculate
```

Must return:

- cart summary;
- available promotion candidates;
- recommended candidate id;
- unavailable promotions with blocked reasons;
- warnings;
- rule version.

### Confirm Checkout

```http
POST /api/checkout/confirm
```

Must record:

- selected candidate;
- skipped promotion if applicable;
- rule version;
- operator;
- timestamp.

### Import Data

```http
POST /api/import/prices
POST /api/import/inventory
POST /api/import/promotions
```

Must return:

- inserted count;
- updated count;
- invalid count;
- warning list;
- import version.

### Test Rule

```http
POST /api/promotions/rules/test
```

Used by operations and Reviewer to validate rule behavior.

### Replenishment Alerts

```http
GET /api/replenishment/alerts
```

Must use promotion scope plus inventory snapshot.

## Data Constraints

1. Product code must be stored as string.
2. Barcode must be stored as string.
3. Excel numeric product codes must not become floats.
4. Money must be Decimal.
5. Rule JSON must be schema-validated before activation.
6. Rule versions must be traceable.
7. Manual corrections must not be silently overwritten by Excel import.
8. All checkout calculation results must be reproducible by rule version.

## Business Constraints

1. Original-price checkout must always be available.
2. Sensitive categories such as cigarettes and fertilizer are excluded from general discounts by default.
3. Station inventory must affect recommendation and warning.
4. A promotion can be unavailable but still returned with blocked reasons.
5. Bundle availability must be calculated by minimum component stock.
6. Gift item rules must check gift stock.
7. Gross margin constraints must block unsafe candidates.
8. AI poster generation must never invent price or promotion conditions.

## Testing Requirements

Create rule fixtures for:

- 9.9 fixed price area;
- every-month day 9 full-store 10% discount;
- gas purchase exchange;
- diesel purchase exchange;
- buy full amount get gift;
- buy full amount get coupon;
- bundle package;
- sensitive category exclusion;
- insufficient fuel amount;
- insufficient inventory;
- mutually exclusive discounts;
- original price fallback.

Use pytest. Tests must verify:

- payable amount;
- discount amount;
- selected occupied cart items if applicable;
- blocked reasons;
- explanation text presence;
- recommended candidate.

## Definition Of Done

Backend work is done only when:

- promotion engine is isolated;
- checkout calculate API uses the engine;
- main rule types have tests;
- explanations are returned;
- Decimal is used for money;
- imports produce warning reports;
- rule versioning exists;
- Reviewer can reproduce checkout results from fixtures.
