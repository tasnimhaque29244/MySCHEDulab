<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MySched</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<div class="page-shell">

    <header class="top-brand">
        <img src="${pageContext.request.contextPath}/assets/images/ulab-logo.png" alt="ULAB Logo" class="top-logo">
        <h1>MySched</h1>
        <p>University of Liberal Arts Class Scheduler</p>
    </header>

    <section class="upload-card">
        <div class="upload-inner">
            <input type="text" id="semester" class="text-input semester-input" placeholder="Semester e.g. Spring 2026">
            <input type="file" id="file" class="file-input" accept=".xlsx">
            <button id="uploadBtn" class="green-btn upload-btn">UPLOAD</button>
            <p id="uploadStatus" class="status-text"></p>
        </div>
    </section>
       <section class="faculty-search-card">
    <div class="faculty-search-head">
        <h2>Find Courses by Faculty</h2>
        <p>Search by faculty code for the current uploaded semester.</p>
    </div>

    <div class="faculty-search-row">
        <input type="text" id="facultyCodeSearch" class="text-input faculty-search-input" placeholder="Faculty code e.g. ASR">
        <button id="facultySearchBtn" class="subtle-btn" type="button">Search</button>
    </div>

    <div id="facultyCourseResults" class="faculty-course-results">
        <div class="empty-box">Upload a schedule first, then search by faculty code.</div>
    </div>
</section>
    <section class="main-card">
        <section class="list-card">
            <div class="list-card-header">
                <h2>Course List:</h2>
                <div class="list-actions">
                    <button type="button" class="subtle-btn" onclick="selectAllCourses()">Select All</button>
                    <button type="button" class="subtle-btn" onclick="clearSelectedCourses()">Clear All</button>
                </div>
            </div>

            <div id="courseList" class="course-list">
                <div class="empty-box">Upload a schedule to see detected courses.</div>
            </div>
        </section>

        <section class="selected-card">
            <h2>Added Courses</h2>

            <div id="selectedCourseList" class="selected-course-list">
                <div class="empty-box">No courses added yet.</div>
            </div>

            <div class="generate-wrap">
                <button id="generateBtn" class="green-btn generate-btn">Generate routine</button>
            </div>
        </section>
    </section>
        <section class="preference-card">
    <div class="preference-head">
        <h2>Routine Preferences</h2>
        <p>Filter routine options before they are generated.</p>
    </div>

    <div class="preference-grid">
        <label class="preference-field">
            <span>No classes before</span>
            <select id="noClassesBefore" class="preference-select">
                <option value="">No preference</option>
                <option value="08:00">8:00 AM</option>
                <option value="09:25">9:25 AM</option>
                <option value="10:50">10:50 AM</option>
                <option value="12:15">12:15 PM</option>
                <option value="13:40">1:40 PM</option>
                <option value="15:05">3:05 PM</option>
                <option value="16:30">4:30 PM</option>
            </select>
        </label>

        <label class="preference-field">
            <span>No classes after</span>
            <select id="noClassesAfter" class="preference-select">
                <option value="">No preference</option>
                <option value="09:20">9:20 AM</option>
                <option value="10:45">10:45 AM</option>
                <option value="12:10">12:10 PM</option>
                <option value="13:35">1:35 PM</option>
                <option value="15:00">3:00 PM</option>
                <option value="16:25">4:25 PM</option>
                <option value="17:50">5:50 PM</option>
            </select>
        </label>
    </div>

    <div class="day-filter-wrap">
        <span class="day-filter-title">Avoid these days</span>

        <div class="day-filter-list">
            <label><input type="checkbox" class="blocked-day" value="SUN"> Sun</label>
            <label><input type="checkbox" class="blocked-day" value="MON"> Mon</label>
            <label><input type="checkbox" class="blocked-day" value="TUE"> Tue</label>
            <label><input type="checkbox" class="blocked-day" value="WED"> Wed</label>
            <label><input type="checkbox" class="blocked-day" value="THU"> Thu</label>
            <label><input type="checkbox" class="blocked-day" value="SAT"> Sat</label>
        </div>
    </div>
</section>
    <section class="results-block">
        <div class="results-head">
            <h2>Generated routines</h2>
            <div id="resultSummary" class="result-summary"></div>
        </div>

        <div id="results" class="results-grid">
            <div class="empty-box">No routines generated yet.</div>
        </div>
    </section>

    <footer class="tiny-footer">
        <p class="tiny-label">ABOUT US</p>
        <p>CSE3200 — Design Project II</p>
        <p>Section 4</p>
        <p>Instructor: Atanu Shuvam Roy</p>
        <p>Group Members:</p>
        <p>Tasnim Haque Sahil — 231014094</p>
        <p>Md. Rakin Sadab — 223014046</p>
        <p>Asad Uz Zaman Nur — 223014153</p>
    </footer>
</div>

<script>
    window.APP_CONTEXT = "${pageContext.request.contextPath}";
</script>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=2"></script>
</body>
</html>