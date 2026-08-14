import os
import tempfile
from flask import Flask, request, jsonify
from magic_hour import Client

app = Flask(__name__)

KEY = os.environ.get("MAGIC_HOUR_API_KEY")


@app.get("/")
def home():
    return jsonify({
        "status": "RoomAI Backend OK",
        "magic_hour_key": bool(KEY),
        "key_length": len(KEY or ""),
        "key_last4": (KEY or "")[-4:]
    })


@app.post("/generate")
def generate():
    try:
        if not KEY:
            return jsonify({
                "error": "MAGIC_HOUR_API_KEY is not configured"
            }), 500

        if "image" not in request.files:
            return jsonify({"error": "No image provided"}), 400

        image = request.files["image"]

        if not image.filename:
            return jsonify({"error": "Image filename is missing"}), 400

        room = request.form.get("room", "Living Room")
        style = request.form.get("style", "Modern")

        extension = (
            image.filename.rsplit(".", 1)[-1].lower()
            if "." in image.filename
            else "jpg"
        )

        if extension not in ("jpg", "jpeg", "png", "webp"):
            extension = "jpg"

        with tempfile.NamedTemporaryFile(
            suffix="." + extension,
            delete=False
        ) as f:
            image.save(f)
            image_path = f.name

        try:
            client = Client(token=KEY)

            file_path = client.v1.files.upload_file(image_path)

            prompt = (
                f"Transform this room into a beautiful {style} {room}. "
                "Add appropriate furniture, decoration, lighting and "
                "interior design elements suitable for this room. "
                "Preserve the original walls, windows, doors, floor, "
                "room geometry, perspective and camera angle. "
                "Do not change the architecture. "
                "Make the result photorealistic, elegant and professionally designed."
            )

            result = client.v1.ai_image_editor.generate(
                assets={
                    "image_file_paths": [file_path]
                },
                style={
                    "prompt": prompt
                },
                name="RoomAI",
                image_count=1,
                model="qwen-edit",
                aspect_ratio="auto",
                resolution="640px",
                wait_for_completion=True,
                download_outputs=False
            )

            if result.status != "complete":
                return jsonify({
                    "error": "Magic Hour generation failed",
                    "status": result.status,
                    "details": getattr(result, "error_message", None)
                }), 500

            downloads = getattr(result, "downloads", None)

            if downloads:
                return jsonify({
                    "status": "complete",
                    "image_url": downloads[0].url
                })

            return jsonify({
                "error": "Generation completed but no download URL was returned"
            }), 500

        finally:
            try:
                os.remove(image_path)
            except Exception:
                pass

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            "error": "Magic Hour request failed",
            "details": str(e),
            "exception_type": type(e).__name__
        }), 500
