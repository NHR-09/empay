/* ─── STATE ─────────────────────────────────────────── */
const user = JSON.parse(sessionStorage.getItem('user'));
if (!user) window.location.href = '/index.html';

const ROLE = user.role || '';
const IS_ADMIN    = ROLE === 'ADMIN';
const IS_HR       = ROLE === 'HR_OFFICER';
const IS_EMPLOYEE = ROLE === 'EMPLOYEE';
const CAN_MANAGE  = IS_ADMIN || IS_HR;
const CAN_PAYROLL = IS_ADMIN || ROLE === 'PAYROLL_OFFICER';

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

/* ─── INIT ───────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  initUser();
  initSidebar();
  initDateFilters();
  showSection('employees');
  loadSection('employees');
  loadNotifications();
});

/* ─── DYNAMIC DATE FILTERS ───────────────────────────── */
function initDateFilters() {
  const now = new Date();
  const curMonth = now.getMonth() + 1;
  const curYear = now.getFullYear();
  const monthNames = ['January','February','March','April','May','June',
                      'July','August','September','October','November','December'];
  // Populate all month selects
  document.querySelectorAll('.month-select, #genMonth, #hrAttMonth, #filterPayMonth').forEach(sel => {
    const keepFirst = sel.querySelector('option[value="0"]');
    sel.innerHTML = '';
    if (keepFirst) sel.appendChild(keepFirst);
    for (let i = 1; i <= 12; i++) {
      const opt = document.createElement('option');
      opt.value = i;
      opt.textContent = monthNames[i - 1];
      if (i === curMonth) opt.selected = true;
      sel.appendChild(opt);
    }
  });
  // Populate all year selects
  document.querySelectorAll('#genYear, #filterPayYear, #hrAttYear, #empPayYear').forEach(sel => {
    sel.innerHTML = '';
    for (let y = curYear; y >= curYear - 2; y--) {
      const opt = document.createElement('option');
      opt.value = y; opt.textContent = y;
      sel.appendChild(opt);
    }
  });
  // Populate employee payslip month filter
  document.querySelectorAll('#empPayMonth').forEach(sel => {
    const keepFirst = sel.querySelector('option[value="0"]');
    sel.innerHTML = '';
    if (keepFirst) sel.appendChild(keepFirst);
    for (let i = 1; i <= 12; i++) {
      const opt = document.createElement('option');
      opt.value = i; opt.textContent = monthNames[i-1];
      sel.appendChild(opt);
    }
  });
  // Populate HR manager dropdown for Add Employee
  populateHrManagerDropdown();
}

function initUser() {
  const initials = ((user.firstName||'')[0]||'') + ((user.lastName||'')[0]||'');
  document.getElementById('avatarInitials').textContent = initials.toUpperCase();
  if (document.getElementById('topAvatarInitials')) {
    document.getElementById('topAvatarInitials').textContent = initials.toUpperCase();
  }
  document.getElementById('sidebarName').textContent = user.firstName + ' ' + user.lastName;
  document.getElementById('sidebarRole').textContent = ROLE.replace('_', ' ');

  // Role-based nav visibility
  // EMPLOYEE: employees, attendance, timeoff, mypayslips, profile
  // ADMIN: employees, add employee, attendance, timeoff — NO payroll
  // HR: employees, payroll, hrpanel, reports

  if (IS_ADMIN) {
    document.getElementById('navAddEmployee')?.classList.remove('d-none');
  }
  if (IS_HR) {
    document.getElementById('navPayroll')?.classList.remove('d-none');
    document.getElementById('navHrPanel')?.classList.remove('d-none');
    document.getElementById('navReports')?.classList.remove('d-none');
  }
  if (IS_EMPLOYEE) {
    document.getElementById('navMyPayslips')?.classList.remove('d-none');
  }
}

/* ─── SIDEBAR ────────────────────────────────────────── */
function initSidebar() {
  document.querySelectorAll('.sidebar .nav-link[data-section]').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      const sec = link.dataset.section;
      document.querySelectorAll('.sidebar .nav-link').forEach(a => a.classList.remove('active'));
      link.classList.add('active');
      showSection(sec);
      loadSection(sec);
    });
  });
  const topProfileLink = document.getElementById('navMyProfile');
  if (topProfileLink) {
    topProfileLink.addEventListener('click', e => {
      e.preventDefault();
      document.querySelectorAll('.sidebar .nav-link').forEach(a => a.classList.remove('active'));
      document.querySelector('.sidebar .nav-link[data-section="profile"]')?.classList.add('active');
      showSection('profile');
      loadSection('profile');
    });
  }
}

function showSection(sec) {
  document.querySelectorAll('.section').forEach(s => s.classList.add('d-none'));
  document.getElementById('sec-' + sec)?.classList.remove('d-none');
  const titles = { employees:'Employees', attendance:'Attendance', timeoff:'Time Off',
    payroll:'Payroll', reports:'Reports', profile:'My Profile', settings:'Settings',
    hrpanel:'HR Panel', addEmployee:'Add Employee', mypayslips:'My Payslips' };
  document.getElementById('pageTitle').textContent = titles[sec] || sec;
}

function loadSection(sec) {
  if (sec === 'employees')   loadEmployees();
  if (sec === 'attendance')  loadAttendance();
  if (sec === 'timeoff')     loadTimeOff();
  if (sec === 'payroll')     loadPayroll();
  if (sec === 'mypayslips')  loadMyPayroll();
  if (sec === 'reports')     loadReports();
  if (sec === 'profile')     loadProfile();
  if (sec === 'hrpanel')     loadHrPanel();
}

/* ─── UTILS ──────────────────────────────────────────── */
function showAlert(id, msg, type='error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.className = 'alert py-2 small ' + (type === 'success' ? 'alert-success' : 'alert-danger');
  el.classList.remove('d-none');
  setTimeout(() => el.classList.add('d-none'), 5000);
}

function badge(status) {
  const cls = {
    ACTIVE:'success', INACTIVE:'secondary', PRESENT:'success', ABSENT:'danger',
    APPROVED:'success', REJECTED:'danger', PENDING:'warning',
    GENERATED:'warning', PAID:'primary', CANCELLED:'secondary'
  }[status?.toUpperCase()] || 'warning';
  return `<span class="badge bg-${cls} badge-custom">${status}</span>`;
}

function moneyINR(val) {
  return '₹' + parseFloat(val||0).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2});
}

function monthName(m) { return MONTHS[(m||1)-1] || m; }

function emptyRow(cols, msg='No data found.') {
  return `<tr><td colspan="${cols}" style="text-align:center;padding:40px;color:var(--muted);">${msg}</td></tr>`;
}

function fmtTime(dt) {
  if (!dt) return '—';
  const d = new Date(dt);
  return d.toLocaleTimeString('en-IN', {hour:'2-digit', minute:'2-digit'});
}

/* ─── EMPLOYEES ───────────────────────────────────────── */
let allEmployees = [];

async function loadEmployees() {
  try {
    if (CAN_MANAGE || IS_ADMIN) {
      // Admin/HR see full user list from admin API
      const res = await fetch('/api/admin/users');
      allEmployees = res.ok ? await res.json() : [];
    } else {
      // Regular employees see employee list (read-only)
      const res = await fetch(`/api/employees?email=${encodeURIComponent(user.email)}`);
      const empList = res.ok ? await res.json() : [];
      allEmployees = empList.map(e => ({
        loginId: e.employeeCode || '',
        firstName: e.firstName || '',
        lastName: e.lastName || '',
        email: e.email || '',
        designation: e.designation || '',
        employmentType: e.employmentType || '',
        role: '',
        status: e.status || 'ACTIVE'
      }));
    }
  } catch { allEmployees = []; }
  renderEmployeeTable(allEmployees);
}

function renderEmployeeTable(list) {
  const tbody = document.getElementById('empBody');
  const thead = document.getElementById('empHead');
  const isManager = CAN_MANAGE || IS_ADMIN;

  if (thead) {
    thead.innerHTML = `<tr><th>Login ID</th><th>Name</th><th>Email</th><th>${isManager ? 'Role' : 'Designation'}</th><th>Status</th><th>Actions</th></tr>`;
  }

  if (!list.length) { tbody.innerHTML = emptyRow(6, 'No employees found.'); return; }
  tbody.innerHTML = list.map(e => `
    <tr>
      <td><code class="small">${e.loginId||e.employeeCode||'—'}</code></td>
      <td><strong>${e.firstName} ${e.lastName}</strong></td>
      <td class="text-muted">${e.email}</td>
      <td>${e.role ? e.role.replace('_',' ') : (e.designation || '—')}</td>
      <td>${badge(e.status)}</td>
      <td>
        <button class="btn btn-outline-secondary btn-sm" onclick="showEmpInfo('${e.loginId||e.employeeCode}')">Info</button>
        ${isManager ? `
          <button class="btn btn-outline-${e.status==='ACTIVE'?'warning':'success'} btn-sm ms-1" onclick="toggleUserStatus('${e.loginId}','${e.status}')">
            ${e.status==='ACTIVE' ? 'Deactivate' : 'Activate'}
          </button>
          <button class="btn btn-outline-danger btn-sm ms-1" onclick="removeEmployee('${e.loginId}')">Remove</button>
        ` : ''}
      </td>
    </tr>`).join('');
}

function filterEmployees(q) {
  const lq = q.toLowerCase();
  renderEmployeeTable(allEmployees.filter(e =>
    (e.firstName+' '+e.lastName+e.email+e.role).toLowerCase().includes(lq)
  ));
}

async function submitAddEmployee() {
  const payload = {
    firstName:        document.getElementById('addFirstName').value.trim(),
    lastName:         document.getElementById('addLastName').value.trim(),
    email:            document.getElementById('addEmail').value.trim(),
    phone:            document.getElementById('addPhone').value.trim(),
    role:             document.getElementById('addRole').value,
    designation:      document.getElementById('addDesignation').value.trim(),
    hrManagerLoginId: document.getElementById('addHrManager').value,
    requestedBy:      user.email
  };
  if (!payload.firstName || !payload.lastName || !payload.email) {
    showAlert('addEmpAlert','First name, last name and email are required.'); return;
  }
  try {
    const res = await fetch('/api/admin/create-user', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (res.ok) {
      showAlert('addEmpAlert', `✓ Created! Login ID: ${data.loginId} — Password sent to ${data.email}`, 'success');
      loadEmployees();
    } else { showAlert('addEmpAlert', data.message); }
  } catch { showAlert('addEmpAlert','Network error.'); }
}

async function showEmpInfo(code) {
  try {
    const res = await fetch(`/api/employees/info/${encodeURIComponent(code)}`);
    if (!res.ok) return;
    const e = await res.json();
    document.getElementById('empInfoContent').innerHTML = `
      <table class="table table-sm">
        <tr><td class="text-muted" style="width:40%">Name</td><td><strong>${e.firstName} ${e.lastName}</strong></td></tr>
        <tr><td class="text-muted">Code</td><td>${e.employeeCode}</td></tr>
        <tr><td class="text-muted">Email</td><td>${e.email}</td></tr>
        <tr><td class="text-muted">Phone</td><td>${e.phone||'—'}</td></tr>
        <tr><td class="text-muted">Designation</td><td>${e.designation||'—'}</td></tr>
        <tr><td class="text-muted">Joining Date</td><td>${e.joiningDate}</td></tr>
        <tr><td class="text-muted">Employment Type</td><td>${e.employmentType}</td></tr>
        <tr><td class="text-muted">Status</td><td>${badge(e.status)}</td></tr>
        <tr><td class="text-muted">HR Manager</td><td>${e.hrManager||'—'}</td></tr>
      </table>`;
    new bootstrap.Modal(document.getElementById('empInfoModal')).show();
  } catch {}
}

async function removeEmployee(loginId) {
  if (!confirm(`Remove user ${loginId}? This cannot be undone.`)) return;
  const res = await fetch(`/api/admin/users/${loginId}`, { method:'DELETE' });
  const data = await res.json();
  if (res.ok) loadEmployees();
  else alert(data.message);
}

async function toggleUserStatus(loginId, currentStatus) {
  const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  const res = await fetch(`/api/admin/users/${loginId}/status`, {
    method: 'PATCH',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ status: newStatus })
  });
  const data = await res.json();
  if (res.ok) loadEmployees();
  else alert(data.message);
}

/* ─── ATTENDANCE ──────────────────────────────────────── */
let todayAtt = {};

async function loadAttendance() {
  await loadTodayStatus();
  await loadMyAttendance();
}

async function loadMyAttendance() {
  const res = await fetch(`/api/attendance/my?email=${encodeURIComponent(user.email)}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('attBody');
  tbody.innerHTML = list.length ? list.map(a => `
    <tr>
      <td>${a.date}</td>
      <td>${fmtTime(a.checkIn)}</td>
      <td>${fmtTime(a.checkOut)}</td>
      <td>${a.totalHours ? a.totalHours + 'h' : '—'}</td>
      <td>${badge(a.status)}</td>
    </tr>`).join('') : emptyRow(5);
}

async function loadTodayStatus() {
  try {
    const res = await fetch(`/api/attendance/today-status?email=${encodeURIComponent(user.email)}`);
    todayAtt = res.ok ? await res.json() : {};
  } catch { todayAtt = {}; }
  renderCheckinCard();
}

function renderCheckinCard() {
  const statusEl = document.getElementById('attStatus');
  const btnIn = document.getElementById('btnCheckin');
  const btnOut = document.getElementById('btnCheckout');

  if (todayAtt.checkedIn && todayAtt.checkedOut) {
    statusEl.innerHTML = `<span class="text-success">✓ Checked in at ${fmtTime(todayAtt.checkIn)} · Checked out at ${fmtTime(todayAtt.checkOut)}</span>`;
    btnIn.classList.add('d-none');
    btnOut.classList.add('d-none');
  } else if (todayAtt.checkedIn) {
    statusEl.innerHTML = `<span class="text-warning">● Checked in at ${fmtTime(todayAtt.checkIn)}</span>`;
    btnIn.classList.add('d-none');
    btnOut.classList.remove('d-none');
  } else {
    statusEl.innerHTML = `<span class="text-muted">Not checked in yet</span>`;
    btnIn.classList.remove('d-none');
    btnOut.classList.add('d-none');
  }
}

function updateClock() {
  const el = document.getElementById('liveClock');
  if (!el) return;
  const now = new Date();
  el.textContent = now.toLocaleTimeString('en-IN', {hour:'2-digit', minute:'2-digit', second:'2-digit'});
  const d = document.getElementById('liveDate');
  if (d) d.textContent = now.toLocaleDateString('en-IN', {weekday:'long', day:'numeric', month:'long', year:'numeric'});
}
setInterval(updateClock, 1000);

async function doCheckIn() {
  try {
    const res = await fetch('/api/attendance/checkin', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email: user.email})
    });
    const data = await res.json();
    if (res.ok) { await loadTodayStatus(); loadMyAttendance(); }
    else alert(data.message);
  } catch { alert('Network error.'); }
}

async function doCheckOut() {
  try {
    const res = await fetch('/api/attendance/checkout', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email: user.email})
    });
    const data = await res.json();
    if (res.ok) { await loadTodayStatus(); loadMyAttendance(); }
    else alert(data.message);
  } catch { alert('Network error.'); }
}

/* ─── TIME OFF / LEAVE ────────────────────────────────── */
function loadTimeOff() {
  if (IS_ADMIN || IS_HR) {
    document.getElementById('btnApproveTab')?.classList.remove('d-none');
  }
  switchTimeoffTab('apply');
}

function switchTimeoffTab(tab) {
  document.querySelectorAll('#timeoffTabs .nav-link').forEach(b => b.classList.remove('active'));
  const idx = {apply:0, my:1, approve:2}[tab] || 0;
  const btns = document.querySelectorAll('#timeoffTabs .nav-item .nav-link');
  if (btns[idx]) btns[idx].classList.add('active');
  document.getElementById('timeoffApply').classList.toggle('d-none', tab!=='apply');
  document.getElementById('timeoffMy').classList.toggle('d-none', tab!=='my');
  document.getElementById('timeoffApprove').classList.toggle('d-none', tab!=='approve');
  if (tab==='my') loadMyLeaves();
  if (tab==='approve') loadAllLeaves();
}

async function submitLeave() {
  const payload = {
    email:     user.email,
    leaveType: document.getElementById('leaveType').value,
    startDate: document.getElementById('leaveStart').value,
    endDate:   document.getElementById('leaveEnd').value,
    reason:    document.getElementById('leaveReason').value.trim()
  };
  if (!payload.startDate || !payload.endDate) { showAlert('leaveAlert','Dates are required.'); return; }
  try {
    const res = await fetch('/api/leaves/apply', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    showAlert('leaveAlert', res.ok ? '✓ Leave request submitted.' : data.message, res.ok?'success':'error');
    if (res.ok) { document.getElementById('leaveForm').reset(); loadMyLeaves(); }
  } catch { showAlert('leaveAlert','Network error.'); }
}

async function loadMyLeaves() {
  const res = await fetch(`/api/leaves/my?email=${encodeURIComponent(user.email)}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('myLeavesBody');
  tbody.innerHTML = list.length ? list.map(l => `
    <tr>
      <td>${l.leaveType}</td>
      <td>${l.startDate}</td>
      <td>${l.endDate}</td>
      <td>${badge(l.status)}</td>
      <td class="small">${l.reason||'—'}</td>
    </tr>`).join('') : emptyRow(5);
}

async function loadAllLeaves() {
  const res = await fetch(`/api/leaves/all?email=${encodeURIComponent(user.email)}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('approveLeavesBody');
  tbody.innerHTML = list.length ? list.map(l => `
    <tr>
      <td><strong>${l.employeeName}</strong><br><small class="text-muted">${l.employeeCode}</small></td>
      <td>${l.leaveType}</td>
      <td>${l.startDate}</td>
      <td>${l.endDate}</td>
      <td class="small">${l.reason||'—'}</td>
      <td>${badge(l.status)}</td>
      <td>
        ${l.status==='PENDING' && (CAN_PAYROLL||CAN_MANAGE) ? `
          <button class="btn btn-success btn-sm" onclick="updateLeave('${l.id}','APPROVED')">Approve</button>
          <button class="btn btn-danger btn-sm ms-1" onclick="updateLeave('${l.id}','REJECTED')">Reject</button>
        ` : ''}
      </td>
    </tr>`).join('') : emptyRow(7);
}

async function updateLeave(id, status) {
  const res = await fetch(`/api/leaves/${id}/status`, {
    method:'PATCH', headers:{'Content-Type':'application/json'},
    body: JSON.stringify({ status, approverEmail: user.email })
  });
  const data = await res.json();
  if (res.ok) loadAllLeaves();
  else alert(data.message);
}

/* ─── PAYROLL (HR only) ───────────────────────────────── */
function loadPayroll() {
  // HR-only: show generate + all payrolls
  populateEmployeeDropdown();
  switchPayrollTab('generate');
}

function switchPayrollTab(tab) {
  document.querySelectorAll('#sec-payroll .nav-link').forEach(b => b.classList.remove('active'));
  const btns = document.querySelectorAll('#sec-payroll .nav-item .nav-link');
  if (tab==='generate' && btns[0]) btns[0].classList.add('active');
  if (tab==='all' && btns[1]) btns[1].classList.add('active');
  document.getElementById('payrollGenerate').classList.toggle('d-none', tab!=='generate');
  document.getElementById('payrollAll').classList.toggle('d-none', tab!=='all');
  if (tab==='all') loadAllPayroll();
}

function renderPayslipCard(p) {
  const presentLabel = (p.presentDays === 0 && p.leavesTaken === 0) ? 'Full Month (no tracking)' : `${p.presentDays} days worked, ${p.leavesTaken} leaves`;
  return `
    <div class="payslip-card">
      <div class="payslip-header">
        <div>
          <div class="payslip-period">${p.employeeName || (user.firstName + ' ' + user.lastName)}</div>
          <div style="font-size:12px;color:var(--muted);margin-top:2px">${p.employeeCode || ''} · ${monthName(p.payMonth)} ${p.payYear}</div>
        </div>
        <div style="text-align:right">
          <div class="payslip-net">${moneyINR(p.netSalary)}</div>
          <div style="margin-top:4px">${badge(p.status)}</div>
        </div>
      </div>
      <div class="payslip-details">
        <div class="payslip-info-row"><span>Pay Period</span><span>${monthName(p.payMonth)} ${p.payYear}</span></div>
        <div class="payslip-info-row"><span>Working Days</span><span>${p.totalWorkingDays}</span></div>
        <div class="payslip-info-row"><span>Attendance</span><span>${presentLabel}</span></div>
      </div>
      <div class="payslip-grid">
        <div>
          <div class="payslip-section-title">Earnings</div>
          <div class="payslip-row"><span>Basic Salary</span><span>${moneyINR(p.basicSalary)}</span></div>
          <div class="payslip-row"><span>HRA (40%)</span><span>${moneyINR(p.hra)}</span></div>
          <div class="payslip-row"><span>Performance Bonus</span><span>${moneyINR(p.bonus)}</span></div>
          <div class="payslip-row total"><span>Gross Salary</span><span>${moneyINR(p.grossSalary)}</span></div>
        </div>
        <div>
          <div class="payslip-section-title">Deductions</div>
          <div class="payslip-row deduction"><span>Provident Fund (12%)</span><span>- ${moneyINR(p.pfDeduction)}</span></div>
          <div class="payslip-row deduction"><span>Professional Tax</span><span>- ${moneyINR(p.professionalTax)}</span></div>
          <div class="payslip-row deduction total"><span>Total Deductions</span><span>- ${moneyINR(p.totalDeductions)}</span></div>
        </div>
      </div>
      <div class="payslip-footer">
        <span>Net Pay</span>
        <div style="display:flex;align-items:center;gap:12px">
          <button class="btn btn-ghost btn-sm" onclick="downloadPayslipTxt('${p.id}')">⬇ Download .txt</button>
          <button class="btn btn-ghost btn-sm" onclick="downloadPayslipPdf('${p.id}')" style="margin-left:4px">⬇ PDF</button>
          <span class="payslip-net-big">${moneyINR(p.netSalary)}</span>
        </div>
      </div>
    </div>`;
}

async function loadMyPayroll() {
  // Get filter values if available
  const monthSel = document.getElementById('empPayMonth');
  const yearSel = document.getElementById('empPayYear');
  const filterMonth = monthSel ? parseInt(monthSel.value) : 0;
  const filterYear = yearSel ? parseInt(yearSel.value) : new Date().getFullYear();

  const res = await fetch(`/api/payroll/my?email=${encodeURIComponent(user.email)}`);
  let list = res.ok ? await res.json() : [];

  // Apply client-side month/year filter
  if (filterMonth > 0) {
    list = list.filter(p => p.payMonth === filterMonth && p.payYear === filterYear);
  } else {
    list = list.filter(p => p.payYear === filterYear);
  }

  const container = document.getElementById('myPayslipsList');
  if (!container) return;
  if (!list.length) { container.innerHTML = '<p class="text-muted text-center py-5">No payslips found for this period.</p>'; return; }
  container.innerHTML = list.map(p => renderPayslipCard(p)).join('');
}

async function populateEmployeeDropdown() {
  const sel = document.getElementById('genEmpEmail');
  if (!sel) return;
  try {
    const res = await fetch('/api/admin/users');
    const users = res.ok ? await res.json() : [];
    sel.innerHTML = '<option value="">— Select Employee —</option>' +
      users.filter(u => u.status === 'ACTIVE').map(u => `<option value="${u.email}">${u.firstName} ${u.lastName} (${u.email})</option>`).join('');
  } catch {}
}

async function submitGeneratePayroll() {
  const payload = {
    generatorEmail: user.email,
    employeeEmail:  document.getElementById('genEmpEmail').value.trim(),
    month: parseInt(document.getElementById('genMonth').value),
    year:  parseInt(document.getElementById('genYear').value)
  };
  if (!payload.employeeEmail) { showAlert('genAlert','Please select an employee.'); return; }
  try {
    const res = await fetch('/api/payroll/generate', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    showAlert('genAlert', res.ok ? `✓ Payroll generated for ${data.employeeName}. Net: ${moneyINR(data.netSalary)}` : data.message, res.ok?'success':'error');
    if (res.ok) loadAllPayroll();
  } catch { showAlert('genAlert','Network error.'); }
}

async function loadAllPayroll() {
  const m = document.getElementById('filterPayMonth')?.value || 0;
  const y = document.getElementById('filterPayYear')?.value || new Date().getFullYear();
  const res = await fetch(`/api/payroll/all?email=${encodeURIComponent(user.email)}&month=${m}&year=${y}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('allPayBody');
  if (!tbody) return;
  tbody.innerHTML = list.length ? list.map(p => `
    <tr>
      <td><strong>${p.employeeName}</strong><br><small class="text-muted">${p.employeeCode}</small></td>
      <td>${monthName(p.payMonth)} ${p.payYear}</td>
      <td>${moneyINR(p.grossSalary)}</td>
      <td class="fw-semibold text-success">${moneyINR(p.netSalary)}</td>
      <td>${badge(p.status)}</td>
      <td>
        <button class="btn btn-outline-secondary btn-sm" onclick="viewPayslip('${p.id}')">View</button>
        <button class="btn btn-outline-primary btn-sm ms-1" onclick="downloadPayslipTxt('${p.id}')">TXT</button>
        ${p.status==='GENERATED' ? `<button class="btn btn-success btn-sm ms-1" onclick="markPaid('${p.id}')">Mark Paid</button>` : ''}
      </td>
    </tr>
    <tr id="slip-${p.id}" class="d-none">
      <td colspan="6" style="padding:0">${renderPayslipCard(p)}</td>
    </tr>`).join('') : emptyRow(6);
}

function viewPayslip(id) {
  const row = document.getElementById('slip-' + id);
  if (row) row.classList.toggle('d-none');
}

async function markPaid(id) {
  const res = await fetch(`/api/payroll/${id}/status`, {
    method:'PATCH', headers:{'Content-Type':'application/json'}, body: JSON.stringify({status:'PAID'})
  });
  if (res.ok) loadAllPayroll();
}

/* ─── PROFILE ─────────────────────────────────────────── */
async function loadProfile() {
  try {
    const res = await fetch(`/api/employees/profile?email=${encodeURIComponent(user.email)}`);
    const p = res.ok ? await res.json() : {};
    const initials = ((p.firstName||'')[0]||'') + ((p.lastName||'')[0]||'');
    document.getElementById('profileInitials').textContent = initials.toUpperCase();
    document.getElementById('profileName').textContent = (p.firstName||'') + ' ' + (p.lastName||'');
    document.getElementById('profileRole').textContent = (p.role||'').replace('_',' ');
    document.getElementById('profileEmail').textContent = p.email||'—';
    document.getElementById('profileMobile').textContent = p.phone||'—';
    document.getElementById('profileDepartment').textContent = p.designation||'—';
    document.getElementById('profileManager').textContent = p.hrManager||'—';

    document.getElementById('profFirstName').value  = p.firstName||'';
    document.getElementById('profLastName').value   = p.lastName||'';
    document.getElementById('profEmail').value      = p.email||'';
    document.getElementById('profPhone').value      = p.phone||'';
    document.getElementById('profDesignation').value = p.designation||'';
    document.getElementById('profEmpCode').value    = p.employeeCode||'';
    document.getElementById('profBankAccount').value = p.bankAccountNo||'';
    document.getElementById('profPanNumber').value   = p.panNumber||'';
    document.getElementById('profAadhaar').value     = p.aadhaarNumber||'';
    document.getElementById('profJoiningDate').value = p.joiningDate||'';
    document.getElementById('profEmploymentType').value = p.employmentType||'';
    document.getElementById('profHrManager').value   = p.hrManager||'';
  } catch(e) { console.error(e); }
}

async function saveProfile() {
  const payload = {
    email:         user.email,
    firstName:     document.getElementById('profFirstName').value.trim(),
    lastName:      document.getElementById('profLastName').value.trim(),
    phone:         document.getElementById('profPhone').value.trim(),
    designation:   document.getElementById('profDesignation').value.trim(),
    bankAccountNo: document.getElementById('profBankAccount').value.trim(),
    panNumber:     document.getElementById('profPanNumber').value.trim(),
    aadhaarNumber: document.getElementById('profAadhaar').value.trim()
  };
  try {
    const res = await fetch('/api/employees/profile', {
      method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    showAlert('profileAlert', res.ok ? '✓ Profile updated.' : data.message, res.ok?'success':'error');
    if (res.ok) loadProfile();
  } catch { showAlert('profileAlert','Network error.'); }
}

/* ─── NOTIFICATIONS ──────────────────────────────────── */
async function loadNotifications() {
  try {
    const res = await fetch(`/api/notifications?email=${encodeURIComponent(user.email)}`);
    if (!res.ok) return;
    const data = await res.json();
    const badge = document.getElementById('notifCount');
    if (data.unread > 0) {
      badge.textContent = data.unread;
      badge.style.display = 'flex';
    } else {
      badge.style.display = 'none';
    }
    const list = document.getElementById('notifList');
    if (!data.items || !data.items.length) {
      list.innerHTML = '<div style="padding:16px;text-align:center;color:var(--muted);font-size:12px">No notifications</div>';
      return;
    }
    list.innerHTML = data.items.slice(0, 20).map(n => `
      <div class="notif-item ${n.read ? '' : 'unread'}" onclick="markNotifRead('${n.id}')">
        <div class="notif-type-badge notif-${(n.type||'system').toLowerCase()}">${n.type || 'SYSTEM'}</div>
        <div class="notif-msg">${n.message}</div>
        <div class="notif-time">${new Date(n.createdAt).toLocaleString()}</div>
      </div>
    `).join('');
  } catch {}
}

function toggleNotifDropdown() {
  const dd = document.getElementById('notifDropdown');
  dd.style.display = dd.style.display === 'none' ? 'block' : 'none';
  if (dd.style.display === 'block') loadNotifications();
}

async function markNotifRead(id) {
  await fetch(`/api/notifications/${id}/read`, { method: 'PATCH' });
  loadNotifications();
}

async function markAllNotifRead() {
  await fetch('/api/notifications/read-all', {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ email: user.email })
  });
  loadNotifications();
}

// Close dropdown on outside click
document.addEventListener('click', e => {
  const wrap = document.getElementById('notifWrap');
  if (wrap && !wrap.contains(e.target)) {
    document.getElementById('notifDropdown').style.display = 'none';
  }
});

/* ─── PAYSLIP DOWNLOADS ─────────────────────────────── */
function downloadPayslipPdf(id) { window.open(`/api/payroll/${id}/pdf`, '_blank'); }
function downloadPayslipTxt(id) { window.open(`/api/payroll/${id}/txt`, '_blank'); }

/* ─── HR MANAGER DROPDOWN ────────────────────────────── */
async function populateHrManagerDropdown() {
  const sel = document.getElementById('addHrManager');
  if (!sel) return;
  try {
    const res = await fetch('/api/admin/hr-managers');
    const hrs = res.ok ? await res.json() : [];
    sel.innerHTML = '<option value="">— No HR Manager —</option>' +
      hrs.map(h => `<option value="${h.loginId}">${h.name} (${h.email})</option>`).join('');
  } catch {}
}

/* ─── HR PANEL ───────────────────────────────────────── */
let hrTeamData = [];
let hrSalaryEmpCode = '';

async function loadHrPanel() {
  try {
    const res = await fetch(`/api/employees/hr-team?email=${encodeURIComponent(user.email)}`);
    hrTeamData = res.ok ? await res.json() : [];
  } catch { hrTeamData = []; }
  renderHrTeam();
  populateHrAttDropdown();
}

function renderHrTeam() {
  const tbody = document.getElementById('hrTeamBody');
  if (!tbody) return;
  if (!hrTeamData.length) { tbody.innerHTML = emptyRow(8, 'No employees assigned to you.'); return; }
  tbody.innerHTML = hrTeamData.map(e => `
    <tr>
      <td><code class="small">${e.employeeCode}</code></td>
      <td><strong>${e.firstName} ${e.lastName}</strong></td>
      <td>${e.designation||'—'}</td>
      <td>${e.bankAccountNo ? '<i class="bi bi-check-circle-fill text-success"></i>' : '<i class="bi bi-x-circle text-danger"></i>'}</td>
      <td>${e.panNumber ? '<i class="bi bi-check-circle-fill text-success"></i>' : '<i class="bi bi-x-circle text-danger"></i>'}</td>
      <td>${e.aadhaarNumber ? '<i class="bi bi-check-circle-fill text-success"></i>' : '<i class="bi bi-x-circle text-danger"></i>'}</td>
      <td>${moneyINR(e.basicSalary)}</td>
      <td><button class="btn btn-primary btn-sm" onclick="openHrSalaryForm('${e.employeeCode}','${e.firstName} ${e.lastName}')">Set Salary</button></td>
    </tr>`).join('');
}

function openHrSalaryForm(code, name) {
  hrSalaryEmpCode = code;
  document.getElementById('hrSalaryTitle').textContent = 'Set Salary — ' + name;
  document.getElementById('hrSalaryForm').classList.remove('d-none');
  const emp = hrTeamData.find(e => e.employeeCode === code);
  if (emp) {
    document.getElementById('hrBasicSalary').value = emp.basicSalary || '';
    document.getElementById('hrDesignation').value = emp.designation || '';
  }
}

function closeHrSalaryForm() {
  document.getElementById('hrSalaryForm').classList.add('d-none');
  hrSalaryEmpCode = '';
}

async function submitHrSalary() {
  const payload = {
    hrEmail: user.email,
    employeeCode: hrSalaryEmpCode,
    basicSalary: document.getElementById('hrBasicSalary').value,
    designation: document.getElementById('hrDesignation').value.trim(),
    employmentType: document.getElementById('hrEmploymentType').value,
    pfNumber: document.getElementById('hrPfNumber').value.trim()
  };
  try {
    const res = await fetch('/api/employees/salary', {
      method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    showAlert('hrSalaryAlert', res.ok ? '✓ Salary updated.' : data.message, res.ok?'success':'error');
    if (res.ok) { closeHrSalaryForm(); loadHrPanel(); }
  } catch { showAlert('hrSalaryAlert','Network error.'); }
}

function populateHrAttDropdown() {
  const sel = document.getElementById('hrAttEmpCode');
  if (!sel) return;
  sel.innerHTML = '<option value="">— Select —</option>' +
    hrTeamData.map(e => `<option value="${e.employeeCode}">${e.firstName} ${e.lastName}</option>`).join('');
}

async function fetchHrAttendance() {
  const code = document.getElementById('hrAttEmpCode')?.value;
  const month = document.getElementById('hrAttMonth')?.value;
  const year = document.getElementById('hrAttYear')?.value;
  if (!code) { alert('Select an employee.'); return; }
  try {
    const emp = hrTeamData.find(e => e.employeeCode === code);
    if (!emp) return;
    const res = await fetch(`/api/attendance/my?email=${encodeURIComponent(emp.email)}`);
    const list = res.ok ? await res.json() : [];
    const filtered = list.filter(a => {
      const d = new Date(a.date);
      return (d.getMonth()+1) == month && d.getFullYear() == year;
    });
    const tbody = document.getElementById('hrAttBody');
    const wrap = document.getElementById('hrAttTableWrap');
    if (wrap) wrap.hidden = false;
    tbody.innerHTML = filtered.length ? filtered.map(a => `
      <tr>
        <td>${a.date}</td><td>${fmtTime(a.checkIn)}</td><td>${fmtTime(a.checkOut)}</td>
        <td>${a.totalHours ? a.totalHours + 'h' : '—'}</td><td>${badge(a.status)}</td>
      </tr>`).join('') : emptyRow(5, 'No attendance records found.');
  } catch { alert('Error fetching attendance.'); }
}

/* ─── REPORTS ────────────────────────────────────────── */
let reportChart1 = null, reportChart2 = null;

async function loadReports() {
  try {
    const statsRes = await fetch(`/api/admin/stats?email=${encodeURIComponent(user.email)}`);
    const stats = statsRes.ok ? await statsRes.json() : {};
    document.getElementById('reportStats').innerHTML = `
      <div class="col-md-3"><div class="card border-0 shadow-sm stat-card"><div class="card-body">
        <div class="text-muted small fw-semibold text-uppercase">Total Employees</div>
        <div class="display-6 fw-bold">${stats.totalEmployees||0}</div>
      </div></div></div>
      <div class="col-md-3"><div class="card border-0 shadow-sm" style="border-left:3px solid #198754!important"><div class="card-body">
        <div class="text-muted small fw-semibold text-uppercase">Active</div>
        <div class="display-6 fw-bold text-success">${stats.activeEmployees||0}</div>
      </div></div></div>`;
  } catch {}

  // Chart 1: Payroll trend (last 6 months)
  try {
    const now = new Date();
    const labels = [], data = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      labels.push(MONTHS[d.getMonth()] + ' ' + d.getFullYear());
      const m = d.getMonth()+1, y = d.getFullYear();
      const res = await fetch(`/api/payroll/all?email=${encodeURIComponent(user.email)}&month=${m}&year=${y}`);
      const list = res.ok ? await res.json() : [];
      data.push(list.reduce((s,p) => s + parseFloat(p.netSalary||0), 0));
    }
    if (reportChart1) reportChart1.destroy();
    reportChart1 = new Chart(document.getElementById('reportChart1'), {
      type: 'bar',
      data: { labels, datasets: [{ label:'Total Net Payout (₹)', data, backgroundColor:'rgba(13,110,253,0.6)', borderRadius:4 }] },
      options: { responsive:true, plugins:{ title:{ display:true, text:'Monthly Payroll Trend' } } }
    });
  } catch {}

  // Chart 2: Leave breakdown
  try {
    const res = await fetch(`/api/leaves/all?email=${encodeURIComponent(user.email)}`);
    const list = res.ok ? await res.json() : [];
    const counts = {APPROVED:0, PENDING:0, REJECTED:0};
    list.forEach(l => { if (counts[l.status] !== undefined) counts[l.status]++; });
    if (reportChart2) reportChart2.destroy();
    reportChart2 = new Chart(document.getElementById('reportChart2'), {
      type: 'doughnut',
      data: { labels:['Approved','Pending','Rejected'], datasets:[{ data:[counts.APPROVED,counts.PENDING,counts.REJECTED], backgroundColor:['#198754','#ffc107','#dc3545'] }] },
      options: { responsive:true, plugins:{ title:{ display:true, text:'Leave Status Breakdown' } } }
    });
  } catch {}
}

/* ─── LOGOUT ──────────────────────────────────────────── */
function logout() { sessionStorage.clear(); window.location.href = '/index.html'; }

/* ─── BOOTSTRAP NOTIFICATION HELPERS ─────────────────── */
function toggleNotifications() {
  // Bootstrap handles dropdown via data-bs-toggle, this is a fallback
}
function markAllRead() { markAllNotifRead(); }
