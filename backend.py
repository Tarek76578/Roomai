import os
import tempfile
import traceback

from flask import Flask, request, jsonify
from magic_hour import Client

app = Flask(__name__)

KEY = os.environ.get("MAGIC_HOUR_API_KEY")


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
            return jsonify({
                "error": "AI service is not configured"
            }), 500

        if "image" not in request.files:
            return jsonify({
                "error": "No room image was provided"
            }), 400

        image = request.files["image"]

        if not image.filename:
            return jsonify({
                "error": "Image filename is missing"
            }), 400

        room = request.form.get("room", "Living Room").strip()
        style = request.form.get("style", "Modern").strip()
        user_prompt = request.form.get("prompt", "").strip()

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

        client = Client(token=KEY)

        file_path = client.v1.files.upload_file(image_path)

        base_prompt = (
            f"Transform this {room} into a beautiful {style} interior. "
            "Add appropriate furniture, decoration, lighting and interior "
            "design elements suitable for the room. "
            "Preserve the original walls, windows, doors, floor, room "
            "geometry, perspective and camera angle. "
            "Do not change the architecture. "
            "Keep the result photorealistic, coherent and professionally designed."
        )

        if user_prompt:
            base_prompt += (
                " Additional user instructions: "
                + user_prompt
            )

        result = client.v1.ai_image_editor.generate(
            assets={
                "image_file_paths": [file_path]
            },
            style={
                "prompt": base_prompt
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
                "error": "AI generation failed",
                "status": result.status,
                "details": getattr(
                    result,
                    "error_message",
                    None
                )
            }), 500

        downloads = getattr(result, "downloads", None)

        if not downloads:
            return jsonify({
                "error": "Generation completed but no image URL was returned"
            }), 500

        return jsonify({
            "status": "complete",
            "image_url": downloads[0].url
        })

    except Exception as e:
        traceback.print_exc()

        return jsonify({
            "error": "RoomAI could not complete the generation",
            "details": str(e)
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
