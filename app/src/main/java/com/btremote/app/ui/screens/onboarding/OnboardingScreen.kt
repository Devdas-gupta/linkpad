package com.btremote.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btremote.app.ui.theme.GlowCyan
import com.btremote.app.ui.theme.GlowPink
import com.btremote.app.ui.theme.GlowPrimary
import com.btremote.app.ui.theme.NeonGreen

private data class OnboardPage(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val detail: String
)

private val pages = listOf(
    OnboardPage(
        icon = Icons.Outlined.PlayCircleOutline,
        iconColor = GlowPrimary,
        title = "Welcome to Linkpad",
        subtitle = "Your phone, your remote",
        detail = "Turn your Android into a wireless keyboard, mouse, and media remote for any Bluetooth device — no extra software needed."
    ),
    OnboardPage(
        icon = Icons.Outlined.Lock,
        iconColor = GlowCyan,
        title = "Bluetooth Permissions",
        subtitle = "One-time setup",
        detail = "Linkpad needs Bluetooth access to connect to your devices. Tap \"Grant\" on the next dialog — permissions are used only for Bluetooth HID control."
    ),
    OnboardPage(
        icon = Icons.Outlined.Bluetooth,
        iconColor = GlowPink,
        title = "Pair Your First Device",
        subtitle = "Mac · PC · iPad · TV",
        detail = "Open the Connect tab, tap Scan, and select your device. It will appear as \"Linkpad\" in your Bluetooth settings. Accept the pairing — done!"
    ),
    OnboardPage(
        icon = Icons.Outlined.Check,
        iconColor = NeonGreen,
        title = "You're All Set",
        subtitle = "Start controlling",
        detail = "Use the Touchpad, Keyboard, Air Mouse, Media, or TV Remote tabs. Swipe between them anytime. Enjoy!"
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val finish = {
        viewModel.complete()
        onFinish()
    }

    var page by remember { mutableIntStateOf(0) }
    val current = pages[page.coerceIn(pages.indices)]
    val isLast  = page >= pages.lastIndex

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgColors = if (isLight) {
        listOf(Color(0xFFF4F7FC), Color(0xFFE8EEFA))
    } else {
        listOf(Color(0xFF0A0418), Color(0xFF04040F))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        // Skip button top-right
        TextButton(
            onClick = finish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState > initialState)
                        (slideInHorizontally { it } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { -it } + fadeOut(tween(200)))
                    else
                        (slideInHorizontally { -it } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(200)))
                },
                label = "onboardContent"
            ) { p ->
                val pg = pages[p.coerceIn(pages.indices)]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Icon glow orb
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        pg.iconColor.copy(alpha = 0.30f),
                                        pg.iconColor.copy(alpha = 0.06f)
                                    )
                                )
                            )
                            .border(1.dp, pg.iconColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(pg.icon, null, tint = pg.iconColor, modifier = Modifier.size(52.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            pg.subtitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                            color = pg.iconColor.copy(alpha = 0.8f)
                        )
                        Text(
                            pg.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            pg.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Page dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (i == page) pages[i].iconColor
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Next / Get Started button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (isLight) {
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            } else {
                                listOf(current.iconColor, current.iconColor.copy(alpha = 0.65f))
                            }
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (page >= pages.lastIndex) finish() else page++
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isLast) "GET STARTED" else "NEXT",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
