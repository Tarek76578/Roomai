import os
import time
import json
import urllib.request
from flask import Flask, request, jsonify

app = Flask(__name__)

API = "https://api.magichour.ai"
KEY = os.environ.get("MAGIC_HOUR_API_KEY")

@app.get("/")
def home():
    return jsonify({"status": "RoomAI Backend OK"})

@app.post("/generate")
def generate():
    if not KEY:
        return jsonify({"error": "MAGIC_HOUR_API_KEY is not configured"}), 500

    if "image" not in request.files:
        return jsonify({"error": "No image provided"}), 400

    image = request.files["image"]
    image_data = image.read()

    extension = image.filename.rsplit(".", 1)[-1].lower() if "." in image.filename else "jpg"

    def api_request(url, method="GET", data=None, headers=None):
        req = urllib.request.Request(
            url,
            data=data,
            headers=headers or {},
            method=method
        )
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.read()

    # 1. Get upload URL
    payload = {
        "items": [{
            "extension": extension,
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
    upload_url = item["upload_url"]
    file_path = item["file_path"]

    # 2. Upload image
    api_request(
        upload_url,
        "PUT",
        image_data,
        {"Content-Type": "image/jpeg"}
    )

    # 3. Create design
    prompt = (
        "Transform this room into a beautiful modern minimalist living room. "
        "Add a comfortable sofa, coffee table, TV cabinet, elegant rug, "
        "indoor plants and warm lighting. Preserve the original walls, "
        "windows, doors, floor, room geometry, perspective and camera angle. "
        "Make it photorealistic and professionally designed."
    )

    payload = {
        "name": "RoomAI",
        "image_count": 1,
        "model": "qwen-edit",
        "aspect_ratio": "auto",
        "resolution": "640px",
        "style": {"prompt": prompt},
        "assets": {"image_file_paths": [file_path]}
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

    # 4. Wait for result
    status_url = API + "/v1/image-projects/" + project_id

    for _ in range(60):
        response = api_request(
            status_url,
            "GET",
            headers={"Authorization": "Bearer " + KEY}
        )

        data = json.loads(response)
        status = data.get("status")

        if status == "complete":
            downloads = data.get("downloads", [])

            if not downloads:
                return jsonify({"error": "No download URL"}), 500

            return jsonify({
                "status": "complete",
                "image_url": downloads[0]["url"]
            })

        if status in ("error", "canceled"):
            return jsonify({
                "error": "Magic Hour failed",
                "details": data
            }), 500

        time.sleep(5)

    return jsonify({"error": "Generation timeout"}), 504


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 10000))
    app.run(host="0.0.0.0", port=port)
