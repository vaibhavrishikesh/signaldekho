package com.signaldekho.app.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import java.io.File

/** Every display string the renderer needs, resolved in composition by the caller. */
data class RenderHeader(
    val title: String,
    val subtitle: String,
    val wifiLabel: String,
    val simLabel: String,
    val notMeasured: String?,
    val watermark: String,
    val gradeLabels: Map<Grade, String>,
)

object ReportImageRenderer {
    private const val W = 1080
    private const val PAD = 48f

    private const val TITLE_H = 64f
    private const val SUBTITLE_GAP = 20f
    private const val SUBTITLE_H = 34f
    private const val BLOCK_GAP = 40f
    private const val TITLE_BLOCK = TITLE_H + SUBTITLE_GAP + SUBTITLE_H + BLOCK_GAP

    private const val ROW_NAME_H = 52f
    private const val ROW_SIGNAL_H = 46f
    private const val ROW_H = ROW_NAME_H + 2 * ROW_SIGNAL_H // name + up to two signal lines

    private const val WATERMARK_BLOCK = 60f

    private const val BAR_LEFT = PAD + 120f
    private const val BAR_RIGHT = PAD + 420f
    private const val BAR_WIDTH = BAR_RIGHT - BAR_LEFT // 300f

    private fun gradeArgb(g: Grade): Int = when (g) {
        Grade.EXCELLENT -> 0xFF1D9E75.toInt()
        Grade.GOOD -> 0xFF639922.toInt()
        Grade.WEAK -> 0xFFEF9F27.toInt()
        Grade.VERY_WEAK -> 0xFFE24B4A.toInt()
    }

    fun render(context: Context, header: RenderHeader, rows: List<ReportRow>, findingTexts: List<String>): File {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = TITLE_H; isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = SUBTITLE_H }
        val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 32f }
        val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 36f; textAlign = Paint.Align.RIGHT }
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = SUBTITLE_H }
        val wrapPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 40f }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6E6E6.toInt() }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val blockWidth = (W - 2 * PAD).toInt()

        // Build every StaticLayout before allocating the bitmap so the measured heights can
        // drive the bitmap height and nothing clips.
        val notMeasuredLayout = header.notMeasured?.let { text ->
            StaticLayout.Builder.obtain(text, 0, text.length, wrapPaint, blockWidth).build()
        }
        val findingLayouts = findingTexts.map { txt ->
            val line = "• $txt"
            StaticLayout.Builder.obtain(line, 0, line.length, wrapPaint, blockWidth).build()
        }
        val wrappedHeight = (notMeasuredLayout?.let { it.height + 16 } ?: 0) +
            findingLayouts.sumOf { it.height + 16 }

        val height = (PAD * 2 + TITLE_BLOCK + rows.size * ROW_H + wrappedHeight + WATERMARK_BLOCK).toInt()

        val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        var y = PAD + TITLE_H
        c.drawText(header.title, PAD, y, titlePaint)

        y += SUBTITLE_GAP + SUBTITLE_H
        c.drawText(header.subtitle, PAD, y, subtitlePaint)
        y += BLOCK_GAP

        notMeasuredLayout?.let { layout ->
            c.save()
            c.translate(PAD, y)
            layout.draw(c)
            c.restore()
            y += layout.height + 16
        }

        rows.forEach { row ->
            y += ROW_NAME_H
            c.drawText(row.room, PAD, y, roomPaint)

            val signals = listOfNotNull(
                row.wifiRssi?.let { rssi ->
                    Triple(header.wifiLabel, SignalGrade.wifi(rssi), SignalGrade.wifiFraction(rssi))
                },
                row.cellDbm?.let { dbm ->
                    Triple(header.simLabel, SignalGrade.cell(dbm), SignalGrade.cellFraction(dbm))
                },
            )
            signals.forEach { (label, grade, fraction) ->
                y += ROW_SIGNAL_H
                c.drawText(label, PAD + 16, y, labelPaint)

                trackPaint.color = 0xFFE6E6E6.toInt()
                c.drawRoundRect(RectF(BAR_LEFT, y - 22, BAR_RIGHT, y - 8), 7f, 7f, trackPaint)

                fillPaint.color = gradeArgb(grade)
                c.drawRoundRect(RectF(BAR_LEFT, y - 22, BAR_LEFT + BAR_WIDTH * fraction, y - 8), 7f, 7f, fillPaint)

                gradePaint.color = gradeArgb(grade)
                c.drawText(header.gradeLabels.getValue(grade), W - PAD, y, gradePaint)
            }
            // Reserve the full two-signal-line height even when a room has fewer signals so
            // rows.size * ROW_H matches what was actually drawn.
            y += ROW_SIGNAL_H * (2 - signals.size)
        }

        findingLayouts.forEach { layout ->
            c.save()
            c.translate(PAD, y)
            layout.draw(c)
            c.restore()
            y += layout.height + 16
        }

        c.drawText(header.watermark, PAD, height - PAD, watermarkPaint)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "report.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
