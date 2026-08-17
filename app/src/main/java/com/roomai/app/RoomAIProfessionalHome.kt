package com.roomai.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun RoomAIProfessionalHome(
    nav: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = "Professional Studio",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Tools for designers, merchants and craftsmen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))

        ProfessionalCard(
            title = "Room diagnosis",
            description = "Detect layout, proportion and decoration problems before making changes.",
            icon = Icons.Default.Search
        ) {
            nav.navigate("diagnose")
        }

        Spacer(Modifier.height(10.dp))

        ProfessionalCard(
            title = "Measurements & constraints",
            description = "Record dimensions and protect windows, doors, walls and existing elements.",
            icon = Icons.Default.Straighten
        ) {
            nav.navigate("precision")
        }

        Spacer(Modifier.height(10.dp))

        ProfessionalCard(
            title = "AI Workspace",
            description = "Work through a room project and refine generated design versions.",
            icon = Icons.Default.AutoAwesome
        ) {
            nav.navigate("ai_studio")
        }

        Spacer(Modifier.height(10.dp))

        ProfessionalCard(
            title = "Decision Engine",
            description = "Turn room information and constraints into structured design decisions.",
            icon = Icons.Default.Psychology
        ) {
            nav.navigate("decision_engine")
        }

        Spacer(Modifier.height(10.dp))

        ProfessionalCard(
            title = "Room Memory",
            description = "Keep important room information available while developing the project.",
            icon = Icons.Default.Memory
        ) {
            nav.navigate("room_memory")
        }

        Spacer(Modifier.height(10.dp))

        ProfessionalCard(
            title = "Products & furniture",
            description = "Explore furniture and product-oriented design workflows.",
            icon = Icons.Default.Chair
        ) {
            nav.navigate("products")
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    "Professional workflow",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Photo → measurements → constraints → diagnosis → design → versions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ProfessionalCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = description,
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
}
