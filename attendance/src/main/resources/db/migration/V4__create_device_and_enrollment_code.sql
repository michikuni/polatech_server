-- Enrolled devices: backend stores only the public key.
CREATE TABLE device (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id            BIGINT       NOT NULL,
    public_key             VARCHAR(512) NOT NULL,
    public_key_fingerprint VARCHAR(64)  NOT NULL,
    platform               VARCHAR(16)  NOT NULL,
    device_name            VARCHAR(200) NULL,
    status                 VARCHAR(16)  NOT NULL,
    enrolled_at            DATETIME(6)  NOT NULL,
    last_used_at           DATETIME(6)  NULL,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_device_employee FOREIGN KEY (employee_id) REFERENCES employee (id),
    KEY ix_device_employee_status (employee_id, status),
    KEY ix_device_fingerprint (public_key_fingerprint)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- One-time pairing codes (only the SHA-256 hash is stored).
CREATE TABLE enrollment_code (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id      BIGINT       NOT NULL,
    code_hash        VARCHAR(64)  NOT NULL,
    expires_at       DATETIME(6)  NOT NULL,
    used_at          DATETIME(6)  NULL,
    created_by_admin VARCHAR(100) NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_enrollment_employee FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT uk_enrollment_code_hash UNIQUE (code_hash),
    KEY ix_enrollment_employee (employee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
