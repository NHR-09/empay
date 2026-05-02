-- ============================================================
-- EmPay HRMS - Database Schema
-- ============================================================

-- Create Database
CREATE DATABASE empay_hrms;
\c empay_hrms;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUMS
-- ============================================================

CREATE TYPE attendance_status AS ENUM ('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'REMOTE');
CREATE TYPE leave_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');
CREATE TYPE payroll_status AS ENUM ('GENERATED', 'PROCESSING', 'PAID', 'FAILED');
CREATE TYPE employment_type AS ENUM ('FULL_TIME', 'PART_TIME', 'INTERN', 'CONTRACT');
CREATE TYPE employee_status AS ENUM ('ACTIVE', 'RESIGNED', 'TERMINATED', 'ON_LEAVE');
CREATE TYPE subscription_plan AS ENUM ('FREE', 'PRO', 'ENTERPRISE');

-- ============================================================
-- 1. organizations
-- ============================================================

CREATE TABLE organizations (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_name     VARCHAR(255) NOT NULL,
    company_code     VARCHAR(50)  NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL,
    phone            VARCHAR(20),
    address          TEXT,
    subscription_plan subscription_plan NOT NULL DEFAULT 'FREE',
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 2. roles
-- ============================================================

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

INSERT INTO roles (role_name, description) VALUES
    ('ADMIN',           'Full system access'),
    ('HR_OFFICER',      'Manages employees and leaves'),
    ('PAYROLL_OFFICER', 'Manages payroll and payslips'),
    ('EMPLOYEE',        'Basic employee access');

-- ============================================================
-- 3. users
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role_id         BIGINT      NOT NULL REFERENCES roles(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    profile_image   TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login           TIMESTAMP,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 4. departments
-- ============================================================

CREATE TABLE departments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    department_name VARCHAR(100) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 5. employees
-- ============================================================

CREATE TABLE employees (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id  UUID           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    employee_code    VARCHAR(50)    NOT NULL UNIQUE,
    department_id    UUID           REFERENCES departments(id),
    designation      VARCHAR(100),
    joining_date     DATE           NOT NULL,
    employment_type  employment_type NOT NULL DEFAULT 'FULL_TIME',
    basic_salary     DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    bank_account_no  VARCHAR(50),
    pan_number       VARCHAR(20),
    aadhaar_number   VARCHAR(20),
    pf_number        VARCHAR(30),
    manager_id       UUID           REFERENCES employees(id),
    status           employee_status NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 6. attendance
-- ============================================================

CREATE TABLE attendance (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID             NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    organization_id UUID             NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    attendance_date DATE             NOT NULL,
    check_in        TIMESTAMP,
    check_out       TIMESTAMP,
    total_hours     DECIMAL(5,2),
    status          attendance_status NOT NULL DEFAULT 'PRESENT',
    remarks         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, attendance_date)
);

-- ============================================================
-- 7. leave_types
-- ============================================================

CREATE TABLE leave_types (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    leave_name      VARCHAR(100) NOT NULL,
    max_days        INTEGER      NOT NULL DEFAULT 0,
    is_paid         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ============================================================
-- 8. leave_requests
-- ============================================================

CREATE TABLE leave_requests (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id   UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id UUID         NOT NULL REFERENCES leave_types(id),
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    reason        TEXT,
    status        leave_status NOT NULL DEFAULT 'PENDING',
    approved_by   UUID         REFERENCES users(id),
    approved_at   TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 9. payroll
-- ============================================================

CREATE TABLE payroll (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id        UUID           NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    organization_id    UUID           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    pay_month          INTEGER        NOT NULL CHECK (pay_month BETWEEN 1 AND 12),
    pay_year           INTEGER        NOT NULL,
    total_working_days INTEGER        NOT NULL DEFAULT 0,
    present_days       INTEGER        NOT NULL DEFAULT 0,
    leaves_taken       INTEGER        NOT NULL DEFAULT 0,
    basic_salary       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    hra                DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    bonus              DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    overtime_pay       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    gross_salary       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    total_deductions   DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    net_salary         DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    payroll_status     payroll_status NOT NULL DEFAULT 'GENERATED',
    generated_by       UUID           REFERENCES users(id),
    generated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, pay_month, pay_year)
);

-- ============================================================
-- 10. deductions
-- ============================================================

CREATE TABLE deductions (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payroll_id     UUID          NOT NULL REFERENCES payroll(id) ON DELETE CASCADE,
    deduction_type VARCHAR(100)  NOT NULL,
    amount         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    description    TEXT
);

-- ============================================================
-- 11. payslips
-- ============================================================

CREATE TABLE payslips (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payroll_id   UUID      NOT NULL REFERENCES payroll(id) ON DELETE CASCADE UNIQUE,
    payslip_url  TEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    email_sent   BOOLEAN   NOT NULL DEFAULT FALSE
);

-- ============================================================
-- 12. audit_logs
-- ============================================================

CREATE TABLE audit_logs (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID         REFERENCES users(id),
    action     VARCHAR(100) NOT NULL,
    module     VARCHAR(100) NOT NULL,
    old_value  JSONB,
    new_value  JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_organization       ON users(organization_id);
CREATE INDEX idx_employees_organization   ON employees(organization_id);
CREATE INDEX idx_employees_department     ON employees(department_id);
CREATE INDEX idx_employees_manager        ON employees(manager_id);
CREATE INDEX idx_attendance_employee      ON attendance(employee_id);
CREATE INDEX idx_attendance_date          ON attendance(attendance_date);
CREATE INDEX idx_leave_requests_employee  ON leave_requests(employee_id);
CREATE INDEX idx_payroll_employee         ON payroll(employee_id);
CREATE INDEX idx_payroll_month_year       ON payroll(pay_month, pay_year);
CREATE INDEX idx_deductions_payroll       ON deductions(payroll_id);
CREATE INDEX idx_audit_logs_user          ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_module        ON audit_logs(module);
