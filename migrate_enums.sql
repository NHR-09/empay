-- Convert all custom ENUM columns to VARCHAR to work with Hibernate/JDBC

ALTER TABLE organizations
    ALTER COLUMN subscription_plan TYPE VARCHAR(50);

ALTER TABLE attendance
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE leave_requests
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE payroll
    ALTER COLUMN payroll_status TYPE VARCHAR(20);

ALTER TABLE employees
    ALTER COLUMN employment_type TYPE VARCHAR(20),
    ALTER COLUMN status TYPE VARCHAR(20);

-- Drop the custom enum types (no longer needed)
DROP TYPE IF EXISTS subscription_plan;
DROP TYPE IF EXISTS attendance_status;
DROP TYPE IF EXISTS leave_status;
DROP TYPE IF EXISTS payroll_status;
DROP TYPE IF EXISTS employment_type;
DROP TYPE IF EXISTS employee_status;
