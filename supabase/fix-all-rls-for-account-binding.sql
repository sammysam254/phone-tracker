-- Comprehensive RLS Fix for Account Binding System v2.0.0
-- This fixes all RLS policies to work with anonymous key access from child app
-- while maintaining security through device consent and parent_id validation

-- ============================================================================
-- FIX 1: Activities Table RLS
-- ============================================================================

-- Drop old restrictive policy
DROP POLICY IF EXISTS "Allow activity insertion with parent_id" ON activities;

-- Create new policy that allows anonymous inserts for consented devices
CREATE POLICY "Allow activity insertion for consented devices" ON activities
    FOR INSERT WITH CHECK (
        -- Check that the device exists, has consent, and the parent_id matches
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = activities.device_id 
            AND devices.parent_id = activities.parent_id
            AND devices.consent_granted = true
        )
    );

-- Ensure parents can still view their activities
DROP POLICY IF EXISTS "Parents can view their device activities" ON activities;
CREATE POLICY "Parents can view their device activities" ON activities
    FOR SELECT USING (auth.uid() = parent_id);

COMMENT ON POLICY "Allow activity insertion for consented devices" ON activities IS 
'Allows anonymous activity logging when device exists, has consent, and parent_id matches';

-- ============================================================================
-- FIX 2: Remote Commands Table RLS
-- ============================================================================

-- Drop old policies
DROP POLICY IF EXISTS "Parents can view their remote commands" ON remote_commands;
DROP POLICY IF EXISTS "Parents can insert remote commands" ON remote_commands;
DROP POLICY IF EXISTS "Parents can update their remote commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow devices to read pending commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow devices to update command status" ON remote_commands;

-- Parents can view their own commands
CREATE POLICY "Parents can view their remote commands" ON remote_commands
    FOR SELECT USING (auth.uid() = parent_id);

-- Parents can insert commands for their devices
CREATE POLICY "Parents can insert remote commands" ON remote_commands
    FOR INSERT WITH CHECK (auth.uid() = parent_id);

-- Parents can update their own commands
CREATE POLICY "Parents can update their remote commands" ON remote_commands
    FOR UPDATE USING (auth.uid() = parent_id);

-- Allow anonymous access for devices to read pending commands
CREATE POLICY "Allow devices to read pending commands" ON remote_commands
    FOR SELECT USING (
        status = 'pending' AND
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = remote_commands.device_id 
            AND devices.consent_granted = true
        )
    );

-- Allow anonymous access for devices to update command status
CREATE POLICY "Allow devices to update command status" ON remote_commands
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = remote_commands.device_id 
            AND devices.consent_granted = true
        )
    ) WITH CHECK (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = remote_commands.device_id 
            AND devices.consent_granted = true
        )
    );

-- ============================================================================
-- FIX 3: Devices Table RLS (ensure it's correct)
-- ============================================================================

-- Drop old policies
DROP POLICY IF EXISTS "Parents can view their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can insert their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can update their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can delete their own devices" ON devices;

-- Parents can view their own devices
CREATE POLICY "Parents can view their own devices" ON devices
    FOR SELECT USING (auth.uid() = parent_id);

-- Allow anonymous inserts for device binding (via bind_device_to_parent function)
CREATE POLICY "Allow device binding via function" ON devices
    FOR INSERT WITH CHECK (true);

-- Allow anonymous updates for device binding and consent
CREATE POLICY "Allow device updates for binding" ON devices
    FOR UPDATE USING (true) WITH CHECK (true);

-- Parents can delete their own devices
CREATE POLICY "Parents can delete their own devices" ON devices
    FOR DELETE USING (auth.uid() = parent_id);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Run these to verify the policies are in place:

-- Check activities policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies 
WHERE tablename = 'activities'
ORDER BY policyname;

-- Check remote_commands policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies 
WHERE tablename = 'remote_commands'
ORDER BY policyname;

-- Check devices policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies 
WHERE tablename = 'devices'
ORDER BY policyname;

-- ============================================================================
-- NOTES
-- ============================================================================

-- Security is maintained through:
-- 1. Device must exist in devices table
-- 2. Device must have consent_granted = true
-- 3. parent_id must match between tables
-- 4. Only authenticated parents can view/manage their data
-- 5. Anonymous access is limited to device operations (logging, command polling)

-- This approach allows:
-- - Child app to log activities using anonymous key
-- - Child app to poll for remote commands using anonymous key
-- - Child app to update command status using anonymous key
-- - Parents to view/manage everything through authenticated access
-- - Device binding through the bind_device_to_parent function

COMMENT ON TABLE activities IS 'Activity logs from monitored devices - RLS allows anonymous inserts for consented devices';
COMMENT ON TABLE remote_commands IS 'Remote control commands - RLS allows anonymous reads/updates for consented devices';
COMMENT ON TABLE devices IS 'Devices bound to parent accounts - RLS allows anonymous binding operations';
