import json
import os
import tempfile
import time
import urllib.request
from flask import Flask, jsonify, request

app = Flask(__name__)

API = "https://api.magichour.ai"
KEY = os.environ.get("MAGIC_HOUR_API_KEY")


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
            "products"
        ]
    })


@app.get("/health")
def health():

    return jsonify({
        "status": "ok",
        "configured": bool(KEY)
    })


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
