-- Migration Script: Upgrade to Account Binding System
-- This preserves existing data while updating schema

-- Step 1: Add parent_id to activities if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'activities' AND column_name = 'parent_id'
    ) THEN
        ALTER TABLE activities 
        ADD COLUMN parent_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Step 2: Populate parent_id in activities from devices table
UPDATE activities a
SET parent_id = d.parent_id
FROM devices d
WHERE a.device_id = d.device_id
AND a.parent_id IS NULL;

-- Step 3: Make parent_id required in activities
ALTER TABLE activities 
ALTER COLUMN parent_id SET NOT NULL;

-- Step 4: Update activities RLS policies
DROP POLICY IF EXISTS "Parents can view activities from their devices" ON activities;
DROP POLICY IF EXISTS "Allow activity insertion from devices" ON activities;

CREATE POLICY "Parents can view their device activities" ON activities
    FOR SELECT USING (auth.uid() = parent_id);

CREATE POLICY "Allow activity insertion with parent_id" ON activities
    FOR INSERT WITH CHECK (
        parent_id = (SELECT parent_id FROM devices WHERE device_id = activities.device_id LIMIT 1)
    );

-- Step 5: Create bind_device_to_parent function
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

-- Step 6: Add index on parent_id in activities
CREATE INDEX IF NOT EXISTS idx_activities_parent_id ON activities(parent_id);

-- Verification queries
SELECT 'Migration complete!' as status;
SELECT COUNT(*) as total_activities, COUNT(parent_id) as activities_with_parent_id FROM activities;
SELECT COUNT(*) as total_devices FROM devices;
