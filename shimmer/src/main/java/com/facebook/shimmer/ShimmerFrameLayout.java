/**
 * Copyright (c) 2015-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.shimmer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

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

  /** Starts the shimmer animation. */
  public void startShimmer() {
    mShimmerDrawable.startShimmer();
  }

  /** Stops the shimmer animation. */
  public void stopShimmer() {
    mShimmerDrawable.stopShimmer();
  }

  /** Return whether the shimmer animation has been started. */
  public boolean isShimmerStarted() {
    return mShimmerDrawable.isShimmerStarted();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    final int width = getWidth();
    final int height = getHeight();
    mShimmerDrawable.setBounds(0, 0, width, height);
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
    mShimmerDrawable.draw(canvas);
  }

  @Override
  protected boolean verifyDrawable(@NonNull Drawable who) {
    return super.verifyDrawable(who) || who == mShimmerDrawable;
  }
}
