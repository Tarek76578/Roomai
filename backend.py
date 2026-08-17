import base64
import json
import os
import tempfile
import time
import urllib.request
import urllib.error
import sqlite3
import hashlib
import secrets
import hmac
import re
from flask import Flask, jsonify, request

app = Flask(__name__)

API = "https://api.magichour.ai"
KEY = os.environ.get("MAGIC_HOUR_API_KEY")
GEMINI_KEY = os.environ.get("GEMINI_API_KEY")
GEMINI_KEY_31 = os.environ.get("GEMINI_API_KEY_31")
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-3.5-flash")
GEMINI_MODEL_31 = os.environ.get("GEMINI_MODEL_31", "gemini-3.1-flash-lite")

# ============================================================
# RoomAI Cost Control / Free-Pro foundation
# ============================================================

ROOMAI_FREE_MONTHLY_LIMIT = int(
    os.environ.get("ROOMAI_FREE_MONTHLY_LIMIT", "5")
)

ROOMAI_PRO_MONTHLY_LIMIT = int(
    os.environ.get("ROOMAI_PRO_MONTHLY_LIMIT", "100")
)

ROOMAI_DB = os.environ.get(
    "ROOMAI_USAGE_DB",
    os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "roomai_usage.db"
    )
)

# Pro entitlements are server-controlled.
# Until Google Play Billing is connected, nobody becomes Pro
# merely by sending plan=pro from the Android client.
ROOMAI_PRO_DEVICE_IDS = {
    x.strip()
    for x in os.environ.get(
        "ROOMAI_PRO_DEVICE_IDS",
        ""
    ).split(",")
    if x.strip()
}


ROOMAI_SESSION_DAYS = int(
    os.environ.get("ROOMAI_SESSION_DAYS", "30")
)

EMAIL_RE = re.compile(
    r"^[^@\s]+@[^@\s]+\.[^@\s]+$"
)


def normalize_email(email):
    return email.strip().lower()


def hash_password(password, salt=None):
    if salt is None:
        salt = secrets.token_bytes(16)

    digest = hashlib.pbkdf2_hmac(
        "sha256",
        password.encode("utf-8"),
        salt,
        120000
    )

    return salt.hex() + ":" + digest.hex()


def verify_password(password, stored):
    try:
        salt_hex, digest_hex = stored.split(":", 1)

        salt = bytes.fromhex(salt_hex)

        candidate = hashlib.pbkdf2_hmac(
            "sha256",
            password.encode("utf-8"),
            salt,
            120000
        ).hex()

        return hmac.compare_digest(
            candidate,
            digest_hex
        )

    except Exception:
        return False


def auth_token():
    header = request.headers.get(
        "Authorization",
        ""
    ).strip()

    if not header.startswith("Bearer "):
        return ""

    return header[7:].strip()


def authenticated_user(required=True):
    token = auth_token()

    if not token:
        if required:
            return None
        return None

    connection = db()

    try:
        row = connection.execute(
            """
            SELECT
                users.id,
                users.email,
                users.plan,
                users.device_id
            FROM sessions
            JOIN users
              ON users.id = sessions.user_id
            WHERE sessions.token = ?
              AND sessions.expires_at > ?
            """,
            (
                token,
                int(time.time())
            )
        ).fetchone()

        if not row:
            return None

        return {
            "id": row[0],
            "email": row[1],
            "plan": row[2],
            "device_id": row[3]
        }

    finally:
        connection.close()


def require_user():
    user = authenticated_user()

    if not user:
        return None, (
            jsonify({
                "error": "Authentication required",
                "code": "AUTH_REQUIRED"
            }),
            401
        )

    return user, None


def db():
    connection = sqlite3.connect(
        ROOMAI_DB,
        timeout=10
    )
    connection.execute("""
        CREATE TABLE IF NOT EXISTS usage (
            device_id TEXT NOT NULL,
            month TEXT NOT NULL,
            used INTEGER NOT NULL DEFAULT 0,
            reserved INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (device_id, month)
        )
    """)

    connection.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL,
            device_id TEXT NOT NULL UNIQUE,
            plan TEXT NOT NULL DEFAULT 'free',
            created_at INTEGER NOT NULL
        )
    """)

    connection.execute("""
        CREATE TABLE IF NOT EXISTS sessions (
            token TEXT PRIMARY KEY,
            user_id INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(user_id) REFERENCES users(id)
        )
    """)

    connection.commit()
    return connection


def usage_key():
    user = authenticated_user()

    if user:
        return "user:" + str(user["id"])

    # Authentication is required for quota-protected endpoints.
    # Keep the fallback only for internal compatibility.
    device_id = request.form.get(
        "device_id",
        ""
    ).strip()

    if not device_id:
        device_id = request.headers.get(
            "X-RoomAI-Device",
            ""
        ).strip()

    if not device_id:
        device_id = request.remote_addr or "unknown"

    return device_id[:128]


def current_month():
    return time.strftime("%Y-%m")


def get_plan():
    user = authenticated_user()

    if user:
        return "pro" if user["plan"] == "pro" else "free"

    # Legacy/internal fallback.
    device_id = usage_key()

    if device_id in ROOMAI_PRO_DEVICE_IDS:
        return "pro"

    return "free"


def usage_status():
    key = usage_key()
    month = current_month()
    plan = get_plan()

    limit = (
        ROOMAI_PRO_MONTHLY_LIMIT
        if plan == "pro"
        else ROOMAI_FREE_MONTHLY_LIMIT
    )

    connection = db()

    try:
        row = connection.execute(
            """
            SELECT used, reserved
            FROM usage
            WHERE device_id = ?
              AND month = ?
            """,
            (key, month)
        ).fetchone()
    finally:
        connection.close()

    used = row[0] if row else 0
    reserved = row[1] if row else 0

    return {
        "plan": plan,
        "used": used,
        "reserved": reserved,
        "limit": limit,
        "remaining": max(
            0,
            limit - used - reserved
        ),
        "month": month
    }


def reserve_generation():
    key = usage_key()
    month = current_month()
    plan = get_plan()

    limit = (
        ROOMAI_PRO_MONTHLY_LIMIT
        if plan == "pro"
        else ROOMAI_FREE_MONTHLY_LIMIT
    )

    connection = db()

    try:
        # Lock the SQLite database before checking/updating usage.
        # This prevents two simultaneous requests from consuming
        # the same remaining generation.
        connection.execute("BEGIN IMMEDIATE")

        connection.execute(
            """
            INSERT OR IGNORE INTO usage(
                device_id,
                month,
                used,
                reserved
            )
            VALUES (?, ?, 0, 0)
            """,
            (key, month)
        )

        row = connection.execute(
            """
            SELECT used, reserved
            FROM usage
            WHERE device_id = ?
              AND month = ?
            """,
            (key, month)
        ).fetchone()

        used = row[0]
        reserved = row[1]

        if used + reserved >= limit:
            connection.rollback()

            return False, {
                "plan": plan,
                "used": used,
                "reserved": reserved,
                "limit": limit,
                "remaining": 0,
                "month": month
            }

        connection.execute(
            """
            UPDATE usage
            SET reserved = reserved + 1
            WHERE device_id = ?
              AND month = ?
            """,
            (key, month)
        )

        connection.commit()

        return True, {
            "plan": plan,
            "used": used,
            "reserved": reserved + 1,
            "limit": limit,
            "remaining": max(
                0,
                limit - used - reserved - 1
            ),
            "month": month
        }

    except Exception:
        connection.rollback()
        raise

    finally:
        connection.close()

def commit_generation():
    key = usage_key()
    month = current_month()

    connection = db()

    try:
        connection.execute("BEGIN IMMEDIATE")

        row = connection.execute(
            """
            SELECT used, reserved
            FROM usage
            WHERE device_id = ?
              AND month = ?
            """,
            (key, month)
        ).fetchone()

        if not row or row[1] <= 0:
            connection.rollback()
            return False

        connection.execute(
            """
            UPDATE usage
            SET reserved = reserved - 1,
                used = used + 1
            WHERE device_id = ?
              AND month = ?
              AND reserved > 0
            """,
            (key, month)
        )

        connection.commit()
        return True

    except Exception:
        connection.rollback()
        raise

    finally:
        connection.close()


def release_generation():
    key = usage_key()
    month = current_month()

    connection = db()

    try:
        connection.execute(
            """
            UPDATE usage
            SET reserved = CASE
                    WHEN reserved > 0
                    THEN reserved - 1
                    ELSE 0
                END
            WHERE device_id = ?
              AND month = ?
            """,
            (key, month)
        )

        connection.commit()

    finally:
        connection.close()


def usage_response():
    status = usage_status()

    return jsonify({
        "status": "ok",
        **status
    })



def api_request(url, method="GET", data=None, headers=None):
    req = urllib.request.Request(
        url,
        data=data,
        headers=headers or {},
        method=method
    )
    try:
        with urllib.request.urlopen(req, timeout=90) as r:
            return r.read()
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            "HTTP %s from %s: %s"
            % (e.code, url.split("?")[0], body)
        )
    except urllib.error.URLError as e:
        raise RuntimeError(
            "Network error: %s" % e
        )


def run_editor(image_path, ext, prompt):
    payload = {
        "items": [{
            "extension": ext,
            "type": "image"
        }]
    }

    response = api_request(
        API + "/v1/files/upload-urls",
        "POST",
        json.dumps(payload).encode(),
        {
            "Authorization": "Bearer " + KEY,
            "Content-Type": "application/json"
        }
    )

    item = json.loads(response)["items"][0]

    with open(image_path, "rb") as f:
        image_data = f.read()

    content_type = {
        "jpg": "image/jpeg",
        "jpeg": "image/jpeg",
        "png": "image/png",
        "webp": "image/webp"
    }.get(ext, "image/jpeg")

    api_request(
        item["upload_url"],
        "PUT",
        image_data,
        {"Content-Type": content_type}
    )

    payload = {
        "name": "RoomAI",
        "image_count": 1,
        "model": "qwen-edit",
        "aspect_ratio": "auto",
        "resolution": "640px",
        "style": {
            "prompt": prompt
        },
        "assets": {
            "image_file_paths": [
                item["file_path"]
            ]
        }
    }

    response = api_request(
        API + "/v1/ai-image-editor",
        "POST",
        json.dumps(payload).encode(),
        {
            "Authorization": "Bearer " + KEY,
            "Content-Type": "application/json"
        }
    )

    project_id = json.loads(response)["id"]

    status_url = (
        API +
        "/v1/image-projects/" +
        project_id
    )

    for _ in range(120):

        response = api_request(
            status_url,
            "GET",
            headers={
                "Authorization": "Bearer " + KEY
            }
        )

        result = json.loads(response)
        status = result.get("status")

        if status == "complete":

            downloads = result.get(
                "downloads",
                []
            )

            if not downloads:
                raise RuntimeError(
                    "No result image"
                )

            return downloads[0]["url"]

        if status in (
            "error",
            "canceled"
        ):
            raise RuntimeError(
                json.dumps(result)
            )

        time.sleep(5)

    raise TimeoutError(
        "Generation timeout"
    )


def check_gemini_models():
    if not GEMINI_KEY:
        raise RuntimeError("GEMINI_API_KEY missing")

    url = "https://generativelanguage.googleapis.com/v1beta/models"

    response = api_request(
        url,
        "GET",
        None,
        {
            "x-goog-api-key": GEMINI_KEY
        }
    )

    return json.loads(response)


def diagnose_with_gemini(image_path, mime_type):
    if not GEMINI_KEY:
        raise RuntimeError("GEMINI_API_KEY missing")

    with open(image_path, "rb") as f:
        image_b64 = base64.b64encode(f.read()).decode("utf-8")

    prompt = """
You are RoomAI Advisor, an expert interior-design problem detector.

Analyze the uploaded real room photo. Do NOT redesign the room.
Your job is to identify practical problems that could cause the user
to make a bad room decision.

Return ONLY valid JSON with exactly these top-level fields:

{
  "summary": "short overall diagnosis",
  "score": 0,
  "problems": [
    {
      "title": "problem",
      "severity": "low|medium|high",
      "reason": "why it matters",
      "recommendation": "what to do"
    }
  ],
  "risk_scanner": [
    {
      "type": "space|movement|lighting|storage|access|cleaning|installation|budget|other",
      "severity": "low|medium|high",
      "message": "risk"
    }
  ],
  "keep": ["items that should probably be kept"],
  "replace": ["items that may be worth replacing"],
  "upgrade": ["items that could be improved"],
  "lifestyle_questions": [
    "questions whose answers would improve the diagnosis"
  ]
}

Rules:
- Never invent exact measurements from a single photo.
- Clearly distinguish visual observations from assumptions.
- If something cannot be verified, say so.
- Focus on actionable problems, not generic compliments.
- Do not recommend expensive changes unless justified.
- Preserve the user's existing room where possible.
- The score should represent practical room readiness, not beauty.
"""

    payload = {
        "contents": [{
            "parts": [
                {"text": prompt},
                {
                    "inline_data": {
                        "mime_type": mime_type,
                        "data": image_b64
                    }
                }
            ]
        }],
        "generationConfig": {
            "temperature": 0.2,
            "responseMimeType": "application/json"
        }
    }

    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        + GEMINI_MODEL
        + ":generateContent"
    )

    try:
        response = api_request(
            url,
            "POST",
            json.dumps(payload).encode(),
            {
                "Content-Type": "application/json",
                "x-goog-api-key": GEMINI_KEY
            }
        )
    except RuntimeError as e:
        # Fallback to Gemini 3.1 Flash-Lite when the primary model is unavailable.
        if "HTTP 503" not in str(e) or not GEMINI_KEY_31:
            raise

        fallback_url = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            + GEMINI_MODEL_31
            + ":generateContent"
        )

        response = api_request(
            fallback_url,
            "POST",
            json.dumps(payload).encode(),
            {
                "Content-Type": "application/json",
                "x-goog-api-key": GEMINI_KEY_31
            }
        )

    result = json.loads(response)

    try:
        text = result["candidates"][0]["content"]["parts"][0]["text"]
        diagnosis = json.loads(text)
    except Exception:
        raise RuntimeError(
            "Gemini returned an invalid diagnosis"
        )

    return diagnosis


def diagnose():
    if "image" not in request.files:
        return jsonify({"error": "No image"}), 400

    image = request.files["image"]

    if not image.filename:
        return jsonify({"error": "Invalid image"}), 400

    ext = (
        image.filename.rsplit(".", 1)[-1].lower()
        if "." in image.filename
        else "jpg"
    )

    mime_type = {
        "jpg": "image/jpeg",
        "jpeg": "image/jpeg",
        "png": "image/png",
        "webp": "image/webp"
    }.get(ext, "image/jpeg")

    image_path = None

    try:
        with tempfile.NamedTemporaryFile(
            suffix="." + ext,
            delete=False
        ) as f:
            image.save(f)
            image_path = f.name

        diagnosis = diagnose_with_gemini(
            image_path,
            mime_type
        )

        return jsonify({
            "status": "complete",
            "diagnosis": diagnosis
        })

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        if image_path:
            try:
                os.remove(image_path)
            except:
                pass



def process():

    # Login is optional.
    # Authenticated users are keyed by user id.
    # Guests are keyed by their device id through usage_key().
    user = authenticated_user()

    allowed, usage = reserve_generation()

    if not allowed:
        return jsonify({
            "error": "Generation limit reached",
            "code": "USAGE_LIMIT_REACHED",
            "plan": usage["plan"],
            "used": usage["used"],
            "reserved": usage["reserved"],
            "limit": usage["limit"],
            "remaining": usage["remaining"],
            "month": usage["month"]
        }), 429

    reservation_active = True

    if not KEY:
        release_generation()
        reservation_active = False
        return jsonify({
            "error":
            "MAGIC_HOUR_API_KEY missing"
        }), 500

    if "image" not in request.files:
        release_generation()
        reservation_active = False
        return jsonify({
            "error": "No image"
        }), 400

    image = request.files["image"]

    if not image.filename:
        release_generation()
        reservation_active = False
        return jsonify({
            "error": "Invalid image"
        }), 400

    operation = request.form.get(
        "operation",
        "generate"
    ).strip().lower()

    room = request.form.get(
        "room",
        "Living Room"
    ).strip()

    style = request.form.get(
        "style",
        "Modern"
    ).strip()

    selected = request.form.get(
        "selection",
        ""
    ).strip()

    user_prompt = request.form.get(
        "prompt",
        ""
    ).strip()

    prompts = {

        "generate":
            f"""Transform this {room} into a beautiful
            {style} interior.
            Add appropriate furniture, decoration and lighting.
            Preserve walls, windows, doors, floor, architecture,
            perspective and camera angle.
            Make it photorealistic and professionally designed.""",

        "enhance":
            """Enhance this interior photograph.
            Improve lighting, realism, materials, shadows,
            colors and fine details.
            Preserve the exact architecture and camera angle.
            Do not redesign the room.""",

        "furniture":
            f"""Redesign the furniture in this {room}.
            Preserve architecture, walls, windows, doors,
            floor and camera perspective.
            Use a {style} interior style.
            Focus strongly on furniture quality and placement.""",

        "products":
            f"""Create a professional interior styling pass
            for this {room}.
            Add coordinated furniture, lighting and decor
            products suitable for a {style} interior.
            Preserve architecture and perspective.""",

        "fix":
            f"""Fix ONLY the specific problem identified by the user
            in this {room}.

            Do not redesign the entire room.
            Do not change the room style unless absolutely necessary
            to solve the identified problem.

            Preserve the exact walls, windows, doors, floor,
            ceiling, architecture, camera angle and perspective.

            Preserve all existing objects that are unrelated to
            the identified problem.

            Make the correction realistic, practical and
            professionally designed.

            Problem: {selected}"""
    }

    prompt = prompts.get(
        operation,
        prompts["generate"]
    )

    if selected:
        prompt += (
            " The user selected: " +
            selected + "."
        )

    if user_prompt:
        prompt += (
            " Additional instructions: " +
            user_prompt
        )

    image_path = None

    try:

        ext = (
            image.filename
            .rsplit(".", 1)[-1]
            .lower()
            if "." in image.filename
            else "jpg"
        )

        if ext not in (
            "jpg",
            "jpeg",
            "png",
            "webp"
        ):
            ext = "jpg"

        with tempfile.NamedTemporaryFile(
            suffix="." + ext,
            delete=False
        ) as f:
            image.save(f)
            image_path = f.name

        result = run_editor(
            image_path,
            ext,
            prompt
        )

        if reservation_active:
            commit_generation()
            reservation_active = False

        return jsonify({
            "status": "complete",
            "operation": operation,
            "image_url": result,
            "usage": usage_status()
        })

    except Exception as e:

        if reservation_active:
            release_generation()
            reservation_active = False

        return jsonify({
            "error": str(e)
        }), 500

    finally:

        if image_path:

            try:
                os.remove(image_path)
            except:
                pass


@app.post("/auth/register")
def auth_register():
    data = request.get_json(silent=True) or {}

    email = normalize_email(
        str(data.get("email", ""))
    )

    password = str(
        data.get("password", "")
    )

    device_id = str(
        data.get("device_id", "")
    ).strip()

    if not EMAIL_RE.match(email):
        return jsonify({
            "error": "Invalid email",
            "code": "INVALID_EMAIL"
        }), 400

    if len(password) < 8:
        return jsonify({
            "error": "Password must contain at least 8 characters",
            "code": "INVALID_PASSWORD"
        }), 400

    if not device_id:
        return jsonify({
            "error": "Device ID required",
            "code": "DEVICE_ID_REQUIRED"
        }), 400

    device_id = device_id[:128]

    connection = db()

    try:
        existing_email = connection.execute(
            """
            SELECT id
            FROM users
            WHERE email = ?
            """,
            (email,)
        ).fetchone()

        if existing_email:
            return jsonify({
                "error": "Email already registered",
                "code": "EMAIL_EXISTS"
            }), 409

        existing_device = connection.execute(
            """
            SELECT id, email
            FROM users
            WHERE device_id = ?
            """,
            (device_id,)
        ).fetchone()

        if existing_device:
            return jsonify({
                "error": "This device already has an account",
                "code": "DEVICE_ALREADY_REGISTERED"
            }), 409

        password_hash = hash_password(password)
        now = int(time.time())

        cursor = connection.execute(
            """
            INSERT INTO users(
                email,
                password_hash,
                device_id,
                plan,
                created_at
            )
            VALUES (?, ?, ?, 'free', ?)
            """,
            (
                email,
                password_hash,
                device_id,
                now
            )
        )

        user_id = cursor.lastrowid

        token = secrets.token_urlsafe(48)
        expires_at = now + (
            ROOMAI_SESSION_DAYS * 86400
        )

        connection.execute(
            """
            INSERT INTO sessions(
                token,
                user_id,
                expires_at,
                created_at
            )
            VALUES (?, ?, ?, ?)
            """,
            (
                token,
                user_id,
                expires_at,
                now
            )
        )

        connection.commit()

        return jsonify({
            "status": "ok",
            "token": token,
            "user": {
                "id": user_id,
                "email": email,
                "plan": "free"
            }
        })

    except sqlite3.IntegrityError:
        connection.rollback()

        return jsonify({
            "error": "Account already exists",
            "code": "ACCOUNT_EXISTS"
        }), 409

    finally:
        connection.close()


@app.post("/auth/login")
def auth_login():
    data = request.get_json(silent=True) or {}

    email = normalize_email(
        str(data.get("email", ""))
    )

    password = str(
        data.get("password", "")
    )

    if not email or not password:
        return jsonify({
            "error": "Email and password are required",
            "code": "LOGIN_REQUIRED"
        }), 400

    connection = db()

    try:
        row = connection.execute(
            """
            SELECT
                id,
                email,
                password_hash,
                plan
            FROM users
            WHERE email = ?
            """,
            (email,)
        ).fetchone()

        if not row or not verify_password(
            password,
            row[2]
        ):
            return jsonify({
                "error": "Invalid email or password",
                "code": "INVALID_CREDENTIALS"
            }), 401

        now = int(time.time())
        token = secrets.token_urlsafe(48)

        expires_at = now + (
            ROOMAI_SESSION_DAYS * 86400
        )

        connection.execute(
            """
            INSERT INTO sessions(
                token,
                user_id,
                expires_at,
                created_at
            )
            VALUES (?, ?, ?, ?)
            """,
            (
                token,
                row[0],
                expires_at,
                now
            )
        )

        connection.commit()

        return jsonify({
            "status": "ok",
            "token": token,
            "user": {
                "id": row[0],
                "email": row[1],
                "plan": row[3]
            }
        })

    finally:
        connection.close()


@app.get("/auth/me")
def auth_me():
    user, error = require_user()

    if error:
        return error

    return jsonify({
        "status": "ok",
        "user": {
            "id": user["id"],
            "email": user["email"],
            "plan": user["plan"]
        }
    })


@app.post("/auth/logout")
def auth_logout():
    token = auth_token()

    if token:
        connection = db()

        try:
            connection.execute(
                """
                DELETE FROM sessions
                WHERE token = ?
                """,
                (token,)
            )

            connection.commit()

        finally:
            connection.close()

    return jsonify({
        "status": "ok"
    })


@app.get("/")
def home():

    return jsonify({
        "status":
        "RoomAI Backend OK",

        "magic_hour_configured":
        bool(KEY),

        "operations": [
            "generate",
            "enhance",
            "furniture",
            "products",
            "fix",
            "diagnose"
        ]
    })


@app.post("/usage")
def usage_route():
    # Guest users use device-based quota.
    # Logged-in users use account-based quota.
    return usage_response()


@app.get("/health")
def health():

    return jsonify({
        "status": "ok",
        "configured": bool(KEY)
    })



@app.get("/debug/gemini")
def debug_gemini():
    return {
        "gemini_key_present": bool(os.environ.get("GEMINI_API_KEY")),
        "gemini_model": os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
    }

@app.get("/gemini-models")
def gemini_models_route():
    try:
        return jsonify(check_gemini_models())
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.post("/diagnose")
def diagnose_route():
    return diagnose()


@app.post("/generate")
def generate():
    return process()


# ============================================================
# RoomAI Batch 4 - Real Vision Verification
# ============================================================

def download_image(url):
    try:
        return urllib.request.urlopen(url, timeout=90).read()
    except Exception as e:
        raise RuntimeError("Could not download generated image: %s" % e)



def detect_image_mime(data):
    if not data:
        return "image/jpeg"

    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"

    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"

    if data.startswith(b"RIFF") and len(data) >= 12 and data[8:12] == b"WEBP":
        return "image/webp"

    if data.startswith(b"GIF87a") or data.startswith(b"GIF89a"):
        return "image/gif"

    return "image/jpeg"


def verify_with_gemini(original_bytes, generated_bytes, original_mime, generated_mime, request_data):
    if not GEMINI_KEY:
        raise RuntimeError("GEMINI_API_KEY missing")

    protected = request_data.get("protected_elements", [])
    target = request_data.get("target", "")
    instruction = request_data.get("instruction", "")

    prompt = f"""
You are RoomAI Vision Verification Engine.

Compare IMAGE 1 (ORIGINAL) with IMAGE 2 (GENERATED).

TARGET:
{target}

USER INSTRUCTION:
{instruction}

PROTECTED ELEMENTS:
{json.dumps(protected)}

Your job is NOT to judge beauty.

Determine whether the requested edit was actually performed while preserving
everything that was explicitly protected.

Return ONLY valid JSON with exactly these fields:

{{
  "status": "PASS|FAIL",
  "score": 0,
  "target_changed": true,
  "protected_elements_changed": false,
  "architecture_changed": false,
  "camera_changed": false,
  "perspective_changed": false,
  "unrelated_objects_changed": false,
  "message": "short explanation"
}}

Rules:
- Compare both images carefully.
- Do not invent measurements.
- If the target cannot be visually verified, FAIL.
- If a protected element changed materially, FAIL.
- If architecture changed, FAIL.
- If camera or perspective changed materially, FAIL.
- If unrelated objects changed materially, FAIL.
- PASS only when the requested change is visible and preservation is acceptable.
- Score 0-100.
- Be conservative.
"""

    def call_model(model, key):
        payload = {
            "contents": [{
                "parts": [
                    {"text": prompt},
                    {
                        "inline_data": {
                            "mime_type": original_mime,
                            "data": base64.b64encode(original_bytes).decode("utf-8")
                        }
                    },
                    {
                        "inline_data": {
                            "mime_type": generated_mime,
                            "data": base64.b64encode(generated_bytes).decode("utf-8")
                        }
                    }
                ]
            }],
            "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
            }
        }

        url = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            + model
            + ":generateContent"
        )

        response = api_request(
            url,
            "POST",
            json.dumps(payload).encode(),
            {
                "Content-Type": "application/json",
                "x-goog-api-key": key
            }
        )

        result = json.loads(response)

        text = (
            result["candidates"][0]["content"]["parts"][0]["text"]
        )

        return json.loads(text)

    try:
        return call_model(GEMINI_MODEL, GEMINI_KEY)
    except RuntimeError as e:
        if "HTTP 503" not in str(e) or not GEMINI_KEY_31:
            raise
        return call_model(GEMINI_MODEL_31, GEMINI_KEY_31)


def verify():
    if "original" not in request.files:
        return jsonify({"error": "No original image"}), 400

    original = request.files["original"]

    if not original.filename:
        return jsonify({"error": "Invalid original image"}), 400

    generated_url = request.form.get("generated_url", "").strip()

    if not generated_url:
        return jsonify({"error": "Missing generated_url"}), 400

    target = request.form.get("target", "").strip()
    instruction = request.form.get("instruction", "").strip()

    try:
        protected_raw = request.form.get(
            "protected_elements",
            "[]"
        )

        try:
            protected = json.loads(protected_raw)
        except Exception:
            protected = []

        original_bytes = original.read()

        generated_bytes = download_image(generated_url)

        original_mime = detect_image_mime(original_bytes)
        generated_mime = detect_image_mime(generated_bytes)

        verification = verify_with_gemini(
            original_bytes,
            generated_bytes,
            original_mime,
            generated_mime,
            {
                "target": target,
                "instruction": instruction,
                "protected_elements": protected
            }
        )

        status = verification.get("status", "FAIL").upper()

        score = int(verification.get("score", 0) or 0)

        target_changed = bool(
            verification.get("target_changed", False)
        )

        protected_changed = bool(
            verification.get("protected_elements_changed", False)
        )

        architecture_changed = bool(
            verification.get("architecture_changed", False)
        )

        camera_changed = bool(
            verification.get("camera_changed", False)
        )

        perspective_changed = bool(
            verification.get("perspective_changed", False)
        )

        unrelated_changed = bool(
            verification.get("unrelated_objects_changed", False)
        )

        # Deterministic RoomAI safety gate.
        # Gemini proposes the assessment; RoomAI makes the final decision.
        safe_to_pass = (
            status == "PASS"
            and score >= 70
            and target_changed
            and not protected_changed
            and not architecture_changed
            and not camera_changed
            and not perspective_changed
            and not unrelated_changed
        )

        if safe_to_pass:
            verification["status"] = "PASS"
        else:
            verification["status"] = "FAIL"

            reasons = []

            if status != "PASS":
                reasons.append("vision model did not approve the edit")
            if score < 70:
                reasons.append("verification score below 70")
            if not target_changed:
                reasons.append("requested target change was not verified")
            if protected_changed:
                reasons.append("protected element changed")
            if architecture_changed:
                reasons.append("room architecture changed")
            if camera_changed:
                reasons.append("camera changed")
            if perspective_changed:
                reasons.append("perspective changed")
            if unrelated_changed:
                reasons.append("unrelated object changed")

            verification["message"] = (
                "Verification rejected: " + "; ".join(reasons)
            )

        return jsonify({
            "status": "complete",
            "verification": verification
        })

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500


app.add_url_rule(
    "/verify",
    "verify",
    verify,
    methods=["POST"]
)


if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=int(os.environ.get("PORT", "5000"))
    )
