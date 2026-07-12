-- Single-use challenges issued to devices for the attendance signature flow.
CREATE TABLE challenge (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    device_id   BIGINT      NOT NULL,
    challenge   VARCHAR(64) NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_challenge_device FOREIGN KEY (device_id) REFERENCES device (id),
    KEY ix_challenge_device (device_id),
    KEY ix_challenge_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
