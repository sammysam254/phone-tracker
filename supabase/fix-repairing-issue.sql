-- ============================================================================
-- FIX RE-PAIRING ISSUE - Update device records instead of creating duplicates
-- ============================================================================
-- This script fixes the issue where rescanning QR code creates new device
-- records instead of updating existing ones for the same parent.
-- ============================================================================

-- Drop existing function
DROP FUNCTION IF EXISTS verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT);

-- Create improved function that handles re-pairing correctly
CREATE OR REPLACE FUNCTION verify_qr_pairing(
    p_parent_id TEXT,
    p_pairing_token TEXT,
    p_device_id TEXT,
    p_device_name TEXT,
    p_device_brand TEXT,
    p_android_version TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_parent_name TEXT;
    v_parent_email TEXT;
    v_token_valid BOOLEAN;
    v_old_device_id TEXT;
    v_result JSON;
BEGIN
    -- Verify the pairing token is valid and not expired
    SELECT EXISTS (
        SELECT 1 FROM qr_pairing_tokens
        WHERE parent_id = p_parent_id
        AND token = p_pairing_token
        AND expires_at > NOW()
        AND used = FALSE
    ) INTO v_token_valid;
    
    IF NOT v_token_valid THEN
        RAISE EXCEPTION 'Invalid or expired pairing token';
    END IF;
    
    -- Mark token as used
    UPDATE qr_pairing_tokens
    SET used = TRUE, used_at = NOW()
    WHERE parent_id = p_parent_id AND token = p_pairing_token;
    
    -- Get parent information from users table (Supabase auth schema)
    SELECT 
        COALESCE(raw_user_meta_data->>'name', email) as name,
        email 
    INTO v_parent_name, v_parent_email
    FROM auth.users
    WHERE id = p_parent_id::UUID;
    
    IF v_parent_name IS NULL THEN
        v_parent_name := COALESCE(v_parent_email, 'Parent User');
    END IF;
    
    -- CRITICAL FIX: Check if this parent already has a paired device
    -- If yes, we need to UPDATE that device's ID instead of creating a new one
    SELECT device_id INTO v_old_device_id
    FROM device_pairing
    WHERE parent_id = p_parent_id
    AND status IN ('active', 'paired')
    ORDER BY paired_at DESC
    LIMIT 1;
    
    -- If old device exists and has different ID, mark it as replaced
    IF v_old_device_id IS NOT NULL AND v_old_device_id != p_device_id THEN
        RAISE NOTICE 'Replacing old device % with new device %', v_old_device_id, p_device_id;
        
        -- Mark old device as replaced in device_pairing
        UPDATE device_pairing
        SET status = 'replaced',
            updated_at = NOW()
        WHERE device_id = v_old_device_id
        AND parent_id = p_parent_id;
        
        -- Mark old device as inactive in devices table
        UPDATE devices
        SET last_active = NOW() - INTERVAL '1 year',
            updated_at = NOW()
        WHERE device_id = v_old_device_id
        AND parent_id = p_parent_id::UUID;
    END IF;
    
    -- Insert or update device pairing record with new device ID
    INSERT INTO device_pairing (
        device_id,
        parent_id,
        device_name,
        device_brand,
        android_version,
        status,
        paired_at,
        created_at,
        updated_at
    )
    VALUES (
        p_device_id,
        p_parent_id,
        p_device_name,
        p_device_brand,
        p_android_version,
        'active',
        NOW(),
        NOW(),
        NOW()
    )
    ON CONFLICT (device_id)
    DO UPDATE SET
        parent_id = p_parent_id,
        device_name = p_device_name,
        device_brand = p_device_brand,
        android_version = p_android_version,
        status = 'active',
        paired_at = NOW(),
        updated_at = NOW();
    
    -- CRITICAL: Also create/update device record in devices table for monitoring
    INSERT INTO devices (
        device_id,
        parent_id,
        device_name,
        consent_granted,
        last_active,
        created_at,
        updated_at
    )
    VALUES (
        p_device_id,
        p_parent_id::UUID,
        p_device_name,
        FALSE,
        NOW(),
        NOW(),
        NOW()
    )
    ON CONFLICT (device_id)
    DO UPDATE SET
        parent_id = p_parent_id::UUID,
        device_name = p_device_name,
        last_active = NOW(),
        updated_at = NOW();
    
    -- Build result JSON
    v_result := json_build_object(
        'success', true,
        'parent_name', v_parent_name,
        'parent_email', v_parent_email,
        'device_id', p_device_id,
        'old_device_id', v_old_device_id,
        'paired_at', NOW()
    );
    
    RETURN v_result;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- ============================================================================
-- CLEANUP SCRIPTS
-- ============================================================================

-- Script 1: Reset ALL pairing data (USE WITH CAUTION - DELETES EVERYTHING)
-- Uncomment to use:
/*
DO $$
BEGIN
    RAISE NOTICE 'Resetting all pairing data...';
    
    -- Delete all device pairings
    DELETE FROM device_pairing;
    RAISE NOTICE 'Deleted all device_pairing records';
    
    -- Delete all devices
    DELETE FROM devices;
    RAISE NOTICE 'Deleted all devices records';
    
    -- Delete all activities
    DELETE FROM activities;
    RAISE NOTICE 'Deleted all activities records';
    
    -- Delete all QR tokens
    DELETE FROM qr_pairing_tokens;
    RAISE NOTICE 'Deleted all qr_pairing_tokens';
    
    -- Delete all remote commands
    DELETE FROM remote_commands;
    RAISE NOTICE 'Deleted all remote_commands';
    
    -- Delete all device locks
    DELETE FROM device_locks;
    RAISE NOTICE 'Deleted all device_locks';
    
    RAISE NOTICE 'All pairing data reset complete!';
END $$;
*/

-- Script 2: Reset pairing for a specific parent (SAFER - only affects one parent)
-- Replace 'PARENT_EMAIL_HERE' with actual parent email
-- Uncomment to use:
/*
DO $$
DECLARE
    v_parent_id UUID;
    v_parent_email TEXT := 'PARENT_EMAIL_HERE'; -- CHANGE THIS
BEGIN
    -- Get parent ID
    SELECT id INTO v_parent_id FROM auth.users WHERE email = v_parent_email;
    
    IF v_parent_id IS NULL THEN
        RAISE EXCEPTION 'Parent not found with email: %', v_parent_email;
    END IF;
    
    RAISE NOTICE 'Resetting pairing data for parent: % (ID: %)', v_parent_email, v_parent_id;
    
    -- Delete device pairings for this parent
    DELETE FROM device_pairing WHERE parent_id = v_parent_id::TEXT;
    RAISE NOTICE 'Deleted device_pairing records';
    
    -- Delete devices for this parent
    DELETE FROM devices WHERE parent_id = v_parent_id;
    RAISE NOTICE 'Deleted devices records';
    
    -- Delete activities for this parent's devices
    DELETE FROM activities WHERE device_id IN (
        SELECT device_id FROM devices WHERE parent_id = v_parent_id
    );
    RAISE NOTICE 'Deleted activities records';
    
    -- Delete QR tokens for this parent
    DELETE FROM qr_pairing_tokens WHERE parent_id = v_parent_id::TEXT;
    RAISE NOTICE 'Deleted qr_pairing_tokens';
    
    -- Delete remote commands for this parent
    DELETE FROM remote_commands WHERE parent_id = v_parent_id;
    RAISE NOTICE 'Deleted remote_commands';
    
    RAISE NOTICE 'Pairing reset complete for parent: %', v_parent_email;
END $$;
*/

-- Script 3: Clean up duplicate/old devices for all parents
-- This keeps only the most recent device for each parent
-- Uncomment to use:
/*
DO $$
DECLARE
    v_parent_id TEXT;
    v_latest_device_id TEXT;
    v_old_device_ids TEXT[];
BEGIN
    RAISE NOTICE 'Cleaning up duplicate devices...';
    
    -- For each parent, keep only the most recent device
    FOR v_parent_id IN 
        SELECT DISTINCT parent_id FROM device_pairing WHERE status = 'active'
    LOOP
        -- Get the most recent device for this parent
        SELECT device_id INTO v_latest_device_id
        FROM device_pairing
        WHERE parent_id = v_parent_id AND status = 'active'
        ORDER BY paired_at DESC
        LIMIT 1;
        
        -- Get all other devices for this parent
        SELECT ARRAY_AGG(device_id) INTO v_old_device_ids
        FROM device_pairing
        WHERE parent_id = v_parent_id 
        AND status = 'active'
        AND device_id != v_latest_device_id;
        
        -- Mark old devices as replaced
        IF v_old_device_ids IS NOT NULL THEN
            UPDATE device_pairing
            SET status = 'replaced', updated_at = NOW()
            WHERE device_id = ANY(v_old_device_ids);
            
            UPDATE devices
            SET last_active = NOW() - INTERVAL '1 year', updated_at = NOW()
            WHERE device_id = ANY(v_old_device_ids);
            
            RAISE NOTICE 'Parent %: Kept device %, marked % old devices as replaced', 
                v_parent_id, v_latest_device_id, array_length(v_old_device_ids, 1);
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Duplicate cleanup complete!';
END $$;
*/

-- Script 4: View current pairing status for all parents
SELECT 
    au.email as parent_email,
    dp.device_id,
    dp.device_name,
    dp.status,
    dp.paired_at,
    d.last_active,
    CASE 
        WHEN d.last_active > NOW() - INTERVAL '1 hour' THEN 'Active'
        WHEN d.last_active > NOW() - INTERVAL '1 day' THEN 'Recent'
        WHEN d.last_active > NOW() - INTERVAL '1 week' THEN 'Inactive'
        ELSE 'Very Old'
    END as activity_status
FROM device_pairing dp
JOIN auth.users au ON au.id::TEXT = dp.parent_id
LEFT JOIN devices d ON d.device_id = dp.device_id
WHERE dp.status IN ('active', 'paired')
ORDER BY au.email, dp.paired_at DESC;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check for duplicate active devices per parent
SELECT 
    parent_id,
    COUNT(*) as device_count,
    STRING_AGG(device_id, ', ') as device_ids
FROM device_pairing
WHERE status = 'active'
GROUP BY parent_id
HAVING COUNT(*) > 1;

-- Check device pairing history
SELECT 
    device_id,
    parent_id,
    device_name,
    status,
    paired_at,
    updated_at
FROM device_pairing
ORDER BY parent_id, paired_at DESC;

