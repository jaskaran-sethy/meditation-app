package com.example.myapplication

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.navArgs
import com.example.myapplication.databinding.FragmentMeditationBinding

class MeditationTimerFragment : Fragment() {

    private var _binding: FragmentMeditationBinding? = null
    private lateinit var breathTimer: CountDownTimer
    private val totalBreathsDuration: Long = 5 * 60 * 1000 // 5 minutes in milliseconds
    private lateinit var breathingCircleView: View

    private var maxSize: Int = 0 // max size during inhale
    private var minSize: Int = 0 // min size during exhale

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMeditationBinding.inflate(inflater, container, false)
        breathingCircleView = binding.breathingCircle
        val screenHeight = resources.displayMetrics.heightPixels
        maxSize = (screenHeight * 0.4).toInt()
        minSize = (screenHeight * 0.2).toInt()

        return binding.root
    }

    private val args: MeditationTimerFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startMeditationButton.setOnClickListener {
            startMeditationTimer(args)
            binding.timerLayout.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::breathTimer.isInitialized) {
            breathTimer.cancel()
        }
        _binding = null
    }


    private fun updateBreathingCircleSize(size: Int) {
        val layoutParams = breathingCircleView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.width = size
        layoutParams.height = size
        breathingCircleView.layoutParams = layoutParams
    }

    private fun startMeditationTimer(args: MeditationTimerFragmentArgs) {
        val totalBreathTime = (
                args.inhaleTimeInMilliseconds
                + args.inhaleHoldTimeInMilliseconds
                + args.exhaleTimeInMilliseconds
                + args.exhaleHoldTimeInMilliseconds
                )
        val breathTimes = listOf(
            args.inhaleTimeInMilliseconds,
            args.inhaleHoldTimeInMilliseconds,
            args.exhaleTimeInMilliseconds,
            args.exhaleHoldTimeInMilliseconds)
        val breathActions = listOf(
            "Inhale",
            "Hold Your Breath",
            "Exhale",
            "Hold Your Breath"
        )

        var breathsRemaining = totalBreathsDuration / totalBreathTime
        binding.breathsRemaining.text = String.format("Breaths Remaining: %d", breathsRemaining)
        var currentActionIndex = 0

        fun startNextTimer() {
            breathTimer = object : CountDownTimer(breathTimes[currentActionIndex], 100) {
                var currentActionIndex = 0
                override fun onTick(millisUntilFinished: Long) {
                    binding.meditationTimer.text = String.format(
                        "%02d:%02d",
                        (millisUntilFinished / 1000) / 60,
                        (millisUntilFinished / 1000) % 60
                    )
                }

                override fun onFinish() {

                    currentActionIndex = (currentActionIndex + 1) % breathActions.size
                    binding.breathingAction.text = breathActions[currentActionIndex]
                    if (currentActionIndex == 0) {
                        breathsRemaining--
                        binding.breathsRemaining.text =
                            String.format("Breaths Remaining: %d", breathsRemaining)
                    }

                    val currentAction = breathActions[currentActionIndex]
                    val animator = when (currentAction) {
                        "Inhale" -> ValueAnimator.ofInt(minSize, maxSize)
                        "Exhale" -> ValueAnimator.ofInt(maxSize, minSize)
                        else -> null // No animation for hold
                    }

                    animator?.apply {
                        duration = breathTimes[currentActionIndex] + 1000
                        addUpdateListener { animation ->
                            val animatedValue = animation.animatedValue as Int
                            updateBreathingCircleSize(animatedValue)
                        }
                        start()
                    }

                    if (breathsRemaining != 0L) startNextTimer()

                }
            }
            breathTimer.start()
        }

        val animator = when (breathActions[currentActionIndex]) {
            "Inhale" -> ValueAnimator.ofInt(minSize, maxSize)
            "Exhale" -> ValueAnimator.ofInt(maxSize, minSize)
            else -> null // No animation for hold
        }
        binding.breathingAction.text = breathActions[currentActionIndex]

        animator?.apply {
            duration = breathTimes[currentActionIndex] + 1000
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                updateBreathingCircleSize(animatedValue)
            }
            start()
        }

        startNextTimer()
        binding.startMeditationButton.visibility = View.INVISIBLE
    }
}