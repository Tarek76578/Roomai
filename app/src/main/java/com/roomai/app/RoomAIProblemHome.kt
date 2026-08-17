package com.roomai.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class RoomAIProblemOption(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun RoomAIProblemHome(
    onProblemSelected: (RoomAIProblemOption) -> Unit = {}
) {
    val problems = listOf(
        RoomAIProblemOption(
            title = "Make my room better",
            description = "Find the changes that will have the biggest impact.",
            icon = Icons.Default.Home
        ),
        RoomAIProblemOption(
            title = "Fit my budget",
            description = "Create a practical room plan within my budget.",
            icon = Icons.Default.AttachMoney
        ),
        RoomAIProblemOption(
            title = "Use my existing furniture",
            description = "Keep what works and replace only what needs changing.",
            icon = Icons.Default.Checkroom
        ),
        RoomAIProblemOption(
            title = "Use my space better",
            description = "Solve layout and space problems before buying anything.",
            icon = Icons.Default.SpaceDashboard
        ),
        RoomAIProblemOption(
            title = "I don't know what to buy",
            description = "Turn my room into a clear improvement and shopping plan.",
            icon = Icons.Default.AutoAwesome
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
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
                text = "RoomAI helps you decide what to change, " +
                    "what to keep and what to buy before you spend money.",
                style = MaterialTheme.typography.bodyLarge
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
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Start with your room",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "Upload a room photo and tell RoomAI what you want to solve."
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onProblemSelected(problems.first())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start")
                    }
                }
            }
        }

        item {
            Text(
                text = "What problem do you want to solve?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(problems) { problem ->

            OutlinedCard(
                onClick = {
                    onProblemSelected(problem)
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
                        imageVector = problem.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.size(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = problem.title,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = problem.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }

        item {
            HorizontalDivider()

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Generate is only one step. RoomAI's goal is to solve the room problem.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
