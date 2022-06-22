/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.shimmer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Shimmer is an Android library that provides an easy way to add a shimmer effect to any {@link
 * android.view.View}. It is useful as an unobtrusive loading indicator, and was originally
 * developed for Facebook Home.
 *
 * <p>Find more examples and usage instructions over at: facebook.github.io/shimmer-android
 */
public class ShimmerFrameLayout extends FrameLayout {
  private final Paint mContentPaint = new Paint();
  private final ShimmerDrawable mShimmerDrawable = new ShimmerDrawable();

  private boolean mShowShimmer = true;
  private boolean mStoppedShimmerBecauseVisibility = false;

  public ShimmerFrameLayout(Context context) {
    super(context);
    init(context, null);
  }

  public ShimmerFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public ShimmerFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public ShimmerFrameLayout(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context, attrs);
  }

  private void init(Context context, @Nullable AttributeSet attrs) {
    setWillNotDraw(false);
    mShimmerDrawable.setCallback(this);

    if (attrs == null) {
      setShimmer(new Shimmer.AlphaHighlightBuilder().build());
      return;
    }

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0);
    try {
      Shimmer.Builder shimmerBuilder =
          a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_colored)
                  && a.getBoolean(R.styleable.ShimmerFrameLayout_shimmer_colored, false)
              ? new Shimmer.ColorHighlightBuilder()
              : new Shimmer.AlphaHighlightBuilder();
      setShimmer(shimmerBuilder.consumeAttributes(a).build());
    } finally {
      a.recycle();
    }
  }

  public ShimmerFrameLayout setShimmer(@Nullable Shimmer shimmer) {
    mShimmerDrawable.setShimmer(shimmer);
    if (shimmer != null && shimmer.clipToChildren) {
      setLayerType(LAYER_TYPE_HARDWARE, mContentPaint);
    } else {
      setLayerType(LAYER_TYPE_NONE, null);
    }

    return this;
  }

  public @Nullable Shimmer getShimmer() {
    return mShimmerDrawable.getShimmer();
  }

  /** Starts the shimmer animation. */
  public void startShimmer() {
    if (isAttachedToWindow()) {
      mShimmerDrawable.startShimmer();
    }
  }

  /** Stops the shimmer animation. */
  public void stopShimmer() {
    mStoppedShimmerBecauseVisibility = false;
    mShimmerDrawable.stopShimmer();
  }

  /** Return whether the shimmer animation has been started. */
  public boolean isShimmerStarted() {
    return mShimmerDrawable.isShimmerStarted();
  }

  /**
   * Sets the ShimmerDrawable to be visible.
   *
   * @param startShimmer Whether to start the shimmer again.
   */
  public void showShimmer(boolean startShimmer) {
    mShowShimmer = true;
    if (startShimmer) {
      startShimmer();
    }
    invalidate();
  }

  /** Sets the ShimmerDrawable to be invisible, stopping it in the process. */
  public void hideShimmer() {
    stopShimmer();
    mShowShimmer = false;
    invalidate();
  }

  /** Return whether the shimmer drawable is visible. */
  public boolean isShimmerVisible() {
    return mShowShimmer;
  }

  public boolean isShimmerRunning() {
    return mShimmerDrawable.isShimmerRunning();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    final int width = getWidth();
    final int height = getHeight();
    mShimmerDrawable.setBounds(0, 0, width, height);
  }

  @Override
  protected void onVisibilityChanged(View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);
    // View's constructor directly invokes this method, in which case no fields on
    // this class have been fully initialized yet.
    if (mShimmerDrawable == null) {
      return;
    }
    if (visibility != View.VISIBLE) {
      // GONE or INVISIBLE
      if (isShimmerStarted()) {
        stopShimmer();
        mStoppedShimmerBecauseVisibility = true;
      }
    } else if (mStoppedShimmerBecauseVisibility) {
      mShimmerDrawable.maybeStartShimmer();
      mStoppedShimmerBecauseVisibility = false;
    }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    mShimmerDrawable.maybeStartShimmer();
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopShimmer();
  }

  @Override
  public void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (mShowShimmer) {
      mShimmerDrawable.draw(canvas);
    }
  }

  @Override
  protected boolean verifyDrawable(@NonNull Drawable who) {
    return super.verifyDrawable(who) || who == mShimmerDrawable;
  }

  public void setStaticAnimationProgress(float value) {
    mShimmerDrawable.setStaticAnimationProgress(value);
  }

  public void clearStaticAnimationProgress() {
    mShimmerDrawable.clearStaticAnimationProgress();
  }
}
