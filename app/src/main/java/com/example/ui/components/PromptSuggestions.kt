package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LeoCyanSecondary
import com.example.ui.theme.LeoIndigoPrimary

data class SuggestionItem(
    val icon: ImageVector,
    val title: String,
    val prompt: String
)

val defaultSuggestions = listOf(
    SuggestionItem(
        icon = Icons.Default.Code,
        title = "كود برمجي",
        prompt = "اكتب لي كود Kotlin لإنشاء واجهة بطاقة أنيقة في Jetpack Compose"
    ),
    SuggestionItem(
        icon = Icons.Default.Psychology,
        title = "تفكير وتحليل",
        prompt = "اشرح لي الفرق بين نماذج التفكير العميق والنماذج السريعة ببساطة"
    ),
    SuggestionItem(
        icon = Icons.Default.Lightbulb,
        title = "أفكار إبداعية",
        prompt = "اقترح 5 أفكار ابتكارية لتطبيقات أندرويد لعام 2026"
    ),
    SuggestionItem(
        icon = Icons.AutoMirrored.Filled.Subject,
        title = "تلخيص ذكي",
        prompt = "كيف يساعد الذكاء الاصطناعي في تنظيم الوقت وزيادة الإنتاجية؟"
    ),
    SuggestionItem(
        icon = Icons.Default.Translate,
        title = "ترجمة ولغات",
        prompt = "ترجم هذه العبارة إلى 3 لغات مع شرح السياق: 'النجاح يبدأ بخطوة واثقة'"
    ),
    SuggestionItem(
        icon = Icons.Default.AutoAwesome,
        title = "محادثة عامة",
        prompt = "مرحباً يا ليو! ما هي قدراتك وكيف يمكنك مساعدتي اليوم؟"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptSuggestions(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("prompt_suggestions_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Leo AI Hero Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.leo_logo),
                contentDescription = "Leo AI Emblem",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "مرحباً بك في ليو AI",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "مساعدك الذكي مع محادثات متطورة ومزامنة سحابية مستمرة",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "اختر فكرة للبدء أو اكتب ما يدور ببالك:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of suggestions
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultSuggestions.forEach { item ->
                SuggestionChipItem(
                    item = item,
                    onClick = { onSelectPrompt(item.prompt) }
                )
            }
        }
    }
}

@Composable
fun SuggestionChipItem(
    item: SuggestionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.testTag("suggestion_chip_${item.title}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = LeoCyanSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
