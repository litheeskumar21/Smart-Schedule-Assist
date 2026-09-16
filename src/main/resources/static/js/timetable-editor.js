class TimetableEditor {
  constructor(gridElement, options) {
    this.grid = gridElement;
    this.entries = options.entries || [];
    this.subjects = options.subjects || [];
    this.teachers = options.teachers || [];
    this.classrooms = options.classrooms || [];
    this.periods = options.periods || [];
    this.days = options.days || ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
    this.sectionId = options.sectionId;
    this.csrfToken = options.csrfToken;
    this.csrfHeader = options.csrfHeader || 'X-CSRF-TOKEN';
    
    // State
    this.draggedEntry = null;
    this.conflicts = [];
    
    if (this.grid) {
      this.init();
    }
  }
  
  init() {
    this.renderGrid();
    this.setupDragAndDrop();
    this.setupCellClick();
    this.setupSaveButton();
    this.setupLockToggle();
    this.highlightConflicts();
  }
  
  renderGrid() {
    let html = `
      <table class="timetable-table">
        <thead>
          <tr>
            <th>Time / Day</th>
            ${this.days.map(day => `<th>${day}</th>`).join('')}
          </tr>
        </thead>
        <tbody>
    `;
    
    this.periods.forEach(period => {
      html += `<tr>
        <td style="font-weight:600; text-align:center;">
          ${period.startTime} - ${period.endTime}<br>
          <small class="text-muted">${period.name}</small>
        </td>`;
        
      if (period.isBreak) {
        html += `<td colspan="${this.days.length}" class="cell-break">${period.name}</td>`;
      } else {
        this.days.forEach(day => {
          // Find entry for this day and period
          const entry = this.entries.find(e => e.dayOfWeek === day && e.periodId === period.id);
          
          if (entry) {
            const isLocked = entry.isLocked ? 'cell-locked' : '';
            html += `
              <td class="timetable-cell draggable ${isLocked}" 
                  data-day="${day}" 
                  data-period="${period.id}"
                  data-entry-id="${entry.id || ''}"
                  draggable="${!entry.isLocked}">
                <div class="cell-subject">${this.getSubjectName(entry.subjectId)}</div>
                <div class="cell-teacher">${this.getTeacherName(entry.teacherId)}</div>
                <div class="cell-room">${this.getRoomName(entry.classroomId)}</div>
                ${entry.isLocked ? '<span class="lock-icon" title="Locked">🔒</span>' : ''}
              </td>
            `;
          } else {
            html += `
              <td class="timetable-cell drop-zone" 
                  data-day="${day}" 
                  data-period="${period.id}">
                <div class="cell-free">Empty</div>
              </td>
            `;
          }
        });
      }
      
      html += `</tr>`;
    });
    
    html += `</tbody></table>`;
    this.grid.innerHTML = html;
  }
  
  setupDragAndDrop() {
    const cells = this.grid.querySelectorAll('.timetable-cell:not(.cell-break)');
    
    cells.forEach(cell => {
      if (cell.getAttribute('draggable') === 'true') {
        cell.addEventListener('dragstart', (e) => {
          this.draggedEntry = {
            day: cell.getAttribute('data-day'),
            period: cell.getAttribute('data-period'),
            element: cell
          };
          cell.classList.add('dragging');
          e.dataTransfer.effectAllowed = 'move';
          e.dataTransfer.setData('text/plain', cell.getAttribute('data-entry-id'));
        });
        
        cell.addEventListener('dragend', () => {
          cell.classList.remove('dragging');
          this.draggedEntry = null;
          
          this.grid.querySelectorAll('.drop-zone').forEach(z => {
            z.classList.remove('drag-over');
          });
        });
      }
      
      // Allow dropping on any non-break cell
      cell.addEventListener('dragover', (e) => {
        e.preventDefault(); // Necessary to allow dropping
        e.dataTransfer.dropEffect = 'move';
        
        // Visual feedback
        if (cell !== this.draggedEntry?.element) {
          cell.classList.add('drag-over');
        }
      });
      
      cell.addEventListener('dragleave', () => {
        cell.classList.remove('drag-over');
      });
      
      cell.addEventListener('drop', (e) => {
        e.preventDefault();
        cell.classList.remove('drag-over');
        
        if (!this.draggedEntry || cell === this.draggedEntry.element) return;
        if (cell.classList.contains('cell-locked')) return; // Cannot drop onto locked
        
        const sourceDay = this.draggedEntry.day;
        const sourcePeriod = this.draggedEntry.period;
        const targetDay = cell.getAttribute('data-day');
        const targetPeriod = cell.getAttribute('data-period');
        
        this.swapEntries(sourceDay, sourcePeriod, targetDay, targetPeriod);
      });
    });
  }
  
  swapEntries(day1, period1, day2, period2) {
    const entry1Index = this.entries.findIndex(e => e.dayOfWeek === day1 && e.periodId == period1);
    const entry2Index = this.entries.findIndex(e => e.dayOfWeek === day2 && e.periodId == period2);
    
    if (entry1Index !== -1) {
      this.entries[entry1Index].dayOfWeek = day2;
      this.entries[entry1Index].periodId = parseInt(period2);
    }
    
    if (entry2Index !== -1) {
      this.entries[entry2Index].dayOfWeek = day1;
      this.entries[entry2Index].periodId = parseInt(period1);
    }
    
    // Re-render and check conflicts
    this.init();
  }
  
  setupCellClick() {
    this.grid.addEventListener('click', (e) => {
      const cell = e.target.closest('.timetable-cell');
      if (!cell || cell.classList.contains('cell-break')) return;
      
      // Don't trigger if clicked on lock icon
      if (e.target.classList.contains('lock-icon')) return;
      
      const day = cell.getAttribute('data-day');
      const periodId = parseInt(cell.getAttribute('data-period'));
      const entry = this.entries.find(e => e.dayOfWeek === day && e.periodId === periodId);
      
      if (entry && entry.isLocked) {
        alert("This period is locked and cannot be edited.");
        return;
      }
      
      this.showEntryModal(day, periodId, entry);
    });
  }
  
  showEntryModal(day, periodId, existingEntry) {
    const title = existingEntry ? 'Edit Class' : 'Add Class';
    
    // Subject options
    const subjectOpts = this.subjects.map(s => 
      `<option value="${s.id}" ${existingEntry && existingEntry.subjectId == s.id ? 'selected' : ''}>${s.name}</option>`
    ).join('');
    
    // Teacher options
    const teacherOpts = this.teachers.map(t => 
      `<option value="${t.id}" ${existingEntry && existingEntry.teacherId == t.id ? 'selected' : ''}>${t.name}</option>`
    ).join('');
    
    // Room options
    const roomOpts = this.classrooms.map(r => 
      `<option value="${r.id}" ${existingEntry && existingEntry.classroomId == r.id ? 'selected' : ''}>${r.name}</option>`
    ).join('');
    
    const bodyHtml = `
      <form id="entryForm">
        <div class="form-group">
          <label class="form-label">Day</label>
          <input type="text" class="form-control" value="${day}" disabled>
        </div>
        <div class="form-group">
          <label class="form-label">Subject</label>
          <select id="modalSubject" class="form-select" required>
            <option value="">Select Subject...</option>
            ${subjectOpts}
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Teacher</label>
          <select id="modalTeacher" class="form-select" required>
            <option value="">Select Teacher...</option>
            ${teacherOpts}
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Room</label>
          <select id="modalRoom" class="form-select" required>
            <option value="">Select Room...</option>
            ${roomOpts}
          </select>
        </div>
      </form>
      ${existingEntry ? `<button type="button" class="btn btn-danger btn-sm mt-16" id="modalDeleteBtn">Delete Entry</button>` : ''}
    `;
    
    // Use the global showModal from app.js
    if (typeof showModal === 'function') {
      showModal(title, bodyHtml, () => {
        const form = document.getElementById('entryForm');
        if (!form.checkValidity()) {
          alert("Please fill all required fields");
          return false; // Prevent modal from closing
        }
        
        const subjId = parseInt(document.getElementById('modalSubject').value);
        const teachId = parseInt(document.getElementById('modalTeacher').value);
        const roomId = parseInt(document.getElementById('modalRoom').value);
        
        if (existingEntry) {
          existingEntry.subjectId = subjId;
          existingEntry.teacherId = teachId;
          existingEntry.classroomId = roomId;
        } else {
          this.entries.push({
            id: null, // New entry
            dayOfWeek: day,
            periodId: periodId,
            subjectId: subjId,
            teacherId: teachId,
            classroomId: roomId,
            isLocked: false
          });
        }
        
        this.init(); // Re-render
      });
      
      // Setup delete button if editing
      const delBtn = document.getElementById('modalDeleteBtn');
      if (delBtn) {
        delBtn.addEventListener('click', () => {
          if (confirm('Are you sure you want to remove this class?')) {
            this.entries = this.entries.filter(e => e !== existingEntry);
            closeModal();
            this.init(); // Re-render
          }
        });
      }
    }
  }
  
  setupSaveButton() {
    const saveBtn = document.getElementById('saveTimetableBtn');
    if (!saveBtn) return;
    
    saveBtn.addEventListener('click', () => {
      saveBtn.disabled = true;
      saveBtn.textContent = 'Saving...';
      
      fetch(`/admin/timetables/section/${this.sectionId}/save`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [this.csrfHeader]: this.csrfToken
        },
        body: JSON.stringify(this.entries)
      })
      .then(response => {
        if (!response.ok) throw new Error('Save failed');
        return response.json();
      })
      .then(data => {
        alert('Timetable saved successfully!');
        window.location.reload();
      })
      .catch(err => {
        console.error(err);
        alert('Error saving timetable. Please try again.');
        saveBtn.disabled = false;
        saveBtn.textContent = 'Save Timetable';
      });
    });
  }
  
  setupLockToggle() {
    // Handled in cellClick for now, or could add specific icon listeners
  }
  
  highlightConflicts() {
    // This is basic client-side conflict checking
    // Server-side validation should also happen
    this.conflicts = [];
    
    // Group entries by time slot
    const slots = {};
    this.entries.forEach(e => {
      const key = `${e.dayOfWeek}-${e.periodId}`;
      if (!slots[key]) slots[key] = [];
      slots[key].push(e);
    });
    
    // Since we're editing a single section, we only need to warn if 
    // teacher/room is already booked in another section (would require API call)
    // For now, clear conflicts
  }
  
  // Helpers
  getSubjectName(id) {
    const s = this.subjects.find(x => x.id === id);
    return s ? s.name : 'Unknown';
  }
  
  getTeacherName(id) {
    const t = this.teachers.find(x => x.id === id);
    return t ? t.name : 'Unknown';
  }
  
  getRoomName(id) {
    const r = this.classrooms.find(x => x.id === id);
    return r ? r.name : 'Unknown';
  }
}

// Export for usage in HTML
window.TimetableEditor = TimetableEditor;
