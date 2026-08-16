package com.roomai.app

import android.util.Log
import com.roomai.app.ui.RoomAITheme
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

private const val BACKEND_URL =
    "https://roomai-wagl.onrender.com/generate"

private const val DIAGNOSE_URL =
    "https://roomai-wagl.onrender.com/diagnose"

private const val PREFS = "roomai_designs"

data class SavedDesign(
    val id: String,
    val url: String,
    val room: String,
    val style: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            composable("home") {
                Home(nav)
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

            composable("diagnose") {
                Diagnose()
            }

            composable("menu") {
                Menu(dark, setDark)
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
fun Home(nav: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            StepCard("3", "Generate", "RoomAI sends the request to the AI backend.")
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
                "AI Styles",
                Icons.Default.Palette,
                Modifier.weight(1f)
            ) {
                nav.navigate("styles")
            }

            SmallFeature(
                "AI Enhance",
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
                    Text("Generate Design")
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

    DataOutputStream(connection.outputStream).use { output ->

        writeTextPart(output, boundary, "room", room)
        writeTextPart(output, boundary, "style", style)
        writeTextPart(output, boundary, "operation", operation)
        writeTextPart(output, boundary, "selection", selection)
        writeTextPart(output, boundary, "prompt", userPrompt)

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
        throw Exception(response)
    }

    Regex("\"image_url\"\\s*:\\s*\"([^\"]+)\"")
        .find(response)
        ?.groupValues
        ?.get(1)
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

    DataOutputStream(connection.outputStream).use { output ->

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

    var designs by remember {
        mutableStateOf(loadDesigns(context))
    }

    var preview by remember {
        mutableStateOf<String?>(null)
    }

    preview?.let { url ->
        AlertDialog(
            onDismissRequest = {
                preview = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareDesign(context, url)
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        preview = null
                    }
                ) {
                    Text("Close")
                }
            },
            title = {
                Text("RoomAI Design")
            },
            text = {
                AsyncImage(
                    model = url,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp),
                    contentScale = ContentScale.Crop
                )
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "My Designs",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "${designs.size} saved design" +
                        if (designs.size == 1) "" else "s"
            )
        }

        if (designs.isEmpty()) {
            item {
                EmptyLibrary()
            }
        } else {
            items(
                designs,
                key = { it.id }
            ) { design ->

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = design.url,
                            contentDescription = "Saved design",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                design.style,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(design.room)

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        preview = design.url
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Fullscreen,
                                        null
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("View")
                                }

                                OutlinedButton(
                                    onClick = {
                                        shareDesign(
                                            context,
                                            design.url
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        null
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("Share")
                                }

                                IconButton(
                                    onClick = {
                                        deleteDesign(
                                            context,
                                            design.id
                                        )
                                        designs =
                                            loadDesigns(context)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Delete"
                                    )
                                }
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

            Text(
                "Find practical problems before spending money on your room."
            )
        }

        item {
            if (imageUri == null) {
                OutlinedCard(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Add Room Photo",
                            fontWeight = FontWeight.Bold
                        )

                        Text("Use a clear photo of the whole room")
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

                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
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

                        try {
                            diagnosis = diagnoseRoom(
                                context,
                                uri
                            )
                        } catch (e: Exception) {
                            error =
                                e.message ?: "Diagnosis failed"
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
                        Icons.Default.Search,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Analyze Room")
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
                        Text(
                            "Analysis Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(message)
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
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Room Score",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "${result.score}/100",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            result.summary
                        )
                    }
                }
            }

            if (result.problems.isNotEmpty()) {
                item {
                    Text(
                        "Problems",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(result.problems) { problem ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                problem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                "Severity: ${problem.severity.uppercase()}",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(problem.reason)

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "Recommendation: ${problem.recommendation}"
                            )

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
                                                "Living Room",
                                                "Fix: ${problem.title}"
                                            )
                                        } catch (e: Exception) {
                                            error =
                                                e.message ?: "Could not fix this problem"
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

                            if (fixedProblemTitle == problem.title && fixedResultUrl != null) {
                                Spacer(Modifier.height(14.dp))

                                Text(
                                    "Fixed Result",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                AsyncImage(
                                    model = fixedResultUrl,
                                    contentDescription = "Fixed room result",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            if (result.risks.isNotEmpty()) {
                item {
                    Text(
                        "Risk Scanner",
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
                            Text(
                                risk.type.replaceFirstChar {
                                    it.uppercase()
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                "Severity: ${risk.severity.uppercase()}",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(risk.message)
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
                        "Lifestyle Questions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    DiagnoseListCard(
                        title = "Help RoomAI understand your lifestyle",
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
                                    if (showBefore) "Original Room" else "AI Design",
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
        title = "AI Styles",
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
        title = "AI Enhance",
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
    setDark: (Boolean) -> Unit
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
