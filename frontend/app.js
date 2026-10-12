const API = window.location.hostname === 'localhost' && window.location.port && window.location.port !== '8080'
  ? 'http://localhost:8080/api'
  : `${window.location.origin}/api`;
const $ = (id) => document.getElementById(id);
const state = { view: 'overview', patients: [], doctors: [], appointments: [], query: '', editing: null, user: null, csrf: '' };
const fields = {
  patient: [
    {name:'fullName',label:'Full name',required:true,placeholder:'e.g. Aarav Sharma'},
    {name:'age',label:'Age',type:'number',required:true,min:0,max:120},
    {name:'gender',label:'Gender',type:'select',required:true,options:['Male','Female','Other']},
    {name:'phone',label:'Phone number',required:true,placeholder:'10-digit mobile number'},
    {name:'email',label:'Email address',type:'email',placeholder:'name@example.com'},
    {name:'bloodGroup',label:'Blood group',type:'select',options:['A+','A-','B+','B-','AB+','AB-','O+','O-','Unknown']},
    {name:'address',label:'Address',full:true,placeholder:'City / area'}
  ],
  doctor: [
    {name:'fullName',label:'Doctor name',required:true,placeholder:'e.g. Dr. Priya Reddy'},
    {name:'specialization',label:'Specialization',required:true,placeholder:'e.g. Cardiology'},
    {name:'phone',label:'Phone number',required:true,placeholder:'Contact number'},
    {name:'email',label:'Email address',type:'email',placeholder:'doctor@example.com'},
    {name:'roomNumber',label:'Room / cabin',placeholder:'e.g. 204'},
    {name:'available',label:'Availability',type:'select',required:true,options:[{label:'Available',value:'true'},{label:'Unavailable',value:'false'}]}
  ],
  appointment: [
    {name:'patientId',label:'Patient',type:'select',required:true,options:()=>state.patients.map(p=>({label:`${p.fullName} · #${p.id}`,value:String(p.id)}))},
    {name:'doctorId',label:'Doctor',type:'select',required:true,options:()=>state.doctors.map(d=>({label:`${d.fullName} · ${d.specialization}`,value:String(d.id)}))},
    {name:'appointmentAt',label:'Date and time',type:'datetime-local',required:true},
    {name:'reason',label:'Reason for visit',required:true,full:true,placeholder:'Brief reason for consultation'},
    {name:'status',label:'Status',type:'select',required:true,options:['SCHEDULED','COMPLETED','CANCELLED']},
    {name:'notes',label:'Notes',full:true,placeholder:'Optional notes'}
  ]
};
const labels = { overview:'Overview', patients:'Patients', doctors:'Doctors', appointments:'Appointments' };
function escapeHtml(value='') {
  return String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function initials(name='') { return name.trim().split(/\s+/).slice(0,2).map(x=>x[0]||'').join('').toUpperCase() || 'NA'; }
function fmtDate(value) {
  if(!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? escapeHtml(value) : d.toLocaleString([], {month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'});
}
function showToast(message, error=false) {
  const toast=$('toast'); toast.textContent=message; toast.classList.toggle('error',error); toast.classList.remove('hidden');
  clearTimeout(showToast.timer); showToast.timer=setTimeout(()=>toast.classList.add('hidden'),3500);
}
async function request(path, options={}) {
  const method=(options.method||'GET').toUpperCase();
  const response = await fetch(`${API}${path}`, {credentials:'include',...options,headers:{'Content-Type':'application/json',...(method!=='GET'&&state.csrf?{'X-XSRF-TOKEN':state.csrf}:{}),...(options.headers||{})}});
  if(!response.ok) {
    let message = `Request failed (${response.status})`;
    try { const data=await response.json(); message=data.message || data.detail || message; } catch {}
    throw new Error(message);
  }
  if(response.status===204) return null;
  return response.json();
}
async function refreshData() {
  try {
    let dashboard,patients,doctors,appointments;
    if(state.user.role==='PATIENT') {
      const [patient,doctorList,appointmentList]=await Promise.all([request('/patients/me'),request('/doctors'),request('/appointments/mine')]);
      patients=[patient]; doctors=doctorList; appointments=appointmentList;
      const today=new Date().toISOString().slice(0,10);
      dashboard={patients:1,doctors:doctors.length,appointmentsToday:appointments.filter(a=>String(a.appointmentAt).startsWith(today)).length,scheduledAppointments:appointments.filter(a=>a.status==='SCHEDULED').length,completedAppointments:appointments.filter(a=>a.status==='COMPLETED').length,cancelledAppointments:appointments.filter(a=>a.status==='CANCELLED').length};
    } else {
      [dashboard,patients,doctors,appointments] = await Promise.all([request('/dashboard'),request('/patients'),request('/doctors'),request('/appointments')]);
    }
    Object.assign(state,{patients,doctors,appointments});
    $('apiError').classList.add('hidden'); $('connectionText').textContent='API connected';
    $('connectionText').parentElement.classList.add('connected');
    $('metricPatients').textContent=dashboard.patients;
    $('metricDoctors').textContent=dashboard.doctors;
    $('metricToday').textContent=dashboard.appointmentsToday;
    $('metricScheduled').textContent=dashboard.scheduledAppointments;
    $('patientNavCount').textContent=dashboard.patients;
    $('statusScheduled').textContent=dashboard.scheduledAppointments;
    $('statusCompleted').textContent=dashboard.completedAppointments;
    $('statusCancelled').textContent=dashboard.cancelledAppointments;
    const total=Math.max(1,dashboard.scheduledAppointments+dashboard.completedAppointments+dashboard.cancelledAppointments);
    $('barScheduled').style.width=`${dashboard.scheduledAppointments/total*100}%`;
    $('barCompleted').style.width=`${dashboard.completedAppointments/total*100}%`;
    $('barCancelled').style.width=`${dashboard.cancelledAppointments/total*100}%`;
    renderTable();
  } catch(err) {
    $('apiError').classList.remove('hidden'); $('connectionText').textContent='API disconnected';
    $('connectionText').parentElement.classList.remove('connected');
    $('tableHead').innerHTML=''; $('tableBody').innerHTML=`<tr><td class="loading-cell" colspan="5">Start the backend to load database records.</td></tr>`;
    $('resultCount').textContent='No connection';
  }
}
function setView(view) {
  if(state.user?.role==='PATIENT'&&view!=='appointments') view='appointments';
  state.view=view; state.query=''; $('searchInput').value='';
  document.querySelectorAll('.nav-item').forEach(b=>b.classList.toggle('active',b.dataset.view===view));
  $('breadcrumbCurrent').textContent=labels[view];
  const content={
    overview:['Good evening, Admin ✦','Here’s what’s happening at your clinic today.','Upcoming appointments','Your clinic’s next scheduled visits'],
    patients:['Patient records','A clear view of the people in your care.','All patients','Registered patient records'],
    doctors:['Medical team','Manage doctor profiles and availability.','Doctor directory','Your clinic’s medical team'],
    appointments:['Appointment schedule','Keep every consultation organized.','All appointments','Bookings and consultation schedule']
  }[view];
  $('pageTitle').textContent=content[0]; $('pageSubtitle').textContent=content[1];
  $('tableTitle').textContent=content[2]; $('tableDescription').textContent=content[3];
  $('primaryAction').innerHTML=view==='patients'?'<span>＋</span> Register patient':view==='doctors'?'<span>＋</span> Add doctor':'<span>＋</span> New appointment';
  $('primaryAction').dataset.action=view==='patients'?'patient':view==='doctors'?'doctor':'appointment';
  renderTable();
}
function statusBadge(status) {
  const cls=status==='COMPLETED'?'completed':status==='CANCELLED'?'cancelled':'scheduled';
  return `<span class="status-badge badge-${cls}">${escapeHtml(status||'SCHEDULED')}</span>`;
}
function renderTable() {
  const view=state.view, q=state.query.toLowerCase();
  let rows=[], headers=[];
  if(view==='patients') {
    headers=['Patient','Age / Gender','Phone','Blood group','Actions'];
    rows=state.patients.filter(p=>[p.fullName,p.phone,p.email,p.bloodGroup].join(' ').toLowerCase().includes(q)).map(p=>[
      `<div class="person-cell"><div class="person-avatar">${initials(p.fullName)}</div><div><strong>${escapeHtml(p.fullName)}</strong><small>Patient ID · ${p.id}</small></div></div>`,
      `${escapeHtml(p.age)} / ${escapeHtml(p.gender)}`,escapeHtml(p.phone),escapeHtml(p.bloodGroup||'—'),
      state.user.role!=='PATIENT'?`<button class="row-action" title="Edit patient" data-edit="patient" data-id="${p.id}">✎</button> <button class="row-action" title="Delete patient" data-delete="patient" data-id="${p.id}">×</button>`:'—'
    ]);
  } else if(view==='doctors') {
    headers=['Doctor','Specialization','Phone','Availability','Actions'];
    rows=state.doctors.filter(d=>[d.fullName,d.specialization,d.phone,d.email].join(' ').toLowerCase().includes(q)).map(d=>[
      `<div class="person-cell"><div class="person-avatar">${initials(d.fullName)}</div><div><strong>${escapeHtml(d.fullName)}</strong><small>Doctor ID · ${d.id}</small></div></div>`,
      escapeHtml(d.specialization),escapeHtml(d.phone),`<span class="status-badge ${d.available?'badge-available':'badge-unavailable'}">${d.available?'Available':'Unavailable'}</span>`,
      state.user.role!=='PATIENT'?`<button class="row-action" title="Edit doctor" data-edit="doctor" data-id="${d.id}">✎</button> <button class="row-action" title="Delete doctor" data-delete="doctor" data-id="${d.id}">×</button>`:'—'
    ]);
  } else {
    headers=['Patient / Doctor','Reason','Date & time','Status','Actions'];
    rows=state.appointments.filter(a=>{
      const p=state.patients.find(x=>x.id===a.patientId), d=state.doctors.find(x=>x.id===a.doctorId);
      return [p?.fullName,d?.fullName,a.reason,a.status].join(' ').toLowerCase().includes(q);
    }).sort((a,b)=>new Date(a.appointmentAt)-new Date(b.appointmentAt)).map(a=>{
      const p=state.patients.find(x=>x.id===a.patientId), d=state.doctors.find(x=>x.id===a.doctorId);
      return [
        `<div class="person-cell"><div class="person-avatar">${initials(p?.fullName||'Patient')}</div><div><strong>${escapeHtml(p?.fullName||`Patient #${a.patientId}`)}</strong><small>${escapeHtml(d?.fullName||`Doctor #${a.doctorId}`)}</small></div></div>`,
        escapeHtml(a.reason),fmtDate(a.appointmentAt),statusBadge(a.status),
        `${state.user.role!=='PATIENT'?`<button class="row-action" title="Edit appointment" data-edit="appointment" data-id="${a.id}">✎</button> <button class="row-action" title="Delete appointment" data-delete="appointment" data-id="${a.id}">×</button>`:''}`
      ];
    });
  }
  if(view==='overview') {
    headers=['Patient / Doctor','Reason','Date & time','Status',''];
    rows=state.appointments.filter(a=>a.status==='SCHEDULED').sort((a,b)=>new Date(a.appointmentAt)-new Date(b.appointmentAt)).slice(0,6).map(a=>{
      const p=state.patients.find(x=>x.id===a.patientId),d=state.doctors.find(x=>x.id===a.doctorId);
      return [`<div class="person-cell"><div class="person-avatar">${initials(p?.fullName||'Patient')}</div><div><strong>${escapeHtml(p?.fullName||`Patient #${a.patientId}`)}</strong><small>${escapeHtml(d?.fullName||`Doctor #${a.doctorId}`)}</small></div></div>`,escapeHtml(a.reason),fmtDate(a.appointmentAt),statusBadge(a.status),state.user.role!=='PATIENT'?`<button class="row-action" title="Edit appointment" data-edit="appointment" data-id="${a.id}">✎</button> <button class="row-action" title="Delete appointment" data-delete="appointment" data-id="${a.id}">×</button>`:''];
    });
  }
  $('tableHead').innerHTML=`<tr>${headers.map(h=>`<th>${h}</th>`).join('')}</tr>`;
  $('tableBody').innerHTML=rows.length?rows.map(r=>`<tr>${r.map(c=>`<td>${c}</td>`).join('')}</tr>`).join(''):`<tr><td class="loading-cell" colspan="${headers.length}">${q?'No matching records found.':'No records yet. Use the action button to add your first record.'}</td></tr>`;
  $('resultCount').textContent=`${rows.length} record${rows.length===1?'':'s'}`;
  $('tableFooterText').textContent=`Showing ${rows.length} ${rows.length===1?'record':'records'}`;
}
function openModal(kind,id=null) {
  if(state.user?.role==='PATIENT'&&(kind!=='appointment'||id))return;
  const type=kind==='patient'?'patient':kind==='doctor'?'doctor':'appointment';
  const current=id?state[{patient:'patients',doctor:'doctors',appointment:'appointments'}[type]].find(x=>x.id===Number(id)):null;
  state.editing={type,id:current?.id||null};
  $('modalTitle').textContent=current?`Edit ${type}`:`${type==='patient'?'Register patient':type==='doctor'?'Add doctor':'New appointment'}`;
  $('modalSubtitle').textContent=current?'Update the record details below.':'Add accurate information to the clinic database.';
  $('formError').classList.add('hidden');
  $('formFields').innerHTML=fields[type].map(f=>{
    let options=typeof f.options==='function'?f.options():f.options;
    const val=current?.[f.name]??(type==='appointment'&&f.name==='status'?'SCHEDULED':type==='doctor'&&f.name==='available'?true:'');
    const common=`name="${f.name}" ${f.required?'required':''} ${f.min!==undefined?`min="${f.min}"`:''} ${f.max!==undefined?`max="${f.max}"`:''} placeholder="${escapeHtml(f.placeholder||'')}"`;
    let control='';
    if(f.type==='select') control=`<select ${common}>${(options||[]).map(o=>{const opt=typeof o==='string'?{label:o,value:o}:o;return `<option value="${escapeHtml(opt.value)}" ${String(val)===String(opt.value)?'selected':''}>${escapeHtml(opt.label)}</option>`}).join('')}</select>`;
    else control=`<input ${common} type="${f.type||'text'}" value="${escapeHtml(f.type==='datetime-local'&&val?String(val).slice(0,16):val)}">`;
    return `<div class="field ${f.full?'full':''}"><label for="${f.name}">${f.label}${f.required?' *':''}</label>${control}</div>`;
  }).join('');
  $('saveBtn').textContent=current?'Save changes':'Save record';
  $('modalBackdrop').classList.remove('hidden');
  setTimeout(()=>$('formFields').querySelector('input,select')?.focus(),50);
}
function closeModal() { $('modalBackdrop').classList.add('hidden'); state.editing=null; }
async function saveRecord(event) {
  event.preventDefault();
  if(!state.editing)return;
  const {type,id}=state.editing, form=new FormData($('recordForm')), payload={};
  for(const f of fields[type]) {
    let value=form.get(f.name);
    if(value===null) continue;
    if(value==='') value=null;
    if(['age','patientId','doctorId'].includes(f.name)&&value!==null)value=Number(value);
    if(f.name==='available')value=value==='true';
    payload[f.name]=value;
  }
  if(type==='doctor'&&payload.available===null)payload.available=true;
  if(type==='appointment'&&!payload.status)payload.status='SCHEDULED';
  const path={patient:'patients',doctor:'doctors',appointment:'appointments'}[type];
  $('saveBtn').disabled=true; $('saveBtn').textContent='Saving…';
  try {
    await request(`/${path}${id?`/${id}`:''}`,{method:id?'PUT':'POST',body:JSON.stringify(payload)});
    closeModal(); await refreshData(); showToast(`${type[0].toUpperCase()+type.slice(1)} ${id?'updated':'saved'} successfully.`);
  } catch(err) {
    $('formError').textContent=err.message||'Unable to save record.'; $('formError').classList.remove('hidden');
  } finally {$('saveBtn').disabled=false;$('saveBtn').textContent=id?'Save changes':'Save record';}
}
async function deleteRecord(type,id) {
  const name=type==='patient'?'patient':type==='doctor'?'doctor':'appointment';
  if(!confirm(`Delete this ${name} record? This action cannot be undone.`))return;
  try { await request(`/${{patient:'patients',doctor:'doctors',appointment:'appointments'}[type]}/${id}`,{method:'DELETE'}); await refreshData(); showToast(`${name[0].toUpperCase()+name.slice(1)} deleted.`); }
  catch(err){showToast(err.message||'Unable to delete record.',true);}
}
document.querySelectorAll('.nav-item').forEach(b=>b.addEventListener('click',()=>{setView(b.dataset.view);$('sidebar').classList.remove('open');}));
$('primaryAction').addEventListener('click',()=>openModal($('primaryAction').dataset.action||'appointment'));
$('viewAllBtn').addEventListener('click',()=>setView(state.view==='overview'?'appointments':'overview'));
$('searchInput').addEventListener('input',e=>{state.query=e.target.value;renderTable();});
$('retryBtn').addEventListener('click',refreshData);
$('closeModal').addEventListener('click',closeModal); $('cancelModal').addEventListener('click',closeModal);
$('modalBackdrop').addEventListener('click',e=>{if(e.target===$('modalBackdrop'))closeModal();});
$('recordForm').addEventListener('submit',saveRecord);
$('mobileMenu').addEventListener('click',()=>$('sidebar').classList.toggle('open'));
document.addEventListener('click',e=>{
  const edit=e.target.closest('[data-edit]'); const del=e.target.closest('[data-delete]'); const quick=e.target.closest('[data-action]');
  if(edit)openModal(edit.dataset.edit,edit.dataset.id);
  if(del)deleteRecord(del.dataset.delete,del.dataset.id);
  if(quick)openModal(quick.dataset.action);
});
document.addEventListener('keydown',e=>{if(e.key==='Escape')closeModal();});
$('helpLink').addEventListener('click',e=>{e.preventDefault();showToast('Use synthetic data only. Your role controls which clinic records you can access.');});

const authModes={
  login:{title:'Sign in to MediCare',intro:'Use the Gmail address and password for your clinic account.',fields:[['email','Gmail address','email'],['password','Password','password']]}
};
function renderAuth() {
  const mode='login';
  const config=authModes[mode]; $('authTitle').textContent=config.title; $('authIntro').textContent=config.intro;
  $('authError').classList.add('hidden');
  $('authForm').innerHTML=config.fields.map(([name,label,type])=>{
    let input=`<input id="auth-${name}" name="${name}" type="${type}" ${type==='email'?'autocomplete="email" pattern=".+@gmail\\.com"':''} ${name==='password'?'maxlength="72" autocomplete="current-password"':''} required>`;
    return `<div class="field"><label for="auth-${name}">${label}</label>${input}</div>`;
  }).join('')+`<button class="primary-btn auth-submit" type="submit">Sign in</button>`;
}
function showAccount(account) {
  state.user=account; $('authShell').classList.add('hidden'); document.querySelector('.app-shell').classList.remove('hidden');
  const name=account.fullName||account.email, roleName=account.role==='DOCTOR'?'Doctor · Clinic Admin':account.role==='ADMIN'?'Administrator':'Patient';
  $('userName').textContent=name; $('userRole').textContent=roleName; $('userAvatar').textContent=initials(name); $('userAvatar').classList.add('avatar'); $('userRole').parentElement.previousElementSibling.textContent=initials(name);
  document.querySelector('.top-avatar').textContent=initials(name); document.querySelector('.top-avatar').setAttribute('aria-label',roleName);
  document.querySelector('.app-shell').classList.remove('patient-mode','doctor-mode','admin-mode');
  document.querySelector('.app-shell').classList.add(account.role==='DOCTOR'?'admin-mode':`${account.role.toLowerCase()}-mode`);
  if(account.role==='PATIENT') { setView('appointments'); $('pageTitle').textContent='My appointments'; $('pageSubtitle').textContent='View your visits or request a new appointment.'; $('tableTitle').textContent='My appointments'; $('tableDescription').textContent='Your clinic bookings'; }
  else if(account.role==='DOCTOR') { $('pageTitle').textContent='Clinic dashboard'; $('pageSubtitle').textContent='Manage patients, doctors, and appointments.'; }
  refreshData();
}
async function bootAuth() {
  document.querySelector('.app-shell').classList.add('hidden'); $('authShell').classList.remove('hidden'); renderAuth();
  try { state.csrf=(await request('/auth/csrf')).token; const account=await request('/auth/me'); showAccount(account); }
  catch { /* No session: keep the sign-in page visible. */ }
}
$('authForm').addEventListener('submit',async event=>{
  event.preventDefault(); const data=Object.fromEntries(new FormData(event.currentTarget));
  const error=$('authError'); error.classList.add('hidden');
  try {
    state.csrf=(await request('/auth/csrf')).token;
    const account=await request('/auth/login',{method:'POST',body:JSON.stringify({email:data.email,password:data.password})});
    showAccount(account);
  } catch(err) { error.textContent=err.message||'Unable to complete account access.'; error.classList.remove('hidden'); }
});
$('signOutBtn').addEventListener('click',async()=>{try{await request('/auth/logout',{method:'POST'});}finally{state.user=null;state.csrf=(await request('/auth/csrf')).token;document.querySelector('.app-shell').classList.add('hidden');renderAuth();$('authShell').classList.remove('hidden');}});
const now=new Date(); $('todayLabel').textContent=now.toLocaleDateString([], {weekday:'long',month:'long',day:'numeric'}).toUpperCase();
bootAuth(); setInterval(()=>{if(state.user)refreshData();},30000);
