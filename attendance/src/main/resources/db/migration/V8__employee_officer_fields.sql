-- Reshape the employee record into an "officer" (cán bộ): drop email/department,
-- add position (chức vụ) and rank (cấp bậc). `officer_rank` is used because RANK
-- is a reserved word in MySQL 8. DEFAULT '' keeps any pre-existing rows valid.
ALTER TABLE employee
    DROP COLUMN email,
    DROP COLUMN department,
    ADD COLUMN position     VARCHAR(200) NOT NULL DEFAULT '' AFTER full_name,
    ADD COLUMN officer_rank VARCHAR(200) NOT NULL DEFAULT '' AFTER position;
