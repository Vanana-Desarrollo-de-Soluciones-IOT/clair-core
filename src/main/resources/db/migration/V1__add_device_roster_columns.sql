-- Core/edge roster migration (Plan 02, required before enabling edge HTTP sync).
-- Idempotent so it is safe to run during the additive cutover.
ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_devices_updated_at ON devices (updated_at);

-- Existing rows are retained as active devices. Tombstones are created only by
-- the application when a device is decommissioned.
