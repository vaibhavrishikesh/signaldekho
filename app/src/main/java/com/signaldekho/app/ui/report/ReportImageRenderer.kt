package com.signaldekho.app.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import java.io.File

object ReportImageRenderer {
    private const val W = 1080
    private const val PAD = 48f
    private const val ROW_H = 88f

    private fun gradeArgb(g: Grade): Int = when (g) {
        Grade.GOOD -> 0xFF2E7D32.toInt()
        Grade.OK -> 0xFFF9A825.toInt()
        Grade.WEAK -> 0xFFC62828.toInt()
    }

    fun render(context: Context, rows: List<ReportRow>, findingTexts: List<String>): File {
        val height = (PAD * 2 + 140 + rows.size * ROW_H + findingTexts.size * 72 + 120).toInt()
        val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 64f; isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 34f }

        var y = PAD + 64
        c.drawText("SignalDekho — Coverage Report", PAD, y, title)
        y += 76

        rows.forEach { row ->
            row.wifiRssi?.let {
                body.color = gradeArgb(SignalGrade.wifi(it))
                c.drawCircle(PAD + 16, y - 14, 16f, body)
            }
            body.color = Color.BLACK
            c.drawText(row.room, PAD + 56, y, body)
            val wifiTxt = row.wifiRssi?.let { "WiFi ${it} dBm" } ?: "WiFi —"
            val cellTxt = row.cellDbm?.let { "SIM ${it} dBm" } ?: "SIM —"
            c.drawText("$wifiTxt   $cellTxt", W / 2f, y, body)
            y += ROW_H
        }

        y += 24
        findingTexts.forEach { txt ->
            c.drawText("• $txt", PAD, y, body)
            y += 72
        }

        c.drawText("Made with SignalDekho", PAD, height - PAD, small)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "report.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
