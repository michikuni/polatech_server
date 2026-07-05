-- System-generated punches (the 23:59 auto check-out) have no challenge, so
-- challenge_id must allow NULL. The FK to challenge stays and simply permits NULL.
-- A NULL challenge_id now marks an event the system recorded, not a device.
ALTER TABLE attendance_event
    MODIFY COLUMN challenge_id BIGINT NULL;
