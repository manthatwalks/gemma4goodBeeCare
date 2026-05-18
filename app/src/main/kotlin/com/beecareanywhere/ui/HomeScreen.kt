package com.beecareanywhere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beecareanywhere.data.Settings
import com.beecareanywhere.di.ServiceLocator
import com.beecareanywhere.ui.components.BeeFlightPath
import com.beecareanywhere.ui.components.BeeImage
import com.beecareanywhere.ui.components.ChunkyCard
import com.beecareanywhere.ui.components.HoneyHex
import com.beecareanywhere.ui.components.MaasaiShield
import com.beecareanywhere.ui.theme.BgCream
import com.beecareanywhere.ui.theme.CoralPastel
import com.beecareanywhere.ui.theme.HoneyYellow
import com.beecareanywhere.ui.theme.InkDark
import com.beecareanywhere.ui.theme.InkFaint
import com.beecareanywhere.ui.theme.InkSoft
import com.beecareanywhere.ui.theme.SagePastel
import com.beecareanywhere.ui.theme.SurfaceWhite

@Composable
fun HomeScreen(
    onCheckBees: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            settings = ServiceLocator.provideSettings(),
            checkIns = ServiceLocator.provideCheckInRepository(),
        ),
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val strings = strings(ui.language)
    val isSw = ui.language == Settings.Language.Swahili

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // ── Top row: brand + language toggle ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BeeImage(modifier = Modifier.size(height = 42.dp, width = 56.dp))
                Text(
                    text = "BeeCare",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = InkDark,
                    letterSpacing = 0.2.sp,
                )
            }
            LanguageToggle(
                current = ui.language,
                onSelect = vm::setLanguage,
            )
        }

        // ── Greeting ──────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.greetingBeekeeper,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                color = InkDark,
            )
            Text(
                text = strings.hivesActive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkSoft,
            )
        }

        // ── Hero "Check on your bees" card ────────────────────────────────
        ChunkyCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCheckBees),
            bgColor = HoneyYellow,
            cornerRadius = 28.dp,
            depth = 6.dp,
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 24.dp)) {
                // Honeycomb pattern — top-left, tilted
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-14).dp, y = (-8).dp)
                        .rotate(-8f)
                        .alpha(0.55f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(3) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(3) {
                                    HoneyHex(
                                        modifier = Modifier.size(26.dp),
                                        fillColor = Color(0x8CFFFFFF),
                                        strokeColor = InkDark,
                                        strokeWidth = 1.6f,
                                    )
                                }
                            }
                        }
                    }
                }
                // Dashed bee flight path — right half of the card
                BeeFlightPath(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.52f)
                        .height(220.dp),
                    strokeColor = InkDark,
                )
                // Decorative bee — top-right
                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).rotate(-12f)) {
                    BeeImage(
                        modifier = Modifier.size(height = 72.dp, width = 96.dp),
                        facingLeft = true,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth(0.65f).padding(top = 40.dp)) {
                    Text(
                        text = strings.checkOnBees,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        lineHeight = 32.sp,
                        color = InkDark,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.checkOnBeesSubtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkDark,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    // "Start →" pill
                    Box(
                        modifier = Modifier
                            .background(InkDark, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "${strings.start} →",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HoneyYellow,
                        )
                    }
                }
            }
        }

        // ── Mascot speech bubble ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MaasaiShield(
                modifier = Modifier.size(width = 72.dp, height = 97.dp),
                size = 72.dp,
                inkColor = InkDark,
                redColor = CoralPastel,
                greenColor = SagePastel,
            )
            SpeechBubble(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.asante,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    color = InkDark,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.asanteBody,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkSoft,
                    lineHeight = 17.sp,
                )
            }
        }

        // ── Recent check-ins list ─────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = strings.recentCheckins,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = InkDark,
            )
            if (ui.hives.isEmpty()) {
                Text(
                    text = if (ui.language == Settings.Language.Swahili)
                        "Bado hakuna kumbukumbu. Bonyeza “Kagua nyuki wako” kuanza."
                    else
                        "No check-ins yet. Tap “Check on your bees” to start one.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkSoft,
                )
            } else {
                ui.hives.forEach { hive ->
                    HiveRow(hive = hive)
                }
            }
        }
    }
}

@Composable
private fun LanguageToggle(
    current: Settings.Language,
    onSelect: (Settings.Language) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(SurfaceWhite, CircleShape)
            .border(2.5.dp, InkDark, CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        listOf(Settings.Language.English to "EN", Settings.Language.Swahili to "SW").forEach { (lang, label) ->
            val active = current == lang
            Box(
                modifier = Modifier
                    .background(if (active) InkDark else Color.Transparent, CircleShape)
                    .clickable { onSelect(lang) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.6.sp,
                    color = if (active) HoneyYellow else InkDark,
                )
            }
        }
    }
}

@Composable
private fun SpeechBubble(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // The tail pointing left is drawn via a nested Box offset trick
    Box(modifier = modifier) {
        // Tail (pointing left toward shield) — two triangles layered
        Box(
            modifier = Modifier
                .offset(x = (-10).dp, y = 0.dp)
                .align(Alignment.CenterStart)
                .size(12.dp, 16.dp)
                // CSS triangle via clip is not straightforward; use a rotated square instead
                .rotate(45f)
                .background(InkDark, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = (-6).dp, y = 0.dp)
                .align(Alignment.CenterStart)
                .size(10.dp, 14.dp)
                .rotate(45f)
                .background(SurfaceWhite, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite, RoundedCornerShape(18.dp))
                .border(2.5.dp, InkDark, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .offset(x = 4.dp), // slight indent to not overlap the tail
        ) {
            Column { content() }  // Column imported via foundation.layout
        }
    }
}

@Composable
private fun HiveRow(hive: HiveEntry) {
    ChunkyCard(
        modifier = Modifier.fillMaxWidth(),
        bgColor = SurfaceWhite,
        cornerRadius = 18.dp,
        depth = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hive icon bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(hive.toneHex), CircleShape)
                    .border(2.dp, InkDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hive.id,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = InkDark,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(hive.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = InkDark)
                Text(
                    text = hive.note,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = hive.when_,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp,
                color = InkFaint,
            )
        }
    }
}
