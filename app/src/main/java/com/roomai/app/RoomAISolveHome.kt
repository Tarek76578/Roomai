package com.roomai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class RoomAISolveIntent(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun RoomAISolveHome(
    onStart: (RoomAISolveIntent) -> Unit = {}
) {
    val intents = listOf(
        RoomAISolveIntent(
            "Make my room better",
            "Find the most important improvements first.",
            Icons.Default.Home
        ),
        RoomAISolveIntent(
            "Fit my budget",
            "Build a practical plan around what I can spend.",
            Icons.Default.AttachMoney
        ),
        RoomAISolveIntent(
            "Use my existing furniture",
            "Keep what works and change only what needs changing.",
            Icons.Default.Checkroom
        ),
        RoomAISolveIntent(
            "Use my space better",
            "Solve layout and space problems before buying anything.",
            Icons.Default.SpaceDashboard
        ),
        RoomAISolveIntent(
            "I don't know what to buy",
            "Turn the room into a practical improvement plan.",
            Icons.Default.AutoAwesome
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                text = "Solve your room.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Tell RoomAI what you want to solve. " +
                    "We'll help you decide what to change before you spend money."
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Start with your room",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Upload your room and tell us the problem."
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onStart(intents.first())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose room photo")
                    }
                }
            }
        }

        item {
            Text(
                "What do you want to solve?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(intents.size) { index ->

            val intent = intents[index]

            OutlinedCard(
                onClick = {
                    onStart(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        intent.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            intent.title,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(intent.description)
                    }

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }

        item {
            HorizontalDivider()

            Spacer(Modifier.height(6.dp))

            Text(
                "RoomAI is designed to solve the room problem, " +
                    "not just generate another image."
            )
        }
    }
}

/*
 * Product flow:
 *
 * Problem
 *   -> Diagnose
 *   -> Plan
 *   -> Generate
 *   -> Precision Edit
 *   -> Verify
 *   -> Remember
 *
 * Existing Decision Engine, Precision Engine,
 * Memory and History remain intact.
 */
