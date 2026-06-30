-- Employees that attendance is recorded for.
CREATE TABLE employee (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    employee_code VARCHAR(50)  NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    email         VARCHAR(200) NULL,
    department    VARCHAR(200) NULL,
    active        BIT          NOT NULL DEFAULT b'1',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_code UNIQUE (employee_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
