package com.roomai.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun RoomAIPrecision() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedObject by remember {
        mutableStateOf("Sofa")
    }

    var instruction by remember {
        mutableStateOf("")
    }

    var resultUrl by remember {
        mutableStateOf<String?>(null)
    }

    var verification by remember {
        mutableStateOf<PrecisionVerification?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var state by remember {
        mutableStateOf(
            RoomAIProjectStore.load(context)
        )
    }

    var wallsLocked by remember {
        mutableStateOf(state.structure.wallsLocked)
    }

    var doorsLocked by remember {
        mutableStateOf(state.structure.doorsLocked)
    }

    var windowsLocked by remember {
        mutableStateOf(state.structure.windowsLocked)
    }

    var floorLocked by remember {
        mutableStateOf(state.structure.floorLocked)
    }

    var cameraLocked by remember {
        mutableStateOf(state.structure.cameraLocked)
    }

    val objects = listOf(
        "Sofa",
        "Chair",
        "Table",
        "Bed",
        "Wardrobe",
        "Rug",
        "Curtains",
        "Walls",
        "Floor",
        "Lighting",
        "Decor",
        "Plants"
    )

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            imageUri = uri
            resultUrl = null
            verification = null
            error = null
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "Precision Edit",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Select one thing. RoomAI protects everything else."
            )
        }

        item {
            if (imageUri == null) {

                OutlinedCard(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        Icon(
                            Icons.Default.AddAPhoto,
                            null,
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Select Room Photo",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Use the same room photo for iterative edits."
                        )
                    }
                }

            } else {

                AsyncImage(
                    model = imageUri,
                    contentDescription = "Room",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Change Room Photo")
                }
            }
        }

        item {
            Text(
                "Target",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(objects) { objectName ->

                    FilterChip(
                        selected =
                            selectedObject == objectName,
                        onClick = {
                            selectedObject =
                                objectName
                        },
                        label = {
                            Text(objectName)
                        }
                    )
                }
            }
        }

        item {
            Text(
                "Structural Lock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Locked elements are explicitly protected from the edit."
            )
        }

        item {
            PrecisionLock(
                title = "Walls",
                checked = wallsLocked,
                onCheckedChange = {
                    wallsLocked = it
                }
            )
        }

        item {
            PrecisionLock(
                title = "Doors",
                checked = doorsLocked,
                onCheckedChange = {
                    doorsLocked = it
                }
            )
        }

        item {
            PrecisionLock(
                title = "Windows",
                checked = windowsLocked,
                onCheckedChange = {
                    windowsLocked = it
                }
            )
        }

        item {
            PrecisionLock(
                title = "Floor",
                checked = floorLocked,
                onCheckedChange = {
                    floorLocked = it
                }
            )
        }

        item {
            PrecisionLock(
                title = "Camera & Perspective",
                checked = cameraLocked,
                onCheckedChange = {
                    cameraLocked = it
                }
            )
        }

        item {
            OutlinedTextField(
                value = instruction,
                onValueChange = {
                    instruction = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("What should change?")
                },
                placeholder = {
                    Text(
                        "Example: replace it with a beige L-shaped sofa."
                    )
                },
                minLines = 4
            )
        }

        item {
            Button(
                enabled =
                    imageUri != null &&
                    !loading &&
                    instruction.isNotBlank(),
                onClick = {

                    val uri =
                        imageUri
                            ?: return@Button

                    scope.launch {

                        loading = true
                        error = null
                        resultUrl = null

                        try {

                            val newStructure =
                                RoomStructure(
                                    wallsLocked =
                                        wallsLocked,
                                    doorsLocked =
                                        doorsLocked,
                                    windowsLocked =
                                        windowsLocked,
                                    floorLocked =
                                        floorLocked,
                                    ceilingLocked =
                                        state.structure.ceilingLocked,
                                    cameraLocked =
                                        cameraLocked,
                                    perspectiveLocked =
                                        cameraLocked,
                                    lightingLocked =
                                        state.structure.lightingLocked
                                )

                            val updatedState =
                                state.copy(
                                    structure =
                                        newStructure
                                )

                            RoomAIProjectStore.save(
                                context,
                                updatedState
                            )

                            val protectedElements =
                                buildList {
                                    if (wallsLocked &&
                                        selectedObject != "Walls"
                                    ) {
                                        add(
                                            ProtectedElement(
                                                name = "Walls",
                                                reason = "Keep wall structure unchanged"
                                            )
                                        )
                                    }

                                    if (doorsLocked &&
                                        selectedObject != "Doors"
                                    ) {
                                        add(
                                            ProtectedElement(
                                                name = "Doors",
                                                reason = "Keep doors unchanged"
                                            )
                                        )
                                    }

                                    if (windowsLocked &&
                                        selectedObject != "Windows"
                                    ) {
                                        add(
                                            ProtectedElement(
                                                name = "Windows",
                                                reason = "Keep windows unchanged"
                                            )
                                        )
                                    }

                                    if (floorLocked &&
                                        selectedObject != "Floor"
                                    ) {
                                        add(
                                            ProtectedElement(
                                                name = "Floor",
                                                reason = "Keep floor unchanged"
                                            )
                                        )
                                    }

                                    if (cameraLocked) {
                                        add(
                                            ProtectedElement(
                                                name = "Camera and perspective",
                                                reason = "Keep camera angle and perspective unchanged"
                                            )
                                        )
                                    }

                                    if (selectedObject != "Lighting") {
                                        add(
                                            ProtectedElement(
                                                name = "Lighting",
                                                reason = "Do not change lighting unless required by the target"
                                            )
                                        )
                                    }
                                }

                            val editType =
                                PrecisionEditType.REPLACE

                            val precisionRequest =
                                PrecisionRequest(
                                    target =
                                        PrecisionTarget(
                                            name = selectedObject,
                                            description =
                                                "Edit only the selected room element: $selectedObject",
                                            selection =
                                                selectedObject
                                        ),
                                    editType = editType,
                                    instruction = instruction,
                                    room =
                                        updatedState.roomType,
                                    style =
                                        updatedState.style,
                                    protectedElements =
                                        protectedElements,
                                    sourceVersionId =
                                        state.versions
                                            .lastOrNull()
                                            ?.id
                                            ?: "original"
                                )

                            val precisionResult =
                                RoomAIPrecisionEngine.execute(
                                    context = context,
                                    uri = uri,
                                    request = precisionRequest,
                                    verify = true
                                )

                            verification =
                                precisionResult
                                    .version
                                    .verification

                            resultUrl =
                                precisionResult
                                    .version
                                    .imageUrl

                            if (!precisionResult.accepted) {
                                throw Exception(
                                    "Precision verification failed after " +
                                        "${precisionResult.attempts} attempts. " +
                                        precisionResult
                                            .version
                                            .verification
                                            .message
                                )
                            }

                            state =
                                RoomAIProjectStore
                                    .addVersion(
                                        context,
                                        updatedState,
                                        precisionResult
                                            .version
                                            .imageUrl,
                                        "Verified edit: $selectedObject",
                                        "precision_edit_verified"
                                    )

                            saveDesign(
                                context,
                                precisionResult
                                    .version
                                    .imageUrl,
                                "Verified Precision Edit",
                                selectedObject
                            )

                        } catch (e: Exception) {

                            error =
                                e.message
                                    ?: "Precision edit failed"

                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {

                if (loading) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(10.dp))

                    Text("Editing only $selectedObject...")

                } else {

                    Icon(
                        Icons.Default.AutoFixHigh,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Edit Only $selectedObject"
                    )
                }
            }
        }

        error?.let { message ->

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
                            "Edit failed",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(5.dp)
                        )

                        Text(
                            message,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }
            }
        }

        resultUrl?.let { url ->

            item {
                Text(
                    "New Version",
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
                        RoundedCornerShape(22.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(12.dp)
                    ) {

                        AsyncImage(
                            model = url,
                            contentDescription =
                                "AI result",
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                            contentScale =
                                ContentScale.Crop
                        )

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        Text(
                            "Version ${
                                state.versions.size
                            } saved to Room Memory."
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                shareDesign(
                                    context,
                                    url
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Icon(
                                Icons.Default.Share,
                                null
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text("Share")
                        }
                    }
                }
            }
        }

        verification?.let { result ->

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                if (result.status ==
                                    VerificationStatus.PASS
                                ) {
                                    Icons.Default.Verified
                                } else {
                                    Icons.Default.Warning
                                },
                                contentDescription = null
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    if (result.status ==
                                        VerificationStatus.PASS
                                    ) {
                                        "Vision Verified"
                                    } else {
                                        "Verification Failed"
                                    },
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    "Score: ${result.score}/100"
                                )
                            }
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            result.message
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            "Target changed: ${result.targetChanged}"
                        )

                        Text(
                            "Protected elements changed: ${result.protectedElementsChanged}"
                        )

                        Text(
                            "Architecture changed: ${result.architectureChanged}"
                        )

                        Text(
                            "Camera changed: ${result.cameraChanged}"
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
                    RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(
                        "Iteration",
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
                        "Every successful edit becomes a version. Future batches will add Undo, Compare and Continue Editing."
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        "Saved versions: ${
                            state.versions.size
                        }"
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecisionLock(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
            verticalAlignment =
                androidx.compose.ui.Alignment.CenterVertically
        ) {

            Icon(
                if (checked)
                    Icons.Default.Lock
                else
                    Icons.Default.LockOpen,
                contentDescription =
                    null
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Text(
                title,
                modifier =
                    Modifier.weight(1f),
                fontWeight =
                    FontWeight.Medium
            )

            Switch(
                checked = checked,
                onCheckedChange =
                    onCheckedChange
            )
        }
    }
}
