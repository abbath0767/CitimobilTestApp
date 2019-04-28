package com.ng.citimobiltestapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class CarView : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    private var startCoordIsInit = false
    private var carX = 0f
    private var tmpCarX = 0f
    private var carY = 0f
    private var tmpCarY = 0f
    private var carAngle = 330.0
    private var carIsMove = false

    private lateinit var carPaint: Paint
    private lateinit var arrowPaint: Paint
    private lateinit var interpolator: TimeInterpolator
    private val arrowLen = 30f

    private fun initView() {
        carPaint = Paint().apply {
            color = R.color.yellow.asColor()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        arrowPaint = Paint().apply {
            color = R.color.black.asColor()
            style = Paint.Style.STROKE
            strokeWidth = R.dimen.common_2.dimen()
            isAntiAlias = true
        }
        interpolator = SigmoidInterpolator()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(carX, carY, 20f, carPaint)
        val stopX = (carX + Math.cos(Math.toRadians(carAngle)) * arrowLen).toFloat()
        val stopY = (carY - Math.sin(Math.toRadians(carAngle)) * arrowLen).toFloat()
        canvas.drawLine(carX, carY, stopX, stopY, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (!carIsMove) {
            startMove(event.x, event.y)
            false
        } else {
            true
        }
    }

    private fun startMove(toX: Float, toY: Float) {
        carIsMove = true
        val pairControl = getControlPoint()
        val cx = pairControl.first
        val cy = pairControl.second

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            interpolator = this@CarView.interpolator
            addUpdateListener {
                carX = calcBezier(it.animatedValue as Float, tmpCarX, cx, toX)
                carY = calcBezier(it.animatedValue as Float, tmpCarY, cy, toY)
                carAngle = calcAngle(it.animatedValue as Float, tmpCarX, -tmpCarY, cx, -cy, toX, -toY)
//                log("car angle: $carAngle")
                postInvalidateOnAnimation()
            }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        carIsMove = false
                        tmpCarX = carX
                        tmpCarY = carY
                    }
                }
            )
            start()
        }
    }

    private fun getControlPoint(): Pair<Float, Float> {
        val k = Math.tan(Math.toRadians(carAngle))
        val b = -carY - k * carX

        var cX = 0f
        var cY = 0f

        if (carAngle == 90.0) {
            //check only top border
            cX = (-b / k).toFloat()
            cY = topBorderFunY()
        } else if (carAngle == 270.0) {
            //check only bottom border
            cX = ((-height - b) / k).toFloat()
            cY = bottomBorderFunY()
        } else if (carAngle == 0.0) {
            //check only right border
            val borderK = Math.tan(Math.toRadians(90.0))
            val borderB = -carY - borderK * width.toDouble()
            cX = ((borderB - b) / (k - borderK)).toFloat()
            cY = (k * cX - b).toFloat()
        } else if (carAngle == 180.0) {
            //check only left border
            val borderK = Math.tan(Math.toRadians(90.0))
            val borderB = -carY
            cX = ((borderB - b) / (k - borderK)).toFloat()
            cY = (k * cX - b).toFloat()
        } else if (carAngle < 90.0 && carAngle > 0.0) {
            //check top and right border
            val cXTop = -b / k
            val cYTop = topBorderFunY()
            val borderK = Math.tan(Math.toRadians(90.0))
            val borderB = -carY - borderK * width.toDouble()
            val cXRight = ((borderB - b) / (k - borderK)).toFloat()
            val cYRight = -(k * cXRight + b)
            val distanceToTopPoint = calculateDistance(cXTop.toFloat(), cYTop)
            val distanceToRightPoint = calculateDistance(cXRight, cYRight.toFloat())

            if (distanceToRightPoint < distanceToTopPoint) {
                cX = cXRight
                cY = cYRight.toFloat()
            } else {
                cX = cXTop.toFloat()
                cY = cYTop
            }
        } else if (carAngle > 90.0 && carAngle < 180.0) {
            //check top and left border
            val cXTop = -b / k
            val cYTop = topBorderFunY()
            val borderK = Math.tan(Math.toRadians(90.0))
            val borderB = -carY
            val cXLeft = ((borderB - b) / (k - borderK)).toFloat()
            val cYLeft = -(k * cXLeft + b)
            val distanceToTopPoint = calculateDistance(cXTop.toFloat(), cYTop)
            val distanceToLeftPoint = calculateDistance(cXLeft, cYLeft.toFloat())

            if (distanceToLeftPoint < distanceToTopPoint) {
                cX = cXLeft
                cY = cYLeft.toFloat()
            } else {
                cX = cXTop.toFloat()
                cY = cYTop
            }
        } else if (carAngle > 180.0 && carAngle < 270.0) {
            //check left and bot border
            val cYBot = bottomBorderFunY()
            val cXBot = (-height - b) / k
            val borderK = Math.tan(Math.toRadians(90.0))
            val borderB = -carY
            val cXLeft = ((borderB - b) / (k - borderK)).toFloat()
            val cYLeft = -(k * cXLeft + b)
            val distanceToBotPoint = calculateDistance(cXBot.toFloat(), cYBot)
            val distanceToLeftPoint = calculateDistance(cXLeft, cYLeft.toFloat())

            if (distanceToLeftPoint < distanceToBotPoint) {
                cX = cXLeft
                cY = cYLeft.toFloat()
            } else {
                cX = cXBot.toFloat()
                cY = cYBot
            }
        } else if (carAngle > 270.0) {
            //check bot and right border
            val cYBot = bottomBorderFunY()
            val cXBot = (-height - b) / k
            val borderK = Math.tan(Math.toRadians(90.0))
            val borderB = -carY - borderK * width.toDouble()
            val cXRight = ((borderB - b) / (k - borderK)).toFloat()
            val cYRight = -(k * cXRight + b)
            val distanceToBotPoint = calculateDistance(cXBot.toFloat(), cYBot)
            val distanceToRightPoint = calculateDistance(cXRight, cYRight.toFloat())

            if (distanceToRightPoint < distanceToBotPoint) {
                cX = cXRight
                cY = cYRight.toFloat()
            } else {
                cX = cXBot.toFloat()
                cY = cYBot
            }
        }

        return cX to cY
    }

    private fun calculateDistance(x: Float, y: Float): Double {
        return Math.sqrt(Math.pow((carX - x).toDouble(), 2.0) + Math.pow((carY - y).toDouble(), 2.0))
    }

    private fun topBorderFunY(): Float {
        return 0.0f
    }

    private fun bottomBorderFunY(): Float {
        return height.toFloat()
    }

    private fun calcBezier(time: Float, p0: Float, p1: Float, p2: Float): Float {
        return (
                Math.pow((1 - time).toDouble(), 2.0) * p0
                        + (2f * (1 - time) * time * p1).toDouble()
                        + Math.pow(time.toDouble(), 2.0) * p2
                ).toFloat()

    }

    private fun calcAngle(time: Float, p0x: Float, p0y: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Double {
        val t = 1.0f - time
        val dx = ((t * p1x + time * p2x) - (t * p0x + time * p1x)).toDouble()
        val dy = ((t * p1y + time * p2y) - (t * p0y + time * p1y)).toDouble()
        var theta = Math.toDegrees(Math.atan2(dy, dx))
        if (theta < 0.0f)
            theta += 360.0
        return theta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!startCoordIsInit) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            carX = width / 2f
            carY = height - (height / 6f)
            tmpCarX = carX
            tmpCarY = carY
            startCoordIsInit = true
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}