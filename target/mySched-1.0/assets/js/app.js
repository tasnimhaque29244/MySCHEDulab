let currentBatchId = null;
let allCourses = [];
let selectedCourseSet = new Set();

const uploadBtn = document.getElementById("uploadBtn");
const generateBtn = document.getElementById("generateBtn");
const uploadStatus = document.getElementById("uploadStatus");
const courseList = document.getElementById("courseList");
const selectedCourseList = document.getElementById("selectedCourseList");
const results = document.getElementById("results");
const resultSummary = document.getElementById("resultSummary");
const facultySearchBtn = document.getElementById("facultySearchBtn");
const facultyCourseResults = document.getElementById("facultyCourseResults");

const DAY_ORDER = ["SUN", "MON", "TUE", "WED", "THU", "SAT"];
const DAY_LABELS = {
    SUN: "Sun",
    MON: "Mon",
    TUE: "Tue",
    WED: "Wed",
    THU: "Thu",
    SAT: "Sat"
};
const SCHEDULE_SLOTS = [
    { start: "08:00", end: "09:20" },
    { start: "09:25", end: "10:45" },
    { start: "10:50", end: "12:10" },
    { start: "12:15", end: "13:35" },
    { start: "13:40", end: "15:00" },
    { start: "15:05", end: "16:25" },
    { start: "16:30", end: "17:50" }
];
uploadBtn.addEventListener("click", uploadFile);
generateBtn.addEventListener("click", generateRoutines);
facultySearchBtn.addEventListener("click", searchFacultyCourses);

async function uploadFile() {
    const semester = document.getElementById("semester").value.trim();
    const file = document.getElementById("file").files[0];

    if (!semester || !file) {
        uploadStatus.textContent = "Enter semester and choose a .xlsx file.";
        return;
    }

    const formData = new FormData();
    formData.append("semester", semester);
    formData.append("file", file);

    uploadStatus.textContent = "Uploading and parsing schedule...";
    resetFrontendAfterUploadStart();

    try {
        const response = await fetch(`${window.APP_CONTEXT}/api/uploads`, {
            method: "POST",
            body: formData
        });

        let data = {};
        try {
            data = await response.json();
        } catch (_) {
            data = {};
        }

        if (!response.ok) {
            uploadStatus.textContent = data.error || "Upload failed.";
            return;
        }

        currentBatchId = data.batchId;
        uploadStatus.textContent = `Uploaded successfully: ${data.originalFilename} | ${data.semesterName}`;

        await loadCourses();
    } catch (e) {
        console.error("Upload request failed:", e);
        uploadStatus.textContent = "Upload request failed.";
    }
}

function resetFrontendAfterUploadStart() {
    selectedCourseSet.clear();
    allCourses = [];
    resultSummary.textContent = "";
    courseList.innerHTML = `<div class="empty-box">Loading courses...</div>`;
    selectedCourseList.innerHTML = `<div class="empty-box">No courses added yet.</div>`;
    results.innerHTML = `<div class="empty-box">No routines generated yet.</div>`;
}

async function loadCourses() {
    const response = await fetch(`${window.APP_CONTEXT}/api/courses?batchId=${currentBatchId}`);

    if (!response.ok) {
        throw new Error(`/api/courses failed with status ${response.status}`);
    }

    const data = await response.json();

    if (!Array.isArray(data)) {
        throw new Error("Courses endpoint did not return an array.");
    }

    allCourses = data;
    renderCourseList();
    renderSelectedCourses();
}

function renderCourseList() {
    if (!allCourses.length) {
        courseList.innerHTML = `<div class="empty-box">No courses detected.</div>`;
        return;
    }

    courseList.innerHTML = "";

    allCourses.forEach(course => {
        const label = document.createElement("label");
        label.className = "course-chip";

        const input = document.createElement("input");
        input.type = "checkbox";
        input.checked = selectedCourseSet.has(course);

        const span = document.createElement("span");
        span.textContent = course;

        input.addEventListener("change", () => {
            if (input.checked) {
                selectedCourseSet.add(course);
            } else {
                selectedCourseSet.delete(course);
            }
            renderSelectedCourses();
        });

        label.appendChild(input);
        label.appendChild(span);
        courseList.appendChild(label);
    });
}

function renderSelectedCourses() {
    const selected = Array.from(selectedCourseSet).sort();

    if (!selected.length) {
        selectedCourseList.innerHTML = `<div class="empty-box">No courses added yet.</div>`;
        return;
    }

    selectedCourseList.innerHTML = "";

    selected.forEach(course => {
        const pill = document.createElement("div");
        pill.className = "selected-pill";

        const text = document.createElement("span");
        text.textContent = course;

        const removeBtn = document.createElement("button");
        removeBtn.textContent = "×";
        removeBtn.title = "Remove";

        removeBtn.addEventListener("click", () => {
            selectedCourseSet.delete(course);
            renderCourseList();
            renderSelectedCourses();
        });

        pill.appendChild(text);
        pill.appendChild(removeBtn);
        selectedCourseList.appendChild(pill);
    });
}

function selectAllCourses() {
    allCourses.forEach(course => selectedCourseSet.add(course));
    renderCourseList();
    renderSelectedCourses();
}

function clearSelectedCourses() {
    selectedCourseSet.clear();
    renderCourseList();
    renderSelectedCourses();
}

async function generateRoutines() {
    if (!currentBatchId) {
        alert("Upload a schedule first.");
        return;
    }

    const selected = Array.from(selectedCourseSet);

    if (!selected.length) {
        alert("Select at least one course.");
        return;
    }

    const noClassesBefore = document.getElementById("noClassesBefore")?.value || null;
    const noClassesAfter = document.getElementById("noClassesAfter")?.value || null;
    const excludedDays = Array.from(document.querySelectorAll(".blocked-day:checked"))
        .map(input => input.value);

    results.innerHTML = `<div class="empty-box">Generating routines...</div>`;
    resultSummary.textContent = "";

    try {
        const response = await fetch(`${window.APP_CONTEXT}/api/routines`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                batchId: currentBatchId,
                courseCodes: selected,
                limit: 20,
                noClassesBefore,
                noClassesAfter,
                excludedDays
            })
        });

        const data = await response.json();

        if (!response.ok) {
            results.innerHTML = `<div class="empty-box">${data.error || "Could not generate routines."}</div>`;
            return;
        }

        renderRoutines(data, selected.length);
    } catch (e) {
        console.error(e);
        results.innerHTML = `<div class="empty-box">Failed to generate routines.</div>`;
    }
}

function renderRoutines(routines, selectedCourseCount) {
    results.innerHTML = "";

    if (!routines || !routines.length) {
        resultSummary.textContent = "0 routine option(s)";
        results.innerHTML = `<div class="empty-box">No valid routines found.</div>`;
        return;
    }

    resultSummary.textContent = `${routines.length} routine option(s) found`;

    routines.forEach(routine => {
        const div = document.createElement("div");
        div.className = "routine-card";

        const rows = routine.entries.map(entry => {
            const badgeClass = entry.componentType === "LAB" ? "badge badge-lab" : "badge badge-theory";
            return `
                <tr>
                    <td>${entry.courseCode}</td>
                    <td>${entry.sectionCode}</td>
                    <td><span class="${badgeClass}">${entry.componentType}</span></td>
                    <td>${entry.day}</td>
                    <td>${formatTime(entry.start)}</td>
                    <td>${formatTime(entry.end)}</td>
                    <td>${entry.room || "-"}</td>
                    <td>${entry.faculty || "-"}</td>
                </tr>
            `;
        }).join("");

        div.innerHTML = `
            <div class="routine-card-header">
                <div>
                    <h3>Option ${routine.optionNo}</h3>
                    <div class="routine-meta">${selectedCourseCount} selected course(s)</div>
                </div>
                <div class="routine-meta">${routine.entries.length} meeting block(s)</div>
            </div>

            <div class="routine-body">
                <div>
                    <h4 class="section-title">Weekly Timetable</h4>
                    ${buildTimetable(routine.entries)}
                </div>

                <div>
                    <h4 class="section-title">Routine Details</h4>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>Course</th>
                                    <th>Section</th>
                                    <th>Type</th>
                                    <th>Day</th>
                                    <th>Start</th>
                                    <th>End</th>
                                    <th>Room</th>
                                    <th>Faculty</th>
                                </tr>
                            </thead>
                            <tbody>${rows}</tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        results.appendChild(div);
    });
}


    function buildTimetable(entries) {
    if (!entries || !entries.length) {
        return `<div class="empty-box">No timetable data available.</div>`;
    }

    let html = `<div class="timetable-wrapper">`;
    html += `<div class="timetable-grid" style="grid-template-columns: 120px repeat(${DAY_ORDER.length}, minmax(140px, 1fr));">`;

    html += `<div class="tt-header">Time</div>`;
    DAY_ORDER.forEach(day => {
        html += `<div class="tt-header">${DAY_LABELS[day]}</div>`;
    });

    SCHEDULE_SLOTS.forEach(slot => {
        const slotStartMin = toMinutes(slot.start);
        const slotEndMin = toMinutes(slot.end);

        html += `<div class="tt-time-cell">${formatTime(slot.start)} - ${formatTime(slot.end)}</div>`;

        DAY_ORDER.forEach(day => {
            const matches = entries.filter(entry => {
                if (entry.day !== day) return false;

                const entryStart = toMinutes(normalizeTimeString(entry.start));
                const entryEnd = toMinutes(normalizeTimeString(entry.end));

                return entryStart < slotEndMin && entryEnd > slotStartMin;
            });

            if (!matches.length) {
                html += `<div class="tt-cell"></div>`;
                return;
            }

            const entry = pickBestTimetableEntry(matches);
            const normalizedType = normalizeComponentType(entry.componentType);
            const typeClass = normalizedType === "LAB"
                ? "tt-tag tt-tag-lab"
                : "tt-tag tt-tag-theory";

            html += `
                <div class="tt-cell filled">
                    <div class="tt-course">${entry.courseCode}</div>
                    <div class="tt-section">${entry.sectionCode}</div>
                    <div class="${typeClass}">${normalizedType}</div>
                    <div class="tt-room">${entry.room || "-"}</div>
                    <div class="tt-faculty">${entry.faculty || "-"}</div>
                </div>
            `;
        });
    });

    html += `</div></div>`;
    return html;
}

function pickBestTimetableEntry(matches) {
    // Prefer LAB over THEORY
    const labEntry = matches.find(entry => normalizeComponentType(entry.componentType) === "LAB");
    if (labEntry) {
        return labEntry;
    }
    return matches[0];
}

function normalizeComponentType(type) {
    if (!type) return "THEORY";
    const cleaned = String(type).trim().toUpperCase();
    return cleaned === "LAB" ? "LAB" : "THEORY";
}


function toMinutes(time24) {
    const t = normalizeTimeString(time24);
    const parts = t.split(":");
    return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10);
}

function normalizeTimeString(time24) {
    if (!time24) return "00:00";
    return time24.length >= 5 ? time24.substring(0, 5) : time24;
}

function formatTime(time24) {
    const t = normalizeTimeString(time24);
    const parts = t.split(":");
    let hour = parseInt(parts[0], 10);
    const minute = parts[1];
    const ampm = hour >= 12 ? "PM" : "AM";
    hour = hour % 12;
    if (hour === 0) hour = 12;
    return `${hour}:${minute} ${ampm}`;
}
async function searchFacultyCourses() {
    if (!currentBatchId) {
        facultyCourseResults.innerHTML = `<div class="empty-box">Upload a schedule first.</div>`;
        return;
    }

    const facultyCode = document.getElementById("facultyCodeSearch").value.trim();

    if (!facultyCode) {
        facultyCourseResults.innerHTML = `<div class="empty-box">Enter a faculty code.</div>`;
        return;
    }

    facultyCourseResults.innerHTML = `<div class="empty-box">Searching...</div>`;

    try {
        const response = await fetch(
            `${window.APP_CONTEXT}/api/faculty-courses?batchId=${encodeURIComponent(currentBatchId)}&facultyCode=${encodeURIComponent(facultyCode)}`
        );

        let data = {};
        try {
            data = await response.json();
        } catch (_) {
            data = {};
        }

        if (!response.ok) {
            facultyCourseResults.innerHTML = `<div class="empty-box">${data.error || "Search failed."}</div>`;
            return;
        }

        renderFacultyCourses(data, facultyCode);
    } catch (e) {
        console.error("Faculty search failed:", e);
        facultyCourseResults.innerHTML = `<div class="empty-box">Faculty search failed.</div>`;
    }
}

function renderFacultyCourses(courses, facultyCode) {
    if (!courses || !courses.length) {
        facultyCourseResults.innerHTML = `<div class="empty-box">No courses found for faculty code: ${facultyCode}</div>`;
        return;
    }

    facultyCourseResults.innerHTML = `
        <div class="faculty-result-wrap">
            ${courses.map(course => `<div class="faculty-course-pill">${course}</div>`).join("")}
        </div>
    `;
}