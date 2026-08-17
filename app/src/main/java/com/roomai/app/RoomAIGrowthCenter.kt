package com.roomai.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RoomAISocial(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val action: String
)

private val roomAISocials = listOf(
    RoomAISocial(
        "Instagram",
        Icons.Default.CameraAlt,
        "Show beautiful before & after transformations.",
        "Share"
    ),
    RoomAISocial(
        "TikTok",
        Icons.Default.PlayArrow,
        "Turn transformations into short-form content.",
        "Create"
    ),
    RoomAISocial(
        "Pinterest",
        Icons.Default.PushPin,
        "Publish inspiration people can save.",
        "Prepare"
    ),
    RoomAISocial(
        "Facebook",
        Icons.Default.Public,
        "Share designs with friends and communities.",
        "Share"
    ),
    RoomAISocial(
        "YouTube",
        Icons.Default.PlayCircle,
        "Prepare Shorts and longer design stories.",
        "Prepare"
    )
)

@Composable
fun RoomAIGrowthCenter(
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    val context = LocalContext.current

    var selected by remember {
        mutableStateOf("Instagram")
    }

    var copied by remember {
        mutableStateOf(false)
    }

    val caption =
        "I transformed this room with AI ✨ " +
        "Which version would you choose? " +
        "#RoomAI #InteriorDesign #HomeDesign #AIInteriorDesign"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "RoomAI Growth",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Turn every design into content.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Text(
                        "Design → Content → Share",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "RoomAI should not stop when the image is generated. " +
                            "Every successful transformation can become a social post, " +
                            "a before/after story, a short video concept or a Pinterest idea.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onCreate,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Create a design")
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            "Choose your channel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(roomAISocials) { social ->
                FilterChip(
                    selected = selected == social.name,
                    onClick = {
                        selected = social.name
                    },
                    label = {
                        Text(social.name)
                    },
                    leadingIcon = {
                        Icon(
                            social.icon,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val active =
            roomAISocials.first {
                it.name == selected
            }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            active.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(13.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            active.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            active.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "Recommended content",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    when (active.name) {
                        "TikTok" ->
                            "Before → AI transformation → After. " +
                                "Keep the first seconds visually strong."
                        "Pinterest" ->
                            "Vertical inspiration image with a clear room style " +
                                "and searchable title."
                        "YouTube" ->
                            "Short transformation story: problem → design → result."
                        "Facebook" ->
                            "Before/after post with a simple question to encourage comments."
                        else ->
                            "Before/after carousel with a strong first image and a clear CTA."
                    }
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    "Caption",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        caption,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as android.content.ClipboardManager

                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "RoomAI caption",
                                    caption
                                )
                            )

                            copied = true
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            if (copied) "Copied" else "Copy"
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            shareRoomAIText(
                                context,
                                caption
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(6.dp))

                        Text("Share")
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            "Growth loop",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        GrowthStep(
            number = "01",
            title = "Generate",
            text = "User creates a room transformation."
        )

        GrowthStep(
            number = "02",
            title = "Reveal",
            text = "Make the before/after visually satisfying."
        )

        GrowthStep(
            number = "03",
            title = "Share",
            text = "Give the user ready-to-share content."
        )

        GrowthStep(
            number = "04",
            title = "Discover",
            text = "New users discover RoomAI through shared transformations."
        )

        GrowthStep(
            number = "05",
            title = "Repeat",
            text = "The new user creates another design and starts the loop again."
        )

        Spacer(Modifier.height(28.dp))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    "Important",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "This version uses the Android share system. " +
                        "It does not pretend to have direct publishing access to " +
                        "Instagram, TikTok, Pinterest, Facebook or YouTube. " +
                        "Direct publishing can be connected later through each platform's " +
                        "official APIs and OAuth permissions."
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GrowthStep(
    number: String,
    title: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                number,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shareRoomAIText(
    context: Context,
    text: String
) {
    val intent = Intent(
        Intent.ACTION_SEND
    ).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            text
        )
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            "Share RoomAI"
        )
    )
}
