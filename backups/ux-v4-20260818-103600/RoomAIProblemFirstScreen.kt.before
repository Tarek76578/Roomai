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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class RoomAIProblemChoice(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun RoomAIProblemFirstScreen(
    onContinueToDiagnosis: () -> Unit
) {
    val choices = listOf(
        RoomAIProblemChoice(
            RoomAIProblemFlow.IMPROVE,
            "Make my room better",
            "Find the improvements that matter most.",
            Icons.Default.Home
        ),
        RoomAIProblemChoice(
            RoomAIProblemFlow.BUDGET,
            "Fit my budget",
            "Plan improvements around what I can spend.",
            Icons.Default.AttachMoney
        ),
        RoomAIProblemChoice(
            RoomAIProblemFlow.EXISTING_FURNITURE,
            "Use my existing furniture",
            "Keep what works and change only what needs changing.",
            Icons.Default.Checkroom
        ),
        RoomAIProblemChoice(
            RoomAIProblemFlow.SPACE,
            "Use my space better",
            "Solve layout and space problems before buying.",
            Icons.Default.SpaceDashboard
        ),
        RoomAIProblemChoice(
            RoomAIProblemFlow.SHOPPING,
            "I don't know what to buy",
            "Turn the room into a practical improvement plan.",
            Icons.Default.AutoAwesome
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "What problem can we solve?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "RoomAI should help you make a better room decision — " +
                    "not just generate another image."
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Start with your room",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Choose the problem first. The Decision Engine will diagnose the room."
                    )
                }
            }
        }

        items(choices) { choice ->
            OutlinedCard(
                onClick = {
                    RoomAIProblemFlow.select(choice.id)
                    onContinueToDiagnosis()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        choice.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.size(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            choice.title,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(choice.description)
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

            Spacer(Modifier.height(5.dp))

            Text(
                "Next: diagnose the room, build a plan, then generate only when the solution is clear.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
