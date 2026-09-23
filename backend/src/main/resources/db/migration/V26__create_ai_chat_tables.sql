CREATE TABLE ai_chat_sessions
(
    id         UUID PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_ai_chat_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_chat_sessions_user_id ON ai_chat_sessions (user_id);

CREATE TABLE ai_chat_messages
(
    id         BIGSERIAL PRIMARY KEY,
    session_id UUID         NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_chat_messages_session
        FOREIGN KEY (session_id) REFERENCES ai_chat_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_chat_messages_session_id ON ai_chat_messages (session_id);

CREATE TABLE ai_order_drafts
(
    id                 UUID PRIMARY KEY,
    session_id         UUID           NOT NULL UNIQUE,
    status             VARCHAR(20)    NOT NULL,
    customer_name      VARCHAR(50)    NOT NULL,
    phone              VARCHAR(15)    NOT NULL,
    payment_method     VARCHAR(20)    NOT NULL,
    delivery_method    VARCHAR(20)    NOT NULL,
    city               VARCHAR(50),
    street             VARCHAR(50),
    house              VARCHAR(10),
    apartment          VARCHAR(10),
    address_comment    VARCHAR(200),
    total_amount       DECIMAL(10, 2) NOT NULL,
    confirmed_order_id BIGINT,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP,
    CONSTRAINT fk_ai_order_drafts_session
        FOREIGN KEY (session_id) REFERENCES ai_chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_order_drafts_order
        FOREIGN KEY (confirmed_order_id) REFERENCES orders (id) ON DELETE SET NULL
);

CREATE TABLE ai_order_draft_items
(
    draft_id    UUID           NOT NULL,
    product_id  BIGINT         NOT NULL,
    product_name VARCHAR(50)   NOT NULL,
    quantity    INTEGER        NOT NULL,
    unit_price  DECIMAL(10, 2) NOT NULL,
    subtotal    DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (draft_id, product_id),
    CONSTRAINT fk_ai_order_draft_items_draft
        FOREIGN KEY (draft_id) REFERENCES ai_order_drafts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_order_draft_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);
