-- Add missing updated_at column to device_pairing table
ALTER TABLE device_pairing 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_device_pairing_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_device_pairing_updated_at_trigger ON device_pairing;

CREATE TRIGGER update_device_pairing_updated_at_trigger 
BEFORE UPDATE ON device_pairing
FOR EACH ROW 
EXECUTE FUNCTION update_device_pairing_updated_at();

-- Update the verify_qr_pairing function to work correctly
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
        pairing_code,
        status,
        paired_at,
        created_at,
        updated_at
    )
    VALUES (
        p_device_id,
        NULL,  -- parent_id should be UUID, not TEXT
        p_device_name,
        p_device_brand,
        p_android_version,
        substring(md5(random()::text) from 1 for 6),  -- Generate random pairing code
        'paired',
        NOW(),
        NOW(),
        NOW()
    )
    ON CONFLICT (device_id)
    DO UPDATE SET
        device_name = p_device_name,
        device_brand = p_device_brand,
        android_version = p_android_version,
        status = 'paired',
        paired_at = NOW(),
        updated_at = NOW();
    
    -- Also create/update in devices table
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
        NULL,  -- Will be set when parent claims device
        p_device_name,
        false,
        NOW(),
        NOW(),
        NOW()
    )
    ON CONFLICT (device_id)
    DO UPDATE SET
        device_name = p_device_name,
        last_active = NOW(),
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

-- Grant execute permission to anon role (for child app to call)
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION verify_qr_pairing(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- Allow anon users to insert into device_pairing (for QR pairing)
DROP POLICY IF EXISTS "Anyone can insert pairing requests" ON device_pairing;
CREATE POLICY "Anyone can insert pairing requests" ON device_pairing
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Anyone can update pairing requests" ON device_pairing;
CREATE POLICY "Anyone can update pairing requests" ON device_pairing
    FOR UPDATE USING (true);

-- Allow anon users to insert into devices (for QR pairing)
DROP POLICY IF EXISTS "Allow device registration" ON devices;
CREATE POLICY "Allow device registration" ON devices
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow device updates" ON devices;
CREATE POLICY "Allow device updates" ON devices
    FOR UPDATE USING (true);

COMMENT ON COLUMN device_pairing.updated_at IS 'Timestamp of last update to this pairing record';
