# Monitoring Activities Fix - Quick Summary

## The Problem
Account binding works, but monitoring activities (calls, SMS, etc.) are NOT being logged to the database.

## Root Cause
RLS policy on `activities` table blocks anonymous inserts from the child app.

## The Fix (2 Minutes)
1. Open Supabase Dashboard → SQL Editor
2. Copy and run this SQL:

```sql
-- Drop old policy
DROP POLICY IF EXISTS "Allow activity insertion with parent_id" ON activities;

-- Create new policy
CREATE POLICY "Allow activity insertion for consented devices" ON activities
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = activities.device_id 
            AND devices.parent_id = activities.parent_id
            AND devices.consent_granted = true
        )
    );
```

3. Done! Activities will now be logged.

## Test It
1. Open child app
2. Make a call or send SMS
3. Check Supabase → activities table
4. Check web dashboard

## Files
- `supabase/fix-activities-rls.sql` - Complete fix with comments
- `FIX_MONITORING_ACTIVITIES.md` - Detailed explanation
- `ACCOUNT_BINDING_V2.0.0_DEPLOYED.md` - Deployment status

## Why It Works
- Old policy: Too restrictive, blocked anonymous inserts
- New policy: Allows inserts when device has consent and parent_id matches
- Security: Still validates device exists, has consent, and parent_id is correct
