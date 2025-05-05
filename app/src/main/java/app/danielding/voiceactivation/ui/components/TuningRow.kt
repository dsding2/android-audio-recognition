package app.danielding.voiceactivation.ui.components

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import app.danielding.voiceactivation.TuningStorage
import app.danielding.voiceactivation.processor.ReferenceComparison
import androidx.core.view.isVisible
import app.danielding.voiceactivation.ui.TuningActivity

class TuningRow(
    context: Context,
    private val filename: String,
    private val referenceComparison: ReferenceComparison?
) : LinearLayout(context) {
    private var tuningSliders = mutableMapOf<TuningStorage.WeightType, SeekBar>()
    private var box : View
    private lateinit var progressBar : ProgressBar
    private val mainLayout : LinearLayout

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        mainLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        val button = AudioButton(context, filename).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        box = View(context).apply {
            setBackgroundColor(Color.RED)
            layoutParams = LayoutParams(
                50.dpToPx(), // Small fixed size
                50.dpToPx()
            )
        }

        mainLayout.addView(button)
        addSliders(mainLayout)
        mainLayout.addView(box)
        addView(mainLayout)


        addDropdown()
        if ((referenceComparison?.referenceList?.size ?: 0) == 0) {
            this.setBackgroundColor(Color.RED)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun changeSensitivity(newVal: Double) {
        referenceComparison?.sensitivity = newVal
        TuningStorage.putData(context, filename, newVal)
    }

    private fun sensitivityToSlider(sensitivity: Double): Int {
        return ((sensitivity - .4) / (1.1 - .4) * 100.0).toInt()
    }
    private fun sliderToSensitivity(slider: Int): Double {
        return (slider / 100.0) * (1.1 - .4) + .4
    }

    private fun addSliders(target: LinearLayout) {
        val sliderContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            this.layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }

        val slider = createSeekBar(sensitivityToSlider(TuningStorage.getValue(context, filename)))
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                changeSensitivity(sliderToSensitivity(progress))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 50 // Set to desired initial value
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8 // Optional spacing
                marginStart = slider.thumbOffset + slider.marginStart + 18 // manually add adjustments
                marginEnd = slider.thumbOffset + slider.marginEnd + 18
            }
//            setPadding(slider.thumbOffset, 0, slider.thumbOffset, 0)
        }
        sliderContainer.addView(slider)
        sliderContainer.addView(progressBar)
        target.addView(sliderContainer)
    }

    private fun addDropdown() {
        val tuningLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            this.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        val tuningValues =
            arrayOf(TuningStorage.WeightType.MFCC, TuningStorage.WeightType.DELTA, TuningStorage.WeightType.VOLUME)
        for (i in 0 until tuningValues.size) {
            val newSeek = createSeekBar(TuningStorage.getWeight(context, filename, tuningValues[i]).toInt())
            newSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    TuningStorage.putWeight(context, filename, tuningValues[i], progress.toDouble())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    referenceComparison?.normalize()
                }
            })
            tuningSliders[tuningValues[i]] = newSeek
            tuningLayout.addView(newSeek)
        }
        referenceComparison?.normalize()
        tuningLayout.visibility = GONE
        addView(tuningLayout)

        this.setOnClickListener { v ->
            TransitionManager.beginDelayedTransition(this, AutoTransition())
            tuningLayout.visibility =
                if (tuningLayout.isVisible) GONE else VISIBLE
        }
    }

    fun createSeekBar(p: Int): SeekBar {
        return SeekBar(context).apply {
            max = 100
            progress = p
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun onSimilarity() {
        box.setBackgroundColor(
            Color.GREEN
        )
        box.postDelayed({
            box.setBackgroundColor(Color.RED)
        }, 1500)
    }

    fun setProgress(prog: Double) {
        progressBar.progress=sensitivityToSlider(prog)
    }

    fun resetTuning() {
        for (pair in tuningSliders) {
            pair.value.progress = TuningStorage.getWeight(context, filename, pair.key).toInt()
        }
        referenceComparison?.normalize()
    }
}