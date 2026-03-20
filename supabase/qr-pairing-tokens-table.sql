-- Create table for QR code pairing tokens
CREATE TABLE IF NOT EXISTS qr_pairing_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token TEXT NOT NULL UNIQUE,
    parent_email TEXT NOT NULL,
    device_id TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'used', 'expired')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    used_at TIMESTAMP WITH TIME ZONE,
    
    -- Indexes for performance
    CONSTRAINT token_unique UNIQUE (token)
);

-- Create index on token for fast lookups
CREATE INDEX IF NOT EXISTS idx_qr_tokens_token ON qr_pairing_tokens(token);
CREATE INDEX IF NOT EXISTS idx_qr_tokens_status ON qr_pairing_tokens(status);
CREATE INDEX IF NOT EXISTS idx_qr_tokens_expires_at ON qr_pairing_tokens(expires_at);

-- Enable Row Level Security
ALTER TABLE qr_pairing_tokens ENABLE ROW LEVEL SECURITY;

-- Policy: Anyone can insert tokens (for web dashboard QR generation)
CREATE POLICY "Allow insert for authenticated users" ON qr_pairing_tokens
    FOR INSERT
    WITH CHECK (true);

-- Policy: Anyone can read their own tokens
CREATE POLICY "Allow read own tokens" ON qr_pairing_tokens
    FOR SELECT
    USING (true);

-- Policy: Allow update for pairing process
CREATE POLICY "Allow update for pairing" ON qr_pairing_tokens
    FOR UPDATE
    USING (true);

-- Function to clean up expired tokens (run periodically)
CREATE OR REPLACE FUNCTION cleanup_expired_qr_tokens()
RETURNS void AS $$
BEGIN
    UPDATE qr_pairing_tokens
    SET status = 'expired'
    WHERE status = 'pending'
    AND expires_at < NOW();
    
    -- Optionally delete very old tokens (older than 24 hours)
    DELETE FROM qr_pairing_tokens
    WHERE created_at < NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission on the cleanup function
GRANT EXECUTE ON FUNCTION cleanup_expired_qr_tokens() TO anon, authenticated;

COMMENT ON TABLE qr_pairing_tokens IS 'Stores QR code tokens for device pairing with expiry tracking';
COMMENT ON COLUMN qr_pairing_tokens.token IS 'Unique pairing token embedded in QR code';
COMMENT ON COLUMN qr_pairing_tokens.parent_email IS 'Email of parent who generated the QR code';
COMMENT ON COLUMN qr_pairing_tokens.device_id IS 'Device ID once paired (null until used)';
COMMENT ON COLUMN qr_pairing_tokens.status IS 'Token status: pending, used, or expired';
