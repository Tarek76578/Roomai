package com.roomai.app

import android.content.Context
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

private const val PREFS =
    "roomai_designs"

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
                onClick = {
                    nav.navigate("create")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Design your room",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Upload a room photo and let AI redesign it."
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            nav.navigate("create")
                        }
                    ) {
                        Text("Start Designing")
                    }
                }
            }
        }

        item {
            Text(
                "Explore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            FeatureGrid(nav)
        }

        item {
            Spacer(Modifier.height(20.dp))

            Text(
                "How it works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            StepCard(
                "1",
                "Upload",
                "Choose a photo of your room."
            )

            StepCard(
                "2",
                "Customize",
                "Choose room type, style and instructions."
            )

            StepCard(
                "3",
                "Generate",
                "RoomAI creates a photorealistic design."
            )
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
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Icon(
                icon,
                contentDescription = title
            )

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
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(description)
            }
        }
    }
}

@Composable
fun Create() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var room by remember {
        mutableStateOf("Living Room")
    }

    var style by remember {
        mutableStateOf("Modern")
    }

    var prompt by remember {
        mutableStateOf("")
    }

    var resultUrl by remember {
        mutableStateOf<String?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

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

            Text(
                "Turn your room into a professional interior."
            )
        }

        item {

            if (imageUri == null) {

                OutlinedCard(
                    onClick = {
                        picker.launch("image/*")
                    },
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
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
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
                        .height(260.dp),
                    contentScale = ContentScale.Crop
                )

                OutlinedButton(
                    onClick = {
                        picker.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Photo")
                }
            }
        }

        item {
            SelectionSection(
                title = "Room",
                values = listOf(
                    "Living Room",
                    "Bedroom",
                    "Kitchen",
                    "Office",
                    "Dining Room"
                ),
                selected = room,
                onSelect = {
                    room = it
                }
            )
        }

        item {
            SelectionSection(
                title = "Style",
                values = listOf(
                    "Modern",
                    "Minimalist",
                    "Luxury",
                    "Scandinavian",
                    "Industrial",
                    "Classic",
                    "Bohemian",
                    "Japandi"
                ),
                selected = style,
                onSelect = {
                    style = it
                }
            )
        }

        item {

            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Additional instructions")
                },
                placeholder = {
                    Text(
                        "Example: large sofa, warm lighting, plants..."
                    )
                },
                minLines = 4
            )
        }

        item {

            Button(
                enabled = imageUri != null && !loading,
                onClick = {

                    val uri =
                        imageUri ?: return@Button

                    scope.launch {

                        loading = true
                        error = null
                        resultUrl = null

                        try {

                            val url =
                                generateDesign(
                                    context,
                                    uri,
                                    room,
                                    style,
                                    prompt
                                )

                            resultUrl = url

                            saveDesign(
                                context,
                                url
                            )

                        } catch (e: Exception) {

                            error =
                                e.message
                                    ?: "Generation failed"

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

                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null
                    )

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
                        .height(350.dp),
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
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(values) {

                FilterChip(
                    selected = selected == it,
                    onClick = {
                        onSelect(it)
                    },
                    label = {
                        Text(it)
                    }
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

    val boundary =
        "RoomAI-${UUID.randomUUID()}"

    val connection =
        URL(BACKEND_URL)
            .openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.connectTimeout = 30000
    connection.readTimeout = 300000

    connection.setRequestProperty(
        "Content-Type",
        "multipart/form-data; boundary=$boundary"
    )

    DataOutputStream(
        connection.outputStream
    ).use { output ->

        writeTextPart(
            output,
            boundary,
            "room",
            room
        )

        writeTextPart(
            output,
            boundary,
            "style",
            style
        )

        writeTextPart(
            output,
            boundary,
            "prompt",
            userPrompt
        )

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use {
                    it.readBytes()
                }
                ?: throw Exception(
                    "Could not read selected image"
                )

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

        output.write(
            "\r\n".toByteArray()
        )

        output.write(
            "--$boundary--\r\n".toByteArray()
        )
    }

    val code =
        connection.responseCode

    val stream =
        if (code in 200..299)
            connection.inputStream
        else
            connection.errorStream

    val response =
        stream
            ?.bufferedReader()
            ?.use {
                it.readText()
            }
            ?: throw Exception(
                "Empty backend response"
            )

    if (code !in 200..299) {
        throw Exception(response)
    }

    Regex(
        "\"image_url\"\\s*:\\s*\"([^\"]+)\""
    )
        .find(response)
        ?.groupValues
        ?.get(1)
        ?: throw Exception(
            "Backend returned no image URL"
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
        "Content-Disposition: form-data; " +
                "name=\"$name\"\r\n\r\n"
            .toByteArray()
    )

    output.write(
        value.toByteArray()
    )

    output.write(
        "\r\n".toByteArray()
    )
}

fun saveDesign(
    context: Context,
    url: String
) {

    val prefs =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    val old =
        prefs.getStringSet(
            "urls",
            emptySet()
        )?.toMutableSet()
            ?: mutableSetOf()

    old.add(url)

    prefs.edit()
        .putStringSet(
            "urls",
            old
        )
        .apply()
}

fun loadDesigns(
    context: Context
): List<String> {

    val prefs =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    return prefs.getStringSet(
        "urls",
        emptySet()
    )?.toList()
        ?.reversed()
        ?: emptyList()
}

fun deleteDesign(
    context: Context,
    url: String
) {

    val prefs =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    val urls =
        prefs.getStringSet(
            "urls",
            emptySet()
        )?.toMutableSet()
            ?: mutableSetOf()

    urls.remove(url)

    prefs.edit()
        .putStringSet("urls", urls)
        .apply()
}

@Composable
fun Designs() {

    val context = LocalContext.current

    var designs by remember {
        mutableStateOf(
            loadDesigns(context)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                "My Designs",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your AI interior designs"
            )
        }

        if (designs.isEmpty()) {

            item {

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "No designs yet",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Create your first AI design."
                        )
                    }
                }
            }

        } else {

            items(designs) { url ->

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {

                    Column {

                        AsyncImage(
                            model = url,
                            contentDescription = "Design",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentScale =
                                ContentScale.Crop
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                "AI Interior",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            IconButton(
                                onClick = {

                                    deleteDesign(
                                        context,
                                        url
                                    )

                                    designs =
                                        loadDesigns(
                                            context
                                        )
                                }
                            ) {

                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription =
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

@Composable
fun Styles() {

    FeaturePage(
        "AI Styles",
        "Choose an interior style",
        listOf(
            "Modern" to Icons.Default.Home,
            "Minimalist" to Icons.Default.CropSquare,
            "Luxury" to Icons.Default.Star,
            "Scandinavian" to Icons.Default.Nature,
            "Industrial" to Icons.Default.Build,
            "Classic" to Icons.Default.AutoAwesome,
            "Bohemian" to Icons.Default.LocalFlorist,
            "Japandi" to Icons.Default.Spa
        )
    )
}

@Composable
fun Enhance() {

    FeaturePage(
        "AI Enhance",
        "Improve your generated design",
        listOf(
            "Improve lighting" to Icons.Default.LightMode,
            "Increase realism" to Icons.Default.AutoAwesome,
            "Fix details" to Icons.Default.AutoFixHigh,
            "Improve colors" to Icons.Default.Palette,
            "Sharpen image" to Icons.Default.ZoomIn
        )
    )
}

@Composable
fun Furniture() {

    FeaturePage(
        "Furniture",
        "Explore furniture ideas",
        listOf(
            "Sofa" to Icons.Default.Home,
            "Bed" to Icons.Default.BedroomParent,
            "Table" to Icons.Default.TableBar,
            "Chair" to Icons.Default.Chair,
            "Storage" to Icons.Default.Inventory2,
            "Lighting" to Icons.Default.Light
        )
    )
}

@Composable
fun Products() {

    FeaturePage(
        "Products",
        "Discover products for your interiors",
        listOf(
            "Sofas" to Icons.Default.Home,
            "Beds" to Icons.Default.BedroomParent,
            "Tables" to Icons.Default.TableBar,
            "Chairs" to Icons.Default.Chair,
            "Lighting" to Icons.Default.Light,
            "Decor" to Icons.Default.LocalFlorist
        )
    )
}

@Composable
fun FeaturePage(
    title: String,
    subtitle: String,
    options: List<
            Pair<
                    String,
                    androidx.compose.ui.graphics.vector.ImageVector
                    >
            >
) {

    var selected by remember {
        mutableStateOf<String?>(null)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {

            Text(
                title,
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Text(subtitle)
        }

        items(options) { option ->

            ElevatedCard(
                onClick = {
                    selected = option.first
                },
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        option.second,
                        contentDescription =
                            option.first,
                        modifier =
                            Modifier.size(32.dp)
                    )

                    Spacer(
                        Modifier.width(16.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            option.first,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            "Select this option"
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

            selected?.let {

                Text(
                    "Selected: $it",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
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
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        item {

            Text(
                "Menu",
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )
        }

        item {

            ListItem(
                headlineContent = {
                    Text("AI Styles")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Palette,
                        null
                    )
                }
            )
        }

        item {

            ListItem(
                headlineContent = {
                    Text("AI Enhance")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        null
                    )
                }
            )
        }

        item {

            ListItem(
                headlineContent = {
                    Text("Furniture")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Chair,
                        null
                    )
                }
            )
        }

        item {

            ListItem(
                headlineContent = {
                    Text("Products")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.ShoppingBag,
                        null
                    )
                }
            )
        }

        item {

            ListItem(
                headlineContent = {
                    Text("Dark Mode")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.DarkMode,
                        null
                    )
                },
                trailingContent = {

                    Switch(
                        checked = dark,
                        onCheckedChange =
                            setDark
                    )
                }
            )
        }
    }
}
