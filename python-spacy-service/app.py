from fastapi import FastAPI
from pydantic import BaseModel
import re
import spacy

app = FastAPI()
nlp = spacy.blank("en")


class ParseRequest(BaseModel):
    text: str


class ParseResponse(BaseModel):
    courseCode: str | None
    sectionCode: str | None
    room: str | None
    facultyCode: str | None
    facultyFullName: str | None
    rawText: str


ROOM_PATTERN = re.compile(r'\s-\s*([A-Z]{1,3}\s?\d{2,3}[A-Z]?)\s-')

# handles: - 1 - ROOM -, - 1L - ROOM -, - 1 L - ROOM -
SECTION_BEFORE_ROOM_PATTERN = re.compile(
    r'-\s*([0-9]{1,2}\s*[A-Z]?)\s*(?=-\s*[A-Z]{1,3}\s?\d{2,3}[A-Z]?\s*-)'
)

SECTION_FALLBACK_PATTERN = re.compile(
    r'-\s*([0-9]{1,2}\s*[A-Z]?)\s*-'
)


def normalize_section_code(section: str | None):
    if not section:
        return None
    return re.sub(r'\s+', '', section).upper()


def extract_course_code(text: str):
    dept_match = re.match(r'\s*([A-Z]{2,5})\b', text)
    if not dept_match:
        return None

    dept = dept_match.group(1)

    room_match = ROOM_PATTERN.search(text)
    prefix = text[:room_match.start()] if room_match else text

    nums = re.findall(r'(?<!\d)(\d{4})(?!\d)', prefix)
    if nums:
        return f"{dept} {nums[-1]}"

    direct = re.search(rf'^{dept}\s*/?\s*(\d{{3,4}})', text)
    if direct:
        return f"{dept} {direct.group(1)}"

    return None


def extract_section_code(text: str):
    m = SECTION_BEFORE_ROOM_PATTERN.search(text)
    if m:
        return normalize_section_code(m.group(1))

    m = SECTION_FALLBACK_PATTERN.search(text)
    if m:
        return normalize_section_code(m.group(1))

    return None


def extract_room(text: str):
    m = ROOM_PATTERN.search(text)
    if m:
        return re.sub(r'\s+', ' ', m.group(1)).strip()
    return None


def extract_faculty(text: str):
    m = re.search(r'-\s*([^-\n]+?)\s*$', text)
    if m:
        return m.group(1).strip()
    return None


@app.post("/parse", response_model=ParseResponse)
def parse(req: ParseRequest):
    text = req.text.strip()
    _ = nlp(text)

    course_code = extract_course_code(text)
    section_code = extract_section_code(text)
    room = extract_room(text)
    faculty = extract_faculty(text)

    return ParseResponse(
        courseCode=course_code,
        sectionCode=section_code,
        room=room,
        facultyCode=faculty,
        facultyFullName=None,
        rawText=text
    )