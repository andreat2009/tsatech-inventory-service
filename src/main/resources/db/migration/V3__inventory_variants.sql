ALTER TABLE inventory_item
    ADD COLUMN IF NOT EXISTS variant_key VARCHAR(128) NOT NULL DEFAULT '';

ALTER TABLE inventory_item DROP CONSTRAINT IF EXISTS inventory_item_product_id_key;
DROP INDEX IF EXISTS idx_inventory_product;

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_product_variant ON inventory_item(product_id, variant_key);
CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory_item(product_id);
