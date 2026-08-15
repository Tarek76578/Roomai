package com.roomai.app

import android.os.Bundle
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roomai.app.ui.RoomAITheme

private const val BACKEND_URL = "https://roomai-wagl.onrender.com/generate"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var dark by remember { mutableStateOf(false) }

            RoomAITheme(dark) {
                App(dark = dark, setDark = { dark = it })
            }
        }
    }
}

@Composable
fun App(
    dark: Boolean,
    setDark: (Boolean) -> Unit
) {
    val nav = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavItem(nav, "home", "Home", Icons.Default.Home)
                NavItem(nav, "create", "Create", Icons.Default.Add)
                NavItem(nav, "designs", "Designs", Icons.Default.PhotoLibrary)
                NavItem(nav, "menu", "Menu", Icons.Default.Menu)
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

            composable("menu") {
                Menu(dark, setDark)
            }

            composable("styles") {
                Page("AI Styles")
            }

            composable("enhance") {
                Page("AI Enhance")
            }

            composable("furniture") {
                Page("Furniture")
            }

            composable("products") {
                Page("Products")
            }
        }
    }
}

@Composable
fun NavItem(
    nav: NavHostController,
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    TextButton(
        onClick = {
            nav.navigate(route) {
                launchSingleTop = true
            }
        },
        modifier = Modifier
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label)
            Text(label)
        }
    }
}

@Composable
fun Home(nav: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "RoomAI",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "AI Interior Designer",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(30.dp))

        ElevatedCard(
            onClick = {
                nav.navigate("create")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Design your room",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text("Transform your room with AI")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Explore",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Styles",
                icon = Icons.Default.Palette,
                modifier = Modifier,
                onClick = {
                    nav.navigate("styles")
                }
            )

            FeatureCard(
                title = "Enhance",
                icon = Icons.Default.AutoFixHigh,
                modifier = Modifier,
                onClick = {
                    nav.navigate("enhance")
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Furniture",
                icon = Icons.Default.Chair,
                modifier = Modifier,
                onClick = {
                    nav.navigate("furniture")
                }
            )

            FeatureCard(
                title = "Products",
                icon = Icons.Default.ShoppingBag,
                modifier = Modifier,
                onClick = {
                    nav.navigate("products")
                }
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = title
            )

            Spacer(Modifier.height(10.dp))

            Text(title)
        }
    }
}

@Composable
fun Page(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "RoomAI feature",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun Create() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var room by remember { mutableStateOf("Living Room") }
    var style by remember { mutableStateOf("Modern") }
    var prompt by remember { mutableStateOf("") }
    var resultUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
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
                "Create",
                style = MaterialTheme.typography.headlineLarge
            )
            Text("Create a new AI interior design")
        }

        item {
            if (imageUri == null) {
                Button(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Room Photo")
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Room photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
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
            Text("Room", style = MaterialTheme.typography.titleMedium)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "Living Room",
                        "Bedroom",
                        "Kitchen",
                        "Office",
                        "Dining Room"
                    )
                ) { value ->
                    FilterChip(
                        selected = room == value,
                        onClick = { room = value },
                        label = { Text(value) }
                    )
                }
            }
        }

        item {
            Text("Style", style = MaterialTheme.typography.titleMedium)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "Modern",
                        "Minimalist",
                        "Luxury",
                        "Scandinavian",
                        "Industrial",
                        "Classic",
                        "Bohemian",
                        "Japandi"
                    )
                ) { value ->
                    FilterChip(
                        selected = style == value,
                        onClick = { style = value },
                        label = { Text(value) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Additional instructions") },
                placeholder = {
                    Text("Example: add warm lighting and a large sofa")
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
                            resultUrl = generateDesign(
                                context,
                                uri,
                                room,
                                style,
                                prompt
                            )
                        } catch (e: Exception) {
                            error = e.message ?: "Generation failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
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
                    style = MaterialTheme.typography.headlineSmall
                )

                AsyncImage(
                    model = url,
                    contentDescription = "Generated design",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
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
    prompt: String
): String = withContext(Dispatchers.IO) {

    val boundary = "RoomAI-${UUID.randomUUID()}"

    val connection =
        URL(BACKEND_URL).openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.connectTimeout = 30000
    connection.readTimeout = 180000

    connection.setRequestProperty(
        "Content-Type",
        "multipart/form-data; boundary=$boundary"
    )

    DataOutputStream(connection.outputStream).use { output ->

        writeTextPart(output, boundary, "room", room)
        writeTextPart(output, boundary, "style", style)
        writeTextPart(output, boundary, "prompt", prompt)

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw Exception("Could not read image")

        output.write("--$boundary\r\n".toByteArray())
        output.write(
            "Content-Disposition: form-data; name=\"image\"; filename=\"room.jpg\"\r\n"
                .toByteArray()
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
        if (code in 200..299) connection.inputStream
        else connection.errorStream

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
        "Content-Disposition: form-data; name=\"$name\"\r\n\r\n"
            .toByteArray()
    )
    output.write(value.toByteArray())
    output.write("\r\n".toByteArray())
}

@Composable
fun Designs() {
    Page("My Designs")
}

@Composable
fun Menu(
    dark: Boolean,
    setDark: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "Menu",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(20.dp))

        ListItem(
            headlineContent = {
                Text("AI Styles")
            },
            leadingContent = {
                Icon(Icons.Default.Palette, null)
            }
        )

        ListItem(
            headlineContent = {
                Text("AI Enhance")
            },
            leadingContent = {
                Icon(Icons.Default.AutoFixHigh, null)
            }
        )

        ListItem(
            headlineContent = {
                Text("Furniture")
            },
            leadingContent = {
                Icon(Icons.Default.Chair, null)
            }
        )

        ListItem(
            headlineContent = {
                Text("Products")
            },
            leadingContent = {
                Icon(Icons.Default.ShoppingBag, null)
            }
        )

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
}
