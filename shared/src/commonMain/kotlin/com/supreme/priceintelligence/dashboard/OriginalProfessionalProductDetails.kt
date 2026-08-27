package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.rememberUrlOpener
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.logo_amazon
import com.supreme.priceintelligence.resources.logo_flipkart
import com.supreme.priceintelligence.settings.InsightCustomization
import com.supreme.priceintelligence.settings.SectionStartState
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
@Composable
internal fun OriginalProfessionalProductDetailDialog(
    card: ProductCardUiState,
    networkState: BloomState,
    advancedModeEnabled: Boolean,
    reduceMotionEnabled: Boolean,
    insightCustomization: InsightCustomization =
        InsightCustomization(),
    isHistoryLoading: Boolean,
    priceHistory: List<PriceHistoryEntry>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val item = card.item
    val openUrl = rememberUrlOpener()
    var imageViewerOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var isPriceHistoryExpanded by rememberSaveable(
        insightCustomization.advancedInfoStartState
    ) {
        mutableStateOf(
            insightCustomization.advancedInfoStartState ==
                SectionStartState.EXPANDED
        )
    }

    val historyChevronRotation by animateFloatAsState(
        targetValue =
            if (isPriceHistoryExpanded) {
                180f
            } else {
                0f
            },
        animationSpec = tween(
            durationMillis =
                if (reduceMotionEnabled) {
                    0
                } else {
                    180
                }
        ),
        label = "priceHistoryChevronRotation"
    )

    var showNetworkBanner by remember {
        mutableStateOf(false)
    }
    var networkMessage by remember {
        mutableStateOf("")
    }
    var networkIsError by remember {
        mutableStateOf(false)
    }
    var networkBannerRequest by remember {
        mutableStateOf(0)
    }

    val networkSpeedText = rememberNetworkSpeedText(
        isActive = card.isRefreshing
    )

    val fetchSpotlightPosition = remember {
        Animatable(-0.18f)
    }

    LaunchedEffect(
        card.isRefreshing,
        reduceMotionEnabled
    ) {
        when {
            !card.isRefreshing -> {
                fetchSpotlightPosition.snapTo(-0.18f)
            }

            reduceMotionEnabled -> {
                fetchSpotlightPosition.snapTo(0.12f)
            }

            else -> {
                fetchSpotlightPosition.snapTo(-0.18f)

                while (true) {
                    fetchSpotlightPosition.animateTo(
                        targetValue = 1.18f,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = LinearEasing
                        )
                    )

                    fetchSpotlightPosition.animateTo(
                        targetValue = -0.18f,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
    }

    var fetchStartedAt by remember {
        mutableStateOf<TimeMark?>(null)
    }

    var lastFetchDurationMs by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(card.isRefreshing) {
        if (card.isRefreshing) {
            fetchStartedAt = TimeSource.Monotonic.markNow()
            lastFetchDurationMs = null
        } else {
            fetchStartedAt?.let { startedAt ->
                lastFetchDurationMs = startedAt
                    .elapsedNow()
                    .inWholeMilliseconds
                    .coerceAtLeast(1L)
            }

            fetchStartedAt = null
        }
    }

    LaunchedEffect(networkState, networkBannerRequest) {
        when (networkState) {
            BloomState.ERROR -> {
                networkMessage = "No internet connection"
                networkIsError = true
                showNetworkBanner = true
                delay(5000.milliseconds)
                showNetworkBanner = false
            }

            BloomState.WARNING -> {
                networkMessage = "Slow or unstable connection"
                networkIsError = false
                showNetworkBanner = true
                delay(5000.milliseconds)
                showNetworkBanner = false
            }

            BloomState.SUCCESS,
            BloomState.NONE -> {
                showNetworkBanner = false
            }
        }
    }

    val refreshWithNetworkFeedback = {
        if (networkState == BloomState.ERROR) {
            networkBannerRequest += 1
        }

        onRefresh()
    }

    val hasRetailerUrl =
        !item.amazonUrl.isNullOrBlank() ||
            !item.flipkartUrl.isNullOrBlank()

    val productNeedsFreshPrice = remember(
        item.id,
        item.amazonUrl,
        item.flipkartUrl,
        item.amazonLastPrice,
        item.flipkartLastPrice,
        item.amazonLastChecked,
        item.flipkartLastChecked
    ) {
        item.needsPriceCheck(
            nowMillis =
                Clock.System.now()
                    .toEpochMilliseconds()
        )
    }

    LaunchedEffect(
        item.id,
        productNeedsFreshPrice
    ) {
        if (
            hasRetailerUrl &&
            productNeedsFreshPrice &&
            card.amazonResult == null &&
            card.flipkartResult == null &&
            !card.isRefreshing
        ) {
            refreshWithNetworkFeedback()
        }
    }

    val amazonLivePrice = validDetailPrice(card.amazonResult?.price)
    val flipkartLivePrice = validDetailPrice(card.flipkartResult?.price)
    val amazonSavedPrice = validDetailPrice(item.amazonLastPrice)
    val flipkartSavedPrice = validDetailPrice(item.flipkartLastPrice)
    val amazonBlocked = card.amazonResult?.blocked == true
    val flipkartBlocked = card.flipkartResult?.blocked == true

    val amazonPrice = amazonLivePrice ?: amazonSavedPrice
    val flipkartPrice = flipkartLivePrice ?: flipkartSavedPrice
    val availablePrices = listOfNotNull(
        amazonPrice,
        flipkartPrice
    )

    val anyOnlinePriceIsLower = availablePrices.any { onlinePrice ->
        onlinePrice < item.shopPrice - 0.01
    }

    val hasLiveComparison =
        amazonLivePrice != null || flipkartLivePrice != null

    val detailScrollState = rememberScrollState()
    val showDetailBloom = hasLiveComparison

    val resultBloomProgress = remember {
        Animatable(0f)
    }

    LaunchedEffect(
        showDetailBloom,
        card.isRefreshing,
        reduceMotionEnabled
    ) {
        when {
            card.isRefreshing || !showDetailBloom -> {
                resultBloomProgress.snapTo(0f)
            }

            reduceMotionEnabled -> {
                resultBloomProgress.snapTo(0.92f)
            }

            else -> {
                resultBloomProgress.snapTo(0f)

                resultBloomProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 650
                    )
                )

                resultBloomProgress.animateTo(
                    targetValue = 0.76f,
                    animationSpec = tween(
                        durationMillis = 700
                    )
                )

                resultBloomProgress.animateTo(
                    targetValue = 0.92f,
                    animationSpec = tween(
                        durationMillis = 850
                    )
                )
            }
        }
    }

    val glowPrimary by animateColorAsState(
        targetValue = when {
            card.isRefreshing ->
                Color.Transparent

            !showDetailBloom ->
                Color.Transparent

            anyOnlinePriceIsLower ->
                MaterialTheme.colorScheme.error

            availablePrices.isNotEmpty() ->
                MaterialTheme.supremeColors.competitive

            else ->
                MaterialTheme.colorScheme.surface
        },
        label = "professionalDetailGlowPrimary"
    )

    val glowSecondary by animateColorAsState(
        targetValue = when {
            card.isRefreshing ->
                Color.Transparent

            !showDetailBloom ->
                Color.Transparent

            anyOnlinePriceIsLower ->
                MaterialTheme.colorScheme.error

            availablePrices.isNotEmpty() ->
                MaterialTheme.supremeColors.competitive

            else ->
                MaterialTheme.colorScheme.surface
        },
        label = "professionalDetailGlowSecondary"
    )

    val lastChecked = maxOf(
        item.amazonLastChecked ?: 0L,
        item.flipkartLastChecked ?: 0L
    )

    val statusText = when {
        card.isRefreshing ->
            "Checking live prices…"

        amazonLivePrice != null || flipkartLivePrice != null ->
            "Last checked: Just now"

        lastChecked > 0L ->
            "Last checked: ${formatTimeAgo(lastChecked)}"

        else ->
            "Prices not checked"
    }

    val dialogMotionProgress = remember {
        Animatable(0f)
    }

    var dismissRequested by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        dismissRequested,
        reduceMotionEnabled
    ) {
        if (dismissRequested) {
            if (reduceMotionEnabled) {
                dialogMotionProgress.snapTo(0f)
            } else {
                dialogMotionProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 180
                    )
                )
            }

            onDismiss()
        } else {
            if (reduceMotionEnabled) {
                dialogMotionProgress.snapTo(1f)
            } else {
                dialogMotionProgress.snapTo(0f)
                dialogMotionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 220
                    )
                )
            }
        }
    }

    val requestDismiss = {
        if (!dismissRequested) {
            dismissRequested = true
        }
    }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.supremeColors.scrim.copy(
                        alpha = if (
                            MaterialTheme.supremeColors.isDark
                        ) {
                            0.78f
                        } else {
                            0.52f
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .heightIn(max = 760.dp)
                    .graphicsLayer {
                        val progress =
                            dialogMotionProgress.value

                        alpha = progress
                        scaleX =
                            0.96f + (0.04f * progress)
                        scaleY =
                            0.96f + (0.04f * progress)
                    },
                shape = RoundedCornerShape(24.dp),
                color =
                    MaterialTheme.supremeColors.panelStrong,
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.supremeColors.border
                ),
                shadowElevation =
                    if (MaterialTheme.supremeColors.isDark) {
                        0.dp
                    } else {
                        14.dp
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (card.isRefreshing) {
                                val spotlightCenterY =
                                    size.height *
                                        fetchSpotlightPosition.value

                                val spotlightHalfHeight =
                                    120.dp.toPx()

                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xFFF59E0B)
                                                    .copy(
                                                        alpha =
                                                            0.08f
                                                    ),
                                                Color(0xFFFBBF24)
                                                    .copy(
                                                        alpha =
                                                            0.30f
                                                    ),
                                                Color(0xFFF59E0B)
                                                    .copy(
                                                        alpha =
                                                            0.08f
                                                    ),
                                                Color.Transparent
                                            ),
                                            startY =
                                                spotlightCenterY -
                                                    spotlightHalfHeight,
                                            endY =
                                                spotlightCenterY +
                                                    spotlightHalfHeight
                                        )
                                )
                            } else if (showDetailBloom) {
                                val bloomProgress =
                                    resultBloomProgress.value

                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                glowPrimary.copy(
                                                    alpha =
                                                        0.28f *
                                                            bloomProgress
                                                ),
                                                glowSecondary.copy(
                                                    alpha =
                                                        0.14f *
                                                            bloomProgress
                                                ),
                                                glowSecondary.copy(
                                                    alpha =
                                                        0.06f *
                                                            bloomProgress
                                                ),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY =
                                                size.height *
                                                    (
                                                        0.28f +
                                                            (
                                                                0.24f *
                                                                    bloomProgress
                                                            )
                                                        )
                                        )
                                )

                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors = listOf(
                                                glowPrimary.copy(
                                                    alpha =
                                                        0.40f *
                                                            bloomProgress
                                                ),
                                                glowSecondary.copy(
                                                    alpha =
                                                        0.20f *
                                                            bloomProgress
                                                ),
                                                glowSecondary.copy(
                                                    alpha =
                                                        0.08f *
                                                            bloomProgress
                                                ),
                                                Color.Transparent
                                            ),
                                            center = Offset(
                                                x = size.width / 2f,
                                                y = 0f
                                            ),
                                            radius =
                                                size.width *
                                                    (
                                                        0.55f +
                                                            (
                                                                0.40f *
                                                                    bloomProgress
                                                            )
                                                        )
                                        )
                                )
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 18.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (card.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    strokeWidth = 1.5.dp
                                )

                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Text(
                                text = statusText,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )

                            if (
                                card.isRefreshing &&
                                networkSpeedText != null
                            ) {
                                Text(
                                    text = "  •  ",
                                    color = Color(0xFF64748B)
                                        .copy(alpha = 0.50f),
                                    fontSize = 11.sp
                                )

                                Text(
                                    text = "↓ $networkSpeedText",
                                    color = if (
                                        networkSpeedText == "0 B/s"
                                    ) {
                                        Color(0xFF64748B)
                                    } else {
                                        Color(0xFF10B981)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            } else if (!card.isRefreshing) {
                                lastFetchDurationMs?.let { durationMs ->
                                    Text(
                                        text = "  •  ",
                                        color = Color(0xFF64748B)
                                            .copy(alpha = 0.50f),
                                        fontSize = 11.sp
                                    )

                                    Text(
                                        text = "⚡ ${
                                            formatFetchDuration(durationMs)
                                        }",
                                        color = if (durationMs > 3000L) {
                                            Color(0xFFF59E0B)
                                        } else {
                                            Color(0xFF64748B)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = requestDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close product details",
                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(detailScrollState)
                            .padding(
                                start = 18.dp,
                                end = 18.dp,
                                bottom = 18.dp
                            )
                    ) {
                        AnimatedVisibility(
                            visible = showNetworkBanner,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (networkIsError) {
                                        Color(0xFFEF4444)
                                    } else {
                                        Color(0xFFF59E0B)
                                    }
                                )
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 10.dp
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = networkMessage,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        showNetworkBanner = false
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription =
                                            "Dismiss network message",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight()
                                .background(
                                    color =
                                        MaterialTheme
                                            .supremeColors
                                            .panelMuted,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme
                                            .supremeColors
                                            .border,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            ProfessionalProductImage(
                                imageUrl = item.imageUrl,
                                productName = item.productName,
                                onClick = {
                                    imageViewerOpen = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.productName,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                fontWeight =
                                    FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Supreme Price",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                fontSize = 11.sp
                            )

                            Text(
                                text =
                                    formatIndianPrice(
                                        item.shopPrice
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface,
                                fontSize = 22.sp,
                                lineHeight = 27.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                maxLines = 2
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfessionalRetailerPriceCard(
                                retailerName = "Amazon",
                                logo = Res.drawable.logo_amazon,
                                shopPrice = item.shopPrice,
                                livePrice = amazonLivePrice,
                                savedPrice = amazonSavedPrice,
                                isLoading = card.isRefreshing,
                                isBlocked = amazonBlocked,
                                needsLightLogoBackground = true,
                                reduceMotionEnabled =
                                    reduceMotionEnabled,
                                modifier = Modifier.weight(1f)
                            )

                            ProfessionalRetailerPriceCard(
                                retailerName = "Flipkart",
                                logo = Res.drawable.logo_flipkart,
                                shopPrice = item.shopPrice,
                                livePrice = flipkartLivePrice,
                                savedPrice = flipkartSavedPrice,
                                isLoading = card.isRefreshing,
                                isBlocked = flipkartBlocked,
                                needsLightLogoBackground = false,
                                reduceMotionEnabled =
                                    reduceMotionEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item.amazonUrl
                            ?.takeIf { url -> url.isNotBlank() }
                            ?.let { url ->
                                ProfessionalRetailerLink(
                                    label = "Amazon",
                                    onClick = {
                                        openUrl(url)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                        item.flipkartUrl
                            ?.takeIf { url -> url.isNotBlank() }
                            ?.let { url ->
                                ProfessionalRetailerLink(
                                    label = "Flipkart",
                                    onClick = {
                                        openUrl(url)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .clickable(
                                enabled = !card.isRefreshing &&
                                        (
                                                !item.amazonUrl.isNullOrBlank() ||
                                                        !item.flipkartUrl.isNullOrBlank()
                                                ),
                                onClick = refreshWithNetworkFeedback
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.supremeColors.panelMuted,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.supremeColors.border
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 13.dp
                            ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (card.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    color = Color(0xFF94A3B8),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (card.isRefreshing) {
                                    "Checking prices…"
                                } else {
                                    "Refresh Live Prices"
                                },
                                color = if (card.isRefreshing) {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (advancedModeEnabled) {
                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    MaterialTheme.supremeColors.divider
                                )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isPriceHistoryExpanded = !isPriceHistoryExpanded
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Advanced price information",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription =
                                    if (isPriceHistoryExpanded) {
                                        "Collapse"
                                    } else {
                                        "Expand"
                                    },
                                tint =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        rotationZ =
                                            historyChevronRotation
                                    }
                            )
                        }

                        AnimatedVisibility(
                            visible = isPriceHistoryExpanded,
                            enter =
                                if (reduceMotionEnabled) {
                                    fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 80
                                        )
                                    )
                                } else {
                                    expandVertically(
                                        animationSpec = tween(
                                            durationMillis = 220
                                        ),
                                        expandFrom = Alignment.Top
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 160
                                        )
                                    )
                                },
                            exit =
                                if (reduceMotionEnabled) {
                                    fadeOut(
                                        animationSpec = tween(
                                            durationMillis = 80
                                        )
                                    )
                                } else {
                                    shrinkVertically(
                                        animationSpec = tween(
                                            durationMillis = 180
                                        ),
                                        shrinkTowards = Alignment.Top
                                    ) + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = 120
                                        )
                                    )
                                }
                        ) {
                            Column {
                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                PriceHistorySection(
                                    entries = priceHistory,
                                    isLoading = isHistoryLoading,
                                    shopPrice = item.shopPrice,
                                    informationLevel =
                                        insightCustomization
                                            .advancedInfoLevel,
                                    range =
                                        insightCustomization
                                            .priceHistoryRange,
                                    graphStyle =
                                        insightCustomization
                                            .historyGraphStyle,
                                    graphSize =
                                        insightCustomization
                                            .graphSize,
                                    pointMode =
                                        insightCustomization
                                            .graphPointMode,
                                    retailerChartPalette =
                                        insightCustomization
                                            .retailerChartPalette,
                                    customRetailerChartColors =
                                        insightCustomization
                                            .customRetailerChartColors
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (imageViewerOpen && !item.imageUrl.isNullOrBlank()) {
        ProfessionalImageViewer(
            imageUrl = item.imageUrl,
            productName = item.productName,
            onDismiss = {
                imageViewerOpen = false
            }
        )
    }
}

@Composable
private fun ProfessionalProductImage(
    imageUrl: String?,
    productName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .clickable(
                enabled = !imageUrl.isNullOrBlank(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = productName
                .trim()
                .firstOrNull()
                ?.uppercase()
                ?: "P",
            color = Color(0xFF475569),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
            )
        }
    }
}

@Composable
private fun ProfessionalRetailerPriceCard(
    retailerName: String,
    logo: DrawableResource,
    shopPrice: Double,
    livePrice: Double?,
    savedPrice: Double?,
    isLoading: Boolean,
    isBlocked: Boolean = false,
    needsLightLogoBackground: Boolean,
    reduceMotionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val activePrice = livePrice ?: savedPrice
    val difference = activePrice?.let { onlinePrice ->
        shopPrice - onlinePrice
    }

    val comparison = when {
        difference == null ->
            "UNAVAILABLE"

        difference.absoluteValue <= 0.01 ->
            "MATCHED"

        difference > 0.0 ->
            "LOWER"

        else ->
            "HIGHER"
    }

    val differenceText = when {
        difference == null && isBlocked ->
            if (isLoading) "Checking…" else "$retailerName blocked this check"

        difference == null ->
            if (isLoading) "Checking…" else "Price unavailable"

        difference.absoluteValue <= 0.01 ->
            "Matches shop"

        difference > 0.0 ->
            "${formatIndianPrice(difference)} lower"

        else ->
            "${formatIndianPrice(difference.absoluteValue)} higher"
    }

    val priceColor = when {
        difference == null ->
            MaterialTheme.colorScheme.onSurfaceVariant

        difference > 0.01 ->
            MaterialTheme.colorScheme.error

        else ->
            MaterialTheme.supremeColors.competitive
    }

    val borderColor = when {
        difference == null ->
            MaterialTheme.supremeColors.border

        difference > 0.01 ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.55f)

        else ->
            MaterialTheme.supremeColors.competitive.copy(alpha = 0.55f)
    }

    val arrow = when {
        difference == null ->
            ""

        difference > 0.01 ->
            "▼ "

        difference < -0.01 ->
            "▲ "

        else ->
            ""
    }

    val source = when {
        livePrice != null ->
            "LIVE"

        savedPrice != null ->
            "SAVED"

        else ->
            null
    }

    val priceDisplay =
        (
            if (activePrice != null) {
                "$arrow${formatIndianPrice(activePrice)}"
            } else if (isLoading) {
                "Checking…"
            } else {
                "Unavailable"
            }
        ) to (activePrice != null)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.supremeColors.panelMuted,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = if (needsLightLogoBackground) {
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            } else {
                Modifier.fillMaxWidth()
            },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(logo),
                contentDescription = retailerName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (needsLightLogoBackground) {
                            27.dp
                        } else {
                            35.dp
                        }
                    ),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        AnimatedContent(
            targetState = priceDisplay,
            transitionSpec = {
                if (reduceMotionEnabled) {
                    EnterTransition.None togetherWith
                        ExitTransition.None
                } else {
                    (
                        slideInVertically(
                            animationSpec = tween(
                                durationMillis = 180
                            )
                        ) { height ->
                            height / 3
                        } +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 150
                                )
                            )
                    ) togetherWith (
                        slideOutVertically(
                            animationSpec = tween(
                                durationMillis = 140
                            )
                        ) { height ->
                            -height / 3
                        } +
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 120
                                )
                            )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = "retailerPriceChange"
        ) { displayedPrice ->
            Text(
                text = displayedPrice.first,
                color = priceColor,
                fontSize = if (displayedPrice.second) {
                    18.sp
                } else {
                    12.sp
                },
                lineHeight = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }

        if (source != null) {
            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "$source • $comparison",
                color = priceColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = differenceText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
    }
}

@Composable
private fun ProfessionalRetailerLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.supremeColors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ProfessionalImageViewer(
    imageUrl: String,
    productName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close image",
                    tint = Color.White
                )
            }
        }
    }
}

private fun formatFetchDuration(
    durationMs: Long
): String {
    val roundedTenths = (
        (durationMs.coerceAtLeast(0L) + 50L) / 100L
    ).coerceAtLeast(1L)

    val seconds = roundedTenths / 10L
    val tenths = roundedTenths % 10L

    return "$seconds.${tenths}s"
}

private fun validDetailPrice(
    price: Double?
): Double? = price?.takeIf { value ->
    value.isFinite() && value > 0.0
}