CREATE TABLE inventory_item (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    on_hand INT NOT NULL,
    reserved INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_inventory_product ON inventory_item(product_id);
