-- Convert all custom ENUM columns to VARCHAR to work with Hibernate/JDBC
-- Also fix NOT NULL constraints that conflict with JPA model

ALTER TABLE organizations
    ALTER COLUMN subscription_plan TYPE VARCHAR(50) USING subscription_plan::varchar;

ALTER TABLE attendance
    ALTER COLUMN status TYPE VARCHAR(50) USING status::varchar;

ALTER TABLE leave_requests
    ALTER COLUMN status TYPE VARCHAR(50) USING status::varchar,
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- Critical: leave_type_id is NOT NULL but JPA uses a string leave_type column instead
ALTER TABLE leave_requests ALTER COLUMN leave_type_id DROP NOT NULL;

ALTER TABLE payroll
    ALTER COLUMN payroll_status TYPE VARCHAR(50) USING payroll_status::varchar;

ALTER TABLE employees
    ALTER COLUMN employment_type TYPE VARCHAR(50) USING employment_type::varchar,
    ALTER COLUMN status TYPE VARCHAR(50) USING status::varchar;

-- Drop the custom enum types (no longer needed)
DROP TYPE IF EXISTS subscription_plan;
DROP TYPE IF EXISTS attendance_status;
DROP TYPE IF EXISTS leave_status;
DROP TYPE IF EXISTS payroll_status;
DROP TYPE IF EXISTS employment_type;
DROP TYPE IF EXISTS employee_status;
