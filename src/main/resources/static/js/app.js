document.addEventListener('DOMContentLoaded', function() {
  setupMobileMenu();
  setupNotificationDropdown();
  setupNextClassCountdown();
  setupClassReminders();
  setupDeleteConfirmations();
  setupFormValidation();
  setupTooltips();
  setupAlertDismiss();
  setupCascadingFilters();
  
  // Expose helpful utility
  window.formatTimeAgo = formatTimeAgo;
});

function setupMobileMenu() {
  const menuBtn = document.querySelector('.mobile-menu-btn');
  const sidebar = document.querySelector('.sidebar');
  
  if (!menuBtn || !sidebar) return;
  
  // Ensure overlay exists
  let overlay = document.querySelector('.sidebar-overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    document.body.appendChild(overlay);
  }
  
  menuBtn.addEventListener('click', function() {
    sidebar.classList.toggle('open');
    overlay.classList.toggle('active');
  });
  
  overlay.addEventListener('click', function() {
    sidebar.classList.remove('open');
    overlay.classList.remove('active');
  });
}

function setupNotificationDropdown() {
  const bell = document.querySelector('.notification-bell');
  const dropdown = document.querySelector('.notification-dropdown');
  
  if (!bell || !dropdown) return;
  
  bell.addEventListener('click', function(e) {
    e.stopPropagation();
    dropdown.classList.toggle('show');
  });
  
  document.addEventListener('click', function(e) {
    if (!dropdown.contains(e.target) && !bell.contains(e.target)) {
      dropdown.classList.remove('show');
    }
  });
  
  // Prevent closing when clicking inside dropdown
  dropdown.addEventListener('click', function(e) {
    e.stopPropagation();
  });
}

function setupNextClassCountdown() {
  const countdownEls = document.querySelectorAll('.next-class-countdown');
  if (countdownEls.length === 0) return;
  
  function updateCountdowns() {
    countdownEls.forEach(el => {
      const timeStr = el.getAttribute('data-next-class-time');
      if (!timeStr) return;
      
      const targetTime = new Date(timeStr).getTime();
      const now = new Date().getTime();
      const diff = targetTime - now;
      
      if (diff <= 0) {
        el.textContent = "Class has started";
        el.classList.remove('text-primary');
        el.classList.add('text-success');
        return;
      }
      
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const hours = Math.floor(diff / (1000 * 60 * 60));
      
      if (hours > 0) {
        el.textContent = `Starts in ${hours}h ${minutes}m`;
      } else {
        el.textContent = `Starts in ${minutes} minutes`;
      }
    });
  }
  
  updateCountdowns();
  setInterval(updateCountdowns, 60000); // Update every minute
}

function setupClassReminders() {
  const container = document.querySelector('[data-classes]');
  if (!container) return;
  
  const classesData = container.getAttribute('data-classes');
  if (!classesData) return;
  
  try {
    const classes = JSON.parse(classesData);
    const now = new Date();
    
    classes.forEach(cls => {
      const classTime = new Date(cls.startTime);
      // Only process classes for today
      if (classTime.toDateString() !== now.toDateString()) return;
      
      const timeDiff = classTime.getTime() - now.getTime();
      // 5 minutes in milliseconds
      const reminderOffset = 5 * 60 * 1000;
      
      if (timeDiff > reminderOffset) {
        // Set timeout to trigger 5 mins before class
        setTimeout(() => {
          showBrowserNotification(
            "Upcoming Class",
            `${cls.subject} starts in 5 minutes in ${cls.room}`,
            "/favicon.ico"
          );
        }, timeDiff - reminderOffset);
      }
    });
  } catch (e) {
    console.error("Error parsing class data for reminders", e);
  }
}

function setupDeleteConfirmations() {
  const deleteBtns = document.querySelectorAll('.btn-delete');
  
  deleteBtns.forEach(btn => {
    btn.addEventListener('click', function(e) {
      e.preventDefault();
      const form = this.closest('form');
      const itemName = this.getAttribute('data-item-name') || 'this item';
      
      showModal(
        'Confirm Deletion',
        `<p>Are you sure you want to delete ${itemName}? This action cannot be undone.</p>`,
        function() {
          if (form) form.submit();
        }
      );
    });
  });
}

function setupFormValidation() {
  const forms = document.querySelectorAll('form.needs-validation');
  
  forms.forEach(form => {
    form.addEventListener('submit', function(event) {
      if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation();
      }
      form.classList.add('was-validated');
    }, false);
  });
}

function setupTooltips() {
  // Mobile touch handling is in mobile.js
  // Desktop hover is handled purely by CSS (.timetable-cell:hover .cell-tooltip)
}

function setupAlertDismiss() {
  const alerts = document.querySelectorAll('.alert');
  
  alerts.forEach(alert => {
    // If alert has a close button, wait for click, otherwise auto-dismiss
    const closeBtn = alert.querySelector('.alert-close');
    
    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        alert.style.opacity = '0';
        setTimeout(() => alert.remove(), 300);
      });
    } else {
      setTimeout(() => {
        alert.style.transition = 'opacity 0.5s ease';
        alert.style.opacity = '0';
        setTimeout(() => alert.remove(), 500);
      }, 5000);
    }
  });
}

function setupCascadingFilters() {
  const deptSelect = document.getElementById('departmentFilter');
  const yearSelect = document.getElementById('yearFilter');
  const sectionSelect = document.getElementById('sectionFilter');
  
  if (deptSelect && yearSelect) {
    deptSelect.addEventListener('change', function() {
      // In a real app, this would be an AJAX call
      // For now, if value is empty, disable subsequent selects
      if (!this.value) {
        yearSelect.value = '';
        yearSelect.disabled = true;
        if (sectionSelect) {
          sectionSelect.value = '';
          sectionSelect.disabled = true;
        }
      } else {
        yearSelect.disabled = false;
      }
    });
  }
  
  if (yearSelect && sectionSelect) {
    yearSelect.addEventListener('change', function() {
      if (!this.value) {
        sectionSelect.value = '';
        sectionSelect.disabled = true;
      } else {
        sectionSelect.disabled = false;
      }
    });
  }
}

// Modal Utility
function showModal(title, bodyHtml, onConfirm) {
  // Remove existing modal if any
  let existingModal = document.getElementById('dynamicModal');
  if (existingModal) {
    existingModal.remove();
  }
  
  const modalHtml = `
    <div class="modal-overlay show" id="dynamicModal">
      <div class="modal">
        <div class="modal-header">
          <h3 style="margin:0">${title}</h3>
          <button class="modal-close" onclick="closeModal()">&times;</button>
        </div>
        <div class="modal-body">
          ${bodyHtml}
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
          <button class="btn btn-danger" id="modalConfirmBtn">Confirm</button>
        </div>
      </div>
    </div>
  `;
  
  document.body.insertAdjacentHTML('beforeend', modalHtml);
  
  document.getElementById('modalConfirmBtn').addEventListener('click', function() {
    if (typeof onConfirm === 'function') {
      onConfirm();
    }
    closeModal();
  });
  
  // Close on overlay click
  document.getElementById('dynamicModal').addEventListener('click', function(e) {
    if (e.target === this) {
      closeModal();
    }
  });
}

function closeModal() {
  const modal = document.getElementById('dynamicModal');
  if (modal) {
    modal.classList.remove('show');
    setTimeout(() => modal.remove(), 300); // Wait for potential animation
  }
}

function showBrowserNotification(title, body, icon) {
  if (!('Notification' in window)) return;
  
  if (Notification.permission === 'granted') {
    new Notification(title, { body, icon });
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        new Notification(title, { body, icon });
      }
    });
  }
}

function formatTimeAgo(dateStr) {
  const date = new Date(dateStr);
  const now = new Date();
  const seconds = Math.floor((now - date) / 1000);
  
  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + " years ago";
  
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + " months ago";
  
  interval = seconds / 86400;
  if (interval > 1) return Math.floor(interval) + " days ago";
  
  interval = seconds / 3600;
  if (interval > 1) return Math.floor(interval) + " hours ago";
  
  interval = seconds / 60;
  if (interval > 1) return Math.floor(interval) + " minutes ago";
  
  return Math.floor(seconds) + " seconds ago";
}
