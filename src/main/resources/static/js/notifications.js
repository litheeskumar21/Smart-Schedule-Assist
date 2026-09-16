class NotificationManager {
  constructor() {
    this.unreadCount = 0;
    this.eventSource = null;
    this.bellElement = document.querySelector('.notification-bell');
    this.badgeElement = document.querySelector('.notification-badge');
    this.dropdownElement = document.querySelector('.notification-dropdown');
    this.listElement = document.querySelector('.notification-list');
    
    if (this.bellElement && this.dropdownElement) {
      this.init();
    }
  }
  
  init() {
    this.requestPermission();
    this.setupBellClick();
    this.fetchUnreadCount();
    
    // Check if we should use SSE (only if user is logged in)
    const metaTag = document.querySelector('meta[name="user-id"]');
    if (metaTag) {
      this.connectSSE();
    }
  }
  
  requestPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }
  
  connectSSE() {
    try {
      this.eventSource = new EventSource('/api/notifications/stream');
      
      this.eventSource.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data);
          this.handleNewNotification(notification);
        } catch (e) {
          console.error("Error parsing notification data", e);
        }
      };
      
      this.eventSource.onerror = () => {
        console.warn("SSE connection lost. Reconnecting in 5s...");
        this.eventSource.close();
        setTimeout(() => this.connectSSE(), 5000);
      };
    } catch (e) {
      console.error("Failed to connect to SSE", e);
    }
  }
  
  handleNewNotification(notification) {
    this.unreadCount++;
    this.updateBadge();
    this.addToDropdown(notification);
    this.showBrowserNotification(notification);
  }
  
  updateBadge() {
    if (!this.badgeElement) return;
    
    if (this.unreadCount > 0) {
      this.badgeElement.textContent = this.unreadCount > 99 ? '99+' : this.unreadCount;
      this.badgeElement.style.display = 'block';
    } else {
      this.badgeElement.style.display = 'none';
    }
  }
  
  addToDropdown(notification) {
    if (!this.listElement) return;
    
    // Remove empty state if present
    const emptyState = this.listElement.querySelector('.notification-empty');
    if (emptyState) {
      emptyState.remove();
    }
    
    const timeAgo = typeof window.formatTimeAgo === 'function' 
      ? window.formatTimeAgo(notification.createdAt) 
      : 'Just now';
      
    const html = `
      <div class="notification-item unread" data-id="${notification.id}">
        <div class="notification-title">${notification.title}</div>
        <div class="notification-message">${notification.message}</div>
        <div class="notification-time">${timeAgo}</div>
      </div>
    `;
    
    this.listElement.insertAdjacentHTML('afterbegin', html);
    
    // Add click handler to new item
    const newItem = this.listElement.firstElementChild;
    newItem.addEventListener('click', () => this.markAsRead(notification.id, newItem));
  }
  
  showBrowserNotification(notification) {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/favicon.ico'
      });
    }
  }
  
  markAsRead(id, element) {
    fetch(`/api/notifications/${id}/read`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // Include CSRF token if needed
        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
      }
    })
    .then(response => {
      if (response.ok) {
        if (element) {
          element.classList.remove('unread');
        }
        if (this.unreadCount > 0) {
          this.unreadCount--;
          this.updateBadge();
        }
      }
    })
    .catch(err => console.error("Error marking notification as read", err));
  }
  
  markAllAsRead() {
    fetch('/api/notifications/read-all', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
      }
    })
    .then(response => {
      if (response.ok) {
        this.unreadCount = 0;
        this.updateBadge();
        
        if (this.listElement) {
          const items = this.listElement.querySelectorAll('.notification-item.unread');
          items.forEach(item => item.classList.remove('unread'));
        }
      }
    })
    .catch(err => console.error("Error marking all notifications as read", err));
  }
  
  fetchUnreadCount() {
    fetch('/api/notifications/unread-count')
      .then(response => {
        if (!response.ok) throw new Error('Network response was not ok');
        return response.json();
      })
      .then(data => {
        this.unreadCount = data.count || 0;
        this.updateBadge();
      })
      .catch(err => console.error("Error fetching unread count", err));
  }
  
  setupBellClick() {
    // Add mark all read button functionality if it exists
    const markAllBtn = document.querySelector('.mark-all-read');
    if (markAllBtn) {
      markAllBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.markAllAsRead();
      });
    }
    
    // Setup individual item clicks
    if (this.listElement) {
      const items = this.listElement.querySelectorAll('.notification-item');
      items.forEach(item => {
        item.addEventListener('click', () => {
          const id = item.getAttribute('data-id');
          if (id && item.classList.contains('unread')) {
            this.markAsRead(id, item);
          }
        });
      });
    }
  }
}

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
  window.notificationManager = new NotificationManager();
});
