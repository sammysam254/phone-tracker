-- Fix Activities RLS Policy for Account Binding System
-- The issue: Activities are not being logged because the RLS policy is too restrictive
-- The app uses anon key (not authenticated), so we need to allow anonymous inserts
-- when the device exists and has consent

-- Drop the old restrictive policy
DROP POLICY IF EXISTS "Allow activity insertion with parent_id" ON activities;

-- Create new policy that allows anonymous inserts for devices with consent
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

-- Also ensure parents can still view their activities
DROP POLICY IF EXISTS "Parents can view their device activities" ON activities;
CREATE POLICY "Parents can view their device activities" ON activities
    FOR SELECT USING (auth.uid() = parent_id);

COMMENT ON POLICY "Allow activity insertion for consented devices" ON activities IS 
'Allows anonymous activity logging when device exists, has consent, and parent_id matches';
