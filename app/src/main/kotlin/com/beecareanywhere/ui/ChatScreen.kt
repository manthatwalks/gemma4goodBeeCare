package com.beecareanywhere.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beecareanywhere.ui.components.BeeImage
import com.beecareanywhere.ui.components.ChunkyCard
import com.beecareanywhere.ui.components.MaasaiShield
import com.beecareanywhere.ui.theme.BgCream
import com.beecareanywhere.ui.theme.CoralPastel
import com.beecareanywhere.ui.theme.Hairline
import com.beecareanywhere.ui.theme.HoneyDeep
import com.beecareanywhere.ui.theme.HoneyYellow
import com.beecareanywhere.ui.theme.InkDark
import com.beecareanywhere.ui.theme.InkFaint
import com.beecareanywhere.ui.theme.InkSoft
import com.beecareanywhere.ui.theme.SagePastel
import com.beecareanywhere.ui.theme.SageDeep
import com.beecareanywhere.ui.theme.SurfaceWhite

@Composable
fun ChatScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val strings = strings(ui.language)
    val listState = rememberLazyListState()

    // Auto-scroll to bottom as messages arrive
    LaunchedEffect(ui.messages.size, ui.streamingResponse.length) {
        if (ui.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                (ui.messages.size + if (ui.isGenerating) 1 else 0) - 1,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = Hairline, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChatBackButton(onClick = onBack)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.beeCareDiagnosis,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = InkDark,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(7.dp).background(SageDeep, CircleShape))
                    Text(
                        text = strings.runningOffline,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        color = InkSoft,
                    )
                }
            }
            BeeImage(modifier = Modifier.size(height = 34.dp, width = 45.dp))
        }

        // ── Message list ───────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
        ) {
            items(ui.messages) { msg ->
                when (msg) {
                    is ChatMessage.User -> UserBubble(msg)
                    is ChatMessage.BeeResponse -> BeeBubble(text = msg.text)
                }
            }
            // Streaming / typing indicator
            if (ui.isGenerating) {
                item {
                    if (ui.streamingResponse.isNotBlank()) {
                        BeeBubble(text = ui.streamingResponse, streaming = true)
                    } else {
                        TypingBubble()
                    }
                }
            }
        }

        // ── Composer row ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCream)
                .border(width = 2.dp, color = Hairline, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Camera button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceWhite, CircleShape)
                    .border(2.dp, InkDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = InkDark,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Text input
            ChunkyCard(
                modifier = Modifier.weight(1f),
                bgColor = SurfaceWhite,
                cornerRadius = 999.dp,
                depth = 3.dp,
            ) {
                BasicTextField(
                    value = ui.chatDraft,
                    onValueChange = viewModel::updateChatDraft,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                        fontFamily = FontFamily.SansSerif,
                    ),
                    decorationBox = { inner ->
                        if (ui.chatDraft.isEmpty()) {
                            Text(
                                strings.typeFollowUp,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkFaint,
                            )
                        }
                        inner()
                    },
                )
            }

            // Send button
            ChunkyCard(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = viewModel::sendFollowup),
                bgColor = HoneyYellow,
                cornerRadius = 999.dp,
                depth = 3.dp,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("→", fontSize = 18.sp, fontWeight = FontWeight.Black, color = InkDark)
                }
            }
        }
    }
}

@Composable
private fun UserBubble(msg: ChatMessage.User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        if (msg.hasPhoto) {
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 110.dp)
                    .background(HoneyDeep, RoundedCornerShape(18.dp))
                    .border(2.dp, InkDark, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🐝  HIVE PHOTO", fontFamily = FontFamily.Serif, fontSize = 12.sp, color = InkDark)
            }
            Spacer(Modifier.height(6.dp))
        }
        ChunkyCard(
            modifier = Modifier,
            bgColor = CoralPastel,
            cornerRadius = 18.dp,
            depth = 3.dp,
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = msg.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    color = InkDark,
                )
            }
        }
    }
}

@Composable
private fun BeeBubble(text: String, streaming: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(HoneyYellow, CircleShape)
                .border(2.dp, InkDark, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MaasaiShield(
                modifier = Modifier.size(width = 22.dp, height = 30.dp),
                size = 22.dp,
            )
        }
        ChunkyCard(
            modifier = Modifier,
            bgColor = SurfaceWhite,
            cornerRadius = 18.dp,
            depth = 3.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                    color = InkDark,
                )
                if (streaming) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(InkDark, CircleShape)
                            .then(blinkModifier()),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(HoneyYellow, CircleShape)
                .border(2.dp, InkDark, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MaasaiShield(modifier = Modifier.size(width = 22.dp, height = 30.dp), size = 22.dp)
        }
        ChunkyCard(bgColor = SurfaceWhite, cornerRadius = 18.dp, depth = 3.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(0, 150, 300).forEach { delayMs ->
                    AnimatedDot(delayMs)
                }
            }
        }
    }
}

@Composable
private fun AnimatedDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .background(InkDark, CircleShape),
    )
}

@Composable
private fun blinkModifier(): Modifier {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursor_alpha",
    )
    return Modifier.alpha(alpha)
}

@Composable
private fun ChatBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(SurfaceWhite, CircleShape)
            .border(2.5.dp, InkDark, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("←", fontSize = 18.sp, color = InkDark, fontWeight = FontWeight.Bold)
    }
}
