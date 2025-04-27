package app.danielding.voiceactivation


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MFCCHeatmap(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var bufferSize = 50
    private var heatmapData = List(bufferSize) { 0.0 to FloatArray(13) }
    private val paint = Paint().apply {
        strokeWidth = 2f
    }
    private var isRecording = false
    private var startRecordTime = 0.0
    private var endRecordTime = 0.0

    fun readMFCCCoefficients(data: List<Pair<Double, FloatArray>>, recording: Boolean) {
        heatmapData = data
        if (recording && !isRecording) {
            startRecordTime = data.last().first
            endRecordTime = startRecordTime + 1000
        } else if (!recording && isRecording) {
            endRecordTime = data.last().first
        }
        isRecording = recording
        invalidate() // Triggers onDraw()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val frames = heatmapData.size
        val coeffs = heatmapData[0].second.size
        val firstTime = heatmapData[0].first
        val lastTime = heatmapData.last().first
        val cellWidth = width.toFloat() / coeffs
        val cellHeight = height.toFloat() / frames

        fun convert(time: Double): Double {
            return (time - firstTime) / (lastTime - firstTime)
        }
        for (y in 0 until frames) {
            val time = heatmapData[y].first
            val yPos = (convert(time) * height).toFloat()
            val bChannel = if (endRecordTime >= time && time >= startRecordTime) 255 else 0
            for (x in 0 until coeffs) {
                val value = heatmapData[y].second[x]
                val gray = ((value + 5) * 25).coerceIn(0f, 255f).toInt()
                paint.color = Color.rgb(gray, gray, bChannel)
                canvas.drawRect(
                    x * cellWidth,
                    yPos + cellHeight,
                    (x + 1) * cellWidth,
                    yPos,
                    paint
                )
            }
        }
    }
}
