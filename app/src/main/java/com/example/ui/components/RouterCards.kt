package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ConnectionStatus
import com.example.data.Formatters
import com.example.data.RouterUsageSnapshot
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberShield
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseOffline
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TelemetryMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RouterDashboardHeader(
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyanContainer, PurpleContainer)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(CyanAccent.copy(alpha = 0.6f), PurpleAccent.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Router,
                    contentDescription = stringResource(R.string.cd_router_icon),
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.header_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.header_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        ConnectionStatusBadge(status = connectionStatus)
    }
}

@Composable
fun ConnectionStatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, labelRes) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(EmeraldLive, EmeraldContainer, R.string.status_connected)
        ConnectionStatus.DISCOVERING -> Triple(AmberShield, AmberContainer, R.string.status_discovering)
        ConnectionStatus.OFFLINE -> Triple(RoseOffline, RoseContainer, R.string.status_offline)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier.testTag("connection_status_badge"),
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == ConnectionStatus.OFFLINE) dotColor else dotColor.copy(alpha = pulseAlpha)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = dotColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CumulativeHeroCard(
    snapshot: RouterUsageSnapshot,
    modifier: Modifier = Modifier
) {
    val totalBytes = snapshot.totalCombinedBytes
    val rxFraction = if (totalBytes > 0L) {
        (snapshot.totalRxBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.95f)
    } else {
        0.5f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cumulative_hero_card")
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SlateCard,
                        Color(0xFF17253B),
                        SlateCardElevated
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyanAccent.copy(alpha = 0.65f),
                        PurpleAccent.copy(alpha = 0.55f),
                        CyanAccent.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = stringResource(R.string.cd_shield_icon),
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.hero_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = Formatters.formatBytes(totalBytes),
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                modifier = Modifier.testTag("hero_total_usage_text")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    R.string.tracking_since_format,
                    Formatters.formatTrackingDate(snapshot.startedAtMillis)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Proportional Cyan (Download) vs Purple (Upload) Telemetry Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SlateBorder.copy(alpha = 0.6f))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(rxFraction)
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CyanAccent.copy(alpha = 0.7f), CyanAccent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - rxFraction)
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PurpleAccent, PurpleAccent.copy(alpha = 0.75f))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroBreakdownPill(
                    label = stringResource(R.string.total_download_label),
                    value = Formatters.formatBytes(snapshot.totalRxBytes),
                    accent = CyanAccent,
                    container = CyanContainer.copy(alpha = 0.55f),
                    icon = Icons.Rounded.ArrowDownward,
                    iconDescription = stringResource(R.string.cd_download_icon),
                    modifier = Modifier.weight(1f)
                )
                HeroBreakdownPill(
                    label = stringResource(R.string.total_upload_label),
                    value = Formatters.formatBytes(snapshot.totalTxBytes),
                    accent = PurpleAccent,
                    container = PurpleContainer.copy(alpha = 0.55f),
                    icon = Icons.Rounded.ArrowUpward,
                    iconDescription = stringResource(R.string.cd_upload_icon),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroBreakdownPill(
    label: String,
    value: String,
    accent: Color,
    container: Color,
    icon: ImageVector,
    iconDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = TelemetryMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveSpeedRow(
    downloadSpeedBps: Double,
    uploadSpeedBps: Double,
    historyRx: List<Double>,
    historyTx: List<Double>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiveSpeedCard(
            title = stringResource(R.string.live_download_label),
            speedText = Formatters.formatSpeed(downloadSpeedBps),
            accent = CyanAccent,
            icon = Icons.Rounded.ArrowDownward,
            iconDescription = stringResource(R.string.cd_download_icon),
            history = historyRx,
            modifier = Modifier
                .weight(1f)
                .testTag("live_download_card")
        )
        LiveSpeedCard(
            title = stringResource(R.string.live_upload_label),
            speedText = Formatters.formatSpeed(uploadSpeedBps),
            accent = PurpleAccent,
            icon = Icons.Rounded.ArrowUpward,
            iconDescription = stringResource(R.string.cd_upload_icon),
            history = historyTx,
            modifier = Modifier
                .weight(1f)
                .testTag("live_upload_card")
        )
    }
}

@Composable
private fun LiveSpeedCard(
    title: String,
    speedText: String,
    accent: Color,
    icon: ImageVector,
    iconDescription: String,
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SlateCard,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = iconDescription,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = speedText,
                fontFamily = TelemetryMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            SpeedSparkline(
                samples = history,
                lineColor = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            )
        }
    }
}

@Composable
private fun SpeedSparkline(
    samples: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Subtle baseline
        drawLine(
            color = SlateBorder.copy(alpha = 0.45f),
            start = Offset(0f, height - 2f),
            end = Offset(width, height - 2f),
            strokeWidth = 1.dp.toPx()
        )

        if (samples.size < 2) return@Canvas

        val maxVal = (samples.maxOrNull() ?: 0.0).coerceAtLeast(1024.0)
        val stepX = width / (samples.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, value ->
            val x = index * stepX
            val ratio = (value / maxVal).toFloat().coerceIn(0f, 1f)
            val y = height - 3f - (ratio * (height - 6f))
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun RebootProtectionBanner(
    rebootCount: Int,
    modifier: Modifier = Modifier
) {
    val accent = if (rebootCount > 0) EmeraldLive else AmberShield
    val container = if (rebootCount > 0) EmeraldContainer.copy(alpha = 0.85f) else AmberContainer.copy(alpha = 0.7f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reboot_protection_banner"),
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = stringResource(R.string.cd_shield_icon),
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.reboot_protection_banner,
                        Formatters.toBengaliDigits(rebootCount)
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.reboot_protection_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun BatteryProtectionButton(
    isProtected: Boolean,
    onRequestProtection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isProtected) EmeraldContainer else CyanContainer
    val contentColor = if (isProtected) EmeraldLive else CyanAccent

    Button(
        onClick = onRequestProtection,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("battery_protection_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isProtected) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryChargingFull,
                contentDescription = stringResource(R.string.cd_battery_icon),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(
                        if (isProtected) R.string.battery_protect_active else R.string.battery_protect_enable
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
                if (!isProtected) {
                    Text(
                        text = stringResource(R.string.battery_protect_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun TodayUsageCard(
    snapshot: RouterUsageSnapshot,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_usage_card"),
        shape = RoundedCornerShape(20.dp),
        color = SlateCard,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = stringResource(R.string.cd_calendar_icon),
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.today_usage_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        if (snapshot.todayDate.isNotEmpty()) {
                            Text(
                                text = snapshot.todayDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateMuted
                            )
                        }
                    }
                }

                Text(
                    text = Formatters.formatBytes(snapshot.todayCombinedBytes),
                    fontFamily = TelemetryMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InlineMetricChip(
                    prefix = "↓",
                    label = stringResource(R.string.total_download_label),
                    value = Formatters.formatBytes(snapshot.todayRxBytes),
                    accent = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                InlineMetricChip(
                    prefix = "↑",
                    label = stringResource(R.string.total_upload_label),
                    value = Formatters.formatBytes(snapshot.todayTxBytes),
                    accent = PurpleAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RouterSessionCard(
    snapshot: RouterUsageSnapshot,
    onRediscoverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("router_session_card"),
        shape = RoundedCornerShape(20.dp),
        color = SlateCard,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = stringResource(R.string.cd_session_icon),
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.router_session_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onRediscoverClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("rediscover_router_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh_icon),
                        tint = CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Formatters.formatBytes(snapshot.sessionCombinedBytes),
                    fontFamily = TelemetryMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = PurpleAccent
                )
                val endpointSummary = when {
                    !snapshot.routerName.isNullOrBlank() && !snapshot.routerHost.isNullOrBlank() ->
                        "${snapshot.routerName} (${snapshot.routerHost})"
                    !snapshot.routerHost.isNullOrBlank() -> snapshot.routerHost
                    else -> null
                }
                if (endpointSummary != null) {
                    Text(
                        text = stringResource(R.string.router_endpoint_label, endpointSummary),
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InlineMetricChip(
                    prefix = "↓",
                    label = stringResource(R.string.total_download_label),
                    value = Formatters.formatBytes(snapshot.sessionRxBytes),
                    accent = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                InlineMetricChip(
                    prefix = "↑",
                    label = stringResource(R.string.total_upload_label),
                    value = Formatters.formatBytes(snapshot.sessionTxBytes),
                    accent = PurpleAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            if (snapshot.connectionStatus != ConnectionStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        if (snapshot.connectionStatus == ConnectionStatus.OFFLINE) {
                            R.string.session_offline_hint
                        } else {
                            R.string.router_searching_hint
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (snapshot.connectionStatus == ConnectionStatus.OFFLINE) RoseOffline else AmberShield
                )
            }
        }
    }
}

@Composable
private fun InlineMetricChip(
    prefix: String,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = SlateCardElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$prefix $label",
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                fontFamily = TelemetryMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = accent
            )
        }
    }
}
