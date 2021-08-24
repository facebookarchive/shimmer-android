/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.shimmer

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.facebook.shimmer.Shimmer.AlphaHighlightBuilder
import com.facebook.shimmer.Shimmer.ColorHighlightBuilder

/**
 * Shimmer is an Android library that provides an easy way to add a shimmer effect to any [ ]. It is useful as an unobtrusive loading indicator, and was originally
 * developed for Facebook Home.
 *
 *
 * Find more examples and usage instructions over at: facebook.github.io/shimmer-android
 */
class ShimmerFrameLayout : FrameLayout {
  private val mContentPaint = Paint()
  private val mShimmerDrawable: ShimmerDrawable? = ShimmerDrawable()
  /** Return whether the shimmer drawable is visible.  */
  var isShimmerVisible = true
    private set
  private var mStoppedShimmerBecauseVisibility = false

  constructor(context: Context) : super(context) {
    init(context, null)
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    init(context, attrs)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    init(context, attrs)
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(
      context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    init(context, attrs)
  }

  private fun init(context: Context, attrs: AttributeSet?) {
    setWillNotDraw(false)
    mShimmerDrawable!!.callback = this
    if (attrs == null) {
      setShimmer(AlphaHighlightBuilder().build())
      return
    }
    val a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0)
    try {
      val shimmerBuilder = if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_colored)
          && a.getBoolean(R.styleable.ShimmerFrameLayout_shimmer_colored, false)) ColorHighlightBuilder() else AlphaHighlightBuilder()
      setShimmer(shimmerBuilder.consumeAttributes(a)!!.build())
    } finally {
      a.recycle()
    }
  }

  fun setShimmer(shimmer: Shimmer?): ShimmerFrameLayout {
    mShimmerDrawable!!.shimmer = shimmer
    if (shimmer != null && shimmer.clipToChildren) {
      setLayerType(LAYER_TYPE_HARDWARE, mContentPaint)
    } else {
      setLayerType(LAYER_TYPE_NONE, null)
    }
    return this
  }

  val shimmer: Shimmer?
    get() = mShimmerDrawable!!.shimmer

  /** Starts the shimmer animation.  */
  fun startShimmer() {
    mShimmerDrawable!!.startShimmer()
  }

  /** Stops the shimmer animation.  */
  fun stopShimmer() {
    mStoppedShimmerBecauseVisibility = false
    mShimmerDrawable!!.stopShimmer()
  }

  /** Return whether the shimmer animation has been started.  */
  val isShimmerStarted: Boolean
    get() = mShimmerDrawable!!.isShimmerStarted

  /**
   * Sets the ShimmerDrawable to be visible.
   *
   * @param startShimmer Whether to start the shimmer again.
   */
  fun showShimmer(startShimmer: Boolean) {
    isShimmerVisible = true
    if (startShimmer) {
      startShimmer()
    }
    invalidate()
  }

  /** Sets the ShimmerDrawable to be invisible, stopping it in the process.  */
  fun hideShimmer() {
    stopShimmer()
    isShimmerVisible = false
    invalidate()
  }

  public override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    val width = width
    val height = height
    mShimmerDrawable!!.setBounds(0, 0, width, height)
  }

  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)
    // View's constructor directly invokes this method, in which case no fields on
    // this class have been fully initialized yet.
    if (mShimmerDrawable == null) {
      return
    }
    if (visibility != VISIBLE) {
      // GONE or INVISIBLE
      if (isShimmerStarted) {
        stopShimmer()
        mStoppedShimmerBecauseVisibility = true
      }
    } else if (mStoppedShimmerBecauseVisibility) {
      mShimmerDrawable.maybeStartShimmer()
      mStoppedShimmerBecauseVisibility = false
    }
  }

  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    mShimmerDrawable!!.maybeStartShimmer()
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopShimmer()
  }

  public override fun dispatchDraw(canvas: Canvas) {
    super.dispatchDraw(canvas)
    if (isShimmerVisible) {
      mShimmerDrawable!!.draw(canvas)
    }
  }

  override fun verifyDrawable(who: Drawable): Boolean {
    return super.verifyDrawable(who) || who === mShimmerDrawable
  }
}