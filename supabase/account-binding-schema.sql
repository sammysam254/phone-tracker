-- Account Binding System Schema
-- Replaces complex pairing system with simple account-based device binding
-- Parents log into child app with their credentials to bind the device

-- Drop old pairing tables (backup first if needed)
DROP TABLE IF EXISTS device_pairing CASCADE;
DROP TABLE IF EXISTS remote_commands CASCADE;

-- Simplified devices table - directly linked to parent account
DROP TABLE IF EXISTS devices CASCADE;
CREATE TABLE devices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) UNIQUE NOT NULL,
    parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_brand VARCHAR(255),
    android_version VARCHAR(50),
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

-- Activities table (unchanged structure, but simplified)
DROP TABLE IF EXISTS activities CASCADE;
CREATE TABLE activities (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) REFERENCES devices(device_id) ON DELETE CASCADE,
    parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    activity_type activity_type NOT NULL,
    activity_data JSONB,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

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

-- Indexes for better performance
CREATE INDEX idx_devices_parent_id ON devices(parent_id);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_activities_device_id ON activities(device_id);
CREATE INDEX idx_activities_parent_id ON activities(parent_id);
CREATE INDEX idx_activities_timestamp ON activities(timestamp);
CREATE INDEX idx_activities_type ON activities(activity_type);
CREATE INDEX idx_remote_commands_device_id ON remote_commands(device_id);
CREATE INDEX idx_remote_commands_parent_id ON remote_commands(parent_id);
CREATE INDEX idx_remote_commands_status ON remote_commands(status);

-- Row Level Security Policies

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

CREATE POLICY "Parents can view their device activities" ON activities
    FOR SELECT USING (auth.uid() = parent_id);

CREATE POLICY "Allow activity insertion with parent_id" ON activities
    FOR INSERT WITH CHECK (
        parent_id = (SELECT parent_id FROM devices WHERE device_id = activities.device_id LIMIT 1)
    );

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

-- Function for updated_at timestamp
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

-- Function to bind device to parent account (called when parent logs in on child device)
CREATE OR REPLACE FUNCTION bind_device_to_parent(
    device_id_input VARCHAR(255),
    parent_user_id UUID,
    device_name_input VARCHAR(255),
    device_brand_input VARCHAR(255) DEFAULT NULL,
    android_version_input VARCHAR(50) DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    -- Insert or update device record
    INSERT INTO devices (
        device_id,
        parent_id,
        device_name,
        device_brand,
        android_version,
        consent_granted,
        last_active
    ) VALUES (
        device_id_input,
        parent_user_id,
        device_name_input,
        device_brand_input,
        android_version_input,
        false,
        NOW()
    ) ON CONFLICT (device_id) DO UPDATE SET
        parent_id = parent_user_id,
        device_name = device_name_input,
        device_brand = COALESCE(device_brand_input, devices.device_brand),
        android_version = COALESCE(android_version_input, devices.android_version),
        last_active = NOW();
    
    RETURN json_build_object(
        'success', true, 
        'device_id', device_id_input,
        'device_name', device_name_input,
        'parent_id', parent_user_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON TABLE devices IS 'Devices bound to parent accounts via login';
COMMENT ON FUNCTION bind_device_to_parent IS 'Binds a device to a parent account when parent logs in on child app';
