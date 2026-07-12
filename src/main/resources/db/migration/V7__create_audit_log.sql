-- Append-only audit trail of meaningful security/business events.
CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    at          DATETIME(6)  NOT NULL,
    actor_type  VARCHAR(16)  NOT NULL,
    action      VARCHAR(40)  NOT NULL,
    actor_id    VARCHAR(100) NULL,
    target_type VARCHAR(40)  NULL,
    target_id   VARCHAR(64)  NULL,
    detail      VARCHAR(500) NULL,
    ip          VARCHAR(45)  NULL,
    PRIMARY KEY (id),
    KEY ix_audit_at (at),
    KEY ix_audit_action (action),
    KEY ix_audit_actor (actor_type, actor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
