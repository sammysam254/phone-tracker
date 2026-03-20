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
    v_result JSON;
BEGIN
    -- Get parent name (in production, verify parent exists and token is valid)
    v_parent_name := 'Parent User';
    
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
        'paired',
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
        status = 'paired',
        paired_at = NOW(),
        updated_at = NOW();
    
    -- Build result JSON
    v_result := json_build_object(
        'success', true,
        'parent_name', v_parent_name,
        'device_id', p_device_id,
        'paired_at', NOW()
    );
    
    RETURN v_result;
END;
$$;

-- Grant execute permission to anon role
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;
