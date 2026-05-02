-- Fix ENUM-to-VARCHAR migration for JPA compatibility
-- Run once after the initial schema creation

-- Leave requests: make leave_type_id nullable (JPA uses leave_type string instead)
ALTER TABLE leave_requests ALTER COLUMN leave_type_id DROP NOT NULL;
ALTER TABLE leave_requests ALTER COLUMN status TYPE varchar(50) USING status::varchar;
ALTER TABLE leave_requests ALTER COLUMN status SET DEFAULT 'PENDING';

-- Attendance: enum to varchar
ALTER TABLE attendance ALTER COLUMN status TYPE varchar(50) USING status::varchar;

-- Payroll: enum to varchar
ALTER TABLE payroll ALTER COLUMN payroll_status TYPE varchar(50) USING payroll_status::varchar;

-- Employees: enum to varchar
ALTER TABLE employees ALTER COLUMN status TYPE varchar(50) USING status::varchar;
ALTER TABLE employees ALTER COLUMN employment_type TYPE varchar(50) USING employment_type::varchar;

-- Organizations: enum to varchar (if applicable)
ALTER TABLE organizations ALTER COLUMN subscription_plan TYPE varchar(50) USING subscription_plan::varchar;
