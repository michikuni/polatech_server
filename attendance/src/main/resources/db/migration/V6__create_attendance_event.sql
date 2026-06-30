-- Attendance punches (check-in / check-out). Multiple per day allowed.
CREATE TABLE attendance_event (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    employee_id  BIGINT      NOT NULL,
    device_id    BIGINT      NOT NULL,
    type         VARCHAR(16) NOT NULL,
    event_time   DATETIME(6) NOT NULL,
    challenge_id BIGINT      NOT NULL,
    source_ip    VARCHAR(45) NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT fk_attendance_device FOREIGN KEY (device_id) REFERENCES device (id),
    CONSTRAINT fk_attendance_challenge FOREIGN KEY (challenge_id) REFERENCES challenge (id),
    KEY ix_attendance_employee_time (employee_id, event_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
