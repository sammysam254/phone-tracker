-- ============================================================================
-- RESET PAIRING DATA - Quick Reset Scripts
-- ============================================================================
-- Use these scripts to reset pairing data and start fresh
-- ============================================================================

-- ============================================================================
-- OPTION 1: RESET EVERYTHING (Nuclear Option)
-- ============================================================================
-- WARNING: This deletes ALL pairing data for ALL parents!
-- Only use this for testing or complete system reset
-- ============================================================================

-- Uncomment the block below to reset everything:
/*
BEGIN;

-- Delete all activities first (foreign key constraint)
DELETE FROM activities;
RAISE NOTICE '✅ Deleted all activities';

-- Delete all remote commands
DELETE FROM remote_commands;
RAISE NOTICE '✅ Deleted all remote commands';

-- Delete all device locks
DELETE FROM device_locks WHERE TRUE;
RAISE NOTICE '✅ Deleted all device locks';

-- Delete all devices
DELETE FROM devices;
RAISE NOTICE '✅ Deleted all devices';

-- Delete all device pairings
DELETE FROM device_pairing;
RAISE NOTICE '✅ Deleted all device pairings';

-- Delete all QR tokens
DELETE FROM qr_pairing_tokens;
RAISE NOTICE '✅ Deleted all QR tokens';

COMMIT;

SELECT '🎉 ALL PAIRING DATA RESET COMPLETE!' as status;
*/

-- ============================================================================
-- OPTION 2: RESET FOR SPECIFIC PARENT (Recommended)
-- ============================================================================
-- This only affects one parent account - much safer!
-- Replace 'your-email@example.com' with the actual parent email
-- ============================================================================

-- Uncomment and modify the block below:
/*
DO $$
DECLARE
    v_parent_id UUID;
    v_parent_email TEXT := 'your-email@example.com'; -- ⚠️ CHANGE THIS!
    v_device_ids TEXT[];
BEGIN
    -- Get parent ID from Supabase auth.users
    SELECT id INTO v_parent_id 
    FROM auth.users 
    WHERE email = v_parent_email;
    
    IF v_parent_id IS NULL THEN
        RAISE EXCEPTION '❌ Parent not found with email: %', v_parent_email;
    END IF;
    
    RAISE NOTICE '🔄 Resetting pairing data for: % (ID: %)', v_parent_email, v_parent_id;
    
    -- Get all device IDs for this parent
    SELECT ARRAY_AGG(device_id) INTO v_device_ids
    FROM devices
    WHERE parent_id = v_parent_id;
    
    -- Delete activities for this parent's devices
    IF v_device_ids IS NOT NULL THEN
        DELETE FROM activities WHERE device_id = ANY(v_device_ids);
        RAISE NOTICE '✅ Deleted activities for % devices', array_length(v_device_ids, 1);
    END IF;
    
    -- Delete remote commands
    DELETE FROM remote_commands WHERE parent_id = v_parent_id;
    RAISE NOTICE '✅ Deleted remote commands';
    
    -- Delete device locks
    DELETE FROM device_locks WHERE device_id = ANY(v_device_ids);
    RAISE NOTICE '✅ Deleted device locks';
    
    -- Delete devices
    DELETE FROM devices WHERE parent_id = v_parent_id;
    RAISE NOTICE '✅ Deleted devices';
    
    -- Delete device pairings
    DELETE FROM device_pairing WHERE parent_id = v_parent_id::TEXT;
    RAISE NOTICE '✅ Deleted device pairings';
    
    -- Delete QR tokens
    DELETE FROM qr_pairing_tokens WHERE parent_id = v_parent_id::TEXT;
    RAISE NOTICE '✅ Deleted QR tokens';
    
    RAISE NOTICE '🎉 Pairing reset complete for: %', v_parent_email;
    RAISE NOTICE '📱 You can now pair devices again!';
END $$;
*/

-- ============================================================================
-- OPTION 3: CLEAN UP DUPLICATES (Keep Latest Device Only)
-- ============================================================================
-- This removes old/duplicate devices but keeps the most recent one
-- Safe to run - won't delete current active devices
-- ============================================================================

-- Uncomment to clean up duplicates:
/*
DO $$
DECLARE
    v_parent_id TEXT;
    v_latest_device_id TEXT;
    v_old_count INTEGER;
BEGIN
    RAISE NOTICE '🧹 Cleaning up duplicate devices...';
    
    -- For each parent with multiple active devices
    FOR v_parent_id IN 
        SELECT parent_id 
        FROM device_pairing 
        WHERE status = 'active'
        GROUP BY parent_id 
        HAVING COUNT(*) > 1
    LOOP
        -- Get the most recent device
        SELECT device_id INTO v_latest_device_id
        FROM device_pairing
        WHERE parent_id = v_parent_id AND status = 'active'
        ORDER BY paired_at DESC
        LIMIT 1;
        
        -- Mark old devices as replaced
        UPDATE device_pairing
        SET status = 'replaced', updated_at = NOW()
        WHERE parent_id = v_parent_id 
        AND status = 'active'
        AND device_id != v_latest_device_id;
        
        GET DIAGNOSTICS v_old_count = ROW_COUNT;
        
        -- Mark old devices as inactive
        UPDATE devices
        SET last_active = NOW() - INTERVAL '1 year'
        WHERE parent_id = v_parent_id::UUID
        AND device_id != v_latest_device_id;
        
        RAISE NOTICE '✅ Parent %: Kept %, removed % old devices', 
            v_parent_id, v_latest_device_id, v_old_count;
    END LOOP;
    
    RAISE NOTICE '🎉 Duplicate cleanup complete!';
END $$;
*/

-- ============================================================================
-- VERIFICATION QUERIES (Safe to run anytime)
-- ============================================================================

-- View all current pairings
SELECT 
    au.email as parent_email,
    dp.device_id,
    dp.device_name,
    dp.status,
    dp.paired_at,
    d.last_active
FROM device_pairing dp
JOIN auth.users au ON dp.parent_id = au.id::TEXT
LEFT JOIN devices d ON d.device_id = dp.device_id
ORDER BY au.email, dp.paired_at DESC;

-- Check for duplicate active devices
SELECT 
    au.email as parent_email,
    COUNT(*) as active_device_count,
    STRING_AGG(dp.device_id, ', ') as device_ids
FROM device_pairing dp
JOIN auth.users au ON dp.parent_id = au.id::TEXT
WHERE dp.status = 'active'
GROUP BY au.email
HAVING COUNT(*) > 1;

-- Count records by table
SELECT 
    'device_pairing' as table_name, 
    COUNT(*) as record_count 
FROM device_pairing
UNION ALL
SELECT 'devices', COUNT(*) FROM devices
UNION ALL
SELECT 'activities', COUNT(*) FROM activities
UNION ALL
SELECT 'qr_pairing_tokens', COUNT(*) FROM qr_pairing_tokens
UNION ALL
SELECT 'remote_commands', COUNT(*) FROM remote_commands;

