-- Add hr_manager_id column referencing users table
-- Run this if manager_id in employees references employees(id) (self-ref)
-- We add a separate column for HR manager (references users)

ALTER TABLE employees ADD COLUMN IF NOT EXISTS hr_manager_id UUID REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_employees_hr_manager ON employees(hr_manager_id);
