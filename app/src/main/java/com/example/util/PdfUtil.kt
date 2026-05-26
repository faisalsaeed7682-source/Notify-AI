package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.screens.ChatMessage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun exportChatToPdfAndShare(context: Context, messages: List<ChatMessage>, fileName: String) {
    val pdfDocument = PdfDocument()
    val textPaint = TextPaint().apply {
        textSize = 14f
        color = Color.BLACK
        isAntiAlias = true
    }
    
    val pageWidth = 595
    val pageHeight = 842
    
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    var yPosition = 50f
    val xPosition = 50f
    val maxWidth = pageWidth - 100 // 50 margin on each side
    
    for (msg in messages) {
        val prefix = if (msg.isUser) "Me: " else "AI: "
        val text = "$prefix${msg.text}"
        
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.0f, 0.0f)
            .setIncludePad(false)
            .build()
        
        val layoutHeight = staticLayout.height
        
        if (yPosition + layoutHeight > pageHeight - 50) {
            pdfDocument.finishPage(page)
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.pages.size + 1).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }
        
        canvas.save()
        canvas.translate(xPosition, yPosition)
        staticLayout.draw(canvas)
        canvas.restore()
        
        yPosition += layoutHeight + 20f
    }

    pdfDocument.finishPage(page)

    try {
        val pdfsDir = File(context.cacheDir, "pdfs")
        if (!pdfsDir.exists()) pdfsDir.mkdirs()
        
        val safeFileName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
        val file = File(pdfsDir, safeFileName)
        
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, safeFileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    } catch (e: IOException) {
        Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
