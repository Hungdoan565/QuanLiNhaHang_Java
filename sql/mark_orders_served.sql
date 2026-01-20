-- Script to mark all order items as SERVED for orders that are ready
-- This prevents them from reappearing in the Kitchen Display System

-- Mark all READY items as SERVED for a specific order
-- Replace ORDER_ID with the actual order ID
UPDATE order_details 
SET status = 'SERVED' 
WHERE order_id = <ORDER_ID>;

-- Or mark ALL ready items across all orders as SERVED:
-- UPDATE order_details 
-- SET status = 'SERVED' 
-- WHERE status = 'READY';
