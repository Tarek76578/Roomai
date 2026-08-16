package com.roomai.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

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
        var lastVerification =
            PrecisionVerification(
                status = VerificationStatus.NOT_VERIFIED
            )

        while (attempt < MAX_ATTEMPTS) {

            attempt++

            val prompt =
                buildPrecisionPrompt(
                    request = request,
                    attempt = attempt,
                    previousFailure = lastVerification
                )

            val operation =
                when (request.editType) {
                    PrecisionEditType.FIX_PROBLEM -> "fix"
                    PrecisionEditType.REPLACE -> "replace"
                    PrecisionEditType.RECOLOR -> "recolor"
                    PrecisionEditType.RESTYLE -> "restyle"
                    PrecisionEditType.REMOVE -> "remove"
                    PrecisionEditType.ADD -> "add"
                    PrecisionEditType.MOVE -> "move"
                }

            lastUrl =
                generateDesign(
                    context = context,
                    uri = uri,
                    room = request.room,
                    style = request.style,
                    userPrompt = prompt,
                    operation = operation,
                    selection = request.target.selection
                )

            if (!verify) {

                lastVerification =
                    PrecisionVerification(
                        status = VerificationStatus.NOT_VERIFIED,
                        message =
                            "Image generated. Verification was disabled."
                    )

                break
            }

            /*
             * The backend verification contract is intentionally isolated.
             *
             * Until /verify exists on the backend, we do NOT pretend
             * that Kotlin can reliably inspect pixel-level changes.
             */
            lastVerification =
                requestVerification(
                    imageUrl = lastUrl,
                    request = request
                )

            if (lastVerification.status ==
                VerificationStatus.PASS
            ) {
                break
            }
        }

        val accepted =
            !verify ||
                lastVerification.status ==
                VerificationStatus.PASS

        val version =
            PrecisionVersion(
                parentId =
                    request.sourceVersionId
                        .takeIf { it != "original" },
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
                request.protectedElements.joinToString(
                    separator = "\n"
                ) {
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
            ROOMAI PRECISION EDIT

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

    private fun requestVerification(
        imageUrl: String,
        request: PrecisionRequest
    ): PrecisionVerification {

        /*
         * No false PASS.
         *
         * Until the backend exposes a real /verify endpoint,
         * the result remains NOT_VERIFIED.
         */
        return PrecisionVerification(
            status = VerificationStatus.NOT_VERIFIED,
            message =
                "Generation completed, but visual verification requires the backend /verify endpoint."
        )
    }
}
