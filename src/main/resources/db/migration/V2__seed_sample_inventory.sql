INSERT INTO inventory_item (product_id, on_hand, reserved, updated_at)
VALUES
    (1001, 35, 0, NOW()),
    (1002, 20, 0, NOW()),
    (1003, 18, 0, NOW()),
    (1004, 42, 0, NOW()),
    (1005, 16, 0, NOW()),
    (1006, 14, 0, NOW())
ON CONFLICT (product_id) DO UPDATE
SET
    on_hand = EXCLUDED.on_hand,
    reserved = EXCLUDED.reserved,
    updated_at = EXCLUDED.updated_at;
