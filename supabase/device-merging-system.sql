-- Device Merging System for Account Binding v2.0.0
-- Handles cases where a device gets a new device_id but should be merged with existing device

-- Function to find and merge devices based on hardware fingerprint
CREATE OR REPLACE FUNCTION find_and_merge_device(
    new_device_id VARCHAR(255),
    parent_user_id UUID,
    device_name_input VARCHAR(255),
    device_brand_input VARCHAR(255) DEFAULT NULL,
    android_version_input VARCHAR(50) DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    existing_device_record RECORD;
    old_device_id VARCHAR(255);
    merge_result JSON;
    activity_count INTEGER;
BEGIN
    -- Look for existing device with same hardware fingerprint for this parent
    -- Match by device_brand + device_name (same physical device)
    SELECT * INTO existing_device_record
    FROM devices 
    WHERE parent_id = parent_user_id
    AND device_brand = device_brand_input
    AND device_name = device_name_input
    AND device_id != new_device_id  -- Different device_id
    ORDER BY last_active DESC
    LIMIT 1;
    
    -- If we found a matching device, merge it
    IF existing_device_record IS NOT NULL THEN
        old_device_id := existing_device_record.device_id;
        
        -- Count activities that will be merged
        SELECT COUNT(*) INTO activity_count
        FROM activities 
        WHERE device_id = old_device_id;
        
        -- Update the existing device record with new device_id
        UPDATE devices 
        SET 
            device_id = new_device_id,
            android_version = COALESCE(android_version_input, android_version),
            last_active = NOW(),
            updated_at = NOW()
        WHERE id = existing_device_record.id;
        
        -- Update all activities to use new device_id
        UPDATE activities 
        SET device_id = new_device_id
        WHERE device_id = old_device_id;
        
        -- Update all remote commands to use new device_id
        UPDATE remote_commands 
        SET device_id = new_device_id
        WHERE device_id = old_device_id;
        
        -- Return merge success result
        RETURN json_build_object(
            'success', true,
            'merged', true,
            'device_id', new_device_id,
            'old_device_id', old_device_id,
            'device_name', device_name_input,
            'parent_id', parent_user_id,
            'activities_merged', activity_count,
            'message', 'Device merged successfully with existing record'
        );
    ELSE
        -- No existing device found, create new one
        INSERT INTO devices (
            device_id,
            parent_id,
            device_name,
            device_brand,
            android_version,
            consent_granted,
            last_active
        ) VALUES (
            new_device_id,
            parent_user_id,
            device_name_input,
            device_brand_input,
            android_version_input,
            false,
            NOW()
        );
        
        -- Return new device result
        RETURN json_build_object(
            'success', true,
            'merged', false,
            'device_id', new_device_id,
            'device_name', device_name_input,
            'parent_id', parent_user_id,
            'activities_merged', 0,
            'message', 'New device created successfully'
        );
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        -- If merge fails, fall back to regular binding
        RETURN json_build_object(
            'success', false,
            'merged', false,
            'error', SQLERRM,
            'message', 'Device merge failed, will use fallback binding'
        );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get device merge history for troubleshooting
CREATE OR REPLACE FUNCTION get_device_merge_history(parent_user_id UUID)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_agg(
        json_build_object(
            'device_id', device_id,
            'device_name', device_name,
            'device_brand', device_brand,
            'android_version', android_version,
            'consent_granted', consent_granted,
            'last_active', last_active,
            'created_at', created_at,
            'updated_at', updated_at,
            'activity_count', (
                SELECT COUNT(*) FROM activities 
                WHERE activities.device_id = devices.device_id
            )
        )
    ) INTO result
    FROM devices 
    WHERE parent_id = parent_user_id
    ORDER BY last_active DESC;
    
    RETURN COALESCE(result, '[]'::json);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to manually merge two devices (for admin use)
CREATE OR REPLACE FUNCTION manual_device_merge(
    keep_device_id VARCHAR(255),
    merge_device_id VARCHAR(255),
    parent_user_id UUID
)
RETURNS JSON AS $$
DECLARE
    activity_count INTEGER;
    command_count INTEGER;
BEGIN
    -- Verify both devices belong to the same parent
    IF NOT EXISTS (
        SELECT 1 FROM devices 
        WHERE device_id = keep_device_id AND parent_id = parent_user_id
    ) OR NOT EXISTS (
        SELECT 1 FROM devices 
        WHERE device_id = merge_device_id AND parent_id = parent_user_id
    ) THEN
        RETURN json_build_object(
            'success', false,
            'error', 'One or both devices not found for this parent'
        );
    END IF;
    
    -- Count what will be merged
    SELECT COUNT(*) INTO activity_count
    FROM activities WHERE device_id = merge_device_id;
    
    SELECT COUNT(*) INTO command_count
    FROM remote_commands WHERE device_id = merge_device_id;
    
    -- Merge activities
    UPDATE activities 
    SET device_id = keep_device_id
    WHERE device_id = merge_device_id;
    
    -- Merge remote commands
    UPDATE remote_commands 
    SET device_id = keep_device_id
    WHERE device_id = merge_device_id;
    
    -- Delete the merged device record
    DELETE FROM devices WHERE device_id = merge_device_id;
    
    -- Update the kept device's last_active
    UPDATE devices 
    SET last_active = NOW(), updated_at = NOW()
    WHERE device_id = keep_device_id;
    
    RETURN json_build_object(
        'success', true,
        'kept_device_id', keep_device_id,
        'merged_device_id', merge_device_id,
        'activities_merged', activity_count,
        'commands_merged', command_count
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Add index for faster device merging lookups
CREATE INDEX IF NOT EXISTS idx_devices_parent_brand_name 
ON devices(parent_id, device_brand, device_name);

-- Add index for device merge history
CREATE INDEX IF NOT EXISTS idx_devices_last_active 
ON devices(parent_id, last_active DESC);

COMMENT ON FUNCTION find_and_merge_device IS 
'Automatically merges devices with same hardware fingerprint for seamless re-binding';

COMMENT ON FUNCTION get_device_merge_history IS 
'Returns device history for a parent to troubleshoot merging issues';

COMMENT ON FUNCTION manual_device_merge IS 
'Manually merge two devices - for admin use when automatic merge fails';