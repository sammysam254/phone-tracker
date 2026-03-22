-- Function to verify QR code pairing and create device pairing
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
    
    -- Get parent information from users table
    SELECT name, email INTO v_parent_name, v_parent_email
    FROM users
    WHERE id = p_parent_id;
    
    IF v_parent_name IS NULL THEN
        v_parent_name := COALESCE(v_parent_email, 'Parent User');
    END IF;
    
    -- Insert or update device pairing record
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
    
    -- Build result JSON
    v_result := json_build_object(
        'success', true,
        'parent_name', v_parent_name,
        'parent_email', v_parent_email,
        'device_id', p_device_id,
        'paired_at', NOW()
    );
    
    RETURN v_result;
END;
$$;

-- Grant execute permission to anon role
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;
