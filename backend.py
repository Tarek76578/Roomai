import base64
import json
import os
import tempfile
import time
import urllib.request
from flask import Flask, jsonify, request

app = Flask(__name__)

API = "https://api.magichour.ai"
KEY = os.environ.get("MAGIC_HOUR_API_KEY")
GEMINI_KEY = os.environ.get("GEMINI_API_KEY")
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")


def api_request(url, method="GET", data=None, headers=None):
    req = urllib.request.Request(
        url,
        data=data,
        headers=headers or {},
        method=method
    )
    with urllib.request.urlopen(req, timeout=90) as r:
        return r.read()


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

    response = api_request(
        url,
        "POST",
        json.dumps(payload).encode(),
        {
            "Content-Type": "application/json",
            "x-goog-api-key": GEMINI_KEY
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

    if not KEY:
        return jsonify({
            "error":
            "MAGIC_HOUR_API_KEY missing"
        }), 500

    if "image" not in request.files:
        return jsonify({
            "error": "No image"
        }), 400

    image = request.files["image"]

    if not image.filename:
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
            Preserve architecture and perspective."""
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

        return jsonify({
            "status": "complete",
            "operation": operation,
            "image_url": result
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
            "diagnose"
        ]
    })


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

@app.post("/diagnose")
def diagnose_route():
    return diagnose()


@app.post("/generate")
def generate():
    return process()


if __name__ == "__main__":

    app.run(
        host="0.0.0.0",
        port=int(
            os.environ.get(
                "PORT",
                "5000"
            )
        )
    )
