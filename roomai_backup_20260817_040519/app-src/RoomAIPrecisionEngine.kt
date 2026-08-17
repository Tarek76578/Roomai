package com.roomai.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val GENERATE_URL =
    "https://roomai-wagl.onrender.com/generate"

private const val VERIFY_URL =
    "https://roomai-wagl.onrender.com/verify"

enum class PrecisionEditType {
    REPLACE,
    RECOLOR,
    RESTYLE,
    REMOVE,
    ADD,
    MOVE,
    FIX_PROBLEM
}

enum class VerificationStatus {
    NOT_VERIFIED,
    PASS,
    FAIL,
    RETRYING
}

data class PrecisionTarget(
    val name: String,
    val description: String = "",
    val selection: String = name
)

data class ProtectedElement(
    val name: String,
    val reason: String = "User requested preservation"
)

data class PrecisionRequest(
    val target: PrecisionTarget,
    val editType: PrecisionEditType,
    val instruction: String,
    val room: String = "Living Room",
    val style: String = "Modern",
    val protectedElements: List<ProtectedElement> = emptyList(),
    val sourceVersionId: String = "original"
)

data class PrecisionVerification(
    val status: VerificationStatus,
    val score: Int = 0,
    val targetChanged: Boolean = false,
    val protectedElementsChanged: Boolean = false,
    val architectureChanged: Boolean = false,
    val cameraChanged: Boolean = false,
    val perspectiveChanged: Boolean = false,
    val unrelatedObjectsChanged: Boolean = false,
    val message: String = ""
)

data class PrecisionVersion(
    val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    val imageUrl: String,
    val request: PrecisionRequest,
    val verification: PrecisionVerification
)

data class PrecisionResult(
    val version: PrecisionVersion,
    val attempts: Int,
    val accepted: Boolean
)

object RoomAIPrecisionEngine {

    private const val MAX_ATTEMPTS = 2
    private const val MIN_VERIFICATION_SCORE = 70

    suspend fun execute(
        context: Context,
        uri: Uri,
        request: PrecisionRequest,
        verify: Boolean = true
    ): PrecisionResult = withContext(Dispatchers.IO) {

        var attempt = 0
        var lastUrl = ""

        var lastVerification = PrecisionVerification(
            status = VerificationStatus.NOT_VERIFIED
        )

        while (attempt < MAX_ATTEMPTS) {

            attempt++

            val prompt = buildPrecisionPrompt(
                request = request,
                attempt = attempt,
                previousFailure = lastVerification
            )

            val operation = when (request.editType) {
                PrecisionEditType.FIX_PROBLEM -> "fix"
                PrecisionEditType.REPLACE -> "replace"
                PrecisionEditType.RECOLOR -> "recolor"
                PrecisionEditType.RESTYLE -> "restyle"
                PrecisionEditType.REMOVE -> "remove"
                PrecisionEditType.ADD -> "add"
                PrecisionEditType.MOVE -> "move"
            }

            lastUrl = generateDesign(
                context = context,
                uri = uri,
                room = request.room,
                style = request.style,
                userPrompt = prompt,
                operation = operation,
                selection = request.target.selection
            )

            if (!verify) {
                lastVerification = PrecisionVerification(
                    status = VerificationStatus.NOT_VERIFIED,
                    message = "Generation completed without verification."
                )
                break
            }

            lastVerification = verifyGeneratedImage(
                context = context,
                uri = uri,
                generatedUrl = lastUrl,
                request = request
            )

            if (lastVerification.status == VerificationStatus.PASS) {
                break
            }

            if (attempt < MAX_ATTEMPTS) {
                lastVerification = lastVerification.copy(
                    status = VerificationStatus.RETRYING
                )
            }
        }

        val accepted =
            !verify ||
            lastVerification.status == VerificationStatus.PASS

        val version = PrecisionVersion(
            parentId = request.sourceVersionId.takeIf {
                it != "original"
            },
            imageUrl = lastUrl,
            request = request,
            verification = lastVerification
        )

        PrecisionResult(
            version = version,
            attempts = attempt,
            accepted = accepted
        )
    }

    private fun buildPrecisionPrompt(
        request: PrecisionRequest,
        attempt: Int,
        previousFailure: PrecisionVerification
    ): String {

        val protected =
            if (request.protectedElements.isEmpty()) {
                "Preserve every unrelated element."
            } else {
                request.protectedElements.joinToString("\n") {
                    "- ${it.name}: ${it.reason}"
                }
            }

        val retryInstruction =
            if (attempt <= 1) {
                ""
            } else {
                """
                PREVIOUS ATTEMPT FAILED VERIFICATION.

                Failure:
                ${previousFailure.message}

                Tighten the edit.
                Reduce unintended changes.
                Do not change unrelated objects.
                """.trimIndent()
            }

        return """
            ROOMAI PRECISION EDIT ENGINE

            TARGET:
            ${request.target.name}

            TARGET DESCRIPTION:
            ${request.target.description}

            EDIT TYPE:
            ${request.editType.name}

            USER INSTRUCTION:
            ${request.instruction}

            PROTECTED ELEMENTS:
            $protected

            STRICT PRESERVATION:
            - Preserve room architecture.
            - Preserve walls.
            - Preserve doors.
            - Preserve windows.
            - Preserve floor.
            - Preserve ceiling.
            - Preserve camera angle.
            - Preserve perspective.
            - Preserve room geometry.
            - Preserve lighting unless the target is lighting.
            - Preserve unrelated furniture.
            - Preserve unrelated decorations.
            - Do not redesign the whole room.
            - Do not move the camera.
            - Do not create a different room.

            PRECISION RULE:
            Change the smallest possible region required
            to satisfy the user's instruction.

            OUTPUT:
            Photorealistic.
            Same room.
            Same composition.
            Same perspective.

            $retryInstruction
        """.trimIndent()
    }

    private suspend fun generateDesign(
        context: Context,
        uri: Uri,
        room: String,
        style: String,
        userPrompt: String,
        operation: String,
        selection: String
    ): String = withContext(Dispatchers.IO) {

        val boundary = "RoomAI-Generate-${UUID.randomUUID()}"

        val connection =
            URL(GENERATE_URL).openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 180000

        connection.setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=$boundary"
        )

        DataOutputStream(connection.outputStream).use { output ->

            val bytes =
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw Exception("Could not read original image")

            writeFilePart(
                output = output,
                boundary = boundary,
                fieldName = "image",
                fileName = "room.jpg",
                contentType = "image/jpeg",
                bytes = bytes
            )

            writeTextPart(output, boundary, "operation", operation)
            writeTextPart(output, boundary, "room", room)
            writeTextPart(output, boundary, "style", style)
            writeTextPart(output, boundary, "selection", selection)
            writeTextPart(output, boundary, "prompt", userPrompt)

            output.write("--$boundary--\r\n".toByteArray())
        }

        val code = connection.responseCode

        val responseText =
            if (code in 200..299) {
                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } else {
                val error =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "HTTP $code"

                throw Exception(
                    "Generation failed: HTTP $code: $error"
                )
            }

        connection.disconnect()

        val json = JSONObject(responseText)

        val status = json.optString("status")

        if (status != "complete") {
            throw Exception(
                "Generation did not complete: $responseText"
            )
        }

        val imageUrl = json.optString("image_url")

        if (imageUrl.isBlank()) {
            throw Exception(
                "Generation returned no image_url"
            )
        }

        imageUrl
    }

    private suspend fun verifyGeneratedImage(
        context: Context,
        uri: Uri,
        generatedUrl: String,
        request: PrecisionRequest
    ): PrecisionVerification = withContext(Dispatchers.IO) {

        val boundary =
            "RoomAI-Verify-${UUID.randomUUID()}"

        val connection =
            URL(VERIFY_URL).openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 180000

        connection.setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=$boundary"
        )

        DataOutputStream(connection.outputStream).use { output ->

            val bytes =
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw Exception("Could not read original image")

            writeFilePart(
                output = output,
                boundary = boundary,
                fieldName = "original",
                fileName = "original.jpg",
                contentType = "image/jpeg",
                bytes = bytes
            )

            writeTextPart(
                output,
                boundary,
                "generated_url",
                generatedUrl
            )

            writeTextPart(
                output,
                boundary,
                "target",
                request.target.name
            )

            writeTextPart(
                output,
                boundary,
                "instruction",
                request.instruction
            )

            val protectedJson =
                request.protectedElements.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) {
                    JSONObject()
                        .put("name", it.name)
                        .put("reason", it.reason)
                        .toString()
                }

            writeTextPart(
                output,
                boundary,
                "protected_elements",
                protectedJson
            )

            output.write("--$boundary--\r\n".toByteArray())
        }

        val code = connection.responseCode

        val responseText =
            if (code in 200..299) {
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }
            } else {
                val error =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "HTTP $code"

                throw Exception(
                    "Verification failed: HTTP $code: $error"
                )
            }

        connection.disconnect()

        parseVerificationResponse(responseText)
    }

    private fun parseVerificationResponse(
        responseText: String
    ): PrecisionVerification {

        val root = JSONObject(responseText)

        val verification =
            root.optJSONObject("verification")
                ?: throw Exception(
                    "Backend returned no verification object"
                )

        val score = verification.optInt("score", 0)

        val targetChanged =
            verification.optBoolean("target_changed", false)

        val protectedChanged =
            verification.optBoolean(
                "protected_elements_changed",
                false
            )

        val architectureChanged =
            verification.optBoolean(
                "architecture_changed",
                false
            )

        val cameraChanged =
            verification.optBoolean(
                "camera_changed",
                false
            )

        val perspectiveChanged =
            verification.optBoolean(
                "perspective_changed",
                false
            )

        val unrelatedChanged =
            verification.optBoolean(
                "unrelated_objects_changed",
                false
            )

        val modelApproved =
            verification
                .optString("status", "FAIL")
                .uppercase() == "PASS"

        val acceptedBySafetyGate =
            modelApproved &&
                score >= MIN_VERIFICATION_SCORE &&
                targetChanged &&
                !protectedChanged &&
                !architectureChanged &&
                !cameraChanged &&
                !perspectiveChanged &&
                !unrelatedChanged

        val status =
            if (acceptedBySafetyGate) {
                VerificationStatus.PASS
            } else {
                VerificationStatus.FAIL
            }

        return PrecisionVerification(
            status = status,
            score = score,
            targetChanged = targetChanged,
            protectedElementsChanged = protectedChanged,
            architectureChanged = architectureChanged,
            cameraChanged = cameraChanged,
            perspectiveChanged = perspectiveChanged,
            unrelatedObjectsChanged = unrelatedChanged,
            message =
                verification.optString(
                    "message",
                    ""
                )
        )
    }

    private fun writeTextPart(
        output: DataOutputStream,
        boundary: String,
        name: String,
        value: String
    ) {
        output.write(
            "--$boundary\r\n".toByteArray()
        )

        output.write(
            (
                "Content-Disposition: form-data; " +
                    "name=\"$name\"\r\n\r\n"
            ).toByteArray()
        )

        output.write(value.toByteArray())
        output.write("\r\n".toByteArray())
    }

    private fun writeFilePart(
        output: DataOutputStream,
        boundary: String,
        fieldName: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray
    ) {
        output.write(
            "--$boundary\r\n".toByteArray()
        )

        output.write(
            (
                "Content-Disposition: form-data; " +
                    "name=\"$fieldName\"; " +
                    "filename=\"$fileName\"\r\n"
            ).toByteArray()
        )

        output.write(
            "Content-Type: $contentType\r\n\r\n"
                .toByteArray()
        )

        output.write(bytes)
        output.write("\r\n".toByteArray())
    }
}
