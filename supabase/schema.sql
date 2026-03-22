-- Create custom types
CREATE TYPE activity_type AS ENUM ('call', 'sms', 'app_usage', 'camera', 'mic', 'location', 'notification', 'screen_interaction', 'web_activity');

-- Device pairing table for secure parent-child connection
CREATE TABLE device_pairing (
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

-- Devices table
CREATE TABLE devices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) UNIQUE NOT NULL,
    parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    device_name VARCHAR(255) NOT NULL,
    consent_granted BOOLEAN DEFAULT FALSE,
    consent_timestamp TIMESTAMPTZ,
    last_active TIMESTAMPTZ DEFAULT NOW(),
    settings JSONB DEFAULT '{
        "monitorCalls": true,
        "monitorSMS": true,
        "monitorApps": true,
        "monitorCamera": true,
        "monitorMic": true,
        "monitorLocation": false
    }',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Activities table
CREATE TABLE activities (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) REFERENCES devices(device_id) ON DELETE CASCADE,
    activity_type activity_type NOT NULL,
    activity_data JSONB,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for better performance
CREATE INDEX idx_device_pairing_code ON device_pairing(pairing_code);
CREATE INDEX idx_device_pairing_device_id ON device_pairing(device_id);
CREATE INDEX idx_device_pairing_parent_id ON device_pairing(parent_id);
CREATE INDEX idx_devices_parent_id ON devices(parent_id);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_activities_device_id ON activities(device_id);
CREATE INDEX idx_activities_timestamp ON activities(timestamp);
CREATE INDEX idx_activities_type ON activities(activity_type);

-- Row Level Security Policies

-- Device pairing policies
ALTER TABLE device_pairing ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can insert pairing requests" ON device_pairing
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view their pairing requests" ON device_pairing
    FOR SELECT USING (auth.uid() = parent_id OR parent_id IS NULL);

CREATE POLICY "Parents can update pairing requests" ON device_pairing
    FOR UPDATE USING (auth.uid() = parent_id OR parent_id IS NULL);

-- Devices policies
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Parents can view their own devices" ON devices
    FOR SELECT USING (auth.uid() = parent_id);

CREATE POLICY "Parents can insert their own devices" ON devices
    FOR INSERT WITH CHECK (auth.uid() = parent_id);

CREATE POLICY "Parents can update their own devices" ON devices
    FOR UPDATE USING (auth.uid() = parent_id);

CREATE POLICY "Parents can delete their own devices" ON devices
    FOR DELETE USING (auth.uid() = parent_id);

-- Activities policies
ALTER TABLE activities ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Parents can view activities from their devices" ON activities
    FOR SELECT USING (
        device_id IN (
            SELECT device_id FROM devices WHERE parent_id = auth.uid()
        )
    );

CREATE POLICY "Allow activity insertion from devices" ON activities
    FOR INSERT WITH CHECK (
        device_id IN (
            SELECT device_id FROM devices WHERE consent_granted = true
        )
    );

-- Functions for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to get device statistics
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


-- Remote commands table for device control
CREATE TABLE remote_commands (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) REFERENCES devices(device_id) ON DELETE CASCADE,
    parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    command_type VARCHAR(100) NOT NULL,
    command_data JSONB DEFAULT '{}',
    status VARCHAR(50) DEFAULT 'pending',
    result TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

-- Indexes for remote commands
CREATE INDEX idx_remote_commands_device_id ON remote_commands(device_id);
CREATE INDEX idx_remote_commands_parent_id ON remote_commands(parent_id);
CREATE INDEX idx_remote_commands_status ON remote_commands(status);
CREATE INDEX idx_remote_commands_created_at ON remote_commands(created_at);

-- Remote commands policies
ALTER TABLE remote_commands ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Parents can view their remote commands" ON remote_commands
    FOR SELECT USING (auth.uid() = parent_id);

CREATE POLICY "Parents can insert remote commands" ON remote_commands
    FOR INSERT WITH CHECK (auth.uid() = parent_id);

CREATE POLICY "Parents can update their remote commands" ON remote_commands
    FOR UPDATE USING (auth.uid() = parent_id);

CREATE POLICY "Allow devices to read pending commands" ON remote_commands
    FOR SELECT USING (
        device_id IN (
            SELECT device_id FROM devices WHERE consent_granted = true
        ) AND status = 'pending'
    );

CREATE POLICY "Allow devices to update command status" ON remote_commands
    FOR UPDATE USING (
        device_id IN (
            SELECT device_id FROM devices WHERE consent_granted = true
        )
    );
