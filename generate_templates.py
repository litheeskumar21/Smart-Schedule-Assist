import os

base_dir = r'b:\JavaProject\smart-schedule-assist\src\main\resources\templates'

files = {
    'fragments/layout.html': '''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title th:text="${pageTitle} + ' | Smart Schedule Assist'">Smart Schedule Assist</title>
  <link rel="stylesheet" th:href="@{/css/main.css}">
  <link rel="stylesheet" th:href="@{/css/responsive.css}">
  <link rel="stylesheet" th:href="@{/css/print.css}" media="print">
</head>
<body>
  <div class="app-layout">
    <aside class="sidebar" th:fragment="sidebar">
        <!-- Replaced by actual sidebar -->
    </aside>
    <main class="main-content">
        <header class="header" th:fragment="header">
            <!-- Replaced by actual header -->
        </header>
        <div th:replace="${content}"></div>
    </main>
  </div>
</body>
</html>''',

    'fragments/sidebar.html': '''<aside class="sidebar" th:fragment="sidebar(activeMenu)" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
  <div class="sidebar-header">
    <h2>📅 Smart Schedule</h2>
  </div>
  <nav class="sidebar-nav">
    <div sec:authorize="hasRole('STUDENT')">
      <a th:href="@{/student/dashboard}" th:classappend="${activeMenu == 'dashboard'} ? 'active'" class="nav-item">📊 Dashboard</a>
      <a th:href="@{/student/timetable}" th:classappend="${activeMenu == 'timetable'} ? 'active'" class="nav-item">📋 Timetable</a>
      <a th:href="@{/student/notifications}" th:classappend="${activeMenu == 'notifications'} ? 'active'" class="nav-item">🔔 Notifications</a>
      <a th:href="@{/student/announcements}" th:classappend="${activeMenu == 'announcements'} ? 'active'" class="nav-item">📢 Announcements</a>
      <a th:href="@{/student/profile}" th:classappend="${activeMenu == 'profile'} ? 'active'" class="nav-item">👤 Profile</a>
    </div>
    <div sec:authorize="hasRole('TEACHER')">
      <a th:href="@{/teacher/dashboard}" th:classappend="${activeMenu == 'dashboard'} ? 'active'" class="nav-item">📊 Dashboard</a>
      <a th:href="@{/teacher/timetable}" th:classappend="${activeMenu == 'timetable'} ? 'active'" class="nav-item">📋 Timetable</a>
      <a th:href="@{/teacher/absence}" th:classappend="${activeMenu == 'absence'} ? 'active'" class="nav-item">📝 Absence Requests</a>
      <a th:href="@{/teacher/change-requests}" th:classappend="${activeMenu == 'change-requests'} ? 'active'" class="nav-item">🔄 Change Requests</a>
      <a th:href="@{/teacher/notifications}" th:classappend="${activeMenu == 'notifications'} ? 'active'" class="nav-item">🔔 Notifications</a>
      <a th:href="@{/teacher/profile}" th:classappend="${activeMenu == 'profile'} ? 'active'" class="nav-item">👤 Profile</a>
    </div>
    <div sec:authorize="hasRole('ADMIN')">
      <a th:href="@{/admin/dashboard}" th:classappend="${activeMenu == 'dashboard'} ? 'active'" class="nav-item">📊 Dashboard</a>
      <a th:href="@{/admin/timetables}" th:classappend="${activeMenu == 'timetables'} ? 'active'" class="nav-item">📋 Timetables</a>
      <a th:href="@{/admin/students}" th:classappend="${activeMenu == 'students'} ? 'active'" class="nav-item">🎓 Students</a>
      <a th:href="@{/admin/teachers}" th:classappend="${activeMenu == 'teachers'} ? 'active'" class="nav-item">👨‍🏫 Teachers</a>
      <a th:href="@{/admin/departments}" th:classappend="${activeMenu == 'departments'} ? 'active'" class="nav-item">🏢 Departments</a>
      <a th:href="@{/admin/sections}" th:classappend="${activeMenu == 'sections'} ? 'active'" class="nav-item">📚 Sections</a>
      <a th:href="@{/admin/subjects}" th:classappend="${activeMenu == 'subjects'} ? 'active'" class="nav-item">📖 Subjects</a>
      <a th:href="@{/admin/classrooms}" th:classappend="${activeMenu == 'classrooms'} ? 'active'" class="nav-item">🏫 Classrooms</a>
      <a th:href="@{/admin/absence-requests}" th:classappend="${activeMenu == 'absence-requests'} ? 'active'" class="nav-item">📝 Absence Requests</a>
      <a th:href="@{/admin/timetable-requests}" th:classappend="${activeMenu == 'timetable-requests'} ? 'active'" class="nav-item">🔄 Timetable Requests</a>
      <a th:href="@{/admin/announcements}" th:classappend="${activeMenu == 'announcements'} ? 'active'" class="nav-item">📢 Announcements</a>
      <a th:href="@{/admin/notifications}" th:classappend="${activeMenu == 'notifications'} ? 'active'" class="nav-item">🔔 Notifications</a>
      <a th:href="@{/admin/settings}" th:classappend="${activeMenu == 'settings'} ? 'active'" class="nav-item">⚙️ Settings</a>
      <a th:href="@{/admin/activity-log}" th:classappend="${activeMenu == 'activity-log'} ? 'active'" class="nav-item">📋 Activity Log</a>
    </div>
  </nav>
  <div class="sidebar-footer">
    <form th:action="@{/logout}" method="post">
      <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
      <button type="submit" class="nav-item logout-btn">🚪 Logout</button>
    </form>
  </div>
</aside>''',

    'fragments/header.html': '''<header class="header" th:fragment="header(pageTitle)" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
  <button class="mobile-menu-btn" onclick="toggleSidebar()">☰</button>
  <div class="header-title" th:text="${pageTitle}">Page Title</div>
  <div class="header-right">
    <div class="notification-bell" onclick="toggleNotificationDropdown()">
      🔔
      <span class="notification-badge" th:text="${unreadCount}" th:if="${unreadCount != null && unreadCount > 0}">0</span>
    </div>
    <div class="header-user">
      <span sec:authentication="name">User</span>
    </div>
    <div class="notification-dropdown" id="notificationDropdown" style="display:none">
      <!-- Populated by JS -->
    </div>
  </div>
</header>''',

    'auth/login.html': '''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In | Smart Schedule Assist</title>
    <link rel="stylesheet" th:href="@{/css/main.css}">
</head>
<body class="auth-body">
    <div class="auth-card">
        <h2>📅 Smart Schedule Assist</h2>
        <h3>Sign In</h3>
        <div th:if="${param.error}" class="alert alert-error">Invalid credentials. Please try again.</div>
        <div th:if="${param.logout}" class="alert alert-success">You have been logged out.</div>
        <form th:action="@{/login}" method="post">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
            <div class="form-group">
                <label>Student ID / Teacher ID / Admin Username</label>
                <input type="text" name="username" required>
            </div>
            <div class="form-group">
                <label>Password</label>
                <input type="password" name="password" required>
            </div>
            <button type="submit" class="btn btn-primary">Sign In</button>
        </form>
        <div class="auth-links">
            <a th:href="@{/forgot-password}">Forgot Password?</a>
        </div>
    </div>
</body>
</html>''',

    'auth/forgot-password.html': '''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Forgot Password</title>
    <link rel="stylesheet" th:href="@{/css/main.css}">
</head>
<body class="auth-body">
    <div class="auth-card">
        <h2>Forgot Password</h2>
        <form th:action="@{/forgot-password}" method="post">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
            <div class="form-group">
                <label>Username</label>
                <input type="text" name="username" required>
            </div>
            <button type="submit" class="btn btn-primary">Submit</button>
        </form>
        <a th:href="@{/login}">Back to Login</a>
    </div>
</body>
</html>''',

    'auth/reset-password.html': '''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>Reset Password</title><link rel="stylesheet" th:href="@{/css/main.css}"></head>
<body class="auth-body">
    <div class="auth-card">
        <h2>Reset Password</h2>
        <form th:action="@{/reset-password}" method="post">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
            <input type="hidden" name="token" th:value="${token}">
            <div class="form-group">
                <label>New Password</label>
                <input type="password" name="password" required>
            </div>
            <div class="form-group">
                <label>Confirm Password</label>
                <input type="password" name="confirmPassword" required>
            </div>
            <button type="submit" class="btn btn-primary">Submit</button>
        </form>
    </div>
</body>
</html>''',

    'error/403.html': '''<!DOCTYPE html><html xmlns:th="http://www.thymeleaf.org"><head><title>403 - Access Denied</title><link rel="stylesheet" th:href="@{/css/main.css}"></head><body><div class="error-page"><h1>403 - Access Denied</h1><a th:href="@{/}">Go Home</a></div></body></html>''',
    
    'error/404.html': '''<!DOCTYPE html><html xmlns:th="http://www.thymeleaf.org"><head><title>404 - Not Found</title><link rel="stylesheet" th:href="@{/css/main.css}"></head><body><div class="error-page"><h1>404 - Not Found</h1><a th:href="@{/}">Go Home</a></div></body></html>''',
    
    'error/500.html': '''<!DOCTYPE html><html xmlns:th="http://www.thymeleaf.org"><head><title>500 - Server Error</title><link rel="stylesheet" th:href="@{/css/main.css}"></head><body><div class="error-page"><h1>500 - Server Error</h1><a th:href="@{/}">Go Home</a></div></body></html>''',

    'student/dashboard.html': '''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Dashboard | Smart Schedule Assist</title>
    <link rel="stylesheet" th:href="@{/css/main.css}">
    <link rel="stylesheet" th:href="@{/css/responsive.css}">
</head>
<body>
<div class="app-layout">
    <aside class="sidebar" th:replace="~{fragments/sidebar :: sidebar('dashboard')}"></aside>
    <main class="main-content">
        <header class="header" th:replace="~{fragments/header :: header('Dashboard')}"></header>
        
        <div class="dashboard-content">
            <div class="card">
                <h3 th:text="${student.fullName}">Name</h3>
                <p>ID: <span th:text="${student.studentId}"></span></p>
                <p>Dept: <span th:text="${student.department.name}"></span></p>
                <p>Section: <span th:text="${student.section.name}"></span></p>
            </div>
            
            <div class="card next-class-card">
                <h3>Next Class</h3>
                <div th:if="${nextClass != null}">
                    <p th:text="${nextClass.subject.name}"></p>
                    <p th:text="${nextClass.teacher.fullName}"></p>
                    <p th:text="${nextClass.classroom.name}"></p>
                </div>
                <div th:if="${nextClass == null}">
                    <p>No more classes today</p>
                </div>
            </div>
            
            <div class="card">
                <h3>Today's Timetable</h3>
                <table>
                    <tr th:each="period : ${allPeriods}">
                        <td th:text="${period.startTime + ' - ' + period.endTime}"></td>
                        <td>
                            <!-- logic to show entry or FREE -->
                        </td>
                    </tr>
                </table>
            </div>
        </div>
        
    </main>
</div>
<script th:src="@{/js/app.js}"></script>
<script th:src="@{/js/notifications.js}"></script>
<script th:src="@{/js/mobile.js}"></script>
</body>
</html>'''
}

paths = [
    'student/timetable.html', 'student/notifications.html', 'student/announcements.html', 
    'student/profile.html', 'student/change-password.html',
    'teacher/dashboard.html', 'teacher/timetable.html', 'teacher/absence.html', 
    'teacher/change-requests.html', 'teacher/notifications.html', 'teacher/profile.html', 
    'teacher/change-password.html',
    'admin/dashboard.html', 'admin/departments.html', 'admin/sections.html', 'admin/subjects.html', 
    'admin/classrooms.html', 'admin/students.html', 'admin/student-form.html', 'admin/teachers.html', 
    'admin/teacher-form.html', 'admin/timetables.html', 'admin/timetable-edit.html', 
    'admin/absence-requests.html', 'admin/absence-detail.html', 'admin/timetable-requests.html', 
    'admin/announcements.html', 'admin/announcement-form.html', 'admin/notifications.html', 
    'admin/settings.html', 'admin/activity-log.html', 'admin/search.html', 'admin/import.html'
]

for p in paths:
    if p not in files:
        title = p.split('/')[-1].replace('.html', '').replace('-', ' ').title()
        active = p.split('/')[-1].replace('.html', '')
        files[p] = f'''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>{title} | Smart Schedule Assist</title>
    <link rel="stylesheet" th:href="@{{/css/main.css}}">
    <link rel="stylesheet" th:href="@{{/css/responsive.css}}">
</head>
<body>
<div class="app-layout">
    <aside class="sidebar" th:replace="~{{fragments/sidebar :: sidebar('{active}')}}"></aside>
    <main class="main-content">
        <header class="header" th:replace="~{{fragments/header :: header('{title}')}}"></header>
        <div th:if="${{successMessage}}" class="alert alert-success" th:text="${{successMessage}}"></div>
        <div th:if="${{errorMessage}}" class="alert alert-error" th:text="${{errorMessage}}"></div>
        
        <div class="content-wrapper">
            <h2>{title}</h2>
            <!-- TODO: Implement specific content for {p} -->
        </div>
    </main>
</div>
<script th:src="@{{/js/app.js}}"></script>
<script th:src="@{{/js/notifications.js}}"></script>
<script th:src="@{{/js/mobile.js}}"></script>
<script th:if="${{showEditor}}" th:src="@{{/js/timetable-editor.js}}"></script>
</body>
</html>'''

for rel_path, content in files.items():
    full_path = os.path.join(base_dir, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)

print(f'Successfully created {len(files)} files.')
