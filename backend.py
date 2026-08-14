import os
import time
import json
import urllib.request
import urllib.error
from flask import Flask, request, jsonify

app = Flask(__name__)

API = "https://api.magichour.ai"
KEY = os.environ.get("MAGIC_HOUR_API_KEY")


@app.get("/")
def home():
    return jsonify({
        "status": "RoomAI Backend OK",
        "magic_hour_key": bool(os.environ.get("MAGIC_HOUR_API_KEY")),
        "env_keys": [
            k for k in os.environ.keys()
            if "MAGIC" in k.upper() or "HOUR" in k.upper()
        ]
    })


def api_request(url, method="GET", data=None, headers=None):
    req = urllib.request.Request(
        url,
        data=data,
        headers=headers or {},
        method=method
    )

    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.read()

    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Magic Hour HTTP {e.code}: {body}"
        )

    except Exception as e:
        raise RuntimeError(str(e))


@app.post("/generate")
def generate():

    try:
        if not KEY:
            return jsonify({
                "error": "MAGIC_HOUR_API_KEY is not configured"
            }), 500

        if "image" not in request.files:
            return jsonify({
                "error": "No image provided"
            }), 400

        image = request.files["image"]

        if not image.filename:
            return jsonify({
                "error": "Image filename is missing"
            }), 400

        image_data = image.read()

        if not image_data:
            return jsonify({
                "error": "Image is empty"
            }), 400

        room = request.form.get(
            "room",
            "Living Room"
        )

        style = request.form.get(
            "style",
            "Modern"
        )

        extension = (
            image.filename.rsplit(".", 1)[-1].lower()
            if "." in image.filename
            else "jpg"
        )

        if extension not in ("jpg", "jpeg", "png", "webp"):
            extension = "jpg"

        # 1. Get upload URL
        payload = {
            "items": [{
                "extension": extension,
                "type": "image"
            }]
        }

        print("ROOMAI STEP 1: requesting upload URL")
        response = api_request(
            API + "/v1/files/upload-urls",
            "POST",
            json.dumps(payload).encode(),
            {
                "Authorization": "Bearer " + KEY,
                "Content-Type": "application/json"
            }
        )

        upload_data = json.loads(response)

        if "items" not in upload_data or not upload_data["items"]:
            return jsonify({
                "error": "Magic Hour did not return upload URL",
                "details": upload_data
            }), 500

        item = upload_data["items"][0]

        upload_url = item["upload_url"]
        file_path = item["file_path"]

        # 2. Upload image
        content_type = {
            "jpg": "image/jpeg",
            "jpeg": "image/jpeg",
            "png": "image/png",
            "webp": "image/webp"
        }.get(extension, "image/jpeg")

        print("ROOMAI STEP 2: uploading image")
        api_request(
            upload_url,
            "PUT",
            image_data,
            {
                "Content-Type": content_type
            }
        )

        # 3. Create AI prompt
        prompt = (
            f"Transform this room into a beautiful {style} "
            f"{room}. "
            "Add appropriate furniture, decoration, lighting and "
            "interior design elements suitable for this room. "
            "Preserve the original walls, windows, doors, floor, "
            "room geometry, perspective and camera angle. "
            "Do not change the architecture. "
            "Make the result photorealistic, elegant and professionally designed."
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
                "image_file_paths": [file_path]
            }
        }

        print("ROOMAI STEP 3: creating AI design")
        response = api_request(
            API + "/v1/ai-image-editor",
            "POST",
            json.dumps(payload).encode(),
            {
                "Authorization": "Bearer " + KEY,
                "Content-Type": "application/json"
            }
        )

        project = json.loads(response)

        if "id" not in project:
            return jsonify({
                "error": "Magic Hour did not return project ID",
                "details": project
            }), 500

        project_id = project["id"]

        # 4. Wait for result
        status_url = (
            API +
            "/v1/image-projects/" +
            project_id
        )

        for _ in range(90):

            print("ROOMAI STEP 4: checking result")
            response = api_request(
                status_url,
                "GET",
                headers={
                    "Authorization": "Bearer " + KEY
                }
            )

            data = json.loads(response)

            status = data.get("status")

            if status == "complete":

                downloads = data.get(
                    "downloads",
                    []
                )

                if not downloads:
                    return jsonify({
                        "error": "Project completed but no image URL was returned",
                        "details": data
                    }), 500

                return jsonify({
                    "status": "complete",
                    "image_url": downloads[0]["url"],
                    "room": room,
                    "style": style
                })

            if status in ("error", "canceled"):

                return jsonify({
                    "error": "Magic Hour generation failed",
                    "details": data
                }), 500

            time.sleep(5)

        return jsonify({
            "error": "Generation timeout"
        }), 504

    except Exception as e:

        return jsonify({
            "error": str(e)
        }), 500


if __name__ == "__main__":
    port = int(
        os.environ.get("PORT", 10000)
    )

    app.run(
        host="0.0.0.0",
        port=port
    )
