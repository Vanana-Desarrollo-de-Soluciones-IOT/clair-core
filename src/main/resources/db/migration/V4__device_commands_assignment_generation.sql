-- Commands are bound to the assignment (pairing generation) they were issued under, so that
-- unlinking a device voids its outstanding commands and a late acknowledgement from a previous
-- owner cannot touch the next owner's assignment.
ALTER TABLE device_commands ADD COLUMN assignment_id uuid;
-- Attribute existing rows to whatever assignment currently holds the device.
UPDATE device_commands c SET assignment_id = a.id FROM device_assignments a WHERE a.device_id = c.device_id;
-- Outstanding commands with no live assignment can never be acknowledged safely.
UPDATE device_commands SET status = 'EXPIRED' WHERE assignment_id IS NULL AND status IN ('PENDING', 'SENT');
CREATE INDEX idx_device_commands_assignment ON device_commands (assignment_id);
