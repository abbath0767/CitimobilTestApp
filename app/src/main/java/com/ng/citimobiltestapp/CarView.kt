package com.ng.citimobiltestapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
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
    private var carAngle = 90.0
    private var carIsMove = false

    private lateinit var interpolator: TimeInterpolator
    private lateinit var carBitmap: Bitmap
    private lateinit var carMatrix: Matrix

    private fun initView() {
        interpolator = SigmoidInterpolator()

        val d = resources.getDrawable(R.drawable.ic_car_icon_top, null)
        val bm: Bitmap
        if (d is BitmapDrawable) {
            bm = d.bitmap
        } else {
            bm = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            d.setBounds(0, 0, canvas.width, canvas.height)
            d.draw(canvas)
        }
        carBitmap = Bitmap.createScaledBitmap(bm, bm.width / 2, bm.height / 2, false)

        carMatrix = Matrix()
    }

    override fun onDraw(canvas: Canvas) {
        carMatrix.reset()
        carMatrix.setRotate((90 - carAngle).toFloat(), carBitmap.width / 2f, carBitmap.height / 2f)
        carMatrix.postTranslate(carX - carBitmap.width / 2f, carY - carBitmap.height / 2f)
        canvas.drawBitmap(carBitmap, carMatrix, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (!carIsMove) {
            startMove(event.x, event.y)
            false
        } else {
            true
        }
    }

    /**
     * method for start move of car to x/y coord. Using value animator
     * @param   toX   - the coord of final point in x.
     * @param   toY   - the coord of final point in y
     */
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

    /**
     * method for calculate control point.
     * Control point is a point where cross the current car linear function and
     * potential border function. Border can change, in this realization the borders is border of phone screen
     * Car always have current position (x, y) and angle (car looks up - is 90 degree, looks right - is 0 degree)
     * and with this data we can make linear function - y = kx + b, where k and b - variables define
     * the form of a straight.
     * Param k possible calculate of tan current angle:
     * https://en.wikipedia.org/wiki/Slope
     * Param B possible calculate from linear function, as we all know k, we know x and y for each
     * case. For example: top border is line where y = 0 always and x is value from -Infinity to +Infinity,
     * but the value will be enough any x, for example value of current screen width, and etc.
     * P.S. Carefully, all calculated keep in mind that the value of the point (0,0) is the upper left corner,
     * not bottom left!
     * @return  the value of calculated control point
     */
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

    /**
     * calculate distance between entered point and current car position. root of the sum square

     * @param   x     - the point x coord
     * @param   y     - the point y coord
     * @return  the value of distance
     */
    private fun calculateDistance(x: Float, y: Float): Double {
        return Math.sqrt(Math.pow((carX - x).toDouble(), 2.0) + Math.pow((carY - y).toDouble(), 2.0))
    }

    private fun topBorderFunY(): Float {
        return 0.0f
    }

    private fun bottomBorderFunY(): Float {
        return height.toFloat()
    }

    /**
     * move of car - simple quadratic bezier function with 3 point
     * https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Quadratic_B%C3%A9zier_curves
     * @param   time   - the time from start.
     * @param   p0     - the start point
     * @param   p1     - the control point
     * @param   p2     - the end point
     * @return  the value of current position with entered time value
     */
    private fun calcBezier(time: Float, p0: Float, p1: Float, p2: Float): Float {
        return (Math.pow((1 - time).toDouble(), 2.0) * p0
                + (2f * (1 - time) * time * p1).toDouble()
                + Math.pow(time.toDouble(), 2.0) * p2).toFloat()

    }

    /**
     * move of car - simple quadratic bezier function with 3 point
     * https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Quadratic_B%C3%A9zier_curves
     * @param   time   - the time from start.
     * @param   p0x     - the start point x coord
     * @param   p0y     - the start point y coord
     * @param   p1x     - the control point x coord
     * @param   p1y     - the control point y coord
     * @param   p2x     - the end point x coord
     * @param   p2y     - the end point y coord
     * @return  the value of current angle of the car with entered time value
     */
    private fun calcAngle(time: Float, p0x: Float, p0y: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Double {
        val t = 1.0f - time
        val dx = ((t * p1x + time * p2x) - (t * p0x + time * p1x)).toDouble()
        val dy = ((t * p1y + time * p2y) - (t * p0y + time * p1y)).toDouble()
        var theta = Math.toDegrees(Math.atan2(dy, dx))
        if (theta < 0.0f)
            theta += 360.0
        return theta
    }

    /**
     * find default car position
     */
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