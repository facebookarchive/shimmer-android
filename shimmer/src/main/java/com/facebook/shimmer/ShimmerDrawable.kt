/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.shimmer

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

class ShimmerDrawable : Drawable() {
  private val mUpdateListener = AnimatorUpdateListener { invalidateSelf() }
  private val mShimmerPaint = Paint()
  private val mDrawRect = Rect()
  private val mShaderMatrix = Matrix()
  private var mValueAnimator: ValueAnimator? = null
  private var mShimmer: Shimmer? = null
  var shimmer: Shimmer?
    get() = mShimmer
    set(shimmer) {
      mShimmer = shimmer
      if (mShimmer != null) {
        mShimmerPaint.xfermode = PorterDuffXfermode(
            if (mShimmer!!.alphaShimmer) PorterDuff.Mode.DST_IN else PorterDuff.Mode.SRC_IN)
      }
      updateShader()
      updateValueAnimator()
      invalidateSelf()
    }

  /** Starts the shimmer animation.  */
  fun startShimmer() {
    if (mValueAnimator != null && !isShimmerStarted && callback != null) {
      mValueAnimator!!.start()
    }
  }

  /** Stops the shimmer animation.  */
  fun stopShimmer() {
    if (mValueAnimator != null && isShimmerStarted) {
      mValueAnimator!!.cancel()
    }
  }

  /** Return whether the shimmer animation has been started.  */
  val isShimmerStarted: Boolean
    get() = mValueAnimator != null && mValueAnimator!!.isStarted

  public override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    mDrawRect.set(bounds)
    updateShader()
    maybeStartShimmer()
  }

  override fun draw(canvas: Canvas) {
    if (mShimmer == null || mShimmerPaint.shader == null) {
      return
    }
    val tiltTan = Math.tan(Math.toRadians(mShimmer!!.tilt.toDouble())).toFloat()
    val translateHeight = mDrawRect.height() + tiltTan * mDrawRect.width()
    val translateWidth = mDrawRect.width() + tiltTan * mDrawRect.height()
    val dx: Float
    val dy: Float
    val animatedValue = if (mValueAnimator != null) mValueAnimator!!.animatedValue as Float else 0f
    when (mShimmer!!.direction) {
      Shimmer.Direction.LEFT_TO_RIGHT -> {
        dx = offset(-translateWidth, translateWidth, animatedValue)
        dy = 0f
      }
      Shimmer.Direction.RIGHT_TO_LEFT -> {
        dx = offset(translateWidth, -translateWidth, animatedValue)
        dy = 0f
      }
      Shimmer.Direction.TOP_TO_BOTTOM -> {
        dx = 0f
        dy = offset(-translateHeight, translateHeight, animatedValue)
      }
      Shimmer.Direction.BOTTOM_TO_TOP -> {
        dx = 0f
        dy = offset(translateHeight, -translateHeight, animatedValue)
      }
      else -> {
        dx = offset(-translateWidth, translateWidth, animatedValue)
        dy = 0f
      }
    }
    mShaderMatrix.reset()
    mShaderMatrix.setRotate(mShimmer!!.tilt, mDrawRect.width() / 2f, mDrawRect.height() / 2f)
    mShaderMatrix.postTranslate(dx, dy)
    mShimmerPaint.shader.setLocalMatrix(mShaderMatrix)
    canvas.drawRect(mDrawRect, mShimmerPaint)
  }

  override fun setAlpha(alpha: Int) {
    // No-op, modify the Shimmer object you pass in instead
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    // No-op, modify the Shimmer object you pass in instead
  }

  override fun getOpacity(): Int {
    return if (mShimmer != null && (mShimmer!!.clipToChildren || mShimmer!!.alphaShimmer)) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE
  }

  private fun offset(start: Float, end: Float, percent: Float): Float {
    return start + (end - start) * percent
  }

  private fun updateValueAnimator() {
    if (mShimmer == null) {
      return
    }
    val started: Boolean
    if (mValueAnimator != null) {
      started = mValueAnimator!!.isStarted
      mValueAnimator!!.cancel()
      mValueAnimator!!.removeAllUpdateListeners()
    } else {
      started = false
    }
    mValueAnimator = ValueAnimator.ofFloat(0f, 1f + (mShimmer!!.repeatDelay / mShimmer!!.animationDuration).toFloat())
    mValueAnimator!!.interpolator = LinearInterpolator()
    mValueAnimator!!.repeatMode = mShimmer!!.repeatMode
    mValueAnimator!!.startDelay = mShimmer!!.startDelay
    mValueAnimator!!.repeatCount = mShimmer!!.repeatCount
    mValueAnimator!!.duration = mShimmer!!.animationDuration + mShimmer!!.repeatDelay
    mValueAnimator!!.addUpdateListener(mUpdateListener)
    if (started) {
      mValueAnimator!!.start()
    }
  }

  fun maybeStartShimmer() {
    if (mValueAnimator != null && !mValueAnimator!!.isStarted
        && mShimmer != null && mShimmer!!.autoStart
        && callback != null) {
      mValueAnimator!!.start()
    }
  }

  private fun updateShader() {
    val bounds = bounds
    val boundsWidth = bounds.width()
    val boundsHeight = bounds.height()
    if (boundsWidth == 0 || boundsHeight == 0 || mShimmer == null) {
      return
    }
    val width = mShimmer!!.width(boundsWidth.toFloat())
    val height = mShimmer!!.height(boundsHeight.toFloat())
    val shader: Shader
    when (mShimmer!!.shape) {
      Shimmer.Shape.LINEAR -> {
        val vertical = (mShimmer!!.direction == Shimmer.Direction.TOP_TO_BOTTOM
            || mShimmer!!.direction == Shimmer.Direction.BOTTOM_TO_TOP)
        val endX = if (vertical) 0F else width
        val endY = if (vertical) height else 0F
        shader = LinearGradient(
            0F, 0F, endX, endY, mShimmer!!.colors, mShimmer!!.positions, Shader.TileMode.CLAMP)
      }
      Shimmer.Shape.RADIAL -> shader = RadialGradient(
          width / 2f,
          height / 2f,
          (Math.max(width, height) / Math.sqrt(2.0)).toFloat(),
          mShimmer!!.colors,
          mShimmer!!.positions,
          Shader.TileMode.CLAMP)
      else -> {
        val vertical = (mShimmer!!.direction == Shimmer.Direction.TOP_TO_BOTTOM
            || mShimmer!!.direction == Shimmer.Direction.BOTTOM_TO_TOP)
        val endX = if (vertical) 0F else width
        val endY = if (vertical) height else 0F
        shader = LinearGradient(
            0F, 0F, endX, endY, mShimmer!!.colors, mShimmer!!.positions, Shader.TileMode.CLAMP)
      }
    }
    mShimmerPaint.shader = shader
  }

  init {
    mShimmerPaint.isAntiAlias = true
  }
}