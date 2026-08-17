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

        // Only send Authorization when we actually have a token.
        // For guests, send the persistent device id header so the backend
        // can associate usage with the device (guest quota).
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

private fun roomAiPlan(context: Context): String {
    return context.getSharedPreferences(
        ROOMAI_PLAN_PREFS,
        Context.MODE_PRIVATE
    ).getString(
        ROOMAI_PLAN_KEY,
        "free"
    ) ?: "free"
}

data class RoomAIUsage(
    val plan: String = "free",
    val used: Int = 0,
    val limit: Int = FREE_MONTHLY_LIMIT,
    val remaining: Int = FREE_MONTHLY_LIMIT
)


data class SavedDesign(
    val id: String,
    val url: String,
    val room: String,
    val style: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RoomAIAds.initialize(this)

        setContent {
            var dark by remember { mutableStateOf(false) }

            RoomAITheme(dark) {
                RoomAIApp(
                    dark = dark,
                    setDark = { dark = it }
                )
            }
        }
    }
}

@Composable
fun RoomAIApp(
    dark: Boolean,
    setDark: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var token by remember {
        mutableStateOf(roomAiToken(context))
    }

    val scope = rememberCoroutineScope()

    var usage by remember {
        mutableStateOf(RoomAIUsage())
    }

    var usageLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(token) {
        usageLoading = true

        try {
            usage = roomAiUsage(context)
        } catch (e: Exception) {
            if (e.message == "SESSION_EXPIRED") {
                clearRoomAiToken(context)
                token = ""
            }
        } finally {
            usageLoading = false
        }
    }

    val nav = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomItem(nav, "home", "Home", Icons.Default.Home)
                BottomItem(nav, "create", "Create", Icons.Default.Add)
                BottomItem(nav, "designs", "Designs", Icons.Default.PhotoLibrary)
                BottomItem(nav, "menu", "Menu", Icons.Default.Menu)
            }
        }
    ) { padding ->

        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("auth") {
                AuthScreen(
                    dark = false,
                    onAuthenticated = { newToken ->
                        // Persist and update local state so the UI and usage refresh immediately.
                        saveRoomAiToken(context, newToken)
                        token = newToken
                        nav.navigate("home") {
                            popUpTo("auth") {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable("home") {
                RoomAIHomeRedesigned(
                    nav = nav,
                    loggedIn = token.isNotBlank(),
                    usage = usage
                )
            }

            composable("growth") {
                RoomAIGrowthCenter(
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("create") }
                )
            }

            composable("create") {
                Create()
            }

            composable("designs") {
                Designs()
            }

            composable("styles") {
                Styles()
            }

            composable("enhance") {
                Enhance()
            }

            composable("furniture") {
                Furniture()
            }

            composable("products") {
                Products()
            }

            composable("ai_studio") {
                RoomAIWorkspace(nav)
            }

            composable("decision_engine") {
                RoomAIDecisionEngine()
            }

            composable("precision") {
                RoomAIPrecision()
            }

            composable("room_memory") {
                RoomAIMemory()
            }

            composable("legacy_ai_studio") {
                RoomAIPowerStudio()
            }

            composable("diagnose") {
                Diagnose()
            }

            composable("professional") {
                RoomAIProfessionalHome(nav)
            }

            composable("menu") {
                Menu(
                    dark = dark,
                    setDark = setDark,
                    onLogout = {
                        scope.launch {
                            roomAiLogout(context)
                            token = ""
                        }
                    }
                )
            }
        }
    }
}



@Composable
fun RoomAIHomeRedesigned(
    nav: NavHostController,
    loggedIn: Boolean = false,
    usage: RoomAIUsage = RoomAIUsage()
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // ---------------------------------------------------------
        // Header
        // ---------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RoomAI",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Interior design, with control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { nav.navigate("menu") }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // ---------------------------------------------------------
        // Hero
        // ---------------------------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.living_room),
                    contentDescription = "Interior design",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xCC101512)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Transform your space.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Design it. Understand it. Improve it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { nav.navigate("create") },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Create my room")
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // ---------------------------------------------------------
        // Main question
        // ---------------------------------------------------------
        Text(
            text = "What do you want to do?",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nav.navigate("growth")
                },
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Make your design shareable",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Create content for Instagram, TikTok, Pinterest, YouTube and Facebook.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoomAIHomeAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoAwesome,
                title = "Design",
                description = "Create a new concept"
            ) {
                nav.navigate("create")
            }

            RoomAIHomeAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                title = "Analyze",
                description = "Find room problems"
            ) {
                nav.navigate("diagnose")
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoomAIHomeAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Straighten,
                title = "Measure",
                description = "Keep dimensions"
            ) {
                nav.navigate("precision")
            }

            RoomAIHomeAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Folder,
                title = "Projects",
                description = "Continue your work"
            ) {
                nav.navigate("designs")
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---------------------------------------------------------
        // Project concept
        // ---------------------------------------------------------
        Text(
            text = "Your project",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { nav.navigate("designs") },
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My room projects",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Keep designs, measurements, constraints and versions together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---------------------------------------------------------
        // Professional section
        // ---------------------------------------------------------
        Text(
            text = "For professionals",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "A workspace for designers, merchants and craftsmen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { nav.navigate("professional") },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Professional Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Design → measure → analyze → refine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "OPEN STUDIO",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---------------------------------------------------------
        // Usage — kept visible but secondary
        // ---------------------------------------------------------
        if (loggedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI generations",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "${usage.remaining} remaining this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "RoomAI works around your room, not just the image.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RoomAIHomeAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(128.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
