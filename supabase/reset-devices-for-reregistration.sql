-- ============================================================================
-- RESET DEVICES FOR RE-REGISTRATION
-- ============================================================================
-- This script resets device registration status so devices will re-register
-- when they pair again. This is useful after updating the pairing function.
-- ============================================================================

BEGIN;

-- Mark all devices as not registered and reset consent
UPDATE devices
SET 
    consent_granted = FALSE,
    last_active = NOW() - INTERVAL '1 year',
    updated_at = NOW();

-- Mark all device pairings as inactive (but don't delete them)
UPDATE device_pairing
SET 
    status = 'inactive',
    updated_at = NOW();

-- Delete all QR tokens so new ones can be generated
DELETE FROM qr_pairing_tokens;

-- Delete all activities (optional - uncomment if you want to clear history)
-- DELETE FROM activities;

-- Delete all remote commands (optional - uncomment if you want to clear commands)
-- DELETE FROM remote_commands;

COMMIT;

-- Show summary of what was reset
SELECT 
    'Total devices marked for re-registration' as action,
    COUNT(*) as count
FROM devices
WHERE consent_granted = FALSE
UNION ALL
SELECT 
    'Total pairings marked inactive',
    COUNT(*)
FROM device_pairing
WHERE status = 'inactive'
UNION ALL
SELECT 
    'QR tokens deleted',
    0;

-- Show all devices that need re-registration
SELECT 
    d.device_id,
    d.device_name,
    au.email as parent_email,
    d.consent_granted,
    d.last_active,
    dp.status as pairing_status
FROM devices d
LEFT JOIN auth.users au ON d.parent_id = au.id
LEFT JOIN device_pairing dp ON d.device_id = dp.device_id
ORDER BY au.email, d.device_name;
