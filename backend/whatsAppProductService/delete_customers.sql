-- SQL queries to delete customers named Sreeja and Rohith
-- IMPORTANT: Run these queries carefully in pgAdmin

-- Step 1: First, verify which customers will be deleted
SELECT 
    id, 
    name, 
    wa_phone_number, 
    phone_number,
    current_flow_stage,
    registered_at
FROM customers 
WHERE LOWER(name) IN ('sreeja', 'rohith');

-- Step 2: Delete related records first (to avoid foreign key constraints)

-- Delete shopping cart items for these customers
DELETE FROM shopping_cart 
WHERE customer_id IN (
    SELECT id FROM customers WHERE LOWER(name) IN ('sreeja', 'rohith')
);

-- Delete order items for orders placed by these customers
DELETE FROM order_items 
WHERE order_id IN (
    SELECT id FROM customer_orders WHERE customer_id IN (
        SELECT id FROM customers WHERE LOWER(name) IN ('sreeja', 'rohith')
    )
);

-- Delete customer orders
DELETE FROM customer_orders 
WHERE customer_id IN (
    SELECT id FROM customers WHERE LOWER(name) IN ('sreeja', 'rohith')
);

-- Step 3: Finally, delete the customers
DELETE FROM customers 
WHERE LOWER(name) IN ('sreeja', 'rohith');

-- Step 4: Verify deletion
SELECT 
    id, 
    name, 
    wa_phone_number
FROM customers 
WHERE LOWER(name) IN ('sreeja', 'rohith');
-- This should return 0 rows if deletion was successful


-- ============================================
-- ALTERNATIVE: Delete by specific IDs (safer)
-- ============================================
-- Based on the image, the IDs appear to be:
-- Rohith: id = 68
-- sreeja: id = 69

-- Uncomment and use these if you prefer to delete by ID:

/*
-- Verify customers by ID
SELECT id, name, wa_phone_number FROM customers WHERE id IN (68, 69);

-- Delete related records
DELETE FROM shopping_cart WHERE customer_id IN (68, 69);
DELETE FROM order_items WHERE order_id IN (SELECT id FROM customer_orders WHERE customer_id IN (68, 69));
DELETE FROM customer_orders WHERE customer_id IN (68, 69);

-- Delete customers
DELETE FROM customers WHERE id IN (68, 69);

-- Verify
SELECT id, name FROM customers WHERE id IN (68, 69);
*/
