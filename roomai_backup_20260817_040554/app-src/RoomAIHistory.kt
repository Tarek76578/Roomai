package com.roomai.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RoomAIVersion(
    val id: String,
    val parentId: String?,
    val originalUrl: String?,
    val generatedUrl: String,
    val room: String,
    val style: String,
    val prompt: String,
    val createdAt: Long
)

object RoomAIHistory {
    private const val PREFS = "roomai_versions"
    private const val KEY = "versions"
    private const val MAX_VERSIONS = 50

    fun add(
        context: Context,
        generatedUrl: String,
        room: String,
        style: String,
        prompt: String,
        originalUrl: String? = null,
        parentId: String? = null
    ): RoomAIVersion {
        val version = RoomAIVersion(
            id = UUID.randomUUID().toString(),
            parentId = parentId,
            originalUrl = originalUrl,
            generatedUrl = generatedUrl,
            room = room,
            style = style,
            prompt = prompt,
            createdAt = System.currentTimeMillis()
        )

        val current = load(context).toMutableList()
        current.add(0, version)

        val trimmed = current.take(MAX_VERSIONS)

        val array = JSONArray()
        trimmed.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("parentId", item.parentId)
                    .put("originalUrl", item.originalUrl)
                    .put("generatedUrl", item.generatedUrl)
                    .put("room", item.room)
                    .put("style", item.style)
                    .put("prompt", item.prompt)
                    .put("createdAt", item.createdAt)
            )
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()

        return version
    }

    fun load(context: Context): List<RoomAIVersion> {
        val raw = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        RoomAIVersion(
                            id = o.optString("id"),
                            parentId = o.optString("parentId")
                                .takeIf { it.isNotBlank() && it != "null" },
                            originalUrl = o.optString("originalUrl")
                                .takeIf { it.isNotBlank() && it != "null" },
                            generatedUrl = o.optString("generatedUrl"),
                            room = o.optString("room", "Living Room"),
                            style = o.optString("style", "Modern"),
                            prompt = o.optString("prompt"),
                            createdAt = o.optLong("createdAt")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun delete(context: Context, id: String) {
        val remaining = load(context).filterNot { it.id == id }

        val array = JSONArray()
        remaining.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("parentId", item.parentId)
                    .put("originalUrl", item.originalUrl)
                    .put("generatedUrl", item.generatedUrl)
                    .put("room", item.room)
                    .put("style", item.style)
                    .put("prompt", item.prompt)
                    .put("createdAt", item.createdAt)
            )
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
