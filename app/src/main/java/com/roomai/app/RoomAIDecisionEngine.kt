package com.roomai.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BudgetAllocation(
    val item: String,
    val action: String,
    val priority: String,
    val amount: Int
)

private fun severityScore(value: String): Int =
    when (value.lowercase()) {
        "critical" -> 100
        "high" -> 80
        "medium" -> 55
        "low" -> 25
        else -> 40
    }

private fun buildBudgetPlan(
    diagnosis: RoomDiagnosis,
    budget: Int,
    mode: String
): List<BudgetAllocation> {

    if (budget <= 0 || diagnosis.problems.isEmpty()) {
        return emptyList()
    }

    val problems =
        diagnosis.problems
            .sortedByDescending {
                severityScore(it.severity)
            }
            .take(6)

    val weighted = problems.map { problem ->
        when (mode) {
            "Impact" ->
                severityScore(problem.severity).toDouble() * 1.35

            "Budget" ->
                when (problem.severity.lowercase()) {
                    "low" -> 2.0
                    "medium" -> 1.5
                    else -> 1.0
                }

            else ->
                severityScore(problem.severity).toDouble()
        }
    }

    val totalWeight =
        weighted.sum().coerceAtLeast(1.0)

    return problems.mapIndexed { index, problem ->

        val amount =
            (budget * weighted[index] / totalWeight)
                .roundToInt()
                .coerceAtLeast(0)

        BudgetAllocation(
            item = problem.title,
            action = problem.recommendation,
            priority =
                when {
                    severityScore(problem.severity) >= 80 ->
                        "HIGH"

                    severityScore(problem.severity) >= 50 ->
                        "MEDIUM"

                    else ->
                        "LOW"
                },
            amount = amount
        )
    }
}

@Composable
fun RoomAIDecisionEngine() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var diagnosis by remember {
        mutableStateOf<RoomDiagnosis?>(null)
    }

    var solutionBrief by remember {
        mutableStateOf<RoomAISolutionBrief?>(null)
    }

    var solutionImageUrl by remember {
        mutableStateOf<String?>(null)
    }

    var solutionGenerating by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var budget by remember {
        mutableStateOf(
            if (RoomAIProblemFlow.hasConstraint(
                    RoomAIProblemFlow.CONSTRAINT_BUDGET
                )
            ) {
                50000
            } else {
                150000
            }
        )
    }

    var mode by remember {
        mutableStateOf(
            if (RoomAIProblemFlow.hasConstraint(
                    RoomAIProblemFlow.CONSTRAINT_BUDGET
                )
            ) {
                "Budget"
            } else {
                "Balanced"
            }
        )
    }

    val constraintsSummary =
        RoomAIProblemFlow.constraintsSummary()

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            imageUri = uri
            diagnosis = null
            solutionBrief = null
            solutionImageUrl = null
            solutionGenerating = false
            error = null
        }

    val result = diagnosis

    val plan =
        result?.let {
            buildBudgetPlan(
                diagnosis = it,
                budget = budget,
                mode = mode
            )
        } ?: emptyList()

    val planned =
        plan.sumOf { it.amount }

    val remaining =
        (budget - planned).coerceAtLeast(0)

    val currentSolution =
        result?.let {
            buildRoomAISolutionBrief(
                diagnosis = it,
                budgetPlan = plan,
                selectedGoal = RoomAIProblemFlow.labelWithConstraints()
            )
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        item {

            Text(
                "Decision Engine",
                style =
                    MaterialTheme.typography
                        .headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                "Understand the room before spending money."
            )

            if (constraintsSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "Your priorities: $constraintsSummary"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        item {

            if (imageUri == null) {

                OutlinedCard(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia
                                    .ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    shape =
                        RoundedCornerShape(22.dp)
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                    ) {

                        Icon(
                            Icons.Default.AddAPhoto,
                            null,
                            modifier =
                                Modifier.size(42.dp)
                        )

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        Text(
                            "Choose a room photo",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            "RoomAI will diagnose the room and build a prioritized improvement plan."
                        )
                    }
                }

            } else {

                AsyncImage(
                    model = imageUri,
                    contentDescription =
                        "Room",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    contentScale =
                        ContentScale.Crop
                )

                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia
                                    .ImageOnly
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Change Photo")
                }
            }
        }

        item {

            Text(
                "Budget",
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                "${"%,d".format(budget)} DZD"
            )

            Slider(
                value = budget.toFloat(),
                onValueChange = {
                    budget = it.roundToInt()
                },
                valueRange =
                    10000f..1000000f,
                steps = 98
            )
        }

        item {

            Text(
                "Optimization",
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "Impact",
                    "Balanced",
                    "Budget"
                ).forEach { option ->

                    FilterChip(
                        selected =
                            mode == option,
                        onClick = {
                            mode = option
                        },
                        label = {
                            Text(option)
                        }
                    )
                }
            }
        }

        item {

            Button(
                enabled =
                    imageUri != null &&
                    !loading,
                onClick = {

                    val uri =
                        imageUri
                            ?: return@Button

                    scope.launch {

                        loading = true
                        error = null

                        try {

                            diagnosis =
                                diagnoseRoom(
                                    context,
                                    uri
                                )

                        } catch (e: Exception) {

                            error =
                                e.message
                                    ?: "Diagnosis failed"

                        } finally {

                            loading = false
                        }
                    }
                },
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(18.dp)
            ) {

                if (loading) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        "Analyzing room..."
                    )

                } else {

                    Icon(
                        Icons.Default.Psychology,
                        null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        "Analyze & Build Plan"
                    )
                }
            }
        }

        error?.let { message ->

            item {

                Text(
                    message,
                    color =
                        MaterialTheme.colorScheme.error
                )
            }
        }

        result?.let { room ->

            item {

                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(22.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {

                        Text(
                            "Room Score: ${room.score}/100",
                            style =
                                MaterialTheme.typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Text(
                            room.summary.ifBlank {
                                "Room analyzed successfully."
                            }
                        )
                    }
                }
            }

            item {

                Text(
                    "Priority Problems",
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            items(
                room.problems.sortedByDescending {
                    severityScore(it.severity)
                }
            ) { problem ->

                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                problem.title,
                                modifier =
                                    Modifier.weight(1f),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        problem.severity
                                            .uppercase()
                                    )
                                }
                            )
                        }

                        Spacer(
                            Modifier.height(7.dp)
                        )

                        Text(
                            "WHY: ${problem.reason}"
                        )

                        Spacer(
                            Modifier.height(7.dp)
                        )

                        Text(
                            "RECOMMENDATION: ${problem.recommendation}"
                        )
                    }
                }
            }

            item {

                Text(
                    "Keep / Replace / Upgrade",
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            item {

                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {

                        Text(
                            "KEEP: " +
                                room.keep.joinToString()
                                    .ifBlank {
                                        "No items identified"
                                    }
                        )

                        Text(
                            "REPLACE: " +
                                room.replace.joinToString()
                                    .ifBlank {
                                        "No replacements identified"
                                    }
                        )

                        Text(
                            "UPGRADE: " +
                                room.upgrade.joinToString()
                                    .ifBlank {
                                        "No upgrades identified"
                                    }
                        )
                    }
                }
            }

            item {

                Text(
                    "Budget Planning Envelope",
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "$mode optimization"
                )
            }

            if (plan.isEmpty()) {

                item {

                    Text(
                        "No actionable problems were returned by the diagnosis."
                    )
                }

            } else {

                items(plan) { allocation ->

                    ElevatedCard(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp)
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(15.dp)
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    allocation.item,
                                    modifier =
                                        Modifier.weight(1f),
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    "${"%,d".format(
                                        allocation.amount
                                    )} DZD",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }

                            Spacer(
                                Modifier.height(5.dp)
                            )

                            Text(
                                "${allocation.priority} • " +
                                    allocation.action
                            )
                        }
                    }
                }
            }

            item {

                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            "Budget Summary",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            "Available: ${"%,d".format(
                                budget
                            )} DZD"
                        )

                        Text(
                            "Allocated planning envelope: ${"%,d".format(
                                planned
                            )} DZD"
                        )

                        Text(
                            "Remaining: ${"%,d".format(
                                remaining
                            )} DZD"
                        )
                    }
                }
            }

            item {

                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            "Design Reasoning",
                            style =
                                MaterialTheme.typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Text(
                            "RoomAI prioritizes problems by severity, protects items that do not need replacement, and distributes the available budget toward the highest-impact changes."
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            "Next: each recommendation will become a one-tap verified Precision Edit."
                        )
                    }
                }
            }
        }

        currentSolution?.let { solution ->

            item {

                Spacer(Modifier.height(8.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            "Your Solution",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            solution.goal,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(solution.summary)

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Planning envelope: ${"%,d".format(solution.totalBudget)} DZD"
                        )

                        Spacer(Modifier.height(10.dp))

                        solution.actions.take(5).forEachIndexed { index, action ->

                            Text(
                                "${index + 1}. ${action.title}",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "${action.action} • " +
                                    "${action.priority} • " +
                                    "${"%,d".format(action.budget)} DZD"
                            )

                            Spacer(Modifier.height(6.dp))
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            enabled = imageUri != null && !solutionGenerating,
                            onClick = {

                                val uri = imageUri
                                    ?: return@Button

                                solutionGenerating = true
                                solutionImageUrl = null
                                error = null
                                solutionBrief = solution

                                scope.launch {

                                    try {

                                        /*
                                         * IMPORTANT:
                                         *
                                         * We deliberately reuse the existing
                                         * generateDesign() pipeline.
                                         *
                                         * The difference is the prompt:
                                         * it now contains the actual diagnosis,
                                         * priorities, budget and actions.
                                         */

                                        val generatedUrl =
                                            generateDesign(
                                                context = context,
                                                uri = uri,
                                                room = "Room",
                                                style = "Problem Solving",
                                                userPrompt =
                                                    solution.generationBrief(),
                                                operation = "fix",
                                                selection =
                                                    solution.goal
                                            )

                                        solutionImageUrl =
                                            generatedUrl

                                        RoomAIHistory.add(
                                            context = context,
                                            generatedUrl = generatedUrl,
                                            room = "Room",
                                            style = "Problem Solving",
                                            prompt =
                                                solution.generationBrief(),
                                            originalUrl =
                                                uri.toString()
                                        )

                                    } catch (e: Exception) {

                                        error =
                                            e.message
                                                ?: "Could not build the solution."

                                    } finally {

                                        solutionGenerating = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {

                            if (solutionGenerating) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    Modifier.width(8.dp)
                                )

                                Text(
                                    "Building solution..."
                                )

                            } else {

                                Icon(
                                    Icons.Default.AutoAwesome,
                                    null
                                )

                                Spacer(
                                    Modifier.width(8.dp)
                                )

                                Text(
                                    "Build This Solution"
                                )
                            }
                        }
                    }
                }
            }
        }

        solutionImageUrl?.let { generatedUrl ->

            item {

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            "Solution Built",
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        AsyncImage(
                            model = generatedUrl,
                            contentDescription =
                                "RoomAI solution",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            contentScale =
                                ContentScale.Crop
                        )

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        Text(
                            "This result was generated from the diagnosed room problems and the solution plan. Verify dimensions and fit before purchasing."
                        )
                    }
                }
            }
        }

        solutionBrief?.let { solution ->

            item {

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            "Solution Ready",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "The generation step will use the diagnosis and action plan, not a generic redesign prompt."
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            solution.generationBrief()
                        )
                    }
                }
            }
        }
    }
}
