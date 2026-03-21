-- Fix Row Level Security Policies for Remote Commands and Activities

-- ============================================================================
-- REMOTE COMMANDS TABLE
-- ============================================================================

-- Create remote_commands table if it doesn't exist
CREATE TABLE IF NOT EXISTS remote_commands (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    parent_id UUID,
    command_type VARCHAR(50) NOT NULL,
    command_data JSONB,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    result JSONB
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_remote_commands_device_id ON remote_commands(device_id);
CREATE INDEX IF NOT EXISTS idx_remote_commands_status ON remote_commands(status);
CREATE INDEX IF NOT EXISTS idx_remote_commands_created_at ON remote_commands(created_at);

-- Enable RLS
ALTER TABLE remote_commands ENABLE ROW LEVEL SECURITY;

-- Drop existing policies
DROP POLICY IF EXISTS "Allow parents to insert commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow parents to view their commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow devices to view their commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow devices to update command status" ON remote_commands;
DROP POLICY IF EXISTS "Allow anon to insert commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow anon to view commands" ON remote_commands;
DROP POLICY IF EXISTS "Allow anon to update commands" ON remote_commands;

-- Create new permissive policies

-- Allow anyone (including anon) to insert commands
CREATE POLICY "Allow insert commands" ON remote_commands
    FOR INSERT
    WITH CHECK (true);

-- Allow anyone to view commands
CREATE POLICY "Allow view commands" ON remote_commands
    FOR SELECT
    USING (true);

-- Allow anyone to update commands
CREATE POLICY "Allow update commands" ON remote_commands
    FOR UPDATE
    USING (true);

-- Allow anyone to delete old commands
CREATE POLICY "Allow delete commands" ON remote_commands
    FOR DELETE
    USING (true);

-- ============================================================================
-- ACTIVITIES TABLE - Fix RLS
-- ============================================================================

-- Drop existing restrictive policies
DROP POLICY IF EXISTS "Parents can view activities from their devices" ON activities;
DROP POLICY IF EXISTS "Allow activity insertion from devices" ON activities;

-- Create new permissive policies

-- Allow anyone to insert activities
CREATE POLICY "Allow insert activities" ON activities
    FOR INSERT
    WITH CHECK (true);

-- Allow anyone to view activities
CREATE POLICY "Allow view activities" ON activities
    FOR SELECT
    USING (true);

-- ============================================================================
-- DEVICES TABLE - Fix RLS
-- ============================================================================

-- Drop existing restrictive policies
DROP POLICY IF EXISTS "Parents can view their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can insert their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can update their own devices" ON devices;
DROP POLICY IF EXISTS "Parents can delete their own devices" ON devices;

-- Create new permissive policies

-- Allow anyone to view devices
CREATE POLICY "Allow view devices" ON devices
    FOR SELECT
    USING (true);

-- Allow anyone to insert devices (already created in previous fix)
-- CREATE POLICY "Allow device registration" ON devices FOR INSERT WITH CHECK (true);

-- Allow anyone to update devices (already created in previous fix)
-- CREATE POLICY "Allow device updates" ON devices FOR UPDATE USING (true);

-- Allow anyone to delete devices
CREATE POLICY "Allow delete devices" ON devices
    FOR DELETE
    USING (true);

-- ============================================================================
-- DEVICE PAIRING TABLE - Ensure permissive policies
-- ============================================================================

-- These should already exist from previous fix, but ensure they're there
DROP POLICY IF EXISTS "Allow view pairing" ON device_pairing;
CREATE POLICY "Allow view pairing" ON device_pairing
    FOR SELECT
    USING (true);

-- ============================================================================
-- GRANT PERMISSIONS TO ANON ROLE
-- ============================================================================

-- Grant table permissions to anon role
GRANT ALL ON remote_commands TO anon;
GRANT ALL ON activities TO anon;
GRANT ALL ON devices TO anon;
GRANT ALL ON device_pairing TO anon;

-- Grant sequence permissions (for auto-increment IDs)
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon;

-- ============================================================================
-- CLEANUP FUNCTION FOR OLD COMMANDS
-- ============================================================================

-- Function to clean up old completed commands (optional)
CREATE OR REPLACE FUNCTION cleanup_old_commands()
RETURNS void AS $$
BEGIN
    DELETE FROM remote_commands
    WHERE status IN ('completed', 'failed')
    AND created_at < NOW() - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION cleanup_old_commands() TO anon;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE remote_commands IS 'Stores remote control commands sent from parent to child devices';
COMMENT ON POLICY "Allow insert commands" ON remote_commands IS 'Allows anyone to insert commands - needed for web dashboard and apps';
COMMENT ON POLICY "Allow view commands" ON remote_commands IS 'Allows anyone to view commands - needed for child app to fetch pending commands';
COMMENT ON POLICY "Allow update commands" ON remote_commands IS 'Allows anyone to update command status - needed for child app to mark as completed';

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Run these to verify the policies are working:
-- SELECT * FROM remote_commands LIMIT 1;
-- INSERT INTO remote_commands (device_id, command_type) VALUES ('test', 'test');
-- UPDATE remote_commands SET status = 'completed' WHERE id = (SELECT id FROM remote_commands LIMIT 1);
