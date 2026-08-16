package com.roomai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun RoomAIWorkspace(nav: NavHostController) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "RoomAI Workspace",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Your room is a project, not a single AI image."
            )
        }

        item {
            WorkspaceCard(
                title = "Precision Edit",
                description =
                    "Change one object while protecting the rest of the room.",
                icon = Icons.Default.AutoFixHigh
            ) {
                nav.navigate("precision")
            }
        }

        item {
            WorkspaceCard(
                title = "Room Memory",
                description =
                    "Keep your room rules, preferences and design versions.",
                icon = Icons.Default.Psychology
            ) {
                nav.navigate("room_memory")
            }
        }

        item {
            WorkspaceCard(
                title = "Diagnose",
                description =
                    "Find problems before spending money.",
                icon = Icons.Default.Search
            ) {
                nav.navigate("diagnose")
            }
        }

        item {
            WorkspaceCard(
                title = "Power Studio",
                description =
                    "Budget, Products, Seller and Directed Redesign workflows.",
                icon = Icons.Default.AutoAwesome
            ) {
                nav.navigate("legacy_ai_studio")
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
                        "RoomAI Engine",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Analyze → Lock → Edit → Verify → Remember → Iterate"
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "This foundation will power Budget, Products, Shopping and the RoomAI Agent."
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(3.dp))

                Text(description)
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
