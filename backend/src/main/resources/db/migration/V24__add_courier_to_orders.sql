ALTER TABLE orders
    ADD COLUMN courier_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_courier
        FOREIGN KEY (courier_id) REFERENCES users(id)
        ON DELETE SET NULL;

CREATE INDEX idx_order_courier_id ON orders(courier_id);
