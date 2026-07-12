-- Free-text "shift handover" note (Thông tin tiếp nhận ca trực) attached to the
-- check-in that opens a session. Write-once: set when first saved, then read-only.
ALTER TABLE attendance_event
    ADD COLUMN note VARCHAR(1000) NULL AFTER source_ip;
