import os
import json
import time
import tempfile
import urllib.request
from flask import Flask, request, jsonify

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

@app.get("/")
def home():
    return jsonify({
        "status": "RoomAI Backend OK",
        "magic_hour_configured": bool(KEY)
    })

@app.post("/generate")
def generate():
    image_path = None

    try:
        if not KEY:
            return jsonify({"error": "MAGIC_HOUR_API_KEY missing"}), 500

        if "image" not in request.files:
            return jsonify({"error": "No image"}), 400

        image = request.files["image"]

        if not image.filename:
            return jsonify({"error": "Invalid image"}), 400

        room = request.form.get("room", "Living Room").strip()
        style = request.form.get("style", "Modern").strip()
        user_prompt = request.form.get("prompt", "").strip()

        ext = image.filename.rsplit(".", 1)[-1].lower() if "." in image.filename else "jpg"

        if ext not in ("jpg", "jpeg", "png", "webp"):
            ext = "jpg"

        with tempfile.NamedTemporaryFile(suffix="." + ext, delete=False) as f:
            image.save(f)
            image_path = f.name

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

        prompt = (
            f"Transform this {room} into a beautiful {style} interior. "
            "Add appropriate furniture, decoration and lighting. "
            "Preserve the original walls, windows, doors, floor, "
            "room geometry, perspective and camera angle. "
            "Do not change the architecture. "
            "Make it photorealistic and professionally designed."
        )

        if user_prompt:
            prompt += " Additional instructions: " + user_prompt

        payload = {
            "name": "RoomAI",
            "image_count": 1,
            "model": "qwen-edit",
            "aspect_ratio": "auto",
            "resolution": "640px",
            "style": {"prompt": prompt},
            "assets": {"image_file_paths": [item["file_path"]]}
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
        status_url = API + "/v1/image-projects/" + project_id

        for _ in range(120):
            response = api_request(
                status_url,
                "GET",
                headers={"Authorization": "Bearer " + KEY}
            )

            result = json.loads(response)
            status = result.get("status")
            print("STATUS:", status)

            if status == "complete":
                downloads = result.get("downloads", [])

                if not downloads:
                    return jsonify({"error": "No result image"}), 500

                return jsonify({
                    "status": "complete",
                    "image_url": downloads[0]["url"]
                })

            if status in ("error", "canceled"):
                return jsonify({
                    "error": "Generation failed",
                    "details": result
                }), 500

            time.sleep(5)

        return jsonify({"error": "Generation timeout"}), 504

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        if image_path:
            try:
                os.remove(image_path)
            except Exception:
                pass

if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=int(os.environ.get("PORT", "5000"))
    )
