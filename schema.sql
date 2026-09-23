-- ============================
-- users
-- ============================
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15),
    city VARCHAR(50),
    street VARCHAR(50),
    house VARCHAR(10),
    apartment VARCHAR(10),
    email_verified BOOLEAN NOT NULL,
    token_version INT NOT NULL,
    password VARCHAR(100),
    user_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- ============================
-- products
-- ============================
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(30) NOT NULL,
    weight INT NOT NULL,
    pieces INT,
    available BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_product_slug ON products(slug);
CREATE INDEX idx_product_available ON products(available);

-- ============================
-- product_images
-- ============================
CREATE TABLE product_images (
    id BIGINT PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ============================
-- promotions
-- ============================
CREATE TABLE promotions (
    id BIGINT PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(50) NOT NULL,
    description VARCHAR(250),
    discount_percent DECIMAL(5,2) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- ============================
-- promotion_products (many-to-many join table)
-- ============================
CREATE TABLE promotion_products (
    promotion_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT uq_promotion_product UNIQUE (promotion_id, product_id),
    CONSTRAINT fk_promo_products_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_promo_products_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ============================
-- orders
-- ============================
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    courier_id BIGINT,
    customer_name VARCHAR(50) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    city VARCHAR(50),
    street VARCHAR(50),
    house VARCHAR(10),
    apartment VARCHAR(10),
    address_comment VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    delivery_method VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_courier FOREIGN KEY (courier_id) REFERENCES users(id)
);
CREATE INDEX idx_order_user_id ON orders(user_id);
CREATE INDEX idx_order_courier_id ON orders(courier_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_created_at ON orders(created_at);

-- ============================
-- order_items
-- ============================
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============================
-- payments
-- ============================
CREATE TABLE payments (
    id BIGINT PRIMARY KEY,
    stripe_session_id VARCHAR(500) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE INDEX idx_payment_order_id ON payments(order_id);

-- ============================
-- reviews
-- ============================
CREATE TABLE reviews (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(250),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX idx_review_product_id ON reviews(product_id);

-- ============================
-- review_replies
-- ============================
CREATE TABLE review_replies (
    id BIGINT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message VARCHAR(250) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_review_replies_review FOREIGN KEY (review_id) REFERENCES reviews(id),
    CONSTRAINT fk_review_replies_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================
-- tokens
-- ============================
CREATE TABLE tokens (
    id BIGINT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_type VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_token_user_id ON tokens(user_id);

-- ============================
-- audit_logs
-- ============================
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY,
    entity_name VARCHAR(255) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    performed_by VARCHAR(255) NOT NULL,
    action VARCHAR(20) NOT NULL,
    performed_at TIMESTAMP NOT NULL
);

-- ============================
-- ai_chat_sessions
-- ============================
CREATE TABLE ai_chat_sessions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================
-- ai_chat_messages
-- ============================
CREATE TABLE ai_chat_messages (
    id BIGINT PRIMARY KEY,
    session_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id)
);

-- ============================
-- ai_order_drafts
-- ============================
CREATE TABLE ai_order_drafts (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    customer_name VARCHAR(50) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    delivery_method VARCHAR(20) NOT NULL,
    city VARCHAR(50),
    street VARCHAR(50),
    house VARCHAR(10),
    apartment VARCHAR(10),
    address_comment VARCHAR(200),
    total_amount DECIMAL(10,2) NOT NULL,
    confirmed_order_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_order_drafts_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id),
    CONSTRAINT fk_order_drafts_confirmed_order FOREIGN KEY (confirmed_order_id) REFERENCES orders(id)
);

-- ============================
-- ai_order_draft_items
-- ============================
CREATE TABLE ai_order_draft_items (
    draft_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_draft_items_draft FOREIGN KEY (draft_id) REFERENCES ai_order_drafts(id)
);
