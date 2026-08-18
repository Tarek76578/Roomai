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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RoomAIProblemOption(
    val id: String,
    val title: String,
    val icon: ImageVector
)

private data class RoomAIConstraintOption(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun RoomAIProblemFirstScreen(
    onContinueToDiagnosis: () -> Unit
) {
    val problems = listOf(
        RoomAIProblemOption(
            RoomAIProblemFlow.SPACE,
            "Space & layout",
            Icons.Default.SpaceDashboard
        ),
        RoomAIProblemOption(
            RoomAIProblemFlow.LIGHTING,
            "Lighting",
            Icons.Default.LightMode
        ),
        RoomAIProblemOption(
            RoomAIProblemFlow.STORAGE,
            "Storage",
            Icons.Default.Dashboard
        ),
        RoomAIProblemOption(
            RoomAIProblemFlow.EXISTING_FURNITURE,
            "Furniture",
            Icons.Default.Checkroom
        ),
        RoomAIProblemOption(
            RoomAIProblemFlow.FUNCTION,
            "Room function",
            Icons.Default.Build
        ),
        RoomAIProblemOption(
            RoomAIProblemFlow.COLOR,
            "Colors",
            Icons.Default.ColorLens
        ),
        RoomAIProblemOption(
            RoomAIProblemFlow.SPECIFIC_CHANGE,
            "Something specific",
            Icons.Default.Tune
        )
    )

    val constraints = listOf(
        RoomAIConstraintOption(
            RoomAIProblemFlow.CONSTRAINT_BUDGET,
            "Keep spending low",
            Icons.Default.Savings
        ),
        RoomAIConstraintOption(
            RoomAIProblemFlow.CONSTRAINT_KEEP_FURNITURE,
            "Keep my furniture",
            Icons.Default.Checkroom
        ),
        RoomAIConstraintOption(
            RoomAIProblemFlow.CONSTRAINT_MINIMAL_CHANGES,
            "Minimal changes",
            Icons.Default.Tune
        ),
        RoomAIConstraintOption(
            RoomAIProblemFlow.CONSTRAINT_READY_TO_BUY,
            "Ready to buy",
            Icons.Default.ShoppingCart
        )
    )

    var selectedProblem by remember {
        mutableStateOf(RoomAIProblemFlow.selectedProblem)
    }

    var selectedConstraints by remember {
        mutableStateOf(RoomAIProblemFlow.selectedConstraints)
    }

    fun chooseProblem(id: String) {
        RoomAIProblemFlow.select(id)
        selectedProblem = id
    }

    fun toggleConstraint(id: String) {
        RoomAIProblemFlow.toggleConstraint(id)
        selectedConstraints = RoomAIProblemFlow.selectedConstraints
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "What do you want to solve?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Pick the room problem first. Then tell RoomAI how you want the solution to fit your situation."
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepPill(
                    number = "1",
                    label = "Problem",
                    active = true,
                    modifier = Modifier.weight(1f)
                )

                StepPill(
                    number = "2",
                    label = "Constraints",
                    active = false,
                    modifier = Modifier.weight(1f)
                )

                StepPill(
                    number = "3",
                    label = "Photo + diagnosis",
                    active = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "1. What is the main problem?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Choose one primary problem. RoomAI can still detect other problems in the photo."
            )
        }

        items(problems.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { problem ->
                    val selected = selectedProblem == problem.id

                    if (selected) {
                        Card(
                            onClick = { chooseProblem(problem.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            ProblemTileContent(problem)
                        }
                    } else {
                        OutlinedCard(
                            onClick = { chooseProblem(problem.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            ProblemTileContent(problem)
                        }
                    }
                }

                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))

            Text(
                "2. What matters when we solve it?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "These are constraints, not room problems. Select any that apply."
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(constraints) { constraint ->
                    FilterChip(
                        selected = constraint.id in selectedConstraints,
                        onClick = { toggleConstraint(constraint.id) },
                        label = { Text(constraint.title) },
                        leadingIcon = {
                            Icon(
                                constraint.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.size(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Next: show us the room",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "RoomAI will inspect the photo, rank the problems and build a practical plan."
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onContinueToDiagnosis,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null
                )

                Spacer(Modifier.size(8.dp))

                Text("Continue to room photo")
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "You can change your choices before the final solution.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProblemTileContent(
    problem: RoomAIProblemOption
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            problem.icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            problem.title,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StepPill(
    number: String,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (active)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(number, fontWeight = FontWeight.Bold)

            Spacer(Modifier.size(5.dp))

            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
