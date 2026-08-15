package com.roomai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
        modifier = Modifier.weight(1f)
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
                modifier = Modifier.weight(1f),
                onClick = {
                    nav.navigate("styles")
                }
            )

            FeatureCard(
                title = "Enhance",
                icon = Icons.Default.AutoFixHigh,
                modifier = Modifier.weight(1f),
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
                modifier = Modifier.weight(1f),
                onClick = {
                    nav.navigate("furniture")
                }
            )

            FeatureCard(
                title = "Products",
                icon = Icons.Default.ShoppingBag,
                modifier = Modifier.weight(1f),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "Create",
            style = MaterialTheme.typography.headlineLarge
        )

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

        var description by remember {
            mutableStateOf("")
        }

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Describe your design")
            },
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
