package com.roomai.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun RoomAIMemory() {

    val context = LocalContext.current

    var state by remember {
        mutableStateOf(
            RoomAIProjectStore.load(context)
        )
    }

    var roomName by remember {
        mutableStateOf(state.name)
    }

    var roomType by remember {
        mutableStateOf(state.roomType)
    }

    var style by remember {
        mutableStateOf(state.style)
    }

    var budget by remember {
        mutableStateOf(
            if (state.budget == 0) ""
            else state.budget.toString()
        )
    }

    val constraints =
        state.constraints

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "Room Memory",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "RoomAI remembers the decisions that should survive future edits."
            )
        }

        item {
            OutlinedTextField(
                value = roomName,
                onValueChange = {
                    roomName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Project name")
                }
            )
        }

        item {
            OutlinedTextField(
                value = roomType,
                onValueChange = {
                    roomType = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Room type")
                }
            )
        }

        item {
            OutlinedTextField(
                value = style,
                onValueChange = {
                    style = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Preferred style")
                }
            )
        }

        item {
            OutlinedTextField(
                value = budget,
                onValueChange = {
                    budget = it.filter { char ->
                        char.isDigit()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Budget (DZD)")
                }
            )
        }

        item {
            Button(
                onClick = {
                    val updated =
                        state.copy(
                            name = roomName.ifBlank {
                                "My Room"
                            },
                            roomType = roomType.ifBlank {
                                "Room"
                            },
                            style = style.ifBlank {
                                "Modern"
                            },
                            budget =
                                budget.toIntOrNull() ?: 0
                        )

                    RoomAIProjectStore.save(
                        context,
                        updated
                    )

                    state = updated
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    null
                )

                Spacer(Modifier.width(8.dp))

                Text("Save Room Memory")
            }
        }

        item {
            Text(
                "Protected Decisions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            listOf(
                "Walls remain unchanged",
                "Doors remain unchanged",
                "Windows remain unchanged",
                "Floor remains unchanged",
                "Camera remains unchanged",
                "Perspective remains unchanged"
            )
        ) { item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(15.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        null
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(item)
                }
            }
        }

        item {
            Text(
                "Design Versions: ${state.versions.size}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            state.versions.reversed()
        ) { version ->

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(15.dp)
                ) {
                    Text(
                        version.title,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        version.operation
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Version ${state.versions.indexOf(version) + 1}"
                    )
                }
            }
        }

        if (constraints.isEmpty()) {
            item {
                Text(
                    "No custom constraints yet. Precision Studio will add them as the project evolves."
                )
            }
        }
    }
}
