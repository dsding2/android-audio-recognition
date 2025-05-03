package app.danielding.voiceactivation.ui.components

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import app.danielding.voiceactivation.TuningStorage
import app.danielding.voiceactivation.processor.ReferenceComparison

class TuningRow(
    context: Context,
    private val filename: String,
    private val referenceComparison: ReferenceComparison?
) : LinearLayout(context) {
    private var box : View
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 16, 0, 16)
        }
        val button = AudioButton(context, filename).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        val slider = SeekBar(context).apply {
            max = 100
            progress = sensitivityToSlider(TuningStorage.getValue(context, filename))
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f // Takes most space
                marginStart = 16
                marginEnd = 16
            }
        }
        // Listen for slider changes
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                changeSensitivity(sliderToSensitivity(progress))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        box = View(context).apply {
            setBackgroundColor(Color.RED)
            layoutParams = LayoutParams(
                50.dpToPx(), // Small fixed size
                50.dpToPx()
            )
        }

        // Add all views to row
        addView(button)
        addView(slider)
        addView(box)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun changeSensitivity(newVal: Double) {
        referenceComparison?.sensitivity = newVal
        TuningStorage.putData(context, filename, newVal)
    }

    private fun sensitivityToSlider(sensitivity: Double): Int {
        return ((sensitivity - .4) / (.9 - .4) * 100.0).toInt()
    }
    private fun sliderToSensitivity(slider: Int): Double {
        return (slider / 100.0) * (.9 - .4) + .4
    }
    fun onSimilarity() {
        box.setBackgroundColor(
            Color.GREEN
        )
        box.postDelayed({
            box.setBackgroundColor(Color.RED)
        }, 1500)
    }
}