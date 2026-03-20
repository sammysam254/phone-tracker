-- Update existing activity_type enum to include new types
ALTER TYPE activity_type ADD VALUE IF NOT EXISTS 'notification';
ALTER TYPE activity_type ADD VALUE IF NOT EXISTS 'screen_interaction';
ALTER TYPE activity_type ADD VALUE IF NOT EXISTS 'web_activity';
ALTER TYPE activity_type ADD VALUE IF NOT EXISTS 'keyboard_input';
ALTER TYPE activity_type ADD VALUE IF NOT EXISTS 'call_recording';
ALTER TYPE activity_type ADD VALUE IF NOT EXISTS 'emergency_alert';

-- Remote commands table for parent-initiated actions
CREATE TABLE IF NOT EXISTS remote_commands (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    command_type VARCHAR(50) NOT NULL, -- 'activate_camera', 'start_audio_monitoring', 'get_location', etc.
    command_data JSONB,
    status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'completed', 'failed'
    result TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

-- Storage buckets for media files
INSERT INTO storage.buckets (id, name, public) 
VALUES 
    ('monitoring-images', 'monitoring-images', true),
    ('monitoring-audio', 'monitoring-audio', true)
ON CONFLICT (id) DO NOTHING;

-- Device pairing table for secure parent-child connection
CREATE TABLE IF NOT EXISTS device_pairing (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) UNIQUE NOT NULL,
    pairing_code VARCHAR(6) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_brand VARCHAR(255),
    android_version VARCHAR(50),
    status VARCHAR(50) DEFAULT 'waiting_for_parent',
    parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    paired_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours'),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add new indexes for remote commands
CREATE INDEX IF NOT EXISTS idx_remote_commands_device_id ON remote_commands(device_id);
CREATE INDEX IF NOT EXISTS idx_remote_commands_parent_id ON remote_commands(parent_id);
CREATE INDEX IF NOT EXISTS idx_remote_commands_status ON remote_commands(status);
CREATE INDEX IF NOT EXISTS idx_device_pairing_code ON device_pairing(pairing_code);
CREATE INDEX IF NOT EXISTS idx_device_pairing_device_id ON device_pairing(device_id);
CREATE INDEX IF NOT EXISTS idx_device_pairing_parent_id ON device_pairing(parent_id);

-- Row Level Security for device pairing
ALTER TABLE device_pairing ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist and recreate
DROP POLICY IF EXISTS "Anyone can insert pairing requests" ON device_pairing;
DROP POLICY IF EXISTS "Parents can view their pairing requests" ON device_pairing;
DROP POLICY IF EXISTS "Parents can update pairing requests" ON device_pairing;

CREATE POLICY "Anyone can insert pairing requests" ON device_pairing
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view their pairing requests" ON device_pairing
    FOR SELECT USING (auth.uid() = parent_id OR parent_id IS NULL);

-- Row Level Security for remote commands
ALTER TABLE remote_commands ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Parents can manage their device commands" ON remote_commands
    FOR ALL USING (auth.uid() = parent_id);

CREATE POLICY "Devices can view and update their commands" ON remote_commands
    FOR SELECT USING (
        device_id IN (
            SELECT device_id FROM devices WHERE consent_granted = true
        )
    );

CREATE POLICY "Devices can update command status" ON remote_commands
    FOR UPDATE USING (
        device_id IN (
            SELECT device_id FROM devices WHERE consent_granted = true
        )
    );

-- Storage policies for media files
CREATE POLICY "Parents can view media from their devices" ON storage.objects
    FOR SELECT USING (
        bucket_id IN ('monitoring-images', 'monitoring-audio') AND
        (storage.foldername(name))[1] IN (
            SELECT device_id FROM devices WHERE parent_id = auth.uid()
        )
    );

CREATE POLICY "Devices can upload media files" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id IN ('monitoring-images', 'monitoring-audio')
    );

-- Update device statistics function
CREATE OR REPLACE FUNCTION get_device_stats(device_uuid VARCHAR)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_build_object(
        'callCount', COALESCE(SUM(CASE WHEN activity_type = 'call' THEN 1 ELSE 0 END), 0),
        'smsCount', COALESCE(SUM(CASE WHEN activity_type = 'sms' THEN 1 ELSE 0 END), 0),
        'appUsageCount', COALESCE(SUM(CASE WHEN activity_type = 'app_usage' THEN 1 ELSE 0 END), 0),
        'cameraCount', COALESCE(SUM(CASE WHEN activity_type = 'camera' THEN 1 ELSE 0 END), 0),
        'micCount', COALESCE(SUM(CASE WHEN activity_type = 'mic' THEN 1 ELSE 0 END), 0),
        'notificationCount', COALESCE(SUM(CASE WHEN activity_type = 'notification' THEN 1 ELSE 0 END), 0),
        'screenInteractionCount', COALESCE(SUM(CASE WHEN activity_type = 'screen_interaction' THEN 1 ELSE 0 END), 0),
        'webActivityCount', COALESCE(SUM(CASE WHEN activity_type = 'web_activity' THEN 1 ELSE 0 END), 0),
        'keyboardInputCount', COALESCE(SUM(CASE WHEN activity_type = 'keyboard_input' THEN 1 ELSE 0 END), 0),
        'callRecordingCount', COALESCE(SUM(CASE WHEN activity_type = 'call_recording' THEN 1 ELSE 0 END), 0),
        'locationCount', COALESCE(SUM(CASE WHEN activity_type = 'location' THEN 1 ELSE 0 END), 0),
        'totalActivities', COUNT(*)
    ) INTO result
    FROM activities 
    WHERE device_id = device_uuid 
    AND timestamp >= CURRENT_DATE;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to pair device with parent
CREATE OR REPLACE FUNCTION pair_device_with_parent(
    pairing_code_input VARCHAR(6),
    parent_user_id UUID
)
RETURNS JSON AS $$
DECLARE
    pairing_record RECORD;
    result JSON;
BEGIN
    -- Find the pairing request
    SELECT * INTO pairing_record
    FROM device_pairing 
    WHERE pairing_code = pairing_code_input 
    AND status = 'waiting_for_parent'
    AND expires_at > NOW();
    
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Invalid or expired pairing code');
    END IF;
    
    -- Update pairing record
    UPDATE device_pairing 
    SET parent_id = parent_user_id,
        status = 'paired',
        paired_at = NOW()
    WHERE id = pairing_record.id;
    
    -- Create device record
    INSERT INTO devices (
        device_id,
        parent_id,
        device_name,
        consent_granted,
        last_active
    ) VALUES (
        pairing_record.device_id,
        parent_user_id,
        pairing_record.device_name,
        false,
        NOW()
    ) ON CONFLICT (device_id) DO UPDATE SET
        parent_id = parent_user_id,
        device_name = pairing_record.device_name,
        last_active = NOW();
    
    RETURN json_build_object(
        'success', true, 
        'device_id', pairing_record.device_id,
        'device_name', pairing_record.device_name
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;