# Smoke Test Checklist

Use this checklist before demos and acceptance reviews. Keep the raw Excel files in `data/` unchanged.

## Before Testing

- Backend is running on `http://localhost:18082`.
- Frontend is running on `http://localhost:5173`.
- `data/价格.xlsx`, `data/库存.xlsx`, and `data/活动看板.xlsx` have been imported.
- At least one imported promotion rule has been confirmed.
- The browser is opened on the frontend home page.

## Scenario A: Scanned Item Checkout And Confirmation

Preconditions:

- Price and inventory data have been imported.
- At least one `CONFIRMED` promotion rule can match the scanned item.

Steps:

1. Open `收银结算`.
2. Scan or enter a product barcode.
3. Confirm the product is added to the cart.
4. Click `计算促销`.
5. Leave the recommended candidate selected.
6. Click `确认结算`.
7. Copy the displayed `confirmationId`.
8. Open `确认追溯`.
9. Query by `confirmationId`.
10. Open `审计日志` and filter `actionType = CHECKOUT_CONFIRM`.

Expected results:

- Checkout returns original amount, payable amount, discount amount, available candidates, blocked promotions, explanations, rule versions, and original-price fallback.
- Confirmation succeeds and displays `confirmationId`.
- The confirmation trace page displays the selected candidate snapshot and cart snapshot.
- General audit page displays the matching `CHECKOUT_CONFIRM` record.

Verification points:

- Frontend only displays backend amounts.
- `CONFIRMED` rules participate in checkout.
- `CHECKOUT_CONFIRM` audit is written by backend.

## Scenario B: Original Price Fallback Confirmation

Preconditions:

- A cart item that does not match any confirmed promotion is available, or use the `无促销原价` demo case.

Steps:

1. Open `收银结算`.
2. Load or enter a no-promotion item.
3. Click `计算促销`.
4. Click `选择原价方案`.
5. Click `确认结算`.
6. Query the `confirmationId` in `确认追溯`.
7. Query `审计日志` by `entityId = confirmationId`.

Expected results:

- `original-price` remains visible and selectable.
- Confirmation succeeds.
- Trace result shows `skipped = true`.
- Audit log is queryable by confirmation id.

Verification points:

- Original price fallback is always available.
- Frontend does not calculate discount or eligibility.

## Scenario C: Candidate Switching

Preconditions:

- The cart has multiple available candidates, or use a demo/dev-db seed that produces multiple options.

Steps:

1. Add multiple products to the cart.
2. Click `计算促销`.
3. Switch the selected candidate in the available candidate table.
4. Confirm the non-default candidate.
5. Query by `confirmationId`.

Expected results:

- Confirmation request contains the selected `candidateId`.
- Trace page shows the same selected candidate.

Verification points:

- Candidate switching only changes the submitted candidate id.
- The backend-selected snapshot is persisted as truth.

## Scenario D: Duplicate Confirmation Rejection

Preconditions:

- A checkout calculation has already been confirmed once.

Steps:

1. Re-submit `POST /api/checkout/confirm` with the same `calculationId`.
2. Or use browser dev tools / API client to retry the same request.

Expected results:

- Backend returns HTTP `409`.
- Frontend shows the backend error message.
- Existing confirmation record remains unchanged.

Verification points:

- `checkout_confirmation.calculation_id` is unique.
- Duplicate settlement is rejected.

## Scenario E: Inventory Alert And Replenishment

Preconditions:

- Inventory data has been imported.
- Confirmed promotions reference products with low or missing station stock.

Steps:

1. Open `库存预警`.
2. Filter by `LOW`, `CRITICAL`, `OUT_OF_STOCK`, and `NO_STATION_STOCK`.
3. Click `生成补货清单`.
4. Open `补货清单`.
5. Generate or view the latest list.
6. Click `下载 CSV`.

Expected results:

- Alerts show product code, barcode, product name, current quantity, threshold, suggested quantity, related rule, severity, and reason.
- Replenishment list is generated as `DRAFT`.
- Export changes the list to `EXPORTED`.

Verification points:

- Inventory alert logic is backend-only.
- `REPLENISHMENT_GENERATE` and `REPLENISHMENT_EXPORT` audit logs are written.

## Scenario F: Import Errors View And Export

Preconditions:

- A workbook import produced warning or error rows.

Steps:

1. Open `导入异常`.
2. Select an import batch.
3. Filter by `severity`.
4. Optionally filter by `sheetName` or `errorCode`.
5. Click `导出 CSV`.

Expected results:

- Error rows include `importId`, `sheetName`, `rowNumber`, `columnName`, `rawValue`, `errorCode`, `errorMessage`, and `severity`.
- Exported CSV contains the filtered result.

Verification points:

- Import errors are not silently discarded.
- `IMPORT_ERRORS_EXPORT` audit is written.

## Final Commands

```powershell
cd backend
mvn test

cd ../frontend
npm run build
```

Known limitation:

- If Docker Desktop Linux engine is unavailable, `FlywayMigrationTest` is skipped and dev-db migration must be verified later using `docs/database-migration-notes.md`.
