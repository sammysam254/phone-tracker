-- ============================================================================
-- CLEAR DEVICE PAIRING FUNCTION
-- ============================================================================
-- This function clears all activities and pairing records for a specific device
-- Used when the child app's "Re-Pair Device" button is clicked
-- ============================================================================

CREATE OR REPLACE FUNCTION clear_device_pairing(
    p_device_id TEXT,
    p_parent_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_deleted_activities INTEGER := 0;
    v_deleted_pairing INTEGER := 0;
BEGIN
    -- Delete all activities for this device/parent combination
    DELETE FROM activities
    WHERE device_id = p_device_id
    AND parent_id = p_parent_id;
    
    GET DIAGNOSTICS v_deleted_activities = ROW_COUNT;
    
    -- Delete device pairing record
    DELETE FROM device_pairing
    WHERE device_id = p_device_id
    AND parent_id = p_parent_id;
    
    GET DIAGNOSTICS v_deleted_pairing = ROW_COUNT;
    
    -- Return success with counts
    RETURN json_build_object(
        'success', true,
        'deleted_activities', v_deleted_activities,
        'deleted_pairing', v_deleted_pairing,
        'message', 'Device pairing cleared successfully'
    );
    
EXCEPTION WHEN OTHERS THEN
    -- Return error
    RETURN json_build_object(
        'success', false,
        'error', SQLERRM,
        'message', 'Failed to clear device pairing data'
    );
END;
$$;

-- Grant execute permission to authenticated and anonymous users
GRANT EXECUTE ON FUNCTION clear_device_pairing(TEXT, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION clear_device_pairing(TEXT, UUID) TO anon;

-- Add comment
COMMENT ON FUNCTION clear_device_pairing IS 'Clears all activities and pairing records for a device/parent combination to allow re-pairing';
