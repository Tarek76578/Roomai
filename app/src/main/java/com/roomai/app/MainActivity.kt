package com.roomai.app

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
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

            context.getSharedPreferences(
                ROOMAI_PLAN_PREFS,
                Context.MODE_PRIVATE
            ).edit()
                .remove("account_email")
                .putString(ROOMAI_PLAN_KEY, "free")
                .apply()
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
    )?.lowercase()
        ?.let {
            if (it == "pro") "pro" else "free"
        }
        ?: "free"
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

            context.getSharedPreferences(
                ROOMAI_PLAN_PREFS,
                Context.MODE_PRIVATE
            ).edit()
                .putString(
                    ROOMAI_PLAN_KEY,
                    usage.plan
                )
                .apply()

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

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        "home",
        "create",
        "designs",
        "menu"
    )

    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomItem(
                        nav = nav,
                        route = "home",
                        label = "Home",
                        icon = Icons.Default.Home
                    )

                    BottomItem(
                        nav = nav,
                        route = "create",
                        label = "Create",
                        icon = Icons.Default.Add
                    )

                    BottomItem(
                        nav = nav,
                        route = "designs",
                        label = "Designs",
                        icon = Icons.Default.PhotoLibrary
                    )

                    BottomItem(
                        nav = nav,
                        route = "menu",
                        label = "Menu",
                        icon = Icons.Default.Menu
                    )
                }
            }
        }
    ) { padding ->

        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {

            // ----------------------------------------------------
            // Authentication
            // ----------------------------------------------------

            composable("auth") {
                AuthScreen(
                    dark = dark,
                    onAuthenticated = { newToken ->
                        saveRoomAiToken(context, newToken)
                        token = newToken

                        nav.navigate("home") {
                            popUpTo("auth") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ----------------------------------------------------
            // REAL HOME
            // ----------------------------------------------------

            composable("home") {
                RoomAIHomeRedesigned(
                    nav = nav,
                    loggedIn = token.isNotBlank(),
                    usage = usage
                )
            }

            // ----------------------------------------------------
            // PROBLEM-FIRST FLOW
            //
            // This is no longer the app's root.
            // It is entered deliberately from Home.
            // ----------------------------------------------------

            composable("problem_first") {
                RoomAIProblemFirstScreen(
                    onContinueToDiagnosis = {
                        nav.navigate("decision_engine") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ----------------------------------------------------
            // GROWTH
            // ----------------------------------------------------

            composable("growth") {
                RoomAIGrowthCenter(
                    onBack = {
                        nav.popBackStack()
                    },
                    onCreate = {
                        nav.navigate("create") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ----------------------------------------------------
            // TOP LEVEL: CREATE
            // ----------------------------------------------------

            composable("create") {
                Create()
            }

            // ----------------------------------------------------
            // TOP LEVEL: DESIGNS
            // ----------------------------------------------------

            composable("designs") {
                Designs()
            }

            // ----------------------------------------------------
            // SECONDARY TOOLS
            // ----------------------------------------------------

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

            // ----------------------------------------------------
            // ROOM SOLVING FLOW
            // ----------------------------------------------------

            composable("decision_engine") {
                RoomAIDecisionEngine()
            }

            composable("precision") {
                RoomAIPrecision()
            }

            composable("room_memory") {
                RoomAIMemory()
            }

            composable("diagnose") {
                Diagnose()
            }

            composable("professional") {
                RoomAIProfessionalHome(nav)
            }

            // ----------------------------------------------------
            // Legacy studio route kept for compatibility.
            // It is NOT a Home route.
            // ----------------------------------------------------

            composable("legacy_ai_studio") {
                RoomAIPowerStudio()
            }

            // ----------------------------------------------------
            // TOP LEVEL: MENU
            // ----------------------------------------------------

            composable("menu") {
                Menu(
                    dark = dark,
                    setDark = setDark,
                    onLogout = {
                        scope.launch {
                            roomAiLogout(context)
                            token = ""

                            nav.navigate("home") {
                                popUpTo("home") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun roomAISafeBack(
    navController: NavController
) {
    if (!navController.popBackStack()) {
        navController.navigate("home") {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }
}

@Composable
fun RoomAIHomeRedesigned(
    nav: NavHostController,
    loggedIn: Boolean = false,
    usage: RoomAIUsage = RoomAIUsage()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Text(
            text = "RoomAI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Solve the problem before you spend money.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // =====================================================
        // ONE PRIMARY USER ACTION
        // =====================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nav.navigate("problem_first") {
                        launchSingleTop = true
                    }
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Analyze my room",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Take a photo and tell RoomAI what you want to improve. We will diagnose the room and guide you to the next step.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        nav.navigate("problem_first") {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Start")
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // =====================================================
        // SIMPLE EXPLANATION — NO EXTRA ACTIONS
        // =====================================================

        Text(
            text = "What happens next?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "1. Add a photo\n2. Tell us what matters most\n3. Get a diagnosis\n4. Decide what to do",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )

        Spacer(Modifier.height(28.dp))

        // =====================================================
        // NO DUPLICATE HOME BUTTONS
        //
        // Create / Designs / Menu already exist in the
        // application's bottom navigation.
        // =====================================================

        Text(
            text = "Your other areas are available from the navigation bar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
    }
}


// Recovered historical domain contract.
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


// Recovered historical domain contract.
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
