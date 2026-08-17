package com.roomai.app

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.draw.clip

import android.util.Log
import com.roomai.app.ui.RoomAITheme
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject


private const val ROOMAI_TEST_INTERSTITIAL_AD =
    "ca-app-pub-3940256099942544/1033173712"

private object RoomAIAds {

    private var interstitialAd: InterstitialAd? = null
    private var loading = false

    fun initialize(context: Context) {
        MobileAds.initialize(context)
        load(context)
    }

    private fun load(context: Context) {
        if (loading || interstitialAd != null) return

        loading = true

        InterstitialAd.load(
            context,
            ROOMAI_TEST_INTERSTITIAL_AD,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    loading = false
                    interstitialAd = ad

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                load(context)
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: com.google.android.gms.ads.AdError
                            ) {
                                interstitialAd = null
                                load(context)
                            }
                        }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    interstitialAd = null
                }
            }
        )
    }

    fun showAfterGeneration(context: Context) {
        if (roomAiPlan(context) == "pro") return

        val activity = context as? android.app.Activity ?: return
        val ad = interstitialAd

        if (ad == null) {
            load(context)
            return
        }

        interstitialAd = null

        activity.runOnUiThread {
            ad.show(activity)
        }

        load(context)
    }
}

private const val BACKEND_URL =
    "https://roomai-wagl.onrender.com/generate"

private const val DIAGNOSE_URL =
    "https://roomai-wagl.onrender.com/diagnose"

private const val AUTH_BASE_URL =
    "https://roomai-wagl.onrender.com"

private const val ROOMAI_TOKEN_KEY = "auth_token"


private const val PREFS = "roomai_designs"

private const val ROOMAI_PLAN_PREFS = "roomai_account"
private const val ROOMAI_PLAN_KEY = "plan"
private const val ROOMAI_DEVICE_KEY = "device_id"

private const val FREE_MONTHLY_LIMIT = 5
private const val PRO_MONTHLY_LIMIT = 100

private fun roomAiToken(context: Context): String {
    return context.getSharedPreferences(
        ROOMAI_PLAN_PREFS,
        Context.MODE_PRIVATE
    ).getString(
        ROOMAI_TOKEN_KEY,
        ""
    ) ?: ""
}

private fun saveRoomAiToken(
    context: Context,
    token: String
) {
    context.getSharedPreferences(
        ROOMAI_PLAN_PREFS,
        Context.MODE_PRIVATE
    ).edit()
        .putString(ROOMAI_TOKEN_KEY, token)
        .apply()
}

private fun clearRoomAiToken(context: Context) {
    context.getSharedPreferences(
        ROOMAI_PLAN_PREFS,
        Context.MODE_PRIVATE
    ).edit()
        .remove(ROOMAI_TOKEN_KEY)
        .apply()
}

private suspend fun roomAiAuthRequest(
    endpoint: String,
    email: String,
    password: String,
    context: Context,
    includeDevice: Boolean
): JSONObject = withContext(Dispatchers.IO) {

    val connection =
        URL(AUTH_BASE_URL + endpoint)
            .openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.connectTimeout = 30000
    connection.readTimeout = 30000

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    val payload = JSONObject()
        .put("email", email.trim())
        .put("password", password)

    if (includeDevice) {
        payload.put(
            "device_id",
            roomAiDeviceId(context)
        )
    }

    connection.outputStream.use { output ->
        output.write(
            payload.toString().toByteArray()
        )
    }

    val code = connection.responseCode

    val stream =
        if (code in 200..299)
            connection.inputStream
        else
            connection.errorStream

    val response =
        stream?.bufferedReader()?.use { it.readText() }
            ?: """{"error":"Empty server response"}"""

    if (code !in 200..299) {
        val message = try {
            JSONObject(response)
                .optString("error", response)
        } catch (_: Exception) {
            response
        }

        throw Exception(message)
    }

    JSONObject(response)
}



private suspend fun roomAiLogout(context: Context) =
    withContext(Dispatchers.IO) {

        val token = roomAiToken(context)

        if (token.isBlank()) {
            return@withContext
        }

        try {
            val connection =
                URL(AUTH_BASE_URL + "/auth/logout")
                    .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )

            connection.responseCode

        } catch (_: Exception) {
            // Local logout must still succeed if the network is unavailable.
        } finally {
            clearRoomAiToken(context)
        }
    }

private suspend fun roomAiUsage(context: Context): RoomAIUsage =
    withContext(Dispatchers.IO) {

        val token = roomAiToken(context)

        val connection =
            URL(AUTH_BASE_URL + "/usage")
                .openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        // Send Authorization only when we actually have a token.
        if (!token.isNullOrBlank()) {
            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )
        } else {
            connection.setRequestProperty(
                "X-RoomAI-Device",
                roomAiDeviceId(context)
            )
        }

        val code = connection.responseCode

        val stream =
            if (code in 200..299)
                connection.inputStream
            else
                connection.errorStream

        val response =
            stream?.bufferedReader()?.use { it.readText() }
                ?: throw Exception("Empty usage response")

        if (code == 401) {
            clearRoomAiToken(context)
            throw Exception("SESSION_EXPIRED")
        }

        if (code !in 200..299) {
            throw Exception(response)
        }

        val json = JSONObject(response)

        RoomAIUsage(
            plan = json.optString("plan", "free"),
            used = json.optInt("used", 0),
            limit = json.optInt("limit", FREE_MONTHLY_LIMIT),
            remaining = json.optInt(
                "remaining",
                FREE_MONTHLY_LIMIT
            )
        )
    }

private fun roomAiDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences(
        ROOMAI_PLAN_PREFS,
        Context.MODE_PRIVATE
    )

    val existing = prefs.getString(
        ROOMAI_DEVICE_KEY,
        null
    )

    if (!existing.isNullOrBlank()) {
        return existing
    }

    val id = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: UUID.randomUUID().toString()

    prefs.edit()
        .putString(ROOMAI_DEVICE_KEY, id)
        .apply()

    return id
}



suspend fun generateDesign(
    context: Context,
    uri: Uri,
    room: String,
    style: String,
    userPrompt: String,
    operation: String = "generate",
    selection: String = ""
): String = withContext(Dispatchers.IO) {

    val boundary = "RoomAI-${UUID.randomUUID()}"

    val connection =
        URL(BACKEND_URL).openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.connectTimeout = 30000
    connection.readTimeout = 300000

    connection.setRequestProperty(
        "Content-Type",
        "multipart/form-data; boundary=$boundary"
    )

    val authToken = roomAiToken(context)

    if (authToken.isNotBlank()) {
        connection.setRequestProperty(
            "Authorization",
            "Bearer $authToken"
        )
    }

    connection.setRequestProperty(
        "X-RoomAI-Device",
        roomAiDeviceId(context)
    )

    DataOutputStream(connection.outputStream).use { output ->

        writeTextPart(output, boundary, "room", room)
        writeTextPart(output, boundary, "style", style)
        writeTextPart(output, boundary, "operation", operation)
        writeTextPart(output, boundary, "selection", selection)
        writeTextPart(output, boundary, "prompt", userPrompt)

        writeTextPart(
            output,
            boundary,
            "device_id",
            roomAiDeviceId(context)
        )

        writeTextPart(
            output,
            boundary,
            "plan",
            roomAiPlan(context)
        )

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw Exception("Could not read selected image")

        output.write(
            "--$boundary\r\n".toByteArray()
        )

        output.write(
            (
                "Content-Disposition: form-data; " +
                        "name=\"image\"; " +
                        "filename=\"room.jpg\"\r\n"
            ).toByteArray()
        )

        output.write(
            "Content-Type: image/jpeg\r\n\r\n".toByteArray()
        )

        output.write(bytes)
        output.write("\r\n".toByteArray())
        output.write("--$boundary--\r\n".toByteArray())
    }

    val code = connection.responseCode

    val stream =
        if (code in 200..299)
            connection.inputStream
        else
            connection.errorStream

    val response =
        stream?.bufferedReader()?.use { it.readText() }
            ?: throw Exception("Empty backend response")

    if (code !in 200..299) {
        throw Exception(response)
    }

    Regex("\"image_url\"\\s*:\\s*\"([^\"]+)\"")
        .find(response)
        ?.groupValues
        ?.get(1)
        ?: throw Exception("Backend returned no image URL")
}

fun writeTextPart(
    output: DataOutputStream,
    boundary: String,
    name: String,
    value: String
) {
    output.write("--$boundary\r\n".toByteArray())
    output.write(
        "Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray()
    )
    output.write(value.toByteArray())
    output.write("\r\n".toByteArray())
}

fun writeFilePart(
    output: DataOutputStream,
    boundary: String,
    fieldName: String,
    fileName: String,
    contentType: String,
    bytes: ByteArray
) {
    output.write("--$boundary\r\n".toByteArray())

    output.write(
        (
            "Content-Disposition: form-data; " +
                    "name=\"$fieldName\"; " +
                    "filename=\"$fileName\"\r\n"
        ).toByteArray()
    )

    output.write(
        "Content-Type: $contentType\r\n\r\n".toByteArray()
    )

    output.write(bytes)
    output.write("\r\n".toByteArray())
}
