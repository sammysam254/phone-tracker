-- Device Lock Management Schema
-- Run this in Supabase SQL Editor

-- Create device_locks table
CREATE TABLE IF NOT EXISTS device_locks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT NOT NULL,
    is_locked BOOLEAN DEFAULT false,
    unlock_code TEXT,
    lock_message TEXT,
    locked_at TIMESTAMP WITH TIME ZONE,
    unlocked_at TIMESTAMP WITH TIME ZONE,
    locked_by UUID,
    unlock_code_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(device_id)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_device_locks_device_id ON device_locks(device_id);
CREATE INDEX IF NOT EXISTS idx_device_locks_is_locked ON device_locks(is_locked);

-- Enable RLS
ALTER TABLE device_locks ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can manage locks for their devices" ON device_locks;

-- RLS Policies
CREATE POLICY "Users can manage locks for their devices"
    ON device_locks
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM monitored_devices 
            WHERE monitored_devices.device_id = device_locks.device_id
            AND monitored_devices.parent_id = auth.uid()
        )
    );

-- Function to generate unlock code
CREATE OR REPLACE FUNCTION generate_unlock_code()
RETURNS TEXT AS $$
DECLARE
    code TEXT;
BEGIN
    -- Generate 6-digit code
    code := LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
    RETURN code;
END;
$$ LANGUAGE plpgsql;

-- Function to lock device
CREATE OR REPLACE FUNCTION lock_device(
    p_device_id TEXT,
    p_lock_message TEXT DEFAULT 'Device locked by parent'
)
RETURNS JSON AS $$
DECLARE
    v_unlock_code TEXT;
    v_lock_id UUID;
    v_result JSON;
BEGIN
    -- Generate unlock code
    v_unlock_code := generate_unlock_code();
    
    -- Insert or update lock record
    INSERT INTO device_locks (
        device_id,
        is_locked,
        unlock_code,
        lock_message,
        locked_at,
        locked_by,
        unlock_code_expires_at
    ) VALUES (
        p_device_id,
        true,
        v_unlock_code,
        p_lock_message,
        NOW(),
        auth.uid(),
        NOW() + INTERVAL '24 hours'
    )
    ON CONFLICT (device_id) 
    DO UPDATE SET
        is_locked = true,
        unlock_code = v_unlock_code,
        lock_message = p_lock_message,
        locked_at = NOW(),
        locked_by = auth.uid(),
        unlock_code_expires_at = NOW() + INTERVAL '24 hours',
        updated_at = NOW()
    RETURNING id INTO v_lock_id;
    
    -- Return result
    SELECT json_build_object(
        'success', true,
        'lock_id', v_lock_id,
        'unlock_code', v_unlock_code,
        'device_id', p_device_id,
        'locked_at', NOW()
    ) INTO v_result;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to unlock device
CREATE OR REPLACE FUNCTION unlock_device(
    p_device_id TEXT,
    p_unlock_code TEXT DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    v_lock_record RECORD;
    v_result JSON;
BEGIN
    -- Get current lock record
    SELECT * INTO v_lock_record
    FROM device_locks
    WHERE device_id = p_device_id
    AND is_locked = true;
    
    IF NOT FOUND THEN
        RETURN json_build_object(
            'success', false,
            'error', 'Device is not locked'
        );
    END IF;
    
    -- If unlock code provided, verify it
    IF p_unlock_code IS NOT NULL THEN
        IF v_lock_record.unlock_code != p_unlock_code THEN
            RETURN json_build_object(
                'success', false,
                'error', 'Invalid unlock code'
            );
        END IF;
        
        IF v_lock_record.unlock_code_expires_at < NOW() THEN
            RETURN json_build_object(
                'success', false,
                'error', 'Unlock code has expired'
            );
        END IF;
    END IF;
    
    -- Unlock device
    UPDATE device_locks
    SET is_locked = false,
        unlocked_at = NOW(),
        updated_at = NOW()
    WHERE device_id = p_device_id;
    
    -- Return result
    SELECT json_build_object(
        'success', true,
        'device_id', p_device_id,
        'unlocked_at', NOW()
    ) INTO v_result;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get device lock status
CREATE OR REPLACE FUNCTION get_device_lock_status(p_device_id TEXT)
RETURNS JSON AS $$
DECLARE
    v_lock_record RECORD;
    v_result JSON;
BEGIN
    SELECT * INTO v_lock_record
    FROM device_locks
    WHERE device_id = p_device_id
    ORDER BY created_at DESC
    LIMIT 1;
    
    IF NOT FOUND THEN
        RETURN json_build_object(
            'is_locked', false,
            'device_id', p_device_id
        );
    END IF;
    
    SELECT json_build_object(
        'is_locked', v_lock_record.is_locked,
        'device_id', p_device_id,
        'lock_message', v_lock_record.lock_message,
        'locked_at', v_lock_record.locked_at,
        'unlocked_at', v_lock_record.unlocked_at,
        'unlock_code_expires_at', v_lock_record.unlock_code_expires_at
    ) INTO v_result;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
