-- Fix Remaining RLS Policies for Account Binding v2.0.0
-- Run this if you already have some policies in place

-- ============================================================================
-- FIX 1: Remote Commands Table RLS (This is what's causing 401 errors)
-- ============================================================================

-- Drop old policies if they exist
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
-- FIX 2: Devices Table RLS (Allow anonymous binding operations)
-- ============================================================================

-- Drop old policies if they exist
DROP POLICY IF EXISTS "Parents can view their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can insert their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can update their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can delete their own devices" ON devices;
DROP POLICY IF EXISTS "Allow device binding via function" ON devices;
DROP POLICY IF EXISTS "Allow device updates for binding" ON devices;

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
-- FIX 3: Ensure Activities Policy Exists (Skip if already there)
-- ============================================================================

-- Check if the policy exists, if not create it
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'activities' 
        AND policyname = 'Parents can view their device activities'
    ) THEN
        CREATE POLICY "Parents can view their device activities" ON activities
            FOR SELECT USING (auth.uid() = parent_id);
    END IF;
END $$;

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Check all policies are in place
SELECT 
    tablename, 
    policyname,
    cmd as command,
    CASE 
        WHEN qual IS NOT NULL THEN 'Has USING clause'
        ELSE 'No USING clause'
    END as using_clause,
    CASE 
        WHEN with_check IS NOT NULL THEN 'Has WITH CHECK clause'
        ELSE 'No WITH CHECK clause'
    END as with_check_clause
FROM pg_policies 
WHERE tablename IN ('activities', 'remote_commands', 'devices')
ORDER BY tablename, policyname;

-- ============================================================================
-- SUCCESS MESSAGE
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '✅ RLS policies updated successfully!';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Test activity logging on child device';
    RAISE NOTICE '2. Test remote monitoring features in dashboard';
    RAISE NOTICE '3. Check for 401 errors (should be gone)';
END $$;
