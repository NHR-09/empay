"""
EmPay HRMS - Database Seed Script
Populates: users, employees, attendance, leave_requests, leave_balances,
           payroll, deductions, payslips, notifications, audit_logs

Requirements:
    pip install psycopg2-binary bcrypt

Usage:
    python seed.py
"""

import psycopg2
import bcrypt
import random
import uuid
from datetime import date, datetime, timedelta

# ── DB CONFIG ──────────────────────────────────────────────────────────────────
DB = dict(host="localhost", port=5432, dbname="empay_hrms",
          user="postgres", password="postgres")

# ── CONSTANTS ──────────────────────────────────────────────────────────────────
FIRST_NAMES = ["Aarav","Vivaan","Aditya","Vihaan","Arjun","Sai","Reyansh","Ayaan",
               "Krishna","Ishaan","Priya","Ananya","Divya","Sneha","Pooja","Kavya",
               "Meera","Riya","Nisha","Deepa","Rahul","Rohit","Amit","Suresh","Vijay",
               "Rajesh","Manoj","Nikhil","Kiran","Sanjay","Lakshmi","Sunita","Geeta",
               "Rekha","Usha","Asha","Mala","Seema","Neha","Swati"]

LAST_NAMES  = ["Sharma","Verma","Patel","Singh","Kumar","Gupta","Joshi","Mehta",
               "Shah","Nair","Reddy","Rao","Iyer","Pillai","Menon","Bhat","Kaur",
               "Malhotra","Chopra","Agarwal","Desai","Jain","Mishra","Tiwari","Pandey"]

DESIGNATIONS = ["Software Engineer","Senior Engineer","Tech Lead","Product Manager",
                "HR Executive","Payroll Analyst","Sales Executive","Marketing Analyst",
                "Finance Analyst","Data Analyst","DevOps Engineer","QA Engineer",
                "Business Analyst","Scrum Master","UI/UX Designer"]

EMP_TYPES    = ["FULL_TIME","PART_TIME","CONTRACT","INTERN"]
LEAVE_TYPES  = ["CASUAL","SICK","EARNED"]
LEAVE_STATUS = ["PENDING","APPROVED","REJECTED","CANCELLED"]
ATT_STATUS   = ["PRESENT","ABSENT","LATE","HALF_DAY"]
PAY_STATUS   = ["GENERATED","PAID"]
AUDIT_ACTIONS = ["LOGIN","LOGOUT","CREATE_USER","UPDATE_PROFILE","APPLY_LEAVE",
                 "APPROVE_LEAVE","REJECT_LEAVE","GENERATE_PAYROLL","UPDATE_PAYROLL_STATUS",
                 "CHECKIN","CHECKOUT"]
AUDIT_MODULES = ["AUTH","USER","LEAVE","PAYROLL","ATTENDANCE"]
NOTIF_TYPES   = ["LEAVE","PAYROLL","SYSTEM"]

HASHED_PW = bcrypt.hashpw(b"Password@123", bcrypt.gensalt()).decode()

def rnd_date(start: date, end: date) -> date:
    return start + timedelta(days=random.randint(0, (end - start).days))

def rnd_salary() -> float:
    return round(random.randint(25000, 150000) / 1000) * 1000

# ── MAIN ───────────────────────────────────────────────────────────────────────
def seed():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    print("Connected to database.")

    # ── 1. Fetch org & role IDs ────────────────────────────────────────────────
    cur.execute("""
        SELECT u.organization_id FROM users u
        JOIN roles r ON u.role_id = r.id
        WHERE r.role_name = 'ADMIN' LIMIT 1
    """)
    row = cur.fetchone()
    if not row:
        print("ERROR: No organization found. Run empay_schema.sql + dummy_data.sql first.")
        return
    org_id = str(row[0])
    print(f"  Using organization: {org_id}")

    cur.execute("SELECT id FROM roles WHERE role_name = 'EMPLOYEE'")
    emp_role_id = cur.fetchone()[0]

    cur.execute("SELECT id FROM roles WHERE role_name = 'ADMIN'")
    admin_role_id = cur.fetchone()[0]

    # Fetch existing employee count to generate unique codes
    cur.execute("SELECT COUNT(*) FROM employees")
    existing_emp_count = cur.fetchone()[0]

    # ── 2. Fetch admin user for audit/payroll references ──────────────────────
    cur.execute("SELECT id FROM users WHERE role_id = %s LIMIT 1", (admin_role_id,))
    admin_row = cur.fetchone()
    admin_user_id = str(admin_row[0]) if admin_row else None

    # ── 3. Create 100 users + employees ───────────────────────────────────────
    print("Seeding 100 users and employees...")
    user_ids = []
    emp_ids  = []

    for i in range(100):
        uid  = str(uuid.uuid4())
        fn   = random.choice(FIRST_NAMES)
        ln   = random.choice(LAST_NAMES)
        idx  = existing_emp_count + i + 1
        email = f"seed.{fn.lower()}{ln.lower()}{idx}@empay.com"
        phone = f"9{random.randint(100000000, 999999999)}"
        login_id = f"EMP{idx:04d}"

        cur.execute("""
            INSERT INTO users (id, organization_id, role_id, first_name, last_name,
                               email, password, phone, is_active, must_change_password, login_id)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,TRUE,FALSE,%s)
            ON CONFLICT (email) DO NOTHING
        """, (uid, org_id, emp_role_id, fn, ln, email, HASHED_PW, phone, login_id))

        eid         = str(uuid.uuid4())
        basic       = rnd_salary()
        join_date   = rnd_date(date(2020, 1, 1), date(2024, 6, 1))
        emp_type    = random.choice(EMP_TYPES)
        designation = random.choice(DESIGNATIONS)

        cur.execute("""
            INSERT INTO employees (id, user_id, organization_id, employee_code,
                                   designation, joining_date, employment_type, basic_salary, status)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,'ACTIVE')
            ON CONFLICT (employee_code) DO NOTHING
        """, (eid, uid, org_id, login_id, designation, join_date, emp_type, basic))

        user_ids.append(uid)
        emp_ids.append((eid, basic))

    conn.commit()
    print(f"  Created {len(emp_ids)} employees.")

    # ── 4. Attendance (100+ records, ~2 per employee) ─────────────────────────
    print("Seeding attendance records...")
    att_count = 0
    used_att  = set()  # (emp_id, date) uniqueness

    for eid, _ in emp_ids:
        for _ in range(random.randint(2, 4)):
            att_date = rnd_date(date(2024, 1, 1), date(2025, 5, 31))
            key = (eid, att_date)
            if key in used_att:
                continue
            used_att.add(key)

            status   = random.choices(ATT_STATUS, weights=[70, 10, 10, 10])[0]
            check_in = datetime(att_date.year, att_date.month, att_date.day,
                                random.randint(8, 10), random.randint(0, 59))
            check_out = check_in + timedelta(hours=random.randint(6, 10))
            total_h   = round((check_out - check_in).seconds / 3600, 2)

            cur.execute("""
                INSERT INTO attendance (id, employee_id, organization_id, attendance_date,
                                        check_in, check_out, total_hours, status)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT (employee_id, attendance_date) DO NOTHING
            """, (str(uuid.uuid4()), eid, org_id, att_date, check_in, check_out, total_h, status))
            att_count += 1

    conn.commit()
    print(f"  Created ~{att_count} attendance records.")

    # ── 5. Leave balances ─────────────────────────────────────────────────────
    print("Seeding leave balances...")
    defaults = {"CASUAL": 12, "SICK": 10, "EARNED": 15}
    for eid, _ in emp_ids:
        for lt, total in defaults.items():
            used = random.randint(0, total // 2)
            cur.execute("""
                INSERT INTO leave_balance (id, employee_id, leave_type, total_days,
                                           used_days, remaining_days, year)
                VALUES (%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT DO NOTHING
            """, (str(uuid.uuid4()), eid, lt, total, used, total - used, 2025))
    conn.commit()
    print("  Leave balances seeded.")

    # ── 6. Seed leave_types + leave requests (100+ records) ──────────────────
    print("Seeding leave types...")
    leave_type_map = {}  # name -> id
    for lt_name in LEAVE_TYPES:
        lt_id = str(uuid.uuid4())
        cur.execute("""
            INSERT INTO leave_types (id, organization_id, leave_name, max_days, is_paid)
            VALUES (%s,%s,%s,%s,FALSE)
            ON CONFLICT DO NOTHING
        """, (lt_id, org_id, lt_name, {"CASUAL": 12, "SICK": 10, "EARNED": 15}[lt_name]))
        # fetch actual id in case of conflict
        cur.execute("SELECT id FROM leave_types WHERE leave_name = %s AND organization_id = %s", (lt_name, org_id))
        leave_type_map[lt_name] = str(cur.fetchone()[0])
    conn.commit()
    print("  Leave types seeded.")

    print("Seeding leave requests...")
    leave_count = 0
    for eid, _ in emp_ids:
        for _ in range(random.randint(1, 3)):
            lt         = random.choice(LEAVE_TYPES)
            lt_id      = leave_type_map[lt]
            start      = rnd_date(date(2024, 1, 1), date(2025, 4, 30))
            end        = start + timedelta(days=random.randint(1, 5))
            status     = random.choice(LEAVE_STATUS)
            reasons    = ["Medical appointment","Family function","Personal work",
                          "Vacation","Sick","Festival","Emergency"]
            reason     = random.choice(reasons)

            approved_by = admin_user_id if status in ("APPROVED","REJECTED") else None
            approved_at = datetime.now() if status in ("APPROVED","REJECTED") else None

            cur.execute("""
                INSERT INTO leave_requests (id, employee_id, leave_type_id, leave_type, start_date,
                                           end_date, reason, status, approved_by, approved_at)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """, (str(uuid.uuid4()), eid, lt_id, lt, start, end, reason, status,
                  approved_by, approved_at))
            leave_count += 1

    conn.commit()
    print(f"  Created {leave_count} leave requests.")

    # ── 7. Payroll + deductions + payslips (100+ records) ─────────────────────
    print("Seeding payroll records...")
    pay_count = 0
    used_pay  = set()  # (emp_id, month, year)

    for eid, basic in emp_ids:
        for _ in range(random.randint(1, 3)):
            month = random.randint(1, 12)
            year  = random.choice([2024, 2025])
            key   = (eid, month, year)
            if key in used_pay:
                continue
            used_pay.add(key)

            total_days   = 26
            present_days = random.randint(18, 26)
            leaves_taken = random.randint(0, 3)
            eff_days     = min(present_days + leaves_taken, total_days)

            earned_basic = round(basic * eff_days / total_days, 2)
            hra          = round(earned_basic * 0.40, 2)
            bonus        = round(basic * 0.10, 2)
            gross        = round(earned_basic + hra + bonus, 2)
            pf           = round(basic * 0.12, 2)
            prof_tax     = 200.00
            total_ded    = round(pf + prof_tax, 2)
            net          = round(gross - total_ded, 2)
            status       = random.choice(PAY_STATUS)

            pay_id = str(uuid.uuid4())
            cur.execute("""
                INSERT INTO payroll (id, employee_id, organization_id, pay_month, pay_year,
                    total_working_days, present_days, leaves_taken, basic_salary, hra, bonus,
                    gross_salary, total_deductions, net_salary, payroll_status,
                    generated_by, pf_deduction, professional_tax)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT (employee_id, pay_month, pay_year) DO NOTHING
            """, (pay_id, eid, org_id, month, year, total_days, present_days, leaves_taken,
                  earned_basic, hra, bonus, gross, total_ded, net, status,
                  admin_user_id, pf, prof_tax))

            # Deductions
            cur.execute("""
                INSERT INTO deductions (id, payroll_id, deduction_type, amount, description)
                VALUES (%s,%s,'Provident Fund (12%%)',%s,'Employee PF at 12%% of basic')
            """, (str(uuid.uuid4()), pay_id, pf))
            cur.execute("""
                INSERT INTO deductions (id, payroll_id, deduction_type, amount, description)
                VALUES (%s,%s,'Professional Tax',%s,'Monthly professional tax')
            """, (str(uuid.uuid4()), pay_id, prof_tax))

            # Payslip
            cur.execute("""
                INSERT INTO payslips (id, payroll_id, generated_at, email_sent)
                VALUES (%s,%s,NOW(),TRUE)
                ON CONFLICT (payroll_id) DO NOTHING
            """, (str(uuid.uuid4()), pay_id))

            pay_count += 1

    conn.commit()
    print(f"  Created {pay_count} payroll records.")

    # ── 8. Notifications (100+ records) ───────────────────────────────────────
    print("Seeding notifications...")
    notif_msgs = {
        "LEAVE":   ["Your leave request has been approved.",
                    "Your leave request has been rejected.",
                    "A new leave request is pending your approval."],
        "PAYROLL": ["Your payslip for this month has been generated.",
                    "Your salary has been marked as PAID.",
                    "Payroll processing is complete."],
        "SYSTEM":  ["Welcome to EmPay HRMS!",
                    "Your profile has been updated.",
                    "Password changed successfully.",
                    "System maintenance scheduled for Sunday."]
    }
    for uid in user_ids[:100]:
        for _ in range(random.randint(1, 3)):
            ntype = random.choice(NOTIF_TYPES)
            msg   = random.choice(notif_msgs[ntype])
            is_read = random.choice([True, False])
            cur.execute("""
                INSERT INTO notifications (id, user_id, message, type, is_read, created_at)
                VALUES (%s,%s,%s,%s,%s,%s)
            """, (str(uuid.uuid4()), uid, msg, ntype, is_read,
                  datetime.now() - timedelta(days=random.randint(0, 60))))
    conn.commit()
    print("  Notifications seeded.")

    # ── 9. Audit logs (100+ records) ──────────────────────────────────────────
    print("Seeding audit logs...")
    for _ in range(120):
        uid    = random.choice(user_ids) if user_ids else None
        action = random.choice(AUDIT_ACTIONS)
        module = random.choice(AUDIT_MODULES)
        cur.execute("""
            INSERT INTO audit_logs (id, user_id, action, module, old_value, new_value,
                                    ip_address, created_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
        """, (str(uuid.uuid4()), uid, action, module,
              '{}', '{"status":"success"}',
              f"192.168.{random.randint(1,5)}.{random.randint(1,254)}",
              datetime.now() - timedelta(days=random.randint(0, 90))))
    conn.commit()
    print("  Audit logs seeded.")

    cur.close()
    conn.close()
    print("\nDone! Database seeded successfully.")
    print(f"  100 users/employees | {att_count} attendance | {leave_count} leave requests")
    print(f"  {pay_count} payroll records | notifications | 120 audit logs")

if __name__ == "__main__":
    seed()
