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

enum class PrecisionEditType {
    REPLACE, RECOLOR, RESTYLE, REMOVE, ADD, MOVE, FIX_PROBLEM
}

enum class VerificationStatus {
    NOT_VERIFIED, PASS, FAIL, RETRYING
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

    private suspend fun verifyGeneratedImage(
        context: Context,
        uri: Uri,
        generatedUrl: String,
        request: PrecisionRequest
    ): PrecisionVerification = withContext(Dispatchers.IO) {

        val boundary = "RoomAI-Verify-${UUID.randomUUID()}"

        val connection =
            URL(DIAGNOSE_URL.replace("/diagnose", "/verify"))
                .openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 120000

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

            output.write(
                "--$boundary\r\n".toByteArray()
            )

            output.write(
                (
                    "Content-Disposition: form-data; " +
                    "name=\"original\"; " +
                    "filename=\"original.jpg\"\r\n"
                ).toByteArray()
            )

            output.write(
                "Content-Type: image/jpeg\r\n\r\n".toByteArray()
            )

            output.write(bytes)
            output.write("\r\n".toByteArray())

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

            output.write(
                "--$boundary--\r\n".toByteArray()
            )
        }

        val code = connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response =
            stream?.bufferedReader()?.use {
                it.readText()
            } ?: throw Exception("Empty verification response")

        if (code !in 200..299) {
            throw Exception(
                "Verification HTTP $code: $response"
            )
        }

        val root = JSONObject(response)

        val verification =
            root.optJSONObject("verification")
                ?: throw Exception(
                    "Backend returned no verification"
                )

        val status =
            when (
                verification
                    .optString("status")
                    .uppercase()
            ) {
                "PASS" -> VerificationStatus.PASS
                "FAIL" -> VerificationStatus.FAIL
                else -> VerificationStatus.FAIL
            }

        PrecisionVerification(
            status = status,
            score = verification.optInt("score", 0),
            targetChanged =
                verification.optBoolean(
                    "target_changed",
                    false
                ),
            protectedElementsChanged =
                verification.optBoolean(
                    "protected_elements_changed",
                    false
                ),
            architectureChanged =
                verification.optBoolean(
                    "architecture_changed",
                    false
                ),
            cameraChanged =
                verification.optBoolean(
                    "camera_changed",
                    false
                ),
            perspectiveChanged =
                verification.optBoolean(
                    "perspective_changed",
                    false
                ),
            unrelatedObjectsChanged =
                verification.optBoolean(
                    "unrelated_objects_changed",
                    false
                ),
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

        output.write(
            value.toByteArray()
        )

        output.write(
            "\r\n".toByteArray()
        )
    }
}
