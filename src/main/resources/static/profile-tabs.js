// Profile Tab Switching
function switchProfileTab(tab) {
  document.querySelectorAll('#sec-profile .nav-link').forEach(l => l.classList.remove('active'));
  event.target.classList.add('active');
  
  document.getElementById('profileResume').classList.add('d-none');
  document.getElementById('profilePrivate').classList.add('d-none');
  document.getElementById('profileSalary').classList.add('d-none');
  document.getElementById('profileSecurity').classList.add('d-none');
  document.getElementById('profileSkills').classList.add('d-none');
  document.getElementById('profileCertifications').classList.add('d-none');
  
  if (tab === 'resume') {
    document.getElementById('profileResume').classList.remove('d-none');
    loadDocuments();
  } else if (tab === 'private') {
    document.getElementById('profilePrivate').classList.remove('d-none');
  } else if (tab === 'salary') {
    document.getElementById('profileSalary').classList.remove('d-none');
  } else if (tab === 'security') {
    document.getElementById('profileSecurity').classList.remove('d-none');
  } else if (tab === 'skills') {
    document.getElementById('profileSkills').classList.remove('d-none');
    loadSkills();
  } else if (tab === 'certifications') {
    document.getElementById('profileCertifications').classList.remove('d-none');
    loadCertifications();
  }
}

// Skills Management
async function loadSkills() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const res = await fetch(`/api/profile/skills?email=${user.email}`);
  const skills = await res.json();
  
  const html = skills.map(s => `
    <div class="card mb-2">
      <div class="card-body py-2 d-flex justify-content-between align-items-center">
        <div>
          <strong>${s.skillName}</strong> - ${s.proficiencyLevel || 'N/A'} 
          <span class="text-muted">(${s.yearsOfExperience || 0} years)</span>
        </div>
        <button class="btn btn-sm btn-outline-danger" onclick="deleteSkill('${s.id}')">Delete</button>
      </div>
    </div>
  `).join('');
  
  document.getElementById('skillsList').innerHTML = html || '<p class="text-muted">No skills added yet.</p>';
}

async function addSkill() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const skillName = document.getElementById('skillName').value;
  const proficiencyLevel = document.getElementById('skillLevel').value;
  const yearsOfExperience = parseInt(document.getElementById('skillYears').value) || 0;
  
  if (!skillName) {
    showAlert('skillsAlert', 'Please enter skill name', 'danger');
    return;
  }
  
  const res = await fetch('/api/profile/skills', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: user.email, skillName, proficiencyLevel, yearsOfExperience })
  });
  
  const data = await res.json();
  showAlert('skillsAlert', data.message, res.ok ? 'success' : 'danger');
  
  if (res.ok) {
    document.getElementById('skillName').value = '';
    document.getElementById('skillYears').value = '';
    loadSkills();
  }
}

async function deleteSkill(id) {
  if (!confirm('Delete this skill?')) return;
  await fetch(`/api/profile/skills/${id}`, { method: 'DELETE' });
  loadSkills();
}

// Certifications Management
async function loadCertifications() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const res = await fetch(`/api/profile/certifications?email=${user.email}`);
  const certs = await res.json();
  
  const html = certs.map(c => `
    <div class="card mb-2">
      <div class="card-body py-2">
        <div class="d-flex justify-content-between align-items-start">
          <div>
            <strong>${c.certificationName}</strong><br>
            <small class="text-muted">${c.issuingOrganization || 'N/A'}</small><br>
            ${c.credentialId ? `<small>ID: ${c.credentialId}</small>` : ''}
            ${c.credentialUrl ? `<br><a href="${c.credentialUrl}" target="_blank" class="small">View Credential</a>` : ''}
          </div>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteCertification('${c.id}')">Delete</button>
        </div>
      </div>
    </div>
  `).join('');
  
  document.getElementById('certificationsList').innerHTML = html || '<p class="text-muted">No certifications added yet.</p>';
}

async function addCertification() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const certificationName = document.getElementById('certName').value;
  const issuingOrganization = document.getElementById('certOrg').value;
  const credentialId = document.getElementById('certId').value;
  const credentialUrl = document.getElementById('certUrl').value;
  
  if (!certificationName) {
    showAlert('certAlert', 'Please enter certification name', 'danger');
    return;
  }
  
  const res = await fetch('/api/profile/certifications', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: user.email, certificationName, issuingOrganization, credentialId, credentialUrl })
  });
  
  const data = await res.json();
  showAlert('certAlert', data.message, res.ok ? 'success' : 'danger');
  
  if (res.ok) {
    document.getElementById('certName').value = '';
    document.getElementById('certOrg').value = '';
    document.getElementById('certId').value = '';
    document.getElementById('certUrl').value = '';
    loadCertifications();
  }
}

async function deleteCertification(id) {
  if (!confirm('Delete this certification?')) return;
  await fetch(`/api/profile/certifications/${id}`, { method: 'DELETE' });
  loadCertifications();
}

// Documents Management
async function loadDocuments() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const res = await fetch(`/api/profile/documents?email=${user.email}&type=RESUME`);
  const docs = await res.json();
  
  const html = docs.map(d => `
    <div class="card mb-2">
      <div class="card-body py-2 d-flex justify-content-between align-items-center">
        <div>
          <strong>${d.documentName}</strong><br>
          <a href="${d.documentUrl}" target="_blank" class="small">View Document</a>
        </div>
        <button class="btn btn-sm btn-outline-danger" onclick="deleteDocument('${d.id}')">Delete</button>
      </div>
    </div>
  `).join('');
  
  document.getElementById('documentsList').innerHTML = html || '<p class="text-muted">No documents uploaded yet.</p>';
}

async function addDocument() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const documentName = document.getElementById('docName').value;
  const documentUrl = document.getElementById('docUrl').value;
  
  if (!documentName || !documentUrl) {
    showAlert('resumeAlert', 'Please enter document name and URL', 'danger');
    return;
  }
  
  const res = await fetch('/api/profile/documents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: user.email, documentType: 'RESUME', documentName, documentUrl })
  });
  
  const data = await res.json();
  showAlert('resumeAlert', data.message, res.ok ? 'success' : 'danger');
  
  if (res.ok) {
    document.getElementById('docName').value = '';
    document.getElementById('docUrl').value = '';
    loadDocuments();
  }
}

async function deleteDocument(id) {
  if (!confirm('Delete this document?')) return;
  await fetch(`/api/profile/documents/${id}`, { method: 'DELETE' });
  loadDocuments();
}

// Save Private Info
async function savePrivateInfo() {
  const user = JSON.parse(sessionStorage.getItem('user'));
  const res = await fetch('/api/employee/update-profile', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: user.email,
      firstName: document.getElementById('profFirstName').value,
      lastName: document.getElementById('profLastName').value,
      phone: document.getElementById('profPhone').value,
      aadhaarNumber: document.getElementById('profAadhaar').value,
      panNumber: document.getElementById('profPanNumber').value
    })
  });
  
  const data = await res.json();
  showAlert('privateAlert', data.message, res.ok ? 'success' : 'danger');
}

function showAlert(elementId, message, type) {
  const el = document.getElementById(elementId);
  el.className = `alert alert-${type} py-2 small`;
  el.textContent = message;
  el.classList.remove('d-none');
  setTimeout(() => el.classList.add('d-none'), 3000);
}
