-- Khet Freshness Order Service - Initial Schema
-- Version: 1.0
-- Date: June 2026

-- ============================================
-- ENUMS
-- ============================================

CREATE TYPE product_item AS ENUM ('WHEAT', 'RICE', 'ATTA');

CREATE TYPE order_status AS ENUM (
    'PENDING_PAYMENT',
    'PAYMENT_CONFIRMED',
    'PACKED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED'
);

CREATE TYPE customer_language AS ENUM ('hindi', 'english');

CREATE TYPE conversation_state_enum AS ENUM (
    'COLLECTING_ITEM',
    'COLLECTING_ADDRESS',
    'CONFIRMING_ADDRESS',
    'READY_TO_PAY'
);

CREATE TYPE address_mode_enum AS ENUM ('SAVED', 'NEW');

-- ============================================
-- TABLES
-- ============================================

-- Customers table
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150),
    language customer_language NOT NULL DEFAULT 'hindi',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_phone ON customers(phone);

-- Customer addresses table
CREATE TABLE customer_addresses (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    line1 VARCHAR(150) NOT NULL,
    line2 VARCHAR(150),
    landmark VARCHAR(150),
    city VARCHAR(50) NOT NULL DEFAULT 'Varanasi',
    pincode VARCHAR(10),
    full_text TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses(customer_id);
CREATE UNIQUE INDEX idx_customer_addresses_default ON customer_addresses(customer_id)
    WHERE is_default = TRUE;

-- Products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_item product_item NOT NULL UNIQUE,
    name_english VARCHAR(50) NOT NULL,
    name_hindi VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Price records table
CREATE TABLE price_records (
    id BIGSERIAL PRIMARY KEY,
    product_item product_item NOT NULL,
    sell_price NUMERIC(10, 2) NOT NULL,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_item, effective_date)
);

CREATE INDEX idx_price_records_product_date ON price_records(product_item, effective_date DESC);

-- Stock table
CREATE TABLE stock (
    id BIGSERIAL PRIMARY KEY,
    product_item product_item NOT NULL UNIQUE,
    available_kg NUMERIC(10, 2) NOT NULL DEFAULT 0,
    reserved_kg NUMERIC(10, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stock_non_negative CHECK (available_kg >= 0 AND reserved_kg >= 0),
    CONSTRAINT chk_stock_reserved_lte_available CHECK (reserved_kg <= available_kg)
);

CREATE INDEX idx_stock_product_item ON stock(product_item);

-- Orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    address_id BIGINT REFERENCES customer_addresses(id),
    delivery_address_text TEXT,
    item product_item NOT NULL,
    quantity_kg NUMERIC(10, 2) NOT NULL,
    price_per_kg NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status order_status NOT NULL DEFAULT 'PENDING_PAYMENT',
    razorpay_order_id VARCHAR(100),
    payment_id VARCHAR(100),
    delivery_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_quantity_positive CHECK (quantity_kg > 0),
    CONSTRAINT chk_order_price_positive CHECK (price_per_kg > 0 AND total_amount > 0)
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_delivery_date ON orders(delivery_date);
CREATE INDEX idx_orders_payment_id ON orders(payment_id);

-- Order conversation state table
CREATE TABLE order_conversation_state (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    state conversation_state_enum NOT NULL,
    item product_item,
    quantity_kg NUMERIC(10, 2),
    address_mode address_mode_enum,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversation_state_phone ON order_conversation_state(phone);
CREATE INDEX idx_conversation_state_created_at ON order_conversation_state(created_at);

-- Payment webhooks table (for debugging and audit)
CREATE TABLE payment_webhooks (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    razorpay_signature VARCHAR(500),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_webhooks_event_type ON payment_webhooks(event_type);
CREATE INDEX idx_payment_webhooks_processed_at ON payment_webhooks(processed_at DESC);

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customer_addresses_updated_at BEFORE UPDATE ON customer_addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stock_updated_at BEFORE UPDATE ON stock
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversation_state_updated_at BEFORE UPDATE ON order_conversation_state
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- SEED DATA
-- ============================================

-- Insert products
INSERT INTO products (product_item, name_english, name_hindi) VALUES
    ('WHEAT', 'Wheat', 'गेहूं'),
    ('RICE', 'Rice', 'चावल'),
    ('ATTA', 'Wheat Flour', 'आटा');

-- Insert initial stock (zero stock for all products)
INSERT INTO stock (product_item, available_kg, reserved_kg) VALUES
    ('WHEAT', 0, 0),
    ('RICE', 0, 0),
    ('ATTA', 0, 0);

-- Insert initial price records (as per spec: wheat ₹2,800/kg, rice ₹2,500/kg)
-- Note: These are sample prices, actual MSP is much lower per quintal
INSERT INTO price_records (product_item, sell_price, effective_date) VALUES
    ('WHEAT', 28.00, CURRENT_DATE),
    ('RICE', 25.00, CURRENT_DATE),
    ('ATTA', 32.00, CURRENT_DATE);

-- ============================================
-- COMMENTS
-- ============================================

COMMENT ON TABLE customers IS 'Customer records with phone as primary identifier';
COMMENT ON TABLE customer_addresses IS 'Customer delivery addresses, supports multiple addresses per customer';
COMMENT ON TABLE products IS 'Product catalog with English and Hindi names';
COMMENT ON TABLE price_records IS 'Historical price records, supports price changes over time';
COMMENT ON TABLE stock IS 'Real-time stock inventory with reservation tracking';
COMMENT ON TABLE orders IS 'Order records with full lifecycle tracking';
COMMENT ON TABLE order_conversation_state IS 'Temporary conversation state for in-progress orders';
COMMENT ON TABLE payment_webhooks IS 'Audit log for all payment webhook events';

COMMENT ON COLUMN stock.available_kg IS 'Total stock available for all orders';
COMMENT ON COLUMN stock.reserved_kg IS 'Stock reserved by pending orders';
COMMENT ON COLUMN customer_addresses.is_default IS 'Only one default address per customer';
COMMENT ON COLUMN orders.delivery_address_text IS 'Snapshot of address at order time';
