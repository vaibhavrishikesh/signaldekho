package com.signaldekho.app.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import java.io.File

object ReportImageRenderer {
    private const val W = 1080
    private const val PAD = 48f
    private const val ROW_H = 88f
    private const val NULL_ARGB = 0xFF888888.toInt()

    private fun gradeArgb(g: Grade): Int = when (g) {
        Grade.EXCELLENT -> 0xFF1B5E20.toInt()
        Grade.GOOD -> 0xFF2E7D32.toInt()
        Grade.WEAK -> 0xFFC62828.toInt()
        Grade.VERY_WEAK -> 0xFF8B0000.toInt()
    }

    fun render(context: Context, rows: List<ReportRow>, findingTexts: List<String>): File {
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 64f; isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 44f; textAlign = Paint.Align.RIGHT }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 34f }
        val findingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }

        val findingWidth = (W - 2 * PAD).toInt()
        val findingLayouts = findingTexts.map { txt ->
            val line = "• $txt"
            StaticLayout.Builder.obtain(line, 0, line.length, findingPaint, findingWidth).build()
        }
        val findingsHeight = findingLayouts.sumOf { it.height + 16 }

        val height = (PAD * 2 + 140 + rows.size * ROW_H + findingsHeight + 120).toInt()
        val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        var y = PAD + 64
        c.drawText("SignalDekho — Coverage Report", PAD, y, title)
        y += 76

        rows.forEach { row ->
            c.drawText(row.room, PAD, y, body)

            value.color = row.wifiRssi?.let { gradeArgb(SignalGrade.wifi(it)) } ?: NULL_ARGB
            val wifiTxt = row.wifiRssi?.let { "WiFi ${it} dBm" } ?: "WiFi —"
            c.drawText(wifiTxt, W - PAD - 340f, y, value)

            value.color = row.cellDbm?.let { gradeArgb(SignalGrade.cell(it)) } ?: NULL_ARGB
            val cellTxt = row.cellDbm?.let { "SIM ${it} dBm" } ?: "SIM —"
            c.drawText(cellTxt, W - PAD, y, value)

            y += ROW_H
        }

        y += 24
        findingLayouts.forEach { layout ->
            c.save()
            c.translate(PAD, y)
            layout.draw(c)
            c.restore()
            y += layout.height + 16
        }

        c.drawText("Made with SignalDekho", PAD, height - PAD, small)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "report.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
