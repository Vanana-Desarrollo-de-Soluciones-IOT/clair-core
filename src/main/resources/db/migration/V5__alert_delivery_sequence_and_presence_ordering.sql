-- Every alert lifecycle transition (open, acknowledge, resolve) gets a strictly increasing
-- sequence so the edge can page transitions durably instead of re-reading old resolved alerts.
-- The edge receipt is recorded separately from the business acknowledgement.
CREATE TABLE alert_transition_counter (id integer NOT NULL, last_value bigint NOT NULL, PRIMARY KEY (id));
INSERT INTO alert_transition_counter (id, last_value) VALUES (1, 0);
ALTER TABLE alerts ADD COLUMN transition_sequence bigint NOT NULL DEFAULT 0;
ALTER TABLE alerts ADD COLUMN edge_receipt_sequence bigint;
-- Backfill in occurrence order so existing alerts page out oldest first.
UPDATE alerts a SET transition_sequence = numbered.seq
FROM (SELECT id, row_number() OVER (ORDER BY occurred_at, id) AS seq FROM alerts) numbered
WHERE a.id = numbered.id;
UPDATE alert_transition_counter SET last_value = (SELECT COALESCE(MAX(transition_sequence), 0) FROM alerts) WHERE id = 1;
CREATE INDEX idx_alert_transition_sequence ON alerts (transition_sequence);
-- Presence events are applied in occurrence order; this is the watermark of the last one applied.
ALTER TABLE device_assignments ADD COLUMN presence_at timestamp(6) with time zone;
