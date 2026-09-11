-- Old LocalTime values cannot supply a measurement date or offset. Keep recorded_at as the
-- best available historical measurement time and created_at as Core receipt time.
ALTER TABLE telemetry_evaluations ADD COLUMN reading_id uuid;
UPDATE telemetry_evaluations SET reading_id = id;
ALTER TABLE telemetry_evaluations ALTER COLUMN reading_id SET NOT NULL;
ALTER TABLE telemetry_evaluations ADD CONSTRAINT uq_telemetry_device_reading UNIQUE (device_id, reading_id);
-- Retain the legacy column for historical inspection, but new ingestion no longer writes it.
ALTER TABLE telemetry_evaluations ALTER COLUMN device_time DROP NOT NULL;
ALTER TABLE telemetry_evaluations ALTER COLUMN pm_pm1_0 TYPE double precision USING pm_pm1_0::double precision;
ALTER TABLE telemetry_evaluations ALTER COLUMN pm_pm2_5 TYPE double precision USING pm_pm2_5::double precision;
ALTER TABLE telemetry_evaluations ALTER COLUMN pm_pm10 TYPE double precision USING pm_pm10::double precision;
