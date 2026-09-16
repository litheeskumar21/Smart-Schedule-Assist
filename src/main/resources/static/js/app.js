// Smart Schedule Assist - App v2.0

// ── Theme System ──
const THEME_KEY = 'ssa-theme';
const SIDEBAR_KEY = 'ssa-sidebar-collapsed';

function initTheme() {
  const saved = localStorage.getItem(THEME_KEY) || 'light';
  document.documentElement.setAttribute('data-theme', saved);
  updateThemeIcon(saved);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const next = current === 'light' ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem(THEME_KEY, next);
  updateThemeIcon(next);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('themeToggleBtn');
  if (!btn) return;
  btn.innerHTML = theme === 'dark'
    ? `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`
    : `<svg viewBox="0 0 24 24"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;
}

// ── Sidebar Collapse ──
function initSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const mainContent = document.querySelector('.main-content');
  if (!sidebar || !mainContent) return;
  const collapsed = localStorage.getItem(SIDEBAR_KEY) === 'true';
  if (collapsed) {
    sidebar.classList.add('collapsed');
    mainContent.classList.add('sidebar-collapsed');
  }
}

function toggleSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const mainContent = document.querySelector('.main-content');
  const overlay = document.querySelector('.sidebar-overlay');
  if (!sidebar) return;
  const isMobile = window.innerWidth <= 1024;
  if (isMobile) {
    sidebar.classList.toggle('mobile-open');
    if (overlay) overlay.classList.toggle('show');
  } else {
    sidebar.classList.toggle('collapsed');
    if (mainContent) mainContent.classList.toggle('sidebar-collapsed');
    localStorage.setItem(SIDEBAR_KEY, sidebar.classList.contains('collapsed'));
  }
}

function closeSidebarMobile() {
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.querySelector('.sidebar-overlay');
  if (sidebar) sidebar.classList.remove('mobile-open');
  if (overlay) overlay.classList.remove('show');
}

// ── Profile Dropdown ──
function toggleProfileDropdown() {
  const dropdown = document.getElementById('profileDropdown');
  if (dropdown) dropdown.classList.toggle('show');
}

// ── Notification Dropdown ──
function toggleNotificationDropdown() {
  const dropdown = document.getElementById('notificationDropdown');
  if (dropdown) dropdown.classList.toggle('show');
  if (dropdown && dropdown.classList.contains('show')) {
    loadNotifications();
  }
}

// ── Header Greeting ──
function initGreeting() {
  const greetEl = document.getElementById('greetingText');
  if (!greetEl) return;
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const name = greetEl.getAttribute('data-name') || '';
  greetEl.textContent = `${greeting}, ${name}`;
}

function initHeaderDate() {
  const el = document.getElementById('headerDate');
  if (!el) return;
  const now = new Date();
  el.textContent = now.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' });
}

// ── Live Countdown ──
function initCountdown() {
  const el = document.getElementById('classCountdown');
  if (!el) return;
  const targetTime = el.getAttribute('data-target');
  if (!targetTime) return;

  function update() {
    const now = new Date();
    const [h, m] = targetTime.split(':').map(Number);
    const target = new Date(now);
    target.setHours(h, m, 0, 0);
    const diff = target - now;
    if (diff <= 0) { el.textContent = 'Starting now'; return; }
    const mins = Math.floor(diff / 60000);
    const secs = Math.floor((diff % 60000) / 1000);
    el.textContent = mins > 0 ? `${mins}m ${secs}s` : `${secs}s`;
  }
  update();
  setInterval(update, 1000);
}

// ── Toast System ──
function showToast(message, type = 'info', duration = 4000) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = {
    success: '<svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>',
    error: '<svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>',
    warning: '<svg viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    info: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  };
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<div class="toast-icon">${icons[type] || icons.info}</div><span class="toast-message">${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('removing');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// ── Bottom Nav Active State ──
function initBottomNav() {
  const path = window.location.pathname;
  document.querySelectorAll('.bottom-nav-item').forEach(item => {
    const href = item.getAttribute('href');
    if (href && path.includes(href)) item.classList.add('active');
  });
}

// ── Load Notifications ──
function loadNotifications() {
  fetch('/api/notifications/recent')
    .then(r => r.json())
    .then(data => {
      const list = document.getElementById('notificationList');
      if (!list) return;
      if (!data || data.length === 0) {
        list.innerHTML = `<div class="notif-empty"><svg viewBox="0 0 24 24"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg><p>You're all caught up!</p></div>`;
        return;
      }
      list.innerHTML = data.map(n => `
        <div class="notification-item ${n.read ? '' : 'unread'}" onclick="markRead(${n.id})">
          <div class="notif-dot"></div>
          <div class="notif-content">
            <div class="notification-title">${n.title}</div>
            <div class="notification-message">${n.message}</div>
            <div class="notification-time">${n.timeAgo}</div>
          </div>
        </div>`).join('');
    })
    .catch(() => {});
}

function markRead(id) {
  fetch(`/api/notifications/${id}/read`, { method: 'POST', headers: { 'X-CSRF-TOKEN': getCsrfToken() } }).catch(() => {});
}

function markAllRead() {
  fetch('/api/notifications/read-all', { method: 'POST', headers: { 'X-CSRF-TOKEN': getCsrfToken() } })
    .then(() => { document.querySelectorAll('.notification-item').forEach(el => el.classList.remove('unread')); const badge = document.querySelector('.notif-count'); if (badge) badge.style.display = 'none'; })
    .catch(() => {});
}

function getCsrfToken() {
  const meta = document.querySelector('meta[name="_csrf"]');
  return meta ? meta.getAttribute('content') : '';
}

// ── Login Role Selector ──
function initRoleSelector() {
  const btns = document.querySelectorAll('.role-btn');
  const slider = document.querySelector('.role-selector-slider');
  if (!btns.length || !slider) return;

  const ROLES = { student: { primary: '#14b8a6', soft: 'rgba(20,184,166,0.12)' }, teacher: { primary: '#f59e0b', soft: 'rgba(245,158,11,0.12)' }, admin: { primary: '#f97316', soft: 'rgba(249,115,22,0.12)' } };

  function updateSlider(activeBtn) {
    const parent = activeBtn.closest('.role-selector');
    const parentRect = parent.getBoundingClientRect();
    const btnRect = activeBtn.getBoundingClientRect();
    slider.style.left = (btnRect.left - parentRect.left) + 'px';
    slider.style.width = btnRect.width + 'px';
  }

  function setRole(role) {
    const colors = ROLES[role];
    if (!colors) return;
    document.documentElement.setAttribute('data-role', role);
    slider.style.background = colors.primary;
    const input = document.querySelector('input[name="selectedRole"]');
    if (input) input.value = role;
  }

  btns.forEach((btn, i) => {
    btn.addEventListener('click', () => {
      btns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      updateSlider(btn);
      setRole(btn.getAttribute('data-role'));
    });
    if (i === 0) { setTimeout(() => { btn.classList.add('active'); updateSlider(btn); setRole(btn.getAttribute('data-role')); }, 50); }
  });
}

// ── Close Dropdowns on Outside Click ──
document.addEventListener('click', (e) => {
  if (!e.target.closest('.notif-bell-wrap')) {
    const d = document.getElementById('notificationDropdown');
    if (d) d.classList.remove('show');
  }
  if (!e.target.closest('.sidebar-profile')) {
    const d = document.getElementById('profileDropdown');
    if (d) d.classList.remove('show');
  }
});

// ── Auto dismiss flash messages ──
function initFlashMessages() {
  const alerts = document.querySelectorAll('.alert');
  alerts.forEach(alert => {
    setTimeout(() => { alert.style.opacity = '0'; alert.style.transition = 'opacity 0.5s'; setTimeout(() => alert.remove(), 500); }, 5000);
  });
}

// ── Init ──
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initSidebar();
  initGreeting();
  initHeaderDate();
  initCountdown();
  initBottomNav();
  initRoleSelector();
  initFlashMessages();

  // Sidebar overlay click to close
  const overlay = document.querySelector('.sidebar-overlay');
  if (overlay) overlay.addEventListener('click', closeSidebarMobile);
});

window.toggleSidebar = toggleSidebar;
window.toggleTheme = toggleTheme;
window.toggleProfileDropdown = toggleProfileDropdown;
window.toggleNotificationDropdown = toggleNotificationDropdown;
window.markAllRead = markAllRead;
window.markRead = markRead;
window.showToast = showToast;
