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
    
    -- Get parent information from Supabase auth schema
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
