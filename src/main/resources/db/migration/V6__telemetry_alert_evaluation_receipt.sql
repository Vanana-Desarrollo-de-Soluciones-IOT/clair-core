-- Alert evaluation runs after the reading commits, in process. A crash between the commit and
-- the evaluation used to lose it silently. The receipt records that alerting handled a reading;
-- a scheduled catch-up replays readings without one.
ALTER TABLE telemetry_evaluations ADD COLUMN alerts_evaluated_at timestamp(6) with time zone;
-- Everything already stored was evaluated (or is too old to matter): do not replay history.
UPDATE telemetry_evaluations SET alerts_evaluated_at = created_at;
CREATE INDEX idx_telemetry_eval_alerts_pending ON telemetry_evaluations (created_at) WHERE alerts_evaluated_at IS NULL;
