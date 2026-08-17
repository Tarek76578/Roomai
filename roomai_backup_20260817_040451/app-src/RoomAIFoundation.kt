package com.roomai.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RoomObject(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val label: String = type,
    val locked: Boolean = false,
    val notes: String = ""
)

data class RoomStructure(
    val wallsLocked: Boolean = true,
    val doorsLocked: Boolean = true,
    val windowsLocked: Boolean = true,
    val floorLocked: Boolean = true,
    val ceilingLocked: Boolean = true,
    val cameraLocked: Boolean = true,
    val perspectiveLocked: Boolean = true,
    val lightingLocked: Boolean = true
)

data class RoomConstraint(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val active: Boolean = true
)

data class DesignVersion(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val operation: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class RoomState(
    val projectId: String = UUID.randomUUID().toString(),
    val name: String = "My Room",
    val roomType: String = "Room",
    val style: String = "Modern",
    val budget: Int = 0,
    val objects: List<RoomObject> = emptyList(),
    val structure: RoomStructure = RoomStructure(),
    val constraints: List<RoomConstraint> = emptyList(),
    val versions: List<DesignVersion> = emptyList()
)

object RoomAIProjectStore {

    private const val PREFS = "roomai_projects_v2"
    private const val KEY_STATE = "active_room_state"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, state: RoomState) {
        val objects = JSONArray()

        state.objects.forEach {
            objects.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("type", it.type)
                    put("label", it.label)
                    put("locked", it.locked)
                    put("notes", it.notes)
                }
            )
        }

        val constraints = JSONArray()

        state.constraints.forEach {
            constraints.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("text", it.text)
                    put("active", it.active)
                }
            )
        }

        val versions = JSONArray()

        state.versions.forEach {
            versions.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("url", it.url)
                    put("title", it.title)
                    put("operation", it.operation)
                    put("createdAt", it.createdAt)
                }
            )
        }

        val json = JSONObject().apply {
            put("projectId", state.projectId)
            put("name", state.name)
            put("roomType", state.roomType)
            put("style", state.style)
            put("budget", state.budget)

            put(
                "structure",
                JSONObject().apply {
                    put("wallsLocked", state.structure.wallsLocked)
                    put("doorsLocked", state.structure.doorsLocked)
                    put("windowsLocked", state.structure.windowsLocked)
                    put("floorLocked", state.structure.floorLocked)
                    put("ceilingLocked", state.structure.ceilingLocked)
                    put("cameraLocked", state.structure.cameraLocked)
                    put("perspectiveLocked", state.structure.perspectiveLocked)
                    put("lightingLocked", state.structure.lightingLocked)
                }
            )

            put("objects", objects)
            put("constraints", constraints)
            put("versions", versions)
        }

        prefs(context)
            .edit()
            .putString(KEY_STATE, json.toString())
            .apply()
    }

    fun load(context: Context): RoomState {
        val raw = prefs(context).getString(KEY_STATE, null)
            ?: return RoomState()

        return try {
            val json = JSONObject(raw)

            val structureJson =
                json.optJSONObject("structure") ?: JSONObject()

            val objects = mutableListOf<RoomObject>()
            val objectArray =
                json.optJSONArray("objects") ?: JSONArray()

            for (i in 0 until objectArray.length()) {
                val item = objectArray.getJSONObject(i)

                objects += RoomObject(
                    id = item.optString("id"),
                    type = item.optString("type"),
                    label = item.optString("label"),
                    locked = item.optBoolean("locked"),
                    notes = item.optString("notes")
                )
            }

            val constraints = mutableListOf<RoomConstraint>()
            val constraintArray =
                json.optJSONArray("constraints") ?: JSONArray()

            for (i in 0 until constraintArray.length()) {
                val item = constraintArray.getJSONObject(i)

                constraints += RoomConstraint(
                    id = item.optString("id"),
                    text = item.optString("text"),
                    active = item.optBoolean("active")
                )
            }

            val versions = mutableListOf<DesignVersion>()
            val versionArray =
                json.optJSONArray("versions") ?: JSONArray()

            for (i in 0 until versionArray.length()) {
                val item = versionArray.getJSONObject(i)

                versions += DesignVersion(
                    id = item.optString("id"),
                    url = item.optString("url"),
                    title = item.optString("title"),
                    operation = item.optString("operation"),
                    createdAt = item.optLong("createdAt")
                )
            }

            RoomState(
                projectId = json.optString(
                    "projectId",
                    UUID.randomUUID().toString()
                ),
                name = json.optString("name", "My Room"),
                roomType = json.optString("roomType", "Room"),
                style = json.optString("style", "Modern"),
                budget = json.optInt("budget", 0),
                objects = objects,
                structure = RoomStructure(
                    wallsLocked =
                        structureJson.optBoolean("wallsLocked", true),
                    doorsLocked =
                        structureJson.optBoolean("doorsLocked", true),
                    windowsLocked =
                        structureJson.optBoolean("windowsLocked", true),
                    floorLocked =
                        structureJson.optBoolean("floorLocked", true),
                    ceilingLocked =
                        structureJson.optBoolean("ceilingLocked", true),
                    cameraLocked =
                        structureJson.optBoolean("cameraLocked", true),
                    perspectiveLocked =
                        structureJson.optBoolean("perspectiveLocked", true),
                    lightingLocked =
                        structureJson.optBoolean("lightingLocked", true)
                ),
                constraints = constraints,
                versions = versions
            )
        } catch (_: Exception) {
            RoomState()
        }
    }

    fun addVersion(
        context: Context,
        state: RoomState,
        url: String,
        title: String,
        operation: String
    ): RoomState {

        val updated = state.copy(
            versions = state.versions +
                DesignVersion(
                    url = url,
                    title = title,
                    operation = operation
                )
        )

        save(context, updated)
        return updated
    }
}

object RoomAIPromptBuilder {

    fun precisionPrompt(
        selectedObject: String,
        userInstruction: String,
        structure: RoomStructure,
        constraints: List<RoomConstraint>
    ): String {

        val protected = buildList {

            if (structure.wallsLocked) add("walls")
            if (structure.doorsLocked) add("doors")
            if (structure.windowsLocked) add("windows")
            if (structure.floorLocked) add("floor")
            if (structure.ceilingLocked) add("ceiling")
            if (structure.cameraLocked) add("camera position")
            if (structure.perspectiveLocked) add("perspective")
            if (structure.lightingLocked) add("lighting direction")
        }

        val activeConstraints =
            constraints
                .filter { it.active }
                .joinToString("\n") {
                    "- ${it.text}"
                }

        return """
            ROOMAI PRECISION EDIT ENGINE

            TARGET OBJECT:
            $selectedObject

            USER REQUEST:
            ${userInstruction.ifBlank {
                "Improve the selected object without changing its identity."
            }}

            PROTECTED STRUCTURE:
            ${protected.joinToString("\n") { "- $it" }}

            ADDITIONAL CONSTRAINTS:
            ${activeConstraints.ifBlank {
                "- Preserve all unrelated elements."
            }}

            HARD RULES:
            1. Modify ONLY the selected object.
            2. Do not redesign the whole room.
            3. Do not move unrelated furniture.
            4. Do not change walls.
            5. Do not change doors.
            6. Do not change windows.
            7. Do not change the floor.
            8. Do not change the ceiling.
            9. Preserve camera position.
            10. Preserve perspective.
            11. Preserve room proportions.
            12. Preserve unrelated lighting.
            13. Preserve all objects not selected.
            14. Keep the result photorealistic.
            15. Do not invent architectural modifications.

            OUTPUT:
            A realistic edited version of the same room.
        """.trimIndent()
    }
}
