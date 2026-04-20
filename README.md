# MySched API Documentation

**University of Liberal Arts**

## API Surface

| Endpoint | Method | Purpose | Notes |
|---|---|---|---|
| `/api/uploads` | POST | Upload and import a semester XLSX workbook. | Creates a new upload batch and parses course offerings and meetings. |
| `/api/courses?batchId={id}` | GET | List distinct course codes for an imported batch. | Used after upload to populate the course picker. |
| `/api/faculty-courses?batchId={id}&facultyCode={code}` | GET | List distinct course codes taught by a faculty code or matching faculty name text. | Search is space-insensitive and checks both `faculty_code` and `faculty_full_name`. |
| `/api/routines` | POST | Generate conflict-free routine options for selected course codes. | Supports preferences: `noClassesBefore`, `noClassesAfter`, `excludedDays`. |
| `/resources/jakartaee11` | GET | Health/ping resource. | Returns plain text `ping Jakarta EE`. |
| `spacy.url (/parse)` | POST | Internal parser service used by the importer. | Configured in `app.properties` as `http://127.0.0.1:8000/parse`. Not intended as a public endpoint. |

## Endpoint Details

### `POST /api/uploads`

Imports an `.xlsx` schedule workbook and creates a new upload batch.

#### Request fields

| Field | Type | Required |
|---|---|---|
| `semester` | text form field | Yes |
| `file` | `.xlsx` file field | Yes |

#### Validation rules

- `semester` must be non-blank.
- `file` must be present.
- The filename must end with `.xlsx`.

#### Temporary storage

- The servlet writes the upload to a temp file under `/tmp` before import.

#### Error response example

```json
{
  "error": "Only .xlsx files are allowed."
}
```

---

### `GET /api/courses`

Returns distinct course codes for a previously imported batch.

#### Query parameters

| Parameter | Type | Required |
|---|---|---|
| `batchId` | Long | Yes |

#### Behavior notes

- The DAO uses `SELECT DISTINCT course_code ... WHERE batch_id = ? ORDER BY course_code`.
- A missing or non-numeric `batchId` results in HTTP 400 with an error JSON body.

---

### `GET /api/faculty-courses`

Returns distinct course codes for a batch that match a faculty search term.

#### Query parameters

| Parameter | Type | Required |
|---|---|---|
| `batchId` | Long | Yes |
| `facultyCode` | String | Yes |

#### Matching logic

- Search is case-insensitive.
- Spaces are stripped from both stored values and the query.
- The SQL uses `LIKE` on both `faculty_code` and `faculty_full_name`, so partial matches are allowed.

---

### `POST /api/routines`

Generates conflict-free routine options from selected course codes, optionally filtered by user preferences.

#### JSON fields

| Field | Type | Required | Description |
|---|---|---|---|
| `batchId` | Long | Yes | Upload batch to use as the source dataset. |
| `courseCodes` | array[string] | Yes | Course codes selected by the user. |
| `limit` | integer | No | Maximum number of routine options. If omitted, `app.maxRoutineResults` is used. |
| `noClassesBefore` | string (`HH:mm`) | No | Reject offerings whose meeting start time is earlier than this boundary. |
| `noClassesAfter` | string (`HH:mm`) | No | Reject offerings whose meeting end time is later than this boundary. |
| `excludedDays` | array[string] | No | Reject offerings that have meetings on any blocked day. Values are normalized to uppercase, e.g. `SUN`, `MON`. |

#### Generator behavior

- The service first loads all offerings for the selected course codes in the batch, then filters them by preferences.
- If any selected course has no remaining offerings after filtering, the service returns an empty list.
- Conflict checking is based on meeting overlap across candidate offerings.
- Response entries are sorted by day order (`SUN`, `MON`, `TUE`, `WED`, `THU`, `SAT`) and then by start time.

---

### Internal parser service: `POST /parse`

The Java importer uses `SpacyClientService` to call the parser URL configured in `app.properties`. The default is `http://127.0.0.1:8000/parse`.

## Data Model Summary

| Table | Key columns | Purpose |
|---|---|---|
| `upload_batch` | `id`, `semester_name`, `original_filename`, `uploaded_at` | One row per imported workbook. |
| `course_offering` | `id`, `batch_id`, `course_code`, `section_code`, `component_type`, `room`, `faculty_code`, `faculty_full_name`, `raw_text` | Course-section offerings detected from the uploaded workbook. |
| `class_meeting` | `id`, `offering_id`, `day_name`, `start_time`, `end_time` | Meeting instances attached to an offering. |

## Operational Notes

- No authentication or authorization is implemented in this source.
- Uploads are additive: every upload creates a new batch. Client calls must continue using the returned `batchId`.
- The faculty search endpoint searches within a single batch only.
- The production deployment analyzed in conversation used Apache as a reverse proxy in front of Tomcat and a separate internal FastAPI/spaCy process.
- The importer depends on the Python parser for best results but can fall back to regex extraction.
