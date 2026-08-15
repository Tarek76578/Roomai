package com.roomai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

import com.roomai.app.ui.RoomAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var dark by remember { mutableStateOf(false) }

            RoomAITheme(dark) {
                App(dark) { dark = it }
            }
        }
    }
}

@Composable
fun App(dark: Boolean, setDark: (Boolean) -> Unit) {
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
            composable("home") { Home(nav) }
            composable("create") { Create() }
            composable("designs") { Designs() }
            composable("menu") { Menu(dark, setDark) }
            composable("styles") { Page("Styles") }
            composable("enhance") { Page("AI Enhance") }
            composable("furniture") { Page("Furniture") }
            composable("products") { Page("Products") }
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
    androidx.compose.material3.NavigationBarItem(
        selected = false,
        onClick = { nav.navigate(route) },
        icon = { Icon(icon, null) },
        label = { Text(label) }
    )
}

@Composable
fun Home(nav: NavHostController) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("RoomAI", style = MaterialTheme.typography.headlineLarge)
        Text("AI Interior Designer")

        Spacer(Modifier.height(30.dp))

        ElevatedCard(
            onClick = { nav.navigate("create") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Design your room",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("Transform your room with AI")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Explore", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                "Styles",
                Icons.Default.Palette
            , Modifier.weight(1f)) { nav.navigate("styles") }

            FeatureCard(
                "Enhance",
                Icons.Default.AutoFixHigh
            , Modifier.weight(1f)) { nav.navigate("enhance") }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                "Furniture",
                Icons.Default.Chair
            , Modifier.weight(1f)) { nav.navigate("furniture") }

            FeatureCard(
                "Products",
                Icons.Default.ShoppingBag
            , Modifier.weight(1f)) { nav.navigate("products") }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(120.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null)
            Spacer(Modifier.height(10.dp))
            Text(title)
        }
    }
}
        }

        ListItem(
            headlineContent = { Text("Furniture") },
            leadingContent = { Icon(Icons.Default.Chair, null) }
        )

        ListItem(
            headlineContent = { Text("Products") },
            leadingContent = { Icon(Icons.Default.ShoppingBag, null) }
        )

        ListItem(
            headlineContent = { Text("Dark Mode") },
            leadingContent = { Icon(Icons.Default.DarkMode, null) },
            trailingContent = {
                Switch(
                    checked = dark,
                    onCheckedChange = setDark
                )
            }
        )
    }
}

@Composable
fun Page(title: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@Composable
fun Create() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Create", style = MaterialTheme.typography.headlineLarge)
        Text("Create a new AI interior design")

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddAPhoto, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Room Photo")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Palette, null)
            Spacer(Modifier.width(8.dp))
            Text("Choose Style")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Describe your design") },
            minLines = 4
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text("Generate Design")
        }
    }
}

@Composable
fun Designs() {
    Page("My Designs")
}

@Composable
fun Menu(dark: Boolean, setDark: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Menu", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(20.dp))

        ListItem(
            headlineContent = { Text("AI Styles") },
            leadingContent = { Icon(Icons.Default.Palette, null) }
        )

        ListItem(
            headlineContent = { Text("AI Enhance") },
            leadingContent = { Icon(Icons.Default.AutoFixHigh, null) }
        )

        ListItem(
            headlineContent = { Text("Furniture") },
            leadingContent = { Icon(Icons.Default.Chair, null) }
        )

        ListItem(
            headlineContent = { Text("Products") },
            leadingContent = { Icon(Icons.Default.ShoppingBag, null) }
        )

        ListItem(
            headlineContent = { Text("Dark Mode") },
            leadingContent = { Icon(Icons.Default.DarkMode, null) },
            trailingContent = {
                Switch(
                    checked = dark,
                    onCheckedChange = setDark
                )
            }
        )
    }
}
