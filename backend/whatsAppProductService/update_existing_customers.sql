-- Migration script to fix existing customer records
-- This updates customers added by executives to have the correct flow stage

-- Update customers added by executives who don't have a proper flow stage
UPDATE customers 
SET current_flow_stage = 'REGISTERED' 
WHERE added_by_executive_id IS NOT NULL 
  AND registered_at IS NOT NULL
  AND (current_flow_stage IS NULL OR current_flow_stage = 'NEW');

-- Verify the update
SELECT 
    id, 
    name, 
    wa_phone_number, 
    current_flow_stage, 
    role, 
    added_by_executive_id,
    registered_at
FROM customers 
WHERE added_by_executive_id IS NOT NULL
ORDER BY registered_at DESC;
