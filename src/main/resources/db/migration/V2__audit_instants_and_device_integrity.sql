-- Interpret legacy wall-clock audit timestamps using the old deployment timezone.
-- Set LEGACY_AUDIT_ZONE before adopting Flyway on an existing installation.
ALTER TABLE alerts ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE device_analytics_snapshots ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE device_assignments ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE device_commands ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE device_daily_summaries ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE device_monthly_summaries ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE devices ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE email_logs ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE organizations ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE payment_record ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE push_notification_logs ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE spaces ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE telemetry_evaluations ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE user_plan ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';
ALTER TABLE users ALTER COLUMN created_at TYPE timestamp(6) with time zone USING created_at AT TIME ZONE '${legacyAuditZone}', ALTER COLUMN updated_at TYPE timestamp(6) with time zone USING updated_at AT TIME ZONE '${legacyAuditZone}';

-- Fail on orphan rows rather than silently deleting user data.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'device_id'
        WHERE c.contype = 'f' AND c.conrelid = 'device_assignments'::regclass
          AND c.confrelid = 'devices'::regclass AND c.conkey = ARRAY[a.attnum]
    ) THEN
        ALTER TABLE device_assignments ADD CONSTRAINT fk_device_assignments_device FOREIGN KEY (device_id) REFERENCES devices(id);
    END IF;
END $$;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'device_id'
        WHERE c.contype = 'f' AND c.conrelid = 'device_commands'::regclass
          AND c.confrelid = 'devices'::regclass AND c.conkey = ARRAY[a.attnum]
    ) THEN
        ALTER TABLE device_commands ADD CONSTRAINT fk_device_commands_device FOREIGN KEY (device_id) REFERENCES devices(id);
    END IF;
END $$;

ALTER TABLE devices DROP COLUMN IF EXISTS device_secret;
