/* ─── STATE ─────────────────────────────────────────── */
const user = JSON.parse(sessionStorage.getItem('user'));
if (!user) window.location.href = '/index.html';

const ROLE = user.role || '';
const CAN_MANAGE   = ['ADMIN', 'HR_OFFICER'].includes(ROLE);
const CAN_PAYROLL  = ['ADMIN', 'PAYROLL_OFFICER'].includes(ROLE);
const IS_ADMIN     = ROLE === 'ADMIN';
const IS_EMPLOYEE  = ROLE === 'EMPLOYEE';

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

/* ─── INIT ───────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  initUser();
  initSidebar();
  initDateFilters();
  showSection('home');
  loadHome();
});

/* ─── DYNAMIC DATE FILTERS ───────────────────────────── */
function initDateFilters() {
  const now = new Date();
  const curMonth = now.getMonth() + 1; // 1-indexed
  const curYear = now.getFullYear();
  const monthNames = ['January','February','March','April','May','June',
                      'July','August','September','October','November','December'];

  // Fill all month selects
  document.querySelectorAll('.month-select').forEach(sel => {
    sel.innerHTML = '';
    for (let i = 1; i <= 12; i++) {
      const opt = document.createElement('option');
      opt.value = i;
      opt.textContent = monthNames[i - 1];
      if (i === curMonth) opt.selected = true;
      sel.appendChild(opt);
    }
  });

  // Fill all year inputs
  document.querySelectorAll('.year-input').forEach(inp => {
    inp.value = curYear;
  });
}

function initUser() {
  const initials = ((user.firstName||'')[0]||'') + ((user.lastName||'')[0]||'');
  document.getElementById('avatarInitials').textContent = initials.toUpperCase();
  document.getElementById('sidebarName').textContent = user.firstName + ' ' + user.lastName;
  document.getElementById('sidebarRole').textContent = ROLE.replace('_', ' ');

  // Hide nav items based on role
  if (IS_EMPLOYEE) {
    document.getElementById('navReports').style.display = 'none';
  }
}

/* ─── SIDEBAR ────────────────────────────────────────── */
function initSidebar() {
  document.querySelectorAll('nav a[data-section]').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      const sec = link.dataset.section;
      document.querySelectorAll('nav a').forEach(a => a.classList.remove('active'));
      link.classList.add('active');
      showSection(sec);
      loadSection(sec);
    });
  });
}

function showSection(sec) {
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.getElementById('sec-' + sec)?.classList.add('active');
  const titles = { home:'Home', employees:'Employees', attendance:'Attendance', timeoff:'Time Off', payroll:'Payroll', reports:'Reports', profile:'Profile' };
  document.getElementById('topTitle').textContent = titles[sec] || sec;
}

function loadSection(sec) {
  if (sec === 'home')       loadHome();
  if (sec === 'employees')  loadEmployees();
  if (sec === 'attendance') loadAttendance();
  if (sec === 'timeoff')    loadTimeOff();
  if (sec === 'payroll')    loadPayroll();
  if (sec === 'reports')    loadReports();
  if (sec === 'profile')    loadProfile();
}

/* ─── UTILS ──────────────────────────────────────────── */
function showAlert(id, msg, type='error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.className = 'alert ' + type + ' show';
  setTimeout(() => el.classList.remove('show'), 5000);
}

function badge(status) {
  const cls = {
    ACTIVE:'active', INACTIVE:'inactive', PRESENT:'present', ABSENT:'absent',
    APPROVED:'approved', REJECTED:'rejected', PENDING:'pending',
    GENERATED:'generated', PAID:'paid', CANCELLED:'cancelled'
  }[status?.toUpperCase()] || 'pending';
  return `<span class="badge badge-${cls}">${status}</span>`;
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

/* ─── HOME / REPORTS ──────────────────────────────────── */
async function loadHome() {
  try {
    const [statsRes, attRes, leavesRes] = await Promise.all([
      fetch(`/api/admin/stats?email=${encodeURIComponent(user.email)}`),
      fetch(`/api/attendance/today-status?email=${encodeURIComponent(user.email)}`),
      fetch(`/api/leaves/my?email=${encodeURIComponent(user.email)}`)
    ]);
    const stats  = statsRes.ok  ? await statsRes.json()  : {};
    const att    = attRes.ok    ? await attRes.json()    : {};
    const leaves = leavesRes.ok ? await leavesRes.json() : [];

    document.getElementById('statTotal').textContent   = stats.totalEmployees  || 0;
    document.getElementById('statActive').textContent  = stats.activeEmployees || 0;
    document.getElementById('statCheckedIn').textContent = att.checkedIn ? 'Yes' : 'No';
    const pending = leaves.filter(l => l.status === 'PENDING').length;
    document.getElementById('statPending').textContent = pending;
  } catch(e) {
    console.error('loadHome error', e);
  }
}

async function loadReports() {
  try {
    const res = await fetch(`/api/admin/stats?email=${encodeURIComponent(user.email)}`);
    const stats = res.ok ? await res.json() : {};
    document.getElementById('rptTotal').textContent  = stats.totalEmployees  || 0;
    document.getElementById('rptActive').textContent = stats.activeEmployees || 0;
  } catch(e) {
    console.error('loadReports error', e);
  }
}

/* ─── EMPLOYEES ───────────────────────────────────────── */
let allEmployees = [];

async function loadEmployees() {
  if (!CAN_MANAGE && !IS_ADMIN) {
    document.getElementById('btnAddEmployee').style.display = 'none';
  }
  try {
    const res = await fetch('/api/admin/users');
    allEmployees = res.ok ? await res.json() : [];
  } catch { allEmployees = []; }
  renderEmployeeTable(allEmployees);
}

function renderEmployeeTable(list) {
  const tbody = document.getElementById('empBody');
  if (!list.length) { tbody.innerHTML = emptyRow(6, 'No employees found.'); return; }
  tbody.innerHTML = list.map((e, i) => `
    <tr>
      <td><span style="font-family:monospace;font-size:12px;color:var(--accent2)">${e.loginId||'—'}</span></td>
      <td><b>${e.firstName} ${e.lastName}</b></td>
      <td style="color:var(--muted)">${e.email}</td>
      <td>${e.role?.replace('_',' ')}</td>
      <td>${badge(e.status)}</td>
      <td>${CAN_MANAGE ? `<button class="btn btn-ghost btn-sm" onclick="removeEmployee('${e.loginId}')">Remove</button>` : ''}</td>
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
    firstName:   document.getElementById('addFirst').value.trim(),
    lastName:    document.getElementById('addLast').value.trim(),
    email:       document.getElementById('addEmail').value.trim(),
    phone:       document.getElementById('addPhone').value.trim(),
    role:        document.getElementById('addRole').value,
    designation: document.getElementById('addDesig').value.trim(),
    requestedBy: user.email
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
      document.getElementById('addEmpForm').reset();
      loadEmployees();
    } else { showAlert('addEmpAlert', data.message); }
  } catch { showAlert('addEmpAlert','Network error.'); }
}

async function removeEmployee(loginId) {
  if (!confirm(`Remove user ${loginId}? This cannot be undone.`)) return;
  const res = await fetch(`/api/admin/users/${loginId}`, { method:'DELETE' });
  const data = await res.json();
  if (res.ok) loadEmployees();
  else alert(data.message);
}

/* ─── ATTENDANCE ──────────────────────────────────────── */
let todayAtt = {};

async function loadAttendance() {
  await loadTodayStatus();
  if (IS_EMPLOYEE || ROLE === 'HR_OFFICER') {
    switchAttTab('my');
  } else {
    switchAttTab('all');
  }
}

async function loadTodayStatus() {
  try {
    const res = await fetch(`/api/attendance/today-status?email=${encodeURIComponent(user.email)}`);
    todayAtt = res.ok ? await res.json() : {};
  } catch { todayAtt = {}; }
  renderCheckinCard();
}

function renderCheckinCard() {
  const card = document.getElementById('checkinCard');
  const btnArea = document.getElementById('checkinBtnArea');
  const statusEl = document.getElementById('checkinStatus');

  if (todayAtt.checkedIn && todayAtt.checkedOut) {
    statusEl.innerHTML = `<span style="color:var(--success)">✓ Checked in at ${fmtTime(todayAtt.checkIn)} &nbsp;·&nbsp; Checked out at ${fmtTime(todayAtt.checkOut)}</span>`;
    btnArea.innerHTML = `<span style="color:var(--muted);font-size:13px">Done for today ✓</span>`;
  } else if (todayAtt.checkedIn) {
    statusEl.innerHTML = `<span style="color:var(--warning)">● Checked in at ${fmtTime(todayAtt.checkIn)}</span>`;
    btnArea.innerHTML = `<button class="btn-checkout" onclick="doCheckout()">Check Out</button>`;
  } else {
    statusEl.innerHTML = `<span style="color:var(--muted)">Not checked in yet</span>`;
    btnArea.innerHTML = `<button class="btn-checkin" onclick="doCheckin()">Check In</button>`;
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

async function doCheckin() {
  try {
    const res = await fetch('/api/attendance/checkin', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email: user.email})
    });
    const data = await res.json();
    showAlert('attAlert', res.ok ? '✓ ' + data.message : data.message, res.ok ? 'success':'error');
    if (res.ok) { await loadTodayStatus(); loadMyAttendance(); }
  } catch { showAlert('attAlert','Network error.'); }
}

async function doCheckout() {
  try {
    const res = await fetch('/api/attendance/checkout', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email: user.email})
    });
    const data = await res.json();
    showAlert('attAlert', res.ok ? '✓ ' + data.message : data.message, res.ok ? 'success':'error');
    if (res.ok) { await loadTodayStatus(); loadMyAttendance(); }
  } catch { showAlert('attAlert','Network error.'); }
}

function switchAttTab(tab) {
  document.querySelectorAll('#attTabs .tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('attTab-'+tab)?.classList.add('active');
  document.getElementById('attMy').style.display  = tab==='my'  ? 'block':'none';
  document.getElementById('attAll').style.display = tab==='all' ? 'block':'none';
  if (tab==='my')  loadMyAttendance();
  if (tab==='all') loadAllAttendance();
}

async function loadMyAttendance() {
  const res = await fetch(`/api/attendance/my?email=${encodeURIComponent(user.email)}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('myAttBody');
  tbody.innerHTML = list.length ? list.map(a => `
    <tr>
      <td>${a.date}</td>
      <td>${fmtTime(a.checkIn)}</td>
      <td>${fmtTime(a.checkOut)}</td>
      <td>${a.totalHours ? a.totalHours + 'h' : '—'}</td>
      <td>${badge(a.status)}</td>
    </tr>`).join('') : emptyRow(5);
}

async function loadAllAttendance() {
  const m = document.getElementById('attMonth').value;
  const y = document.getElementById('attYear').value;
  const res = await fetch(`/api/attendance/all?email=${encodeURIComponent(user.email)}&month=${m}&year=${y}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('allAttBody');
  tbody.innerHTML = list.length ? list.map(a => `
    <tr>
      <td><b>${a.employeeName}</b><br><span style="color:var(--muted);font-size:11px">${a.employeeCode}</span></td>
      <td>${a.date}</td>
      <td>${fmtTime(a.checkIn)}</td>
      <td>${fmtTime(a.checkOut)}</td>
      <td>${a.totalHours ? a.totalHours + 'h' : '—'}</td>
      <td>${badge(a.status)}</td>
    </tr>`).join('') : emptyRow(6);
}

/* ─── TIME OFF / LEAVE ────────────────────────────────── */
function loadTimeOff() {
  switchLeaveTab('my');
}

function switchLeaveTab(tab) {
  document.querySelectorAll('#leaveTabs .tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('leaveTab-'+tab)?.classList.add('active');
  document.getElementById('leaveApply').style.display = tab==='apply' ? 'block':'none';
  document.getElementById('leaveMy').style.display    = tab==='my'    ? 'block':'none';
  document.getElementById('leaveAll').style.display   = tab==='all'   ? 'block':'none';
  if (tab==='my')  loadMyLeaves();
  if (tab==='all') loadAllLeaves();
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
  const tbody = document.getElementById('myLeaveBody');
  tbody.innerHTML = list.length ? list.map(l => `
    <tr>
      <td>${l.leaveType}</td>
      <td>${l.startDate}</td>
      <td>${l.endDate}</td>
      <td style="max-width:200px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${l.reason||'—'}</td>
      <td>${badge(l.status)}</td>
    </tr>`).join('') : emptyRow(5);
}

async function loadAllLeaves() {
  const res = await fetch(`/api/leaves/all?email=${encodeURIComponent(user.email)}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('allLeaveBody');
  tbody.innerHTML = list.length ? list.map(l => `
    <tr>
      <td><b>${l.employeeName}</b><br><span style="color:var(--muted);font-size:11px">${l.employeeCode}</span></td>
      <td>${l.leaveType}</td>
      <td>${l.startDate} → ${l.endDate}</td>
      <td style="max-width:160px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${l.reason||'—'}</td>
      <td>${badge(l.status)}</td>
      <td>
        ${l.status==='PENDING' && (CAN_PAYROLL||CAN_MANAGE) ? `
          <button class="btn btn-success btn-sm" onclick="updateLeave('${l.id}','APPROVED')">Approve</button>
          <button class="btn btn-danger btn-sm" onclick="updateLeave('${l.id}','REJECTED')" style="margin-left:4px">Reject</button>
        ` : ''}
        ${l.status==='PENDING' && l.employeeCode===user.loginId ? `
          <button class="btn btn-ghost btn-sm" onclick="updateLeave('${l.id}','CANCELLED')">Cancel</button>
        ` : ''}
      </td>
    </tr>`).join('') : emptyRow(6);
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

/* ─── PAYROLL ─────────────────────────────────────────── */
function loadPayroll() {
  switchPayrollTab('my');
}

function switchPayrollTab(tab) {
  document.querySelectorAll('#payrollTabs .tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('payrollTab-'+tab)?.classList.add('active');
  document.getElementById('payrollMy').style.display       = tab==='my'       ? 'block':'none';
  document.getElementById('payrollGenerate').style.display = tab==='generate' ? 'block':'none';
  document.getElementById('payrollAll').style.display      = tab==='all'      ? 'block':'none';
  if (tab==='my')  loadMyPayroll();
  if (tab==='all') loadAllPayroll();
}

async function loadMyPayroll() {
  const res = await fetch(`/api/payroll/my?email=${encodeURIComponent(user.email)}`);
  const list = res.ok ? await res.json() : [];
  const container = document.getElementById('myPayrollList');
  if (!list.length) { container.innerHTML = '<p style="color:var(--muted);text-align:center;padding:40px">No payslips yet.</p>'; return; }
  container.innerHTML = list.map(p => `
    <div class="payslip-card">
      <div class="payslip-header">
        <div>
          <div class="payslip-period">${monthName(p.payMonth)} ${p.payYear}</div>
          <div style="font-size:11px;color:var(--muted);margin-top:2px">${p.presentDays} days worked · ${badge(p.status)}</div>
        </div>
        <div class="payslip-net">${moneyINR(p.netSalary)}</div>
      </div>
      <div class="payslip-grid">
        <div>
          <div class="payslip-row"><span>Basic</span><span>${moneyINR(p.basicSalary)}</span></div>
          <div class="payslip-row"><span>HRA</span><span>${moneyINR(p.hra)}</span></div>
          <div class="payslip-row"><span>Bonus</span><span>${moneyINR(p.bonus)}</span></div>
          <div class="payslip-row total"><span>Gross</span><span>${moneyINR(p.grossSalary)}</span></div>
        </div>
        <div>
          <div class="payslip-row deduction"><span>PF (12%)</span><span>-${moneyINR(p.pfDeduction)}</span></div>
          <div class="payslip-row deduction"><span>Prof. Tax</span><span>-${moneyINR(p.professionalTax)}</span></div>
          <div class="payslip-row deduction total"><span>Deductions</span><span>-${moneyINR(p.totalDeductions)}</span></div>
        </div>
      </div>
    </div>`).join('');
}

async function submitGeneratePayroll() {
  const payload = {
    generatorEmail: user.email,
    employeeEmail:  document.getElementById('genEmpEmail').value.trim(),
    month: parseInt(document.getElementById('genMonth').value),
    year:  parseInt(document.getElementById('genYear').value)
  };
  if (!payload.employeeEmail) { showAlert('genAlert','Employee email is required.'); return; }
  try {
    const res = await fetch('/api/payroll/generate', {
      method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    showAlert('genAlert', res.ok ? `✓ Payroll generated. Net: ${moneyINR(data.netSalary)}` : data.message, res.ok?'success':'error');
    if (res.ok) loadAllPayroll();
  } catch { showAlert('genAlert','Network error.'); }
}

async function loadAllPayroll() {
  const m = document.getElementById('payMonth').value;
  const y = document.getElementById('payYear').value;
  const res = await fetch(`/api/payroll/all?email=${encodeURIComponent(user.email)}&month=${m}&year=${y}`);
  const list = res.ok ? await res.json() : [];
  const tbody = document.getElementById('allPayrollBody');
  tbody.innerHTML = list.length ? list.map(p => `
    <tr>
      <td><b>${p.employeeName}</b><br><span style="color:var(--muted);font-size:11px">${p.employeeCode}</span></td>
      <td>${monthName(p.payMonth)} ${p.payYear}</td>
      <td>${p.presentDays}/${p.totalWorkingDays}</td>
      <td>${moneyINR(p.grossSalary)}</td>
      <td style="color:var(--danger)">-${moneyINR(p.totalDeductions)}</td>
      <td style="color:var(--success);font-weight:600">${moneyINR(p.netSalary)}</td>
      <td>${badge(p.status)}</td>
      <td>
        ${p.status==='GENERATED' ? `<button class="btn btn-success btn-sm" onclick="markPaid('${p.id}')">Mark Paid</button>` : ''}
      </td>
    </tr>`).join('') : emptyRow(8);
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
    document.getElementById('profAvatarInit').textContent = initials.toUpperCase();
    document.getElementById('profName').textContent = (p.firstName||'') + ' ' + (p.lastName||'');
    document.getElementById('profRoleLine').textContent = (p.role||'').replace('_',' ');
    document.getElementById('profEmpCode').textContent = p.employeeCode ? 'ID: ' + p.employeeCode : '';

    document.getElementById('profFirstName').value  = p.firstName||'';
    document.getElementById('profLastName').value   = p.lastName||'';
    document.getElementById('profPhone').value      = p.phone||'';
    document.getElementById('profDesig').value      = p.designation||'';
    document.getElementById('profBank').value       = p.bankAccountNo||'';
    document.getElementById('profPAN').value        = p.panNumber||'';
    document.getElementById('profSalary').value     = p.basicSalary||'';
    document.getElementById('profJoining').value    = p.joiningDate||'';
    document.getElementById('profEmpType').value    = p.employmentType||'';
  } catch(e) { console.error(e); }
}

async function saveProfile() {
  const payload = {
    email:       user.email,
    firstName:   document.getElementById('profFirstName').value.trim(),
    lastName:    document.getElementById('profLastName').value.trim(),
    phone:       document.getElementById('profPhone').value.trim(),
    designation: document.getElementById('profDesig').value.trim(),
    bankAccountNo: document.getElementById('profBank').value.trim(),
    panNumber:   document.getElementById('profPAN').value.trim()
  };
  // Only allow salary edit for admin/payroll
  if (CAN_PAYROLL) payload.basicSalary = document.getElementById('profSalary').value;
  try {
    const res = await fetch('/api/employees/profile', {
      method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    const data = await res.json();
    showAlert('profAlert', res.ok ? '✓ Profile updated.' : data.message, res.ok?'success':'error');
    if (res.ok) loadProfile();
  } catch { showAlert('profAlert','Network error.'); }
}

/* ─── LOGOUT ──────────────────────────────────────────── */
function logout() { sessionStorage.clear(); window.location.href = '/index.html'; }
