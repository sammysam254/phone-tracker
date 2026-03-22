-- ============================================================================
-- RESET ALL DEVICE PAIRINGS - Start Fresh
-- ============================================================================
-- This script deletes all device pairing data so devices can pair again
-- WARNING: This will delete ALL pairing data for ALL parents!
-- ============================================================================

BEGIN;

-- Delete all activities first (foreign key constraint)
DELETE FROM activities;

-- Delete all remote commands
DELETE FROM remote_commands;

-- Delete all device locks
DELETE FROM device_locks;

-- Delete all devices
DELETE FROM devices;

-- Delete all device pairings
DELETE FROM device_pairing;

-- Delete all QR tokens
DELETE FROM qr_pairing_tokens;

COMMIT;

-- Show summary
SELECT 
    'device_pairing' as table_name, 
    COUNT(*) as remaining_records 
FROM device_pairing
UNION ALL
SELECT 'devices', COUNT(*) FROM devices
UNION ALL
SELECT 'activities', COUNT(*) FROM activities
UNION ALL
SELECT 'qr_pairing_tokens', COUNT(*) FROM qr_pairing_tokens
UNION ALL
SELECT 'remote_commands', COUNT(*) FROM remote_commands
UNION ALL
SELECT 'device_locks', COUNT(*) FROM device_locks;
