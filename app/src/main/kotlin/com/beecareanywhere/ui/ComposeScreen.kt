package com.beecareanywhere.ui

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beecareanywhere.data.Settings
import com.beecareanywhere.multimodal.rememberImageCapture
import com.beecareanywhere.multimodal.rememberPermissionRequest
import com.beecareanywhere.ui.components.BeeImage
import com.beecareanywhere.ui.components.ChunkyCard
import com.beecareanywhere.ui.components.MaasaiShield
import com.beecareanywhere.ui.theme.BgCream
import com.beecareanywhere.ui.theme.BlushLight
import com.beecareanywhere.ui.theme.CoralPastel
import com.beecareanywhere.ui.theme.Hairline
import com.beecareanywhere.ui.theme.HoneyDeep
import com.beecareanywhere.ui.theme.HoneyYellow
import com.beecareanywhere.ui.theme.InkDark
import com.beecareanywhere.ui.theme.InkFaint
import com.beecareanywhere.ui.theme.InkSoft
import com.beecareanywhere.ui.theme.MintLight
import com.beecareanywhere.ui.theme.SagePastel
import com.beecareanywhere.ui.theme.SurfaceWhite

@Composable
fun ComposeScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val strings = strings(ui.language)

    val launchCamera = rememberImageCapture(onResult = viewModel::onImageCaptured)
    val requestCameraPermission = rememberPermissionRequest(Manifest.permission.CAMERA) { granted ->
        if (granted) launchCamera() else viewModel.showError(strings.cameraPermissionDenied)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Mascot peek — top-right ────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp)
                .offset(x = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                // Speech bubble with right-pointing tail (toward the shield)
                Box {
                    // Bubble body
                    Box(
                        modifier = Modifier
                            .background(SurfaceWhite, RoundedCornerShape(16.dp))
                            .border(2.5.dp, InkDark, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .width(200.dp),
                    ) {
                        Text(
                            text = strings.mascotTip,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = InkDark,
                            textAlign = TextAlign.Start,
                        )
                    }
                    // Tail — black outline diamond
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 8.dp)
                            .size(14.dp)
                            .rotate(45f)
                            .background(InkDark, RoundedCornerShape(2.dp))
                    )
                    // Tail — white inner diamond (slightly smaller, masks the bubble border seam)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 4.dp)
                            .size(11.dp)
                            .rotate(45f)
                            .background(SurfaceWhite, RoundedCornerShape(2.dp))
                    )
                }
                // Shield rotated so top leans in
                Box(modifier = Modifier.rotate(-18f).offset(x = 8.dp)) {
                    MaasaiShield(
                        modifier = Modifier.size(width = 96.dp, height = 130.dp),
                        size = 96.dp,
                    )
                }
            }
        }

        // ── Scrollable form ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp), // room for sticky diagnose bar
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackButton(onClick = onBack)
                Text(
                    text = strings.newCheckin,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = InkDark,
                )
            }

            Spacer(Modifier.height(130.dp)) // space for the mascot overlay

            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Step 1 — Photo
                StepSection(num = "1", tone = HoneyYellow, label = strings.snapAPhoto) {
                    if (ui.capturedImage == null) {
                        PhotoPlaceholder(strings = strings, onClick = requestCameraPermission)
                    } else {
                        PhotoAttached(
                            strings = strings,
                            onRetake = requestCameraPermission,
                            onRemove = viewModel::clearImage,
                        )
                    }
                }

                // Step 2 — Describe
                StepSection(num = "2", tone = CoralPastel, label = strings.describeWhatYouSee) {
                    DescribeButton(
                        text = ui.description,
                        placeholder = strings.describePlaceholder,
                        onClick = { viewModel.setShowDescribePopup(true) },
                    )
                }

                // Step 3 — Question
                StepSection(num = "3", tone = SagePastel, label = strings.askAQuestion) {
                    DescribeButton(
                        text = ui.question,
                        placeholder = strings.questionPlaceholder,
                        minHeight = 64.dp,
                        onClick = { viewModel.setShowQuestionPopup(true) },
                    )
                }
            }
        }

        // ── Sticky diagnose bar ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, BgCream),
                    ),
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            ChunkyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::submitDiagnosis),
                bgColor = HoneyYellow,
                cornerRadius = 999.dp,
                depth = 5.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BeeImage(modifier = Modifier.size(height = 26.dp, width = 34.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.diagnose,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = InkDark,
                    )
                }
            }
        }

        // ── Describe popup ─────────────────────────────────────────────────
        if (ui.showDescribePopup) {
            TextInputPopup(
                title = strings.describeYourPhoto,
                stepNum = "2",
                stepTone = CoralPastel,
                value = ui.description,
                placeholder = buildDescribePlaceholder(ui.language == Settings.Language.Swahili),
                chips = strings.describeChips,
                chipSectionLabel = strings.tapToAdd,
                saveLabel = strings.saveDescription,
                cancelLabel = strings.cancel,
                onChange = viewModel::updateDescription,
                onSave = { viewModel.setShowDescribePopup(false) },
                onClose = { viewModel.setShowDescribePopup(false) },
            )
        }

        // ── Question popup ─────────────────────────────────────────────────
        if (ui.showQuestionPopup) {
            TextInputPopup(
                title = strings.askAQuestion,
                stepNum = "3",
                stepTone = SagePastel,
                value = ui.question,
                placeholder = buildQuestionPlaceholder(ui.language == Settings.Language.Swahili),
                chips = strings.questionChips,
                chipSectionLabel = strings.exampleQuestions,
                saveLabel = strings.saveQuestion,
                cancelLabel = strings.cancel,
                onChange = viewModel::updateQuestion,
                onSave = { viewModel.setShowQuestionPopup(false) },
                onClose = { viewModel.setShowQuestionPopup(false) },
            )
        }
    }
}

@Composable
private fun StepSection(
    num: String,
    tone: Color,
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(tone, CircleShape)
                    .border(2.dp, InkDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(num, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkDark)
            }
            Text(label, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 18.sp, color = InkDark)
        }
        content()
    }
}

@Composable
private fun PhotoPlaceholder(strings: UiStrings, onClick: () -> Unit) {
    ChunkyCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        bgColor = BlushLight,
        cornerRadius = 22.dp,
        depth = 4.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(SurfaceWhite, RoundedCornerShape(16.dp))
                    .border(2.dp, InkDark, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = InkDark,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(strings.tapToTakePhoto, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InkDark)
            Text(strings.orUploadGallery, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSoft)
        }
    }
}

@Composable
private fun PhotoAttached(strings: UiStrings, onRetake: () -> Unit, onRemove: () -> Unit) {
    ChunkyCard(modifier = Modifier.fillMaxWidth(), bgColor = SurfaceWhite, cornerRadius = 22.dp, depth = 4.dp) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Photo thumbnail placeholder (honeycomb pattern)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HoneyDeep)
                    .border(2.dp, InkDark, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🐝", fontSize = 32.sp)
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(strings.photoAttachedLabel, fontWeight = FontWeight.Black, fontSize = 14.sp, color = InkDark)
                    Text(strings.justNow, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSoft)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton(label = strings.retake, onClick = onRetake)
                    ChipButton(label = strings.remove, bgColor = CoralPastel, onClick = onRemove)
                }
            }
        }
    }
}

@Composable
private fun DescribeButton(
    text: String,
    placeholder: String,
    onClick: () -> Unit,
    minHeight: androidx.compose.ui.unit.Dp = 88.dp,
) {
    ChunkyCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        bgColor = SurfaceWhite,
        cornerRadius = 18.dp,
        depth = 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp).height(minHeight)) {
            Text(
                text = text.ifBlank { placeholder },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (text.isBlank()) InkFaint else InkDark,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun ChipButton(
    label: String,
    bgColor: Color = SurfaceWhite,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(bgColor, CircleShape)
            .border(2.dp, InkDark, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 12.sp, color = InkDark)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(SurfaceWhite, CircleShape)
            .border(2.5.dp, InkDark, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("←", fontSize = 18.sp, color = InkDark, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextInputPopup(
    title: String,
    stepNum: String,
    stepTone: Color,
    value: String,
    placeholder: String,
    chips: List<String>,
    chipSectionLabel: String,
    saveLabel: String,
    cancelLabel: String,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        // Open the keyboard automatically when the popup appears.
        // requestFocus alone is sometimes not enough on emulators with hardware
        // keyboard mode; explicitly calling show() forces the IME up.
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            kotlinx.coroutines.delay(80)  // wait for focus to settle
            keyboard?.show()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCream)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 2.dp, color = Hairline, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackButton(onClick = onClose)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(stepTone, CircleShape)
                        .border(2.dp, InkDark, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stepNum, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkDark)
                }
                Text(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 20.sp, color = InkDark)
            }

            // ── Text area — sizes to its content; minLines keeps it readable ─
            ChunkyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                bgColor = SurfaceWhite,
                cornerRadius = 18.dp,
                depth = 3.dp,
            ) {
                androidx.compose.material3.TextField(
                    value = value,
                    onValueChange = onChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    minLines = 6,
                    maxLines = 10,
                    placeholder = {
                        Text(
                            placeholder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkFaint,
                            lineHeight = 22.sp,
                        )
                    },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkDark,
                        lineHeight = 22.sp,
                    ),
                )
            }

            // Push chips + buttons to the bottom of the available space
            Spacer(Modifier.weight(1f))

            // ── Quick-add chips ────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    chipSectionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = InkSoft,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { chip ->
                        ChipButton(
                            label = "+ $chip",
                            onClick = { onChange(if (value.isNotBlank()) "$value $chip" else chip) },
                        )
                    }
                }
            }

            // ── Bottom action bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCream)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceWhite, CircleShape)
                        .border(2.dp, InkDark, CircleShape)
                        .clickable(onClick = onClose)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(cancelLabel, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkDark)
                }
                ChunkyCard(
                    modifier = Modifier.weight(2f).clickable(onClick = onSave),
                    bgColor = HoneyYellow,
                    cornerRadius = 999.dp,
                    depth = 4.dp,
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(saveLabel, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkDark)
                    }
                }
            }
        }
    }
}

private fun buildDescribePlaceholder(isSw: Boolean): String = if (isSw) """
Ninaweza kutumia picha, lakini ili kuepuka utambuzi mbaya tafadhali eleza unachoona.

· Hawa ni nyuki wako, wadudu, nyuki waliokufa, utando, siafu, mende, au kitu kingine?
· Hali ya hewa ikoje?
· Wanaruka sana, wanaondoka kwenye mzinga, au wamejikusanya sehemu moja?
· Hii ni mlangoni, ukutani, fremu, sega, au stendi ya mzinga?
""".trim() else """
I can use the photo, but to avoid misdiagnosis please describe what you see.

· Are these your bees, pests, dead bees, webbing, ants, beetles, or something else?
· What is the weather like?
· Are they flying heavily, leaving the hive, or mostly clustered in one place?
· Is this on the entrance, outside wall, frame, comb, or hive stand?
""".trim()

private fun buildQuestionPlaceholder(isSw: Boolean): String = if (isSw) """
Andika swali lako kwa msaidizi wa nyuki.

· Kwa nini nyuki wangu wanafanya hivi?
· Ni salama kuvuna sasa?
· Itachukua muda gani kupona?
· Nifanye nini baadaye?
""".trim() else """
Type your question for the bee assistant.

· Why are my bees behaving this way?
· Is it safe to harvest now?
· How long until they recover?
· What should I do next?
""".trim()
