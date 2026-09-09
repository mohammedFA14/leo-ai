package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.theme.LeoCyanSecondary
import com.example.ui.theme.LeoSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageItem(
    message: ChatMessageEntity,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(if (isUser) "user_message_item" else "assistant_message_item"),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                // Leo AI Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.leo_logo),
                        contentDescription = "Leo AI",
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                // Message Bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        if (!isUser) {
                            // Assistant Header with model name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "ليو AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = LeoCyanSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatModelTag(message.model),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Attachment Display if present
                        if (message.attachmentName != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    if (message.attachmentType == "image" && message.attachmentUri != null) {
                                        AsyncImage(
                                            model = message.attachmentUri,
                                            contentDescription = message.attachmentName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 180.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (message.attachmentType == "image") Icons.Default.Image else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = if (isUser) MaterialTheme.colorScheme.onPrimary else LeoCyanSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = message.attachmentName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Content with Code Block / Markdown handling
                        FormattedMessageContent(
                            content = message.content,
                            textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onCopyCode = { code ->
                                copyToClipboard(context, code, "تم نسخ الكود البرمجي")
                            }
                        )
                    }
                }

                // Message Footer: Timestamp, Cloud Sync Icon, Copy & Share buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Cloud sync status icon for this message
                    Icon(
                        imageVector = if (message.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = if (message.isSynced) "متزامن سحابياً" else "في انتظار المزامنة",
                        tint = if (message.isSynced) LeoSuccess else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.width(8.dp))

                        // Quick Copy button
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الرد",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    copyToClipboard(context, message.content, "تم نسخ رد ليو")
                                }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Quick Share button
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة الرد",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    shareText(context, message.content)
                                }
                        )
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "المستخدم",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FormattedMessageContent(
    content: String,
    textColor: Color,
    onCopyCode: (String) -> Unit
) {
    // Check if content contains code blocks ```...```
    val parts = content.split("```")
    if (parts.size <= 1) {
        // Standard markdown styled text
        Text(
            text = parseSimpleMarkdown(content),
            color = textColor,
            fontSize = 14.5.sp,
            lineHeight = 21.sp
        )
    } else {
        Column {
            for (i in parts.indices) {
                val part = parts[i]
                if (i % 2 == 0) {
                    // Normal text
                    if (part.isNotBlank()) {
                        Text(
                            text = parseSimpleMarkdown(part.trim()),
                            color = textColor,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                } else {
                    // Code block
                    val lines = part.trim().lines()
                    val language = if (lines.isNotEmpty() && !lines.first().contains(" ")) lines.first() else "code"
                    val codeContent = if (lines.size > 1 && !lines.first().contains(" ")) {
                        lines.drop(1).joinToString("\n")
                    } else {
                        part.trim()
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.ifBlank { "code" },
                                    color = LeoCyanSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onCopyCode(codeContent) }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ الكود",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "نسخ الكود",
                                        color = Color.LightGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = codeContent,
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseSimpleMarkdown(text: String) = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val boldIndex = remaining.indexOf("**")
        if (boldIndex != -1) {
            append(remaining.substring(0, boldIndex))
            val nextBold = remaining.indexOf("**", boldIndex + 2)
            if (nextBold != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(remaining.substring(boldIndex + 2, nextBold))
                }
                remaining = remaining.substring(nextBold + 2)
            } else {
                append(remaining.substring(boldIndex))
                break
            }
        } else {
            append(remaining)
            break
        }
    }
}

private fun formatModelTag(model: String): String {
    return when {
        model.contains("3.5-flash") -> "Gemini Flash"
        model.contains("3.1-pro") -> "Gemini Pro"
        model.contains("deepseek") -> "DeepSeek V3"
        else -> model
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun copyToClipboard(context: Context, text: String, toastMsg: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Leo AI", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, "مشاركة رد ليو AI")
    context.startActivity(shareIntent)
}
