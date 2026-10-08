package com.sahilchanna.reckon.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sahilchanna.reckon.MainActivity
import com.sahilchanna.reckon.model.SaveData
import com.sahilchanna.reckon.model.Store

/**
 * The home-screen widget, the counterpart of `ReckonWidget.swift`: median pace,
 * daily streak, and whether today's Daily is done.
 *
 * Glance has no gradient background and no Canvas, so the amber wash behind the
 * in-app card is dropped here and the sparkline is drawn as bars — lower is
 * better, so a faster round is a taller bar, the same reading as the line.
 * Font weights are capped at Bold because that is all Glance's `FontWeight`
 * exposes.
 */
class ReckonWidget : GlanceAppWidget() {

    // Two shapes to render: a square and a wide one. On Android 12+ the launcher
    // picks whichever matches how the user sized it; below that Glance falls
    // back to the first entry.
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SQUARE, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A Glance widget runs inside the app's own process, so the
        // SharedPreferences file the Store writes is already shared state — no
        // App Group equivalent is needed, unlike the iOS extension.
        val save = Store(context).data.value
        provideContent { WidgetContent(save) }
    }

    private companion object {
        val SQUARE = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

class ReckonWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReckonWidget()
}

/** Called after every save change; the Android stand-in for `reloadAllTimelines`. */
object ReckonWidgetRefresh {
    suspend fun refresh(context: Context) {
        ReckonWidget().updateAll(context)
    }
}

// MARK: colours

private val Ink = ColorProvider(Color(0xFF0B0F12))
private val Chalk = ColorProvider(Color(0xFFE9EFF2))
private val Mute = ColorProvider(Color(0xFF8797A2))
private val Amber = ColorProvider(Color(0xFFFFB020))
private val Good = ColorProvider(Color(0xFF3DDC97))

private fun style(size: Float, weight: FontWeight, color: ColorProvider) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = weight)

// MARK: content

@Composable
private fun WidgetContent(save: SaveData) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= 200.dp

    // Tapping opens straight into a sprint rather than the home screen — the
    // same reckon:// deep link the iOS widget uses.
    val openSprint = actionStartActivity(
        Intent(context, MainActivity::class.java).setData(Uri.parse("reckon://sprint")),
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Ink)
            .padding(12.dp)
            .clickable(openSprint),
        contentAlignment = Alignment.TopStart,
    ) {
        if (wide) WideLayout(save) else SquareLayout(save)
    }
}

@Composable
private fun SquareLayout(save: SaveData) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Reckon.", style = style(13f, FontWeight.Bold, Chalk))
            Spacer(GlanceModifier.defaultWeight())
            StreakBadge(save.dailyStreak)
        }
        Spacer(GlanceModifier.defaultWeight())
        PaceHeadline(save, 34f)
        Text(
            text = if (save.medianPace == null) "no rounds yet" else "median per solve",
            style = style(10.5f, FontWeight.Normal, Mute),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(6.dp))
        DailyLine(save)
    }
}

@Composable
private fun WideLayout(save: SaveData) {
    Row(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Reckon.", style = style(13f, FontWeight.Bold, Chalk))
                Spacer(GlanceModifier.width(6.dp))
                StreakBadge(save.dailyStreak)
            }
            Spacer(GlanceModifier.defaultWeight())
            PaceHeadline(save, 38f)
            Text(
                text = if (save.medianPace == null) "run a sprint to start" else "median per solve",
                style = style(11f, FontWeight.Normal, Mute),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(6.dp))
            DailyLine(save)
        }

        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.End,
        ) {
            if (save.pace.size > 1) {
                Sparkline(save.pace)
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }
            Row(verticalAlignment = Alignment.Bottom) {
                BestChip(
                    value = if (save.bestSprint > 0) "${save.bestSprint}" else "—",
                    label = "sprint",
                )
                Spacer(GlanceModifier.width(10.dp))
                BestChip(
                    value = save.bestMarathon?.let { "%.1fs".format(it) } ?: "—",
                    label = "marathon",
                )
            }
        }
    }
}

@Composable
private fun PaceHeadline(save: SaveData, size: Float) {
    val median = save.medianPace
    if (median == null) {
        Text(text = "Ready", style = style(size * 0.8f, FontWeight.Bold, Amber))
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "%.2f".format(median),
            style = style(size, FontWeight.Bold, Amber),
            maxLines = 1,
        )
        Text(text = "s", style = style(size * 0.42f, FontWeight.Bold, Amber))
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    if (streak <= 0) return

    Row(
        modifier = GlanceModifier
            .background(Amber)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The SF Symbol flame has no Glance equivalent; the emoji is the closest
        // thing a RemoteViews TextView can render without shipping a drawable.
        Text(text = "🔥 $streak", style = style(11f, FontWeight.Bold, Ink))
    }
}

@Composable
private fun DailyLine(save: SaveData) {
    val played = save.playedDailyToday
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (played) "✓" else "○",
            style = style(10f, FontWeight.Bold, if (played) Good else Mute),
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = if (played) "Daily ${save.dailyScore}/15" else "Daily waiting",
            style = style(12f, FontWeight.Medium, if (played) Chalk else Mute),
            maxLines = 1,
        )
    }
}

@Composable
private fun BestChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = value, style = style(15f, FontWeight.Bold, Chalk), maxLines = 1)
        Text(text = label, style = style(9.5f, FontWeight.Normal, Mute), maxLines = 1)
    }
}

/**
 * Same idea as the in-app chart: higher means faster. Glance cannot draw a
 * path, so each round is a bar whose height is its speed within the window.
 *
 * Only the last few rounds are drawn: a Glance `Row` renders at most ten
 * children, and a bar-per-round window of fourteen would be silently truncated.
 */
@Composable
private fun Sparkline(pace: List<Double>) {
    val recent = pace.takeLast(SPARKLINE_ROUNDS)
    val lo = recent.minOrNull() ?: 0.0
    val hi = recent.maxOrNull() ?: 1.0
    val span = (hi - lo).takeIf { it > 0.0 } ?: 1.0

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(46.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        for ((index, seconds) in recent.withIndex()) {
            val fraction = ((hi - seconds) / span).coerceIn(0.0, 1.0)
            Box(
                modifier = GlanceModifier
                    .width(5.dp)
                    .height((8 + 34 * fraction).dp)
                    .background(if (seconds == lo) Good else Amber),
                contentAlignment = Alignment.BottomCenter,
            ) {}
            if (index != recent.lastIndex) Spacer(GlanceModifier.width(3.dp))
        }
    }
}

/** Five bars plus four gaps: nine children, inside the ten a widget Row allows. */
private const val SPARKLINE_ROUNDS = 5
