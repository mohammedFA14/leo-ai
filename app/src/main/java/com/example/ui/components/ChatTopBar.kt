package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AiModel
import com.example.data.sync.CloudSyncUiState
import com.example.data.sync.SyncStatus
import com.example.ui.theme.LeoCyanSecondary
import com.example.ui.theme.LeoSuccess
import com.example.ui.theme.LeoWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    sessionTitle: String,
    selectedModel: AiModel,
    cloudSyncState: CloudSyncUiState,
    onMenuClick: () -> Unit,
    onCloudClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearChatClick: () -> Unit,
    onModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_angle"
    )

    TopAppBar(
        modifier = modifier.testTag("chat_top_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "قائمة المحادثات"
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leo AI Avatar with glowing badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.leo_logo),
                        contentDescription = "Leo AI Logo",
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ليو AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Active status green dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(LeoSuccess)
                        )
                    }

                    // Model and Session indicator chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onModelClick() }
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = when (selectedModel) {
                                AiModel.GEMINI_3_5_FLASH -> "Gemini 3.5 Flash"
                                AiModel.GEMINI_3_1_PRO -> "Gemini 3.1 Pro"
                                AiModel.DEEPSEEK_CHAT -> "DeepSeek V3"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " • $sessionTitle",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            // Cloud Sync Indicator Button with Live Badge
            IconButton(
                onClick = onCloudClick,
                modifier = Modifier.testTag("cloud_sync_button")
            ) {
                BadgedBox(
                    badge = {
                        if (cloudSyncState.unsyncedCount > 0) {
                            Badge(
                                containerColor = LeoWarning,
                                contentColor = Color.Black
                            ) {
                                Text(
                                    text = "${cloudSyncState.unsyncedCount}",
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                ) {
                    when (cloudSyncState.status) {
                        SyncStatus.SYNCING -> {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "جاري المزامنة السحابية",
                                tint = LeoCyanSecondary,
                                modifier = Modifier.rotate(rotationAngle)
                            )
                        }
                        SyncStatus.SYNCED -> {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "متزامن مع السحابة",
                                tint = LeoSuccess
                            )
                        }
                        SyncStatus.OFFLINE -> {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "السحابة غير متصلة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SyncStatus.PENDING, SyncStatus.IDLE, SyncStatus.ERROR -> {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "المزامنة السحابية",
                                tint = if (cloudSyncState.status == SyncStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // More Options Dropdown (Settings, Clear)
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.testTag("more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية"
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("إعدادات ليو AI والمودل") },
                        onClick = {
                            showMoreMenu = false
                            onSettingsClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("المزامنة السحابية") },
                        onClick = {
                            showMoreMenu = false
                            onCloudClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تبديل المودل") },
                        onClick = {
                            showMoreMenu = false
                            onModelClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.SmartToy, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("مسح المحادثة الحالية", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMoreMenu = false
                            onClearChatClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    )
}
