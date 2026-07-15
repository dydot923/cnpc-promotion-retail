ALTER TABLE coupon_template
    ADD COLUMN IF NOT EXISTS discount_rate NUMERIC(8, 4) NOT NULL DEFAULT 0;

ALTER TABLE coupon
    ADD COLUMN IF NOT EXISTS discount_rate NUMERIC(8, 4) NOT NULL DEFAULT 0;

UPDATE coupon_template
SET discount_rate = 0
WHERE discount_rate IS NULL;

UPDATE coupon
SET discount_rate = 0
WHERE discount_rate IS NULL;
