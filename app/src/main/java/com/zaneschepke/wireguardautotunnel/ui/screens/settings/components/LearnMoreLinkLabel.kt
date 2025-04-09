package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun LearnMoreLinkLabel(onClick: (url: String) -> Unit, url: String) {
    // TODO update link when docs are fully updated
    val gettingStarted = buildAnnotatedString {
        pushStringAnnotation(tag = "details", annotation = url)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(stringResource(id = R.string.learn_more))
        }
        pop()
    }
    ClickableText(
        text = gettingStarted,
        style =
            MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                fontStyle = FontStyle.Italic,
            ),
    ) {
        gettingStarted.getStringAnnotations(tag = "details", it, it).firstOrNull()?.let { annotation
            ->
            onClick(annotation.item)
        }
    }
}
