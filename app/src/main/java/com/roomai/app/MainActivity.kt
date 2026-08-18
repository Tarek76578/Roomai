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
    val context = LocalContext.current

    fun openProblem(problem: String) {
        RoomAIProblemFlow.select(problem)

        nav.navigate("problem_first") {
            launchSingleTop = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {

        // =========================================================
        // HEADER
        // =========================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "RoomAI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Solve the room. Then improve it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    nav.navigate("menu") {
                        launchSingleTop = true
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // =========================================================
        // PRIMARY PROBLEM ENTRY
        // =========================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nav.navigate("problem_first") {
                        launchSingleTop = true
                    }
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "What is wrong with your room?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "RoomAI analyzes the evidence, identifies the most important problems and builds a practical solution.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Analyze my room")
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // =========================================================
        // COMMON PROBLEMS
        // =========================================================

        Text(
            text = "What do you want to improve?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Choose the problem that matters most. RoomAI will use it to prioritize the diagnosis.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoomAIProblemQuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Dashboard,
                title = "Too crowded",
                description = "Use space better"
            ) {
                openProblem(RoomAIProblemFlow.SPACE)
            }

            RoomAIProblemQuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LightMode,
                title = "Bad lighting",
                description = "Improve light"
            ) {
                openProblem(RoomAIProblemFlow.LIGHTING)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoomAIProblemQuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Checkroom,
                title = "Furniture",
                description = "Keep what works"
            ) {
                openProblem(RoomAIProblemFlow.EXISTING_FURNITURE)
            }

            RoomAIProblemQuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                title = "Low budget",
                description = "Spend less"
            ) {
                openProblem(RoomAIProblemFlow.BUDGET)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoomAIProblemQuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShoppingBag,
                title = "Don't know what to buy",
                description = "Build a shopping plan"
            ) {
                openProblem(RoomAIProblemFlow.SHOPPING)
            }

            RoomAIProblemQuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoAwesome,
                title = "Something else",
                description = "Describe my problem"
            ) {
                openProblem(RoomAIProblemFlow.SPECIFIC_CHANGE)
            }
        }

        Spacer(Modifier.height(28.dp))

        // =========================================================
        // HOW ROOMAI SOLVES THE PROBLEM
        // =========================================================

        Text(
            text = "How RoomAI works",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        RoomAIStepCard(
            number = "1",
            title = "Understand",
            description = "You tell RoomAI what you want to fix and provide the room evidence."
        )

        Spacer(Modifier.height(8.dp))

        RoomAIStepCard(
            number = "2",
            title = "Diagnose",
            description = "AI separates visible evidence from assumptions and ranks the real problems."
        )

        Spacer(Modifier.height(8.dp))

        RoomAIStepCard(
            number = "3",
            title = "Solve",
            description = "You get prioritized actions, constraints, trade-offs and practical next steps."
        )

        Spacer(Modifier.height(8.dp))

        RoomAIStepCard(
            number = "4",
            title = "Visualize",
            description = "Only after the solution is clear do we generate a visual concept."
        )

        Spacer(Modifier.height(28.dp))

        // =========================================================
        // SECONDARY TOOLS
        // =========================================================
    }
}
