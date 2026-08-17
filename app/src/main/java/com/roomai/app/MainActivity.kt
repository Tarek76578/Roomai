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

internal fun roomAiToken(context: Context): String {
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

        if (token.isNotBlank()) {
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

internal fun roomAiDeviceId(context: Context): String {
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
            startDestination = "problem_first",
            modifier = Modifier.padding(padding)
        ) {
            composable("auth") {
                AuthScreen(
                    dark = false,
                    onAuthenticated = { newToken ->
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


                    composable("problem_first") {

                        RoomAIProblemFirstScreen(

                            onContinueToDiagnosis = {

                                nav.navigate("decision_engine")

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

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    dark: Boolean,
    onAuthenticated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var registerMode by remember {
        mutableStateOf(false)
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf("")
    }

    RoomAITheme(dark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Text(
                        "RoomAI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (registerMode)
                            "Create your account"
                        else
                            "Welcome back"
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Email")
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Password")
                        },
                        singleLine = true
                    )

                    if (registerMode) {
                        Text(
                            "Password must contain at least 8 characters.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (error.isNotBlank()) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() ||
                                password.isBlank()
                            ) {
                                error =
                                    "Email and password are required."
                                return@Button
                            }

                            if (registerMode &&
                                password.length < 8
                            ) {
                                error =
                                    "Password must contain at least 8 characters."
                                return@Button
                            }

                            loading = true
                            error = ""

                            scope.launch {
                                try {
                                    val endpoint =
                                        if (registerMode)
                                            "/auth/register"
                                        else
                                            "/auth/login"

                                    val result =
                                        roomAiAuthRequest(
                                            endpoint = endpoint,
                                            email = email,
                                            password = password,
                                            context = context,
                                            includeDevice = registerMode
                                        )

                                    val newToken =
                                        result.getString("token")

                                    saveRoomAiToken(
                                        context,
                                        newToken
                                    )

                                    onAuthenticated(newToken)

                                } catch (e: Exception) {
                                    error =
                                        e.message
                                            ?: "Authentication failed."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (registerMode)
                                    "Create account"
                                else
                                    "Sign in"
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            registerMode = !registerMode
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (registerMode)
                                "Already have an account? Sign in"
                            else
                                "New to RoomAI? Create account"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomItem(
    nav: NavHostController,
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    NavigationBarItem(
        selected = false,
        onClick = {
            nav.navigate(route) {
                launchSingleTop = true
            }
        },
        icon = {
            Icon(icon, contentDescription = label)
        },
        label = {
            Text(label)
        }
    )
}

@Composable
fun Home(
    nav: NavHostController,
    usage: RoomAIUsage
) {

    val plan = usage.plan
    val limit = usage.limit


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "AI Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (plan == "pro")
                            "PRO"
                        else
                            "FREE"
                    )

                    Text(
                        "${usage.remaining} of ${usage.limit} generations remaining"
                    )

                    Text(
                        "${usage.used} / $limit used this month",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        if (plan == "pro")
                            "Pro • $limit generations/month"
                        else
                            "Free • $limit generations/month"
                    )

                    Spacer(Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Usage is protected by the RoomAI backend."
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                "RoomAI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "AI Interior Designer",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            ElevatedCard(
                onClick = { nav.navigate("diagnose") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "RoomAI Diagnose",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Find problems and risks before you change or buy anything."
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }

        item {
            ElevatedCard(
                onClick = { nav.navigate("create") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        modifier = Modifier.size(46.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Redesign your room",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Upload a room photo, choose a style and let AI create a new interior."
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = { nav.navigate("create") }
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Designing")
                    }
                }
            }
        }

        item {
            Text(
                "AI Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            FeatureGrid(nav)
        }

        item {
            Text(
                "How RoomAI works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            StepCard("1", "Upload", "Choose a photo of your room.")
            StepCard("2", "Customize", "Select room type, style and instructions.")
            StepCard("3", "Create solution", "RoomAI sends the request to the AI backend.")
            StepCard("4", "Save", "Your generated design is added to My Designs.")
        }
    }
}

@Composable
fun FeatureGrid(nav: NavHostController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFeature(
                "Solve my room",
                Icons.Default.Palette,
                Modifier.weight(1f)
            ) {
                nav.navigate("styles")
            }

            SmallFeature(
                "Fix a room problem",
                Icons.Default.AutoFixHigh,
                Modifier.weight(1f)
            ) {
                nav.navigate("enhance")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFeature(
                "Furniture",
                Icons.Default.Chair,
                Modifier.weight(1f)
            ) {
                nav.navigate("furniture")
            }

            SmallFeature(
                "Products",
                Icons.Default.ShoppingBag,
                Modifier.weight(1f)
            ) {
                nav.navigate("products")
            }
        }

        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFeature(
                "AI Studio",
                Icons.Default.AutoAwesome,
                Modifier.weight(1f)
            ) {
                nav.navigate("ai_studio")
            }

            SmallFeature(
                "Diagnose",
                Icons.Default.Search,
                Modifier.weight(1f)
            ) {
                nav.navigate("diagnose")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFeature(
                "Decision Engine",
                Icons.Default.Psychology,
                Modifier.weight(1f)
            ) {
                nav.navigate("decision_engine")
            }

            SmallFeature(
                "Room Memory",
                Icons.Default.Lock,
                Modifier.weight(1f)
            ) {
                nav.navigate("room_memory")
            }
        }
    }
}

@Composable
fun SmallFeature(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, title)

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StepCard(
    number: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                tonalElevation = 4.dp
            ) {
                Text(
                    number,
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 8.dp
                    ),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description)
            }
        }
    }
}

@Composable
fun Create() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var room by remember { mutableStateOf("Living Room") }
    var style by remember { mutableStateOf("Modern") }
    var prompt by remember { mutableStateOf("") }
    var resultUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fixingProblem by remember { mutableStateOf<String?>(null) }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            imageUri = uri
            resultUrl = null
            error = null
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Create Design",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text("Create a personalized AI interior.")
        }

        item {
            if (imageUri == null) {
                OutlinedCard(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            null,
                            modifier = Modifier.size(50.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Add Room Photo",
                            fontWeight = FontWeight.Bold
                        )

                        Text("JPG, PNG or WEBP")
                    }
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Room",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp),
                    contentScale = ContentScale.Crop
                )

                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Photo")
                }
            }
        }

        item {
            SelectionSection(
                "Room",
                listOf(
                    "Living Room",
                    "Bedroom",
                    "Kitchen",
                    "Office",
                    "Dining Room"
                ),
                room
            ) {
                room = it
            }
        }

        item {
            SelectionSection(
                "Style",
                listOf(
                    "Modern",
                    "Minimalist",
                    "Luxury",
                    "Scandinavian",
                    "Industrial",
                    "Classic",
                    "Bohemian",
                    "Japandi"
                ),
                style
            ) {
                style = it
            }
        }

        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("AI Instructions") },
                placeholder = {
                    Text("Example: warm lighting, large sofa, plants...")
                },
                minLines = 4
            )
        }

        item {
            Button(
                enabled = imageUri != null && !loading,
                onClick = {
                    val uri = imageUri ?: return@Button

                    scope.launch {
                        loading = true
                        error = null
                        resultUrl = null

                        try {
                            val url = generateDesign(
                                context,
                                uri,
                                room,
                                style,
                                prompt
                            )

                            resultUrl = url

                            saveDesign(
                                context,
                                url,
                                room,
                                style
                            )

                            RoomAIHistory.add(
                                context = context,
                                generatedUrl = url,
                                room = room,
                                style = style,
                                prompt = prompt,
                                originalUrl = imageUri?.toString()
                            )
                        } catch (e: Exception) {
                            error =
                                e.message ?: "Generation failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(10.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Solve my room")
                }
            }
        }

        item {
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            resultUrl?.let { url ->
                Text(
                    "Your AI Design",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                AsyncImage(
                    model = url,
                    contentDescription = "Generated design",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Saved to My Designs",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SelectionSection(
    title: String,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(values) {
                FilterChip(
                    selected = selected == it,
                    onClick = { onSelect(it) },
                    label = { Text(it) }
                )
            }
        }
    }
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
            "Content-Type: image/jpeg\r\n\r\n"
                .toByteArray()
        )

        output.write(bytes)
        output.write("\r\n".toByteArray())
        output.write("--$boundary--\r\n".toByteArray())
    }

    val code = connection.responseCode

    Log.d("RoomAI", "DIAGNOSE: HTTP $code")

    val stream =
        if (code in 200..299)
            connection.inputStream
        else
            connection.errorStream

    val response =
        stream?.bufferedReader()?.use { it.readText() }
            ?: throw Exception("Empty backend response")

    if (code !in 200..299) {
        if (code == 429 && response.contains("USAGE_LIMIT_REACHED")) {
            throw Exception(
                "Monthly AI generation limit reached. Upgrade to Pro for more generations."
            )
        }

        throw Exception(response)
    }

    Regex("\"image_url\"\\s*:\\s*\"([^\"]+)\"")
        .find(response)
        ?.groupValues
        ?.get(1)
        ?.also {
            RoomAIAds.showAfterGeneration(context)
        }
        ?: throw Exception("Backend returned no image URL")
}


data class RoomProblem(
    val title: String,
    val severity: String,
    val reason: String,
    val recommendation: String
)

suspend fun fixRoomProblem(
    context: Context,
    uri: Uri,
    problem: RoomProblem
): String {
    val instructions = """
        Fix this specific room problem.

        Problem: ${problem.title}
        Severity: ${problem.severity}
        Reason: ${problem.reason}
        Recommendation: ${problem.recommendation}

        Make only the changes necessary to solve this problem.
        Preserve the existing room architecture, walls, doors,
        windows, floor, ceiling, perspective and camera angle.
        Preserve unrelated furniture and objects.
        The result must be photorealistic and practical.
    """.trimIndent()

    return generateDesign(
        context = context,
        uri = uri,
        room = "Living Room",
        style = "Modern",
        userPrompt = instructions,
        operation = "fix",
        selection = problem.title
    )
}

data class RoomRisk(
    val type: String,
    val severity: String,
    val message: String
)

data class RoomDiagnosis(
    val summary: String,
    val score: Int,
    val problems: List<RoomProblem>,
    val risks: List<RoomRisk>,
    val keep: List<String>,
    val replace: List<String>,
    val upgrade: List<String>,
    val lifestyleQuestions: List<String>
)

suspend fun diagnoseRoom(
    context: Context,
    uri: Uri
): RoomDiagnosis = withContext(Dispatchers.IO) {

    val boundary = "RoomAI-Diagnose-${UUID.randomUUID()}"

    Log.d("RoomAI", "DIAGNOSE: starting request")

    val connection =
        URL(DIAGNOSE_URL).openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.connectTimeout = 30000
    connection.readTimeout = 120000

    connection.setRequestProperty(
        "Content-Type",
        "multipart/form-data; boundary=$boundary"
    )

    // Keep /diagnose identity consistent with /generate.
    // Authenticated users are identified by their server-side session.
    // Guests are identified by the installation/device key.
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

        writeTextPart(
            output,
            boundary,
            "device_id",
            roomAiDeviceId(context)
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
            "Content-Type: image/jpeg\r\n\r\n"
                .toByteArray()
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
            ?: throw Exception("Empty diagnosis response")

    Log.d("RoomAI", "DIAGNOSE: response received")

    if (code !in 200..299) {
        throw Exception(response)
    }

    val root = JSONObject(response)
    val diagnosis = root.getJSONObject("diagnosis")

    val problems = mutableListOf<RoomProblem>()
    val problemArray = diagnosis.optJSONArray("problems")

    if (problemArray != null) {
        for (i in 0 until problemArray.length()) {
            val item = problemArray.getJSONObject(i)

            problems.add(
                RoomProblem(
                    title = item.optString("title"),
                    severity = item.optString("severity"),
                    reason = item.optString("reason"),
                    recommendation = item.optString("recommendation")
                )
            )
        }
    }

    val risks = mutableListOf<RoomRisk>()
    val riskArray = diagnosis.optJSONArray("risk_scanner")

    if (riskArray != null) {
        for (i in 0 until riskArray.length()) {
            val item = riskArray.getJSONObject(i)

            risks.add(
                RoomRisk(
                    type = item.optString("type"),
                    severity = item.optString("severity"),
                    message = item.optString("message")
                )
            )
        }
    }

    fun readStrings(name: String): List<String> {
        val result = mutableListOf<String>()
        val array = diagnosis.optJSONArray(name)

        if (array != null) {
            for (i in 0 until array.length()) {
                result.add(array.optString(i))
            }
        }

        return result
    }

    RoomDiagnosis(
        summary = diagnosis.optString("summary"),
        score = diagnosis.optInt("score", 0),
        problems = problems,
        risks = risks,
        keep = readStrings("keep"),
        replace = readStrings("replace"),
        upgrade = readStrings("upgrade"),
        lifestyleQuestions = readStrings("lifestyle_questions")
    )
}

fun writeTextPart(
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

fun saveDesign(
    context: Context,
    url: String,
    room: String,
    style: String
) {
    val prefs =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    val old =
        loadDesigns(context).toMutableList()

    old.removeAll { it.url == url }

    old.add(
        0,
        SavedDesign(
            UUID.randomUUID().toString(),
            url,
            room,
            style
        )
    )

    val serialized =
        old.joinToString("\n") {
            listOf(
                it.id,
                it.url,
                it.room,
                it.style
            ).joinToString("|")
        }

    prefs.edit()
        .putString("designs", serialized)
        .apply()
}

fun loadDesigns(
    context: Context
): List<SavedDesign> {
    val prefs =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    val raw =
        prefs.getString("designs", "")
            ?: ""

    if (raw.isBlank()) return emptyList()

    return raw.lines().mapNotNull { line ->
        val parts = line.split("|", limit = 4)

        if (parts.size == 4) {
            SavedDesign(
                parts[0],
                parts[1],
                parts[2],
                parts[3]
            )
        } else null
    }
}

fun deleteDesign(
    context: Context,
    id: String
) {
    val remaining =
        loadDesigns(context)
            .filter { it.id != id }

    val serialized =
        remaining.joinToString("\n") {
            listOf(
                it.id,
                it.url,
                it.room,
                it.style
            ).joinToString("|")
        }

    context.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE
    )
        .edit()
        .putString("designs", serialized)
        .apply()
}

fun shareDesign(
    context: Context,
    url: String
) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }

    context.startActivity(
        Intent.createChooser(
            intent,
            "Share RoomAI Design"
        )
    )
}

@Composable
fun Designs() {
    val context = LocalContext.current
    var versions by remember {
        mutableStateOf(RoomAIHistory.load(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "History / Versions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Your generated designs and previous versions stay available on this device."
        )

        Spacer(Modifier.height(16.dp))

        if (versions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "No versions yet",
                        fontWeight = FontWeight.Bold
                    )

                    Text("Generate a design to create your first version.")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = versions,
                    key = { it.id }
                ) { version ->

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            AsyncImage(
                                model = version.generatedUrl,
                                contentDescription = "Generated version",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "${version.room} • ${version.style}",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Version ${version.id.take(8)}"
                                    )

                                    if (version.parentId != null) {
                                        Text(
                                            "Derived from previous version",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        RoomAIHistory.delete(
                                            context,
                                            version.id
                                        )
                                        versions =
                                            RoomAIHistory.load(context)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete version"
                                    )
                                }
                            }

                            if (version.prompt.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))

                                Text(
                                    version.prompt,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                version.originalUrl?.let { original ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Original available") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Compare,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }

                                AssistChip(
                                    onClick = {},
                                    label = { Text("Generated") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibrary() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                null,
                modifier = Modifier.size(52.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Your design library is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Generate your first room design and it will appear here."
            )
        }
    }
}

data class ToolOption(
    val title: String,
    val description: String
)



@Composable
fun BeforeAfterSwipe(
    before: Uri,
    after: String,
    modifier: Modifier = Modifier
) {
    var position by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        AsyncImage(
            model = before,
            contentDescription = "Before",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(position)
                .clip(RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = after,
                contentDescription = "After",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .align(Alignment.CenterStart)
                .offset(x = 0.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        position = (position + dragAmount.x / size.width)
                            .coerceIn(0.05f, 0.95f)
                    }
                }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        position = (position + dragAmount.x / size.width)
                            .coerceIn(0.05f, 0.95f)
                    }
                }
        )

        Text(
            "BEFORE",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            fontWeight = FontWeight.Bold
        )

        Text(
            "AFTER",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            fontWeight = FontWeight.Bold
        )
    }
}


private enum class RoomAIStudioMode(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PRECISION("Precision", Icons.Default.AutoFixHigh),
    BUDGET("Budget", Icons.Default.AttachMoney),
    PRODUCTS("Products", Icons.Default.ShoppingBag),
    SELLER("Seller", Icons.Default.Storefront),
    REALITY("Reality", Icons.Default.CheckCircle),
    REDESIGN("Redesign", Icons.Default.AutoAwesome)
}

@Composable
fun RoomAIPowerStudio() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var mode by remember {
        mutableStateOf(RoomAIStudioMode.PRECISION)
    }

    var selectedObject by remember {
        mutableStateOf("Sofa")
    }

    var instruction by remember {
        mutableStateOf("")
    }

    var budget by remember {
        mutableFloatStateOf(100000f)
    }

    var productType by remember {
        mutableStateOf("Sofa")
    }

    var style by remember {
        mutableStateOf("Modern")
    }

    var resultUrl by remember {
        mutableStateOf<String?>(null)
    }

    var diagnosis by remember {
        mutableStateOf<RoomDiagnosis?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var precisionVerification by remember {
        mutableStateOf<PrecisionVerification?>(null)
    }

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            imageUri = uri
            resultUrl = null
            diagnosis = null
            error = null
        }

    val objects = listOf(
        "Sofa",
        "Chair",
        "Table",
        "Bed",
        "Wardrobe",
        "Rug",
        "Curtains",
        "Walls",
        "Floor",
        "Lighting",
        "Decor",
        "Plants"
    )

    val styles = listOf(
        "Modern",
        "Minimalist",
        "Luxury",
        "Scandinavian",
        "Industrial",
        "Classic",
        "Bohemian",
        "Japandi"
    )

    val productTypes = listOf(
        "Sofa",
        "Bed",
        "Chair",
        "Table",
        "Wardrobe",
        "Lighting",
        "Rug",
        "Decor"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "RoomAI Power Studio",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Design • Edit • Diagnose • Budget • Products • Business"
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        "One room. Six AI workflows.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "RoomAI is not only a room generator. Use the studio to make decisions about the real room."
                    )
                }
            }
        }

        item {
            if (imageUri == null) {
                OutlinedCard(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            null,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Add Room / Product Photo",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Use a clear image for better AI control."
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Change Image")
                }
            }
        }

        item {
            Text(
                "AI Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RoomAIStudioMode.values().toList()) { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = {
                            mode = item
                            resultUrl = null
                            diagnosis = null
                            error = null
                        },
                        label = {
                            Text(item.title)
                        },
                        leadingIcon = {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        when (mode) {

            RoomAIStudioMode.PRECISION -> {

                item {
                    StudioSectionTitle(
                        "Precision Edit",
                        "Change one thing and verify that everything else stayed intact."
                    )
                }

                item {
                    StudioChipRow(
                        values = objects,
                        selected = selectedObject,
                        onSelected = {
                            selectedObject = it
                            resultUrl = null
                            precisionVerification = null
                            error = null
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {
                            instruction = it
                            precisionVerification = null
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("What should change?")
                        },
                        placeholder = {
                            Text("Example: replace it with a beige modern sofa.")
                        },
                        minLines = 3
                    )
                }

                item {
                    StudioActionButton(
                        enabled = imageUri != null && !loading,
                        loading = loading,
                        text = "Change Only $selectedObject"
                    ) {
                        val uri = imageUri ?: return@StudioActionButton

                        scope.launch {
                            loading = true
                            error = null
                            resultUrl = null
                            precisionVerification = null

                            try {
                                val protectedElements =
                                    objects
                                        .filter { it != selectedObject }
                                        .map {
                                            ProtectedElement(
                                                name = it,
                                                reason = "Do not modify the unrelated $it"
                                            )
                                        }

                                val precisionRequest =
                                    PrecisionRequest(
                                        target = PrecisionTarget(
                                            name = selectedObject,
                                            description = "The selected room element: $selectedObject",
                                            selection = selectedObject
                                        ),
                                        editType = PrecisionEditType.REPLACE,
                                        instruction = instruction.ifBlank {
                                            "Improve this selected object while preserving its position, role and appearance context."
                                        },
                                        room = "Existing Room",
                                        style = style,
                                        protectedElements = protectedElements
                                    )

                                val result =
                                    RoomAIPrecisionEngine.execute(
                                        context = context,
                                        uri = uri,
                                        request = precisionRequest,
                                        verify = true
                                    )

                                resultUrl = result.version.imageUrl
                                precisionVerification =
                                    result.version.verification

                                if (result.accepted) {
                                    saveDesign(
                                        context,
                                        result.version.imageUrl,
                                        "Precision Verified",
                                        selectedObject
                                    )
                                } else {
                                    error =
                                        "Precision verification failed after ${result.attempts} attempt(s). The result was not accepted."
                                }

                            } catch (e: Exception) {
                                error =
                                    e.message
                                        ?: "Precision edit failed"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }

                item {
                    val verification = precisionVerification
                    val before = imageUri
                    val after = resultUrl

                    if (verification != null && after != null && before != null) {

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    if (verification.status == VerificationStatus.PASS)
                                        "✓ Precision Verified"
                                    else
                                        "✕ Precision Verification Failed",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Verification score: ${verification.score}/100"
                                )

                                Text(
                                    verification.message.ifBlank {
                                        if (verification.status == VerificationStatus.PASS)
                                            "The requested change was detected and protected elements were preserved."
                                        else
                                            "The generated image did not satisfy all preservation rules."
                                    }
                                )

                                Text(
                                    "Attempts: ${if (verification.status == VerificationStatus.PASS) 1 else 2}"
                                )

                                BeforeAfterSwipe(
                                    before = before,
                                    after = after,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            RoomAIStudioMode.BUDGET -> {

                item {
                    StudioSectionTitle(
                        "Smart Budget",
                        "Tell RoomAI how much you want to spend."
                    )
                }

                item {
                    Text(
                        "Budget: ${budget.toInt()} USD",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Slider(
                        value = budget,
                        onValueChange = {
                            budget = it
                        },
                        valueRange = 20000f..1000000f,
                        steps = 19
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text("20K USD")
                        Text("1M USD")
                    }
                }

                item {
                    StudioChipRow(
                        values = styles,
                        selected = style,
                        onSelected = {
                            style = it
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {
                            instruction = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Budget instructions")
                        },
                        placeholder = {
                            Text(
                                "Example: prioritize sofa and lighting."
                            )
                        },
                        minLines = 3
                    )
                }

                item {
                    StudioActionButton(
                        enabled = imageUri != null && !loading,
                        loading = loading,
                        text = "Design Within Budget"
                    ) {
                        val uri = imageUri ?: return@StudioActionButton

                        scope.launch {
                            loading = true
                            error = null
                            resultUrl = null

                            try {
                                val prompt =
                                    """
                                    SMART BUDGET INTERIOR DESIGN.

                                    Maximum budget:
                                    ${budget.toInt()} USD

                                    Preferred style:
                                    $style

                                    User priorities:
                                    ${instruction.ifBlank {
                                        "Use the budget intelligently and prioritize the most visible improvements."
                                    }}

                                    IMPORTANT:
                                    - Keep the existing room architecture.
                                    - Do not change doors or windows.
                                    - Do not create unrealistic luxury items.
                                    - Prefer practical furniture.
                                    - Optimize visual impact per unit of budget.
                                    - Clearly prioritize what should be replaced,
                                      kept or upgraded.
                                    - Do not claim exact market prices from the image.
                                    - Treat the budget as a design constraint.
                                    """.trimIndent()

                                val url = generateDesign(
                                    context = context,
                                    uri = uri,
                                    room = "Existing Room",
                                    style = style,
                                    userPrompt = prompt,
                                    operation = "budget_design",
                                    selection = "${budget.toInt()} USD"
                                )

                                resultUrl = url

                                saveDesign(
                                    context,
                                    url,
                                    "Budget ${budget.toInt()} USD",
                                    style
                                )
                            } catch (e: Exception) {
                                error =
                                    e.message
                                        ?: "Budget design failed"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            }

            RoomAIStudioMode.PRODUCTS -> {

                item {
                    StudioSectionTitle(
                        "Product Match",
                        "Turn the room into a product specification."
                    )
                }

                item {
                    StudioChipRow(
                        values = productTypes,
                        selected = productType,
                        onSelected = {
                            productType = it
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {
                            instruction = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Product requirements")
                        },
                        placeholder = {
                            Text(
                                "Example: beige, 3 seats, modern, compact."
                            )
                        },
                        minLines = 3
                    )
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                "Product Brief",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                """
                                Product: $productType
                                Style: $style
                                Budget: ${budget.toInt()} USD
                                Requirements: ${
                                    instruction.ifBlank {
                                        "Not specified"
                                    }
                                }
                                """.trimIndent()
                            )
                        }
                    }
                }

                item {
                    StudioActionButton(
                        enabled = imageUri != null && !loading,
                        loading = loading,
                        text = "Visualize Product"
                    ) {
                        val uri = imageUri ?: return@StudioActionButton

                        scope.launch {
                            loading = true
                            error = null
                            resultUrl = null

                            try {
                                val prompt =
                                    """
                                    PRODUCT MATCH / VISUALIZATION.

                                    Product:
                                    $productType

                                    Preferred style:
                                    $style

                                    Target budget:
                                    ${budget.toInt()} USD

                                    Requirements:
                                    ${instruction.ifBlank {
                                        "Choose a practical option that fits the room."
                                    }}

                                    Place or visualize the requested product
                                    naturally in the existing room.

                                    Preserve the room architecture.
                                    Preserve windows, doors and floor.
                                    Match perspective and lighting.
                                    Do not redesign unrelated areas.
                                    The result should help the user decide
                                    whether this product category fits the room.
                                    """.trimIndent()

                                val url = generateDesign(
                                    context = context,
                                    uri = uri,
                                    room = "Existing Room",
                                    style = style,
                                    userPrompt = prompt,
                                    operation = "product_match",
                                    selection = productType
                                )

                                resultUrl = url

                                saveDesign(
                                    context,
                                    url,
                                    "Product Match",
                                    productType
                                )
                            } catch (e: Exception) {
                                error =
                                    e.message
                                        ?: "Product visualization failed"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            }

            RoomAIStudioMode.SELLER -> {

                item {
                    StudioSectionTitle(
                        "Seller Studio",
                        "Turn one furniture product photo into marketing scenes."
                    )
                }

                item {
                    StudioChipRow(
                        values = productTypes,
                        selected = productType,
                        onSelected = {
                            productType = it
                        }
                    )
                }

                item {
                    StudioChipRow(
                        values = styles,
                        selected = style,
                        onSelected = {
                            style = it
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {
                            instruction = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Seller instructions")
                        },
                        placeholder = {
                            Text(
                                "Example: premium showroom scene for Instagram."
                            )
                        },
                        minLines = 3
                    )
                }

                item {
                    StudioActionButton(
                        enabled = imageUri != null && !loading,
                        loading = loading,
                        text = "Create Product Scene"
                    ) {
                        val uri = imageUri ?: return@StudioActionButton

                        scope.launch {
                            loading = true
                            error = null
                            resultUrl = null

                            try {
                                val prompt =
                                    """
                                    SELLER PRODUCT MARKETING MODE.

                                    Product type:
                                    $productType

                                    Style:
                                    $style

                                    Seller request:
                                    ${instruction.ifBlank {
                                        "Create a premium realistic interior scene."
                                    }}

                                    IMPORTANT:
                                    - The uploaded product is the hero product.
                                    - Preserve its recognizable design.
                                    - Do not change its core shape or identity.
                                    - Place it naturally inside a realistic room.
                                    - Use professional product photography quality.
                                    - Make the image suitable for social media,
                                      ecommerce and furniture advertising.
                                    - Do not add fake brand logos.
                                    - Do not invent exact product specifications.
                                    """.trimIndent()

                                val url = generateDesign(
                                    context = context,
                                    uri = uri,
                                    room = "Furniture Marketing Scene",
                                    style = style,
                                    userPrompt = prompt,
                                    operation = "seller_scene",
                                    selection = productType
                                )

                                resultUrl = url

                                saveDesign(
                                    context,
                                    url,
                                    "Seller Scene",
                                    productType
                                )
                            } catch (e: Exception) {
                                error =
                                    e.message
                                        ?: "Seller scene failed"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                "Business direction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "This workflow is designed to become a future paid seller feature: product photo → realistic room scene → marketing content."
                            )
                        }
                    }
                }
            }

            RoomAIStudioMode.REALITY -> {

                item {
                    StudioSectionTitle(
                        "Reality Check",
                        "Analyze the room before committing to a design."
                    )
                }

                item {
                    StudioActionButton(
                        enabled = imageUri != null && !loading,
                        loading = loading,
                        text = "Run Reality Check"
                    ) {
                        val uri = imageUri ?: return@StudioActionButton

                        scope.launch {
                            loading = true
                            error = null
                            diagnosis = null

                            try {
                                diagnosis =
                                    diagnoseRoom(
                                        context,
                                        uri
                                    )
                            } catch (e: Exception) {
                                error =
                                    e.message
                                        ?: "Reality check failed"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }

                diagnosis?.let { result ->

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(
                                    "Reality Score",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "${result.score.coerceIn(0, 100)}/100",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(result.summary)
                            }
                        }
                    }

                    items(
                        result.risks
                    ) { risk ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    risk.type.ifBlank {
                                        "Room Risk"
                                    },
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "Severity: ${
                                        risk.severity
                                            .ifBlank { "Review" }
                                            .uppercase()
                                    }"
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(risk.message)
                            }
                        }
                    }

                    items(
                        result.problems
                    ) { problem ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    problem.title,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "Priority: ${
                                        problem.severity
                                            .ifBlank { "Review" }
                                            .uppercase()
                                    }"
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(problem.recommendation)
                            }
                        }
                    }
                }
            }

            RoomAIStudioMode.REDESIGN -> {

                item {
                    StudioSectionTitle(
                        "Directed Redesign",
                        "Generate a redesign using explicit constraints."
                    )
                }

                item {
                    StudioChipRow(
                        values = styles,
                        selected = style,
                        onSelected = {
                            style = it
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {
                            instruction = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Design brief")
                        },
                        placeholder = {
                            Text(
                                "Example: keep the floor, make the room brighter and add storage."
                            )
                        },
                        minLines = 4
                    )
                }

                item {
                    StudioActionButton(
                        enabled = imageUri != null && !loading,
                        loading = loading,
                        text = "Generate Directed Redesign"
                    ) {
                        val uri = imageUri ?: return@StudioActionButton

                        scope.launch {
                            loading = true
                            error = null
                            resultUrl = null

                            try {
                                val prompt =
                                    """
                                    DIRECTED INTERIOR REDESIGN.

                                    Style:
                                    $style

                                    User brief:
                                    ${instruction.ifBlank {
                                        "Improve the room while keeping its architecture."
                                    }}

                                    Preserve the actual room structure.
                                    Preserve doors and windows.
                                    Preserve floor unless explicitly requested.
                                    Respect perspective and scale.
                                    Make furniture placement practical.
                                    Avoid overcrowding.
                                    Make the result photorealistic.
                                    """.trimIndent()

                                val url = generateDesign(
                                    context = context,
                                    uri = uri,
                                    room = "Existing Room",
                                    style = style,
                                    userPrompt = prompt,
                                    operation = "directed_redesign",
                                    selection = ""
                                )

                                resultUrl = url

                                saveDesign(
                                    context,
                                    url,
                                    "Directed Redesign",
                                    style
                                )
                            } catch (e: Exception) {
                                error =
                                    e.message
                                        ?: "Redesign failed"
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            }
        }

        error?.let { message ->
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Something went wrong",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            "Try again. The room photo was not modified."
                        )
                    }
                }
            }
        }

        resultUrl?.let { url ->

            item {
                Text(
                    "Result",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "AI result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Saved automatically to My Designs.",
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                shareDesign(
                                    context,
                                    url
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Share,
                                null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Share Result")
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        "RoomAI Product Loop",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Room → Diagnose → Decide → Edit → Budget → Product → Buy"
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "This is the direction that turns RoomAI from a simple image generator into an interior decision tool."
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioSectionTitle(
    title: String,
    description: String
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(description)
    }
}

@Composable
private fun StudioChipRow(
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(values) { value ->
            FilterChip(
                selected = selected == value,
                onClick = {
                    onSelected(value)
                },
                label = {
                    Text(value)
                }
            )
        }
    }
}

@Composable
private fun StudioActionButton(
    enabled: Boolean,
    loading: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )

            Spacer(Modifier.width(10.dp))
            Text("AI is working...")
        } else {
            Icon(
                Icons.Default.AutoAwesome,
                null
            )

            Spacer(Modifier.width(8.dp))

            Text(text)
        }
    }
}


@Composable
fun Diagnose() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var diagnosis by remember { mutableStateOf<RoomDiagnosis?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var fixingProblem by remember { mutableStateOf<String?>(null) }
    var fixedResultUrl by remember { mutableStateOf<String?>(null) }
    var fixedProblemTitle by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imageUri = uri
        diagnosis = null
        error = null
        fixedResultUrl = null
        fixedProblemTitle = null
    }

    fun severityRank(value: String): Int {
        return when (value.trim().lowercase()) {
            "critical" -> 0
            "high" -> 1
            "important" -> 1
            "medium" -> 2
            "moderate" -> 2
            "low" -> 3
            "minor" -> 3
            else -> 4
        }
    }

    fun severityLabel(value: String): String {
        return when (value.trim().lowercase()) {
            "critical" -> "CRITICAL"
            "high" -> "HIGH"
            "important" -> "IMPORTANT"
            "medium" -> "MEDIUM"
            "moderate" -> "MEDIUM"
            "low" -> "LOW"
            "minor" -> "MINOR"
            else -> value.ifBlank { "REVIEW" }.uppercase()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "RoomAI Diagnose",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Understand what is wrong with your room before changing furniture or spending money."
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "AI Room Analysis",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Layout • lighting • furniture • risks • improvements"
                        )
                    }
                }
            }
        }

        item {
            if (imageUri == null) {
                OutlinedCard(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(215.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Add Room Photo",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Use a clear photo showing the whole room"
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Room photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change Photo")
                }
            }
        }

        item {
            Button(
                enabled = imageUri != null && !loading,
                onClick = {
                    val uri = imageUri ?: return@Button

                    scope.launch {
                        loading = true
                        error = null
                        diagnosis = null
                        fixedResultUrl = null
                        fixedProblemTitle = null

                        try {
                            diagnosis = diagnoseRoom(
                                context,
                                uri
                            )
                        } catch (e: Exception) {
                            error = e.message ?: "Diagnosis failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(10.dp))
                    Text("Analyzing Room...")
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))
                    Text("Analyze My Room")
                }
            }
        }

        error?.let { message ->
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                "Analysis Error",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(message)

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Check your connection and try again.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        diagnosis?.let { result ->

            val orderedProblems =
                result.problems.sortedBy {
                    severityRank(it.severity)
                }

            val topProblem = orderedProblems.firstOrNull()

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Room Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "${result.score.coerceIn(0, 100)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.width(5.dp))

                            Text(
                                "/100",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            result.summary.ifBlank {
                                "RoomAI analyzed the room and found several areas that can be improved."
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        val problemCount = orderedProblems.size
                        val riskCount = result.risks.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiagnoseMetric(
                                value = problemCount.toString(),
                                label = "Problems",
                                modifier = Modifier.weight(1f)
                            )

                            DiagnoseMetric(
                                value = riskCount.toString(),
                                label = "Risks",
                                modifier = Modifier.weight(1f)
                            )

                            DiagnoseMetric(
                                value = result.upgrade.size.toString(),
                                label = "Upgrades",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            topProblem?.let { problem ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                "Recommended Next Step",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                problem.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Priority: ${severityLabel(problem.severity)}",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(problem.recommendation)

                            Spacer(Modifier.height(12.dp))

                            Button(
                                enabled = fixingProblem == null,
                                onClick = {
                                    val uri = imageUri ?: return@Button

                                    scope.launch {
                                        fixingProblem = problem.title
                                        error = null

                                        try {
                                            val url = fixRoomProblem(
                                                context = context,
                                                uri = uri,
                                                problem = problem
                                            )

                                            fixedResultUrl = url
                                            fixedProblemTitle = problem.title

                                            saveDesign(
                                                context,
                                                url,
                                                "Analyzed Room",
                                                "Fix: ${problem.title}"
                                            )
                                        } catch (e: Exception) {
                                            error =
                                                e.message
                                                    ?: "Could not fix this problem"
                                        } finally {
                                            fixingProblem = null
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (fixingProblem == problem.title) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )

                                    Spacer(Modifier.width(8.dp))
                                    Text("Fixing...")
                                } else {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        contentDescription = null
                                    )

                                    Spacer(Modifier.width(8.dp))
                                    Text("Fix Highest Priority Problem")
                                }
                            }
                        }
                    }
                }
            }

            if (orderedProblems.isNotEmpty()) {
                item {
                    Text(
                        "Priority Problems",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(orderedProblems) { problem ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when (severityRank(problem.severity)) {
                                        0, 1 -> Icons.Default.PriorityHigh
                                        2 -> Icons.Default.Warning
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        problem.title.ifBlank {
                                            "Room improvement"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        severityLabel(problem.severity),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(problem.reason)

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "What to do",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(problem.recommendation)

                            Spacer(Modifier.height(12.dp))

                            Button(
                                enabled = fixingProblem == null,
                                onClick = {
                                    val uri = imageUri ?: return@Button

                                    scope.launch {
                                        fixingProblem = problem.title
                                        error = null

                                        try {
                                            val url = fixRoomProblem(
                                                context = context,
                                                uri = uri,
                                                problem = problem
                                            )

                                            fixedResultUrl = url
                                            fixedProblemTitle = problem.title

                                            saveDesign(
                                                context,
                                                url,
                                                "Analyzed Room",
                                                "Fix: ${problem.title}"
                                            )
                                        } catch (e: Exception) {
                                            error =
                                                e.message
                                                    ?: "Could not fix this problem"
                                        } finally {
                                            fixingProblem = null
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (fixingProblem == problem.title) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )

                                    Spacer(Modifier.width(8.dp))
                                    Text("Fixing...")
                                } else {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        contentDescription = null
                                    )

                                    Spacer(Modifier.width(8.dp))
                                    Text("Fix This Problem")
                                }
                            }

                            if (
                                fixedProblemTitle == problem.title &&
                                fixedResultUrl != null &&
                                imageUri != null
                            ) {
                                Spacer(Modifier.height(14.dp))

                                Text(
                                    "Before / After",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                BeforeAfterSwipe(
                                    before = imageUri!!,
                                    after = fixedResultUrl!!
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "No major problems detected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Your room already has a strong foundation. RoomAI can still suggest style and upgrade ideas."
                            )
                        }
                    }
                }
            }

            if (result.risks.isNotEmpty()) {
                item {
                    Text(
                        "Reality & Risk Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(result.risks) { risk ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    risk.type
                                        .replaceFirstChar {
                                            it.uppercase()
                                        }
                                        .ifBlank { "Room Risk" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Severity: ${severityLabel(risk.severity)}",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                risk.message.ifBlank {
                                    "RoomAI detected something that should be reviewed."
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Keep / Replace / Upgrade",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                DiagnoseListCard(
                    title = "Keep",
                    icon = Icons.Default.CheckCircle,
                    values = result.keep
                )
            }

            item {
                DiagnoseListCard(
                    title = "Replace",
                    icon = Icons.Default.Refresh,
                    values = result.replace
                )
            }

            item {
                DiagnoseListCard(
                    title = "Upgrade",
                    icon = Icons.Default.Build,
                    values = result.upgrade
                )
            }

            if (result.lifestyleQuestions.isNotEmpty()) {
                item {
                    Text(
                        "Personalization",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    DiagnoseListCard(
                        title = "Questions that can improve future recommendations",
                        icon = Icons.Default.Person,
                        values = result.lifestyleQuestions
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        diagnosis = null
                        error = null
                        fixedResultUrl = null
                        fixedProblemTitle = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Analyze Another Room")
                }
            }
        }
    }
}

@Composable
fun DiagnoseMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DiagnoseListCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    values: List<String>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (values.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("No items identified.")
            } else {
                values.forEach { value ->
                    Spacer(Modifier.height(8.dp))
                    Text("• $value")
                }
            }
        }
    }
}

@Composable
fun ToolPage(
    title: String,
    subtitle: String,
    operation: String,
    options: List<ToolOption>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var resultUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imageUri = uri
        resultUrl = null
        error = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(subtitle)
        }

        item {
            if (imageUri == null) {
                OutlinedCard(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Add Room Photo",
                            fontWeight = FontWeight.Bold
                        )
                        Text("JPG, PNG or WEBP")
                    }
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Room",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )

                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Photo")
                }
            }
        }

        item {
            Text(
                "Choose an option",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(options) { option ->
            ElevatedCard(
                onClick = {
                    selected = option.title
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            option.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(option.description)
                    }

                    if (selected == option.title) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            null
                        )
                    }
                }
            }
        }

        item {
            Button(
                enabled = imageUri != null &&
                        selected != null &&
                        !loading,
                onClick = {
                    val uri = imageUri ?: return@Button
                    val choice = selected ?: return@Button

                    scope.launch {
                        loading = true
                        error = null
                        resultUrl = null

                        try {
                            val style =
                                if (operation == "generate") choice else "Modern"

                            val url = generateDesign(
                                context = context,
                                uri = uri,
                                room = "Living Room",
                                style = style,
                                userPrompt = "",
                                operation = operation,
                                selection = choice
                            )

                            resultUrl = url

                            saveDesign(
                                context,
                                url,
                                "Living Room",
                                choice
                            )
                        } catch (e: Exception) {
                            error = e.message ?: "Operation failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Processing...")
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Run AI")
                }
            }
        }

        item {
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            resultUrl?.let { url ->
                var showFullScreen by remember { mutableStateOf(false) }
                var showBefore by remember { mutableStateOf(false) }

                Text(
                    "AI Result",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullScreen = true },
                    shape = RoundedCornerShape(22.dp)
                ) {
                    AsyncImage(
                        model = if (showBefore) imageUri else url,
                        contentDescription = if (showBefore) "Original room" else "Generated design",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showBefore = !showBefore },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Compare, null)
                        Spacer(Modifier.width(5.dp))
                        Text(if (showBefore) "After" else "Before")
                    }

                    OutlinedButton(
                        onClick = { shareDesign(context, url) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Share")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Saved to My Designs",
                    color = MaterialTheme.colorScheme.primary
                )

                if (showFullScreen) {
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        offsetX += panChange.x
                        offsetY += panChange.y
                    }

                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = {
                            showFullScreen = false
                        },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            AsyncImage(
                                model = if (showBefore) imageUri else url,
                                contentDescription = "Full screen design",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    }
                                    .transformable(transformState),
                                contentScale = ContentScale.Fit
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        showFullScreen = false
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close"
                                    )
                                }

                                Text(
                                    if (showBefore) "Original Room" else "Solve my room",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Reset zoom"
                                    )
                                }
                            }

                            Text(
                                "Pinch to zoom • Drag to move",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        }
    }

    @Composable
fun Styles() {
    ToolPage(
        title = "Solve my room",
        subtitle = "Choose a style and let AI redesign your room.",
        operation = "generate",
        options = listOf(
            ToolOption("Modern", "Clean lines and contemporary furniture."),
            ToolOption("Minimalist", "Simple forms and uncluttered spaces."),
            ToolOption("Luxury", "Premium materials and elegant details."),
            ToolOption("Scandinavian", "Bright, natural and comfortable interiors."),
            ToolOption("Industrial", "Raw materials and urban character."),
            ToolOption("Classic", "Timeless furniture and refined details."),
            ToolOption("Bohemian", "Layered textures and expressive decoration."),
            ToolOption("Japandi", "Japanese simplicity with Scandinavian warmth.")
        )
    )
}

@Composable
fun Enhance() {
    ToolPage(
        title = "Fix a room problem",
        subtitle = "Improve your existing room image with AI.",
        operation = "enhance",
        options = listOf(
            ToolOption("Improve Lighting", "Create more natural and balanced lighting."),
            ToolOption("Increase Realism", "Improve materials, shadows and realism."),
            ToolOption("Fix Details", "Clean visual artifacts and small details."),
            ToolOption("Improve Colors", "Balance colors while preserving the design."),
            ToolOption("Sharpen Image", "Improve perceived image clarity.")
        )
    )
}

@Composable
fun Furniture() {
    ToolPage(
        title = "Furniture",
        subtitle = "Redesign furniture in your existing room.",
        operation = "furniture",
        options = listOf(
            ToolOption("Sofa", "Living-room seating concepts."),
            ToolOption("Bed", "Bedroom centerpiece concepts."),
            ToolOption("Table", "Dining and coffee tables."),
            ToolOption("Chair", "Dining and accent seating."),
            ToolOption("Storage", "Cabinets and organization."),
            ToolOption("Lighting", "Ambient and decorative lighting.")
        )
    )
}

@Composable
fun Products() {
    ToolPage(
        title = "Products",
        subtitle = "Add coordinated products to your interior.",
        operation = "products",
        options = listOf(
            ToolOption("Sofas", "Sofa ideas for different interior styles."),
            ToolOption("Beds", "Bedroom furniture concepts."),
            ToolOption("Tables", "Dining and living-room tables."),
            ToolOption("Chairs", "Seating products and concepts."),
            ToolOption("Lighting", "Lighting products and ideas."),
            ToolOption("Decor", "Decorative finishing touches.")
        )
    )
}

@Composable
fun Menu(
    dark: Boolean,
    setDark: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }


        item {
            val context = LocalContext.current
            val savedEmail =
                context.getSharedPreferences(
                    ROOMAI_PLAN_PREFS,
                    Context.MODE_PRIVATE
                ).getString(
                    "account_email",
                    ""
                ).orEmpty()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Account",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                if (savedEmail.isBlank())
                                    "Guest • Login to save your designs"
                                else
                                    savedEmail,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onLogout
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Log out")
                    }
                }
            }
        }

        item {
            ListItem(
                headlineContent = {
                    Text("Dark Mode")
                },
                leadingContent = {
                    Icon(Icons.Default.DarkMode, null)
                },
                trailingContent = {
                    Switch(
                        checked = dark,
                        onCheckedChange = setDark
                    )
                }
            )
        }

        item {
            ListItem(
                headlineContent = {
                    Text("AI Interior Designer")
                },
                supportingContent = {
                    Text("RoomAI")
                },
                leadingContent = {
                    Icon(Icons.Default.AutoAwesome, null)
                }
            )
        }

        item {
            ListItem(
                headlineContent = {
                    Text("Design Library")
                },
                supportingContent = {
                    Text("Generated designs are stored locally.")
                },
                leadingContent = {
                    Icon(Icons.Default.PhotoLibrary, null)
                }
            )
        }
    }
}
