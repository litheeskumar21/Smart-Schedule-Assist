// Detect touch device
const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;

document.addEventListener('DOMContentLoaded', function() {
  if (isTouchDevice) {
    document.body.classList.add('touch-device');
  }
  
  setupMobileTooltips();
  setupSwipeNavigation();
  setupDayTabs();
});

// Setup touch-based tooltip (tap to show subject details instead of hover)
function setupMobileTooltips() {
  if (!isTouchDevice) return;
  
  const cells = document.querySelectorAll('.timetable-cell');
  
  cells.forEach(cell => {
    cell.addEventListener('click', function(e) {
      // Don't do this if it's an editor cell (draggable)
      if (this.classList.contains('draggable')) return;
      
      e.stopPropagation();
      
      // Close other open tooltips
      document.querySelectorAll('.cell-tooltip.active').forEach(t => {
        if (t !== this.querySelector('.cell-tooltip')) {
          t.classList.remove('active');
          t.style.display = 'none';
        }
      });
      
      // Toggle this tooltip
      const tooltip = this.querySelector('.cell-tooltip');
      if (tooltip) {
        const isActive = tooltip.classList.contains('active');
        if (isActive) {
          tooltip.classList.remove('active');
          tooltip.style.display = 'none';
        } else {
          tooltip.classList.add('active');
          tooltip.style.display = 'block';
        }
      }
    });
  });
  
  // Close tooltip when clicking outside
  document.addEventListener('click', function(e) {
    if (!e.target.closest('.timetable-cell')) {
      document.querySelectorAll('.cell-tooltip.active').forEach(t => {
        t.classList.remove('active');
        t.style.display = 'none';
      });
    }
  });
}

// Day tabs interaction (mobile timetable view)
function setupDayTabs() {
  const tabs = document.querySelectorAll('.day-tab');
  if (tabs.length === 0) return;
  
  tabs.forEach(tab => {
    tab.addEventListener('click', function() {
      // Update active tab
      document.querySelectorAll('.day-tab').forEach(t => t.classList.remove('active'));
      this.classList.add('active');
      
      // Get target day
      const day = this.getAttribute('data-day');
      
      // Filter mobile cards
      const cards = document.querySelectorAll('.timetable-card');
      if (cards.length > 0) {
        cards.forEach(card => {
          if (day === 'all' || card.getAttribute('data-day') === day) {
            card.style.display = 'flex';
          } else {
            card.style.display = 'none';
          }
        });
      }
      
      // Filter desktop table columns if we wanted to show one day at a time
      // This is generally handled by responsive CSS hiding the table entirely
    });
  });
  
  // Auto-select today
  const today = new Date().getDay(); // 0 = Sunday, 1 = Monday
  const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  
  if (today >= 1 && today <= 5) { // Monday-Friday
    const todayTab = document.querySelector(`.day-tab[data-day="${dayNames[today]}"]`);
    if (todayTab) {
      todayTab.click();
    }
  } else {
    // Select first tab if weekend
    if (tabs.length > 0) tabs[0].click();
  }
}

// Swipe navigation for mobile timetable
function setupSwipeNavigation() {
  let touchStartX = 0;
  let touchEndX = 0;
  
  const swipeArea = document.querySelector('.timetable-cards');
  if (!swipeArea) return;
  
  swipeArea.addEventListener('touchstart', e => {
    touchStartX = e.changedTouches[0].screenX;
  }, { passive: true });
  
  swipeArea.addEventListener('touchend', e => {
    touchEndX = e.changedTouches[0].screenX;
    handleSwipe();
  }, { passive: true });
  
  function handleSwipe() {
    const threshold = 50;
    const tabs = Array.from(document.querySelectorAll('.day-tab'));
    if (tabs.length === 0) return;
    
    const activeIndex = tabs.findIndex(tab => tab.classList.contains('active'));
    if (activeIndex === -1) return;
    
    if (touchEndX < touchStartX - threshold) {
      // Swiped left (next day)
      if (activeIndex < tabs.length - 1) {
        tabs[activeIndex + 1].click();
        tabs[activeIndex + 1].scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    }
    
    if (touchEndX > touchStartX + threshold) {
      // Swiped right (prev day)
      if (activeIndex > 0) {
        tabs[activeIndex - 1].click();
        tabs[activeIndex - 1].scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    }
  }
}
