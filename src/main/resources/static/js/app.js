// Smart Schedule Assist - App v2.0

// ── Theme System ──
const THEME_KEY = 'ssa-theme';
const SIDEBAR_KEY = 'ssa-sidebar-collapsed';

function initTheme() {
  const saved = localStorage.getItem(THEME_KEY) || 'light';
  document.documentElement.setAttribute('data-theme', saved);
  _applyThemeIcons(saved);
}

// Immediately apply theme on script load
try {
  const _savedTheme = localStorage.getItem(THEME_KEY) || 'light';
  document.documentElement.setAttribute('data-theme', _savedTheme);
} catch (e) {}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const next = current === 'light' ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem(THEME_KEY, next);
  _applyThemeIcons(next);
}

// Swap the two pre-rendered SVG icon elements inside the theme button
function _applyThemeIcons(theme) {
  const sun  = document.getElementById('iconSun');
  const moon = document.getElementById('iconMoon');
  if (sun)  sun.style.display  = (theme === 'dark')  ? 'none'  : 'block';
  if (moon) moon.style.display = (theme === 'dark')  ? 'block' : 'none';
}

// Legacy alias kept for safety
function updateThemeIcon(theme) { _applyThemeIcons(theme); }

// ── Sidebar Collapse & Animation ──
function updateSidebarButtonState(isCollapsed) {
  const btn = document.getElementById('sidebarToggleBtn');
  if (!btn) return;
  if (isCollapsed) {
    btn.classList.add('is-collapsed');
    btn.setAttribute('title', 'Open Menu');
    btn.setAttribute('aria-label', 'Open Menu');
  } else {
    btn.classList.remove('is-collapsed');
    btn.setAttribute('title', 'Close Menu');
    btn.setAttribute('aria-label', 'Close Menu');
  }
}

function initSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const mainContent = document.querySelector('.main-content');
  if (!sidebar) return;

  // Ensure overlay element exists
  let overlay = document.querySelector('.sidebar-overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    document.body.appendChild(overlay);
  }
  overlay.addEventListener('click', closeSidebarMobile);

  const isMobile = window.innerWidth <= 1024;
  if (isMobile) {
    sidebar.classList.remove('mobile-open');
    sidebar.classList.remove('open');
    if (mainContent) mainContent.classList.remove('sidebar-collapsed');
    updateSidebarButtonState(true);
  } else {
    const collapsed = localStorage.getItem(SIDEBAR_KEY) === 'true';
    if (collapsed) {
      sidebar.classList.add('collapsed');
      if (mainContent) mainContent.classList.add('sidebar-collapsed');
      updateSidebarButtonState(true);
    } else {
      sidebar.classList.remove('collapsed');
      if (mainContent) mainContent.classList.remove('sidebar-collapsed');
      updateSidebarButtonState(false);
    }
  }
}

function toggleSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const mainContent = document.querySelector('.main-content');
  const overlay = document.querySelector('.sidebar-overlay');
  if (!sidebar) return;

  const isMobile = window.innerWidth <= 1024;
  if (isMobile) {
    const isOpen = sidebar.classList.toggle('mobile-open');
    sidebar.classList.toggle('open', isOpen);
    if (overlay) overlay.classList.toggle('show', isOpen);
    updateSidebarButtonState(!isOpen);
  } else {
    const isCollapsed = sidebar.classList.toggle('collapsed');
    if (mainContent) mainContent.classList.toggle('sidebar-collapsed', isCollapsed);
    localStorage.setItem(SIDEBAR_KEY, isCollapsed);
    updateSidebarButtonState(isCollapsed);
  }
}

function closeSidebarMobile() {
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.querySelector('.sidebar-overlay');
  if (sidebar) {
    sidebar.classList.remove('mobile-open');
    sidebar.classList.remove('open');
  }
  if (overlay) {
    overlay.classList.remove('show');
    overlay.classList.remove('open');
  }
  updateSidebarButtonState(true);
}

window.addEventListener('resize', () => {
  const sidebar = document.querySelector('.sidebar');
  const mainContent = document.querySelector('.main-content');
  const overlay = document.querySelector('.sidebar-overlay');
  if (!sidebar) return;

  const isMobile = window.innerWidth <= 1024;
  if (isMobile) {
    const isOpen = sidebar.classList.contains('mobile-open') || sidebar.classList.contains('open');
    updateSidebarButtonState(!isOpen);
  } else {
    if (overlay) {
      overlay.classList.remove('show');
      overlay.classList.remove('open');
    }
    const isCollapsed = sidebar.classList.contains('collapsed');
    if (mainContent) mainContent.classList.toggle('sidebar-collapsed', isCollapsed);
    updateSidebarButtonState(isCollapsed);
  }
});

// ── Profile Dropdown ──
function toggleProfileDropdown() {
  const dropdown = document.getElementById('profileDropdown');
  if (!dropdown) return;
  const isBlock = dropdown.style.display === 'block';
  dropdown.style.display = isBlock ? 'none' : 'block';
}

// ── Notification Dropdown ──
function toggleNotificationDropdown() {
  const dropdown = document.getElementById('notificationDropdown');
  if (!dropdown) return;
  const isOpen = dropdown.style.display === 'block';
  dropdown.style.display = isOpen ? 'none' : 'block';
  if (!isOpen) loadNotifications();
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
        list.innerHTML = `
          <div style="padding:28px 16px;text-align:center;color:var(--text-muted);">
            <svg viewBox="0 0 24 24" width="28" height="28" stroke="currentColor" stroke-width="1.5" fill="none"
                 style="margin:0 auto 8px;display:block;opacity:0.4;">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <div style="font-size:13px;">You're all caught up!</div>
          </div>`;
        return;
      }
      list.innerHTML = data.map(n => `
        <div onclick="markRead(${n.id})"
             style="padding:12px 16px;display:flex;gap:10px;align-items:flex-start;
                    cursor:pointer;border-bottom:1px solid var(--border);
                    background:${n.read ? 'transparent' : 'rgba(var(--role-primary-rgb,20,184,166),0.06)'};
                    transition:background 0.15s;">
          <div style="width:8px;height:8px;border-radius:50%;margin-top:5px;flex-shrink:0;
                      background:${n.read ? 'transparent' : 'var(--role-primary,#14b8a6)'};"></div>
          <div style="flex:1;min-width:0;">
            <div style="font-size:13px;font-weight:${n.read ? 400 : 600};color:var(--text);
                        white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${n.title || ''}</div>
            <div style="font-size:12px;color:var(--text-muted);margin-top:2px;
                        white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${n.message || ''}</div>
            <div style="font-size:11px;color:var(--text-placeholder);margin-top:4px;">${n.timeAgo || ''}</div>
          </div>
        </div>`).join('');
    })
    .catch(() => {
      const list = document.getElementById('notificationList');
      if (list) list.innerHTML = `<div style="padding:20px 16px;text-align:center;font-size:13px;color:var(--text-muted);">Could not load notifications.</div>`;
    });
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
    if (d) d.style.display = 'none';
  }
  if (!e.target.closest('.user-avatar-wrap')) {
    const d = document.getElementById('profileDropdown');
    if (d) {
      d.style.display = 'none';
      d.classList.remove('show');
    }
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
