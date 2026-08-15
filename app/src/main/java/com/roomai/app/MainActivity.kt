package com.roomai.app

import com.roomai.app.ui.RoomAITheme
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

private const val BACKEND_URL =
    "https://roomai-wagl.onrender.com/generate"

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

            composable("menu") {
                Menu(dark, setDark)
            }
        }
    }
}

@Composable
fun BottomItem(
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

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
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
                    onClick = { picker.launch("image/*") },
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
                    onClick = { picker.launch("image/*") },
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
    userPrompt: String
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
            "Content-Disposition: form-data; " +
                    "name=\"image\"; " +
                    "filename=\"room.jpg\"\r\n"
                .toByteArray()
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
    output.write(
        "--$boundary\r\n".toByteArray()
    )

    output.write(
        "Content-Disposition: form-data; " +
                "name=\"$name\"\r\n\r\n"
            .toByteArray()
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
fun ToolPage(
    title: String,
    subtitle: String,
    options: List<ToolOption>
) {
    var selected by remember {
        mutableStateOf<String?>(null)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(subtitle)
        }

        items(options) { option ->
            ElevatedCard(
                onClick = {
                    selected = option.title
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            option.title,
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(option.description)
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        null
                    )
                }
            }
        }

        item {
            selected?.let {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Selected: $it",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Styles() {
    ToolPage(
        "AI Styles",
        "Choose the visual direction for your next room.",
        listOf(
            ToolOption(
                "Modern",
                "Clean lines and contemporary furniture."
            ),
            ToolOption(
                "Minimalist",
                "Simple forms and uncluttered spaces."
            ),
            ToolOption(
                "Luxury",
                "Premium materials and elegant details."
            ),
            ToolOption(
                "Scandinavian",
                "Bright, natural and comfortable interiors."
            ),
            ToolOption(
                "Industrial",
                "Raw materials and urban character."
            ),
            ToolOption(
                "Classic",
                "Timeless furniture and refined details."
            ),
            ToolOption(
                "Bohemian",
                "Layered textures and expressive decoration."
            ),
            ToolOption(
                "Japandi",
                "Japanese simplicity with Scandinavian warmth."
            )
        )
    )
}

@Composable
fun Enhance() {
    ToolPage(
        "AI Enhance",
        "Choose an enhancement direction for AI image processing.",
        listOf(
            ToolOption(
                "Improve Lighting",
                "Create more natural and balanced lighting."
            ),
            ToolOption(
                "Increase Realism",
                "Improve materials, shadows and realism."
            ),
            ToolOption(
                "Fix Details",
                "Clean visual artifacts and small details."
            ),
            ToolOption(
                "Improve Colors",
                "Balance colors while preserving the design."
            ),
            ToolOption(
                "Sharpen Image",
                "Improve perceived image clarity."
            )
        )
    )
}

@Composable
fun Furniture() {
    ToolPage(
        "Furniture",
        "Explore furniture concepts for your room.",
        listOf(
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
        "Products",
        "Explore product categories inspired by your designs.",
        listOf(
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
