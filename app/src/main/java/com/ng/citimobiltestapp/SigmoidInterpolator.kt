package com.ng.citimobiltestapp

import android.animation.TimeInterpolator

/**
 * this sigmoid function reflects a more realistic and smooth acceleration and deceleration
 * process (in our case, time and speed dependences) with params c = 0.5 and a = 10.0
 */
class SigmoidInterpolator : TimeInterpolator {

    private val c = 0.5
    private val a = 10.0

    override fun getInterpolation(input: Float) = (1f / (1f + Math.pow(Math.E, -a * (input - c)))).toFloat()
}