-- Migration: Add employees, attendance, leave_requests, payroll tables
-- Run this if you already have the base schema without these tables

-- employees table (simplified - no department FK, no manager FK for now)
CREATE TABLE IF NOT EXISTS employees (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    organization_id  UUID           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    employee_code    VARCHAR(50)    NOT NULL UNIQUE,
    designation      VARCHAR(100),
    joining_date     DATE           NOT NULL DEFAULT CURRENT_DATE,
    employment_type  VARCHAR(20)    NOT NULL DEFAULT 'FULL_TIME',
    basic_salary     DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    bank_account_no  VARCHAR(50),
    pan_number       VARCHAR(20),
    pf_number        VARCHAR(30),
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- attendance table
CREATE TABLE IF NOT EXISTS attendance (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID             NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    organization_id UUID             NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    attendance_date DATE             NOT NULL,
    check_in        TIMESTAMP,
    check_out       TIMESTAMP,
    total_hours     DECIMAL(5,2),
    status          VARCHAR(20)      NOT NULL DEFAULT 'PRESENT',
    remarks         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, attendance_date)
);

-- leave_requests table (simplified - no leave_type FK)
CREATE TABLE IF NOT EXISTS leave_requests (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id   UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type    VARCHAR(50)  NOT NULL DEFAULT 'CASUAL',
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    reason        TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    approved_by   UUID         REFERENCES users(id),
    approved_at   TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- payroll table
CREATE TABLE IF NOT EXISTS payroll (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id        UUID           NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    organization_id    UUID           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    pay_month          INTEGER        NOT NULL CHECK (pay_month BETWEEN 1 AND 12),
    pay_year           INTEGER        NOT NULL,
    total_working_days INTEGER        NOT NULL DEFAULT 26,
    present_days       INTEGER        NOT NULL DEFAULT 0,
    leaves_taken       INTEGER        NOT NULL DEFAULT 0,
    basic_salary       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    hra                DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    bonus              DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    gross_salary       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    pf_deduction       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    professional_tax   DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    total_deductions   DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    net_salary         DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    payroll_status     VARCHAR(20)    NOT NULL DEFAULT 'GENERATED',
    generated_by       UUID           REFERENCES users(id),
    generated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, pay_month, pay_year)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_attendance_employee ON attendance(employee_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(attendance_date);
CREATE INDEX IF NOT EXISTS idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX IF NOT EXISTS idx_payroll_employee ON payroll(employee_id);
CREATE INDEX IF NOT EXISTS idx_payroll_month_year ON payroll(pay_month, pay_year);
