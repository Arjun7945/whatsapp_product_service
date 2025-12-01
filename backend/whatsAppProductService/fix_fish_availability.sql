-- Check current fish products and their availability status
SELECT id, name, price_per_kg, is_available, image_url 
FROM fish_products;

-- If you see products with is_available = false, update them:
UPDATE fish_products 
SET is_available = true 
WHERE is_available = false;

-- Verify the update
SELECT id, name, price_per_kg, is_available 
FROM fish_products;
