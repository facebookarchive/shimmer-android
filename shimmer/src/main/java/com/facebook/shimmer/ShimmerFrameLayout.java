/**
 * Copyright (c) 2015-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.shimmer;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
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
  private final ValueAnimator.AnimatorUpdateListener mUpdateListener =
      new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          postInvalidate();
        }
      };

  private final Paint mShimmerPaint = new Paint();
  private final Paint mContentPaint = new Paint();
  private final RectF mDrawRect = new RectF();

  private @Nullable ValueAnimator mValueAnimator;

  private Shimmer mShimmer;

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
    mShimmerPaint.setAntiAlias(true);

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

  public ShimmerFrameLayout setShimmer(Shimmer shimmer) {
    if (shimmer == null) {
      throw new IllegalArgumentException("Given null shimmer");
    }
    mShimmer = shimmer;
    if (mShimmer.clipToChildren) {
      setLayerType(LAYER_TYPE_HARDWARE, mContentPaint);
    } else {
      setLayerType(LAYER_TYPE_NONE, null);
    }
    mShimmerPaint.setXfermode(
        new PorterDuffXfermode(
            mShimmer.alphaShimmer ? PorterDuff.Mode.DST_IN : PorterDuff.Mode.SRC_IN));
    updateShader();
    updateValueAnimator();
    postInvalidate();
    return this;
  }

  /** Starts the shimmer animation. */
  public void startShimmer() {
    if (mValueAnimator != null && !isShimmerStarted()) {
      mValueAnimator.start();
    }
  }

  /** Stops the shimmer animation. */
  public void stopShimmer() {
    if (mValueAnimator != null && isShimmerStarted()) {
      mValueAnimator.cancel();
    }
  }

  /** Return whether the shimmer animation has been started. */
  public boolean isShimmerStarted() {
    return mValueAnimator != null && mValueAnimator.isStarted();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    final int width = getWidth();
    final int height = getHeight();
    mShimmer.updateBounds(width, height);
    mDrawRect.set(2 * -width, 2 * -height, 4 * width, 4 * height);
    updateShader();
    maybeStartShimmer();
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    maybeStartShimmer();
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopShimmer();
  }

  @Override
  public void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    drawShimmer(canvas);
  }

  private void drawShimmer(Canvas canvas) {
    final float width = getWidth();
    final float height = getHeight();
    final float dx;
    final float dy;
    final float animatedValue = mValueAnimator != null ? mValueAnimator.getAnimatedFraction() : 0f;
    switch (mShimmer.direction) {
      default:
      case Shimmer.Direction.LEFT_TO_RIGHT:
        dx = offset(-width, width, animatedValue);
        dy = 0;
        break;
      case Shimmer.Direction.RIGHT_TO_LEFT:
        dx = offset(width, -width, animatedValue);
        dy = 0f;
        break;
      case Shimmer.Direction.TOP_TO_BOTTOM:
        dx = 0f;
        dy = offset(-height, height, animatedValue);
        break;
      case Shimmer.Direction.BOTTOM_TO_TOP:
        dx = 0f;
        dy = offset(height, -height, animatedValue);
        break;
    }

    final int saveCount = canvas.save();
    canvas.translate(dx, dy);
    canvas.rotate(mShimmer.tilt, width / 2f, height / 2f);
    canvas.drawRect(mDrawRect, mShimmerPaint);
    canvas.restoreToCount(saveCount);
  }

  private float offset(float start, float end, float percent) {
    return start + (end - start) * percent;
  }

  private void updateShader() {
    final int viewWidth = getWidth();
    final int viewHeight = getHeight();
    if (viewWidth == 0 || viewHeight == 0) {
      return;
    }
    final int width = mShimmer.width(getWidth());
    final int height = mShimmer.height(getHeight());

    final Shader shader;
    switch (mShimmer.shape) {
      default:
      case Shimmer.Shape.LINEAR:
        boolean vertical =
            mShimmer.direction == Shimmer.Direction.TOP_TO_BOTTOM
                || mShimmer.direction == Shimmer.Direction.BOTTOM_TO_TOP;
        int endX = vertical ? 0 : width;
        int endY = vertical ? height : 0;
        shader =
            new LinearGradient(
                0, 0, endX, endY, mShimmer.colors, mShimmer.positions, Shader.TileMode.CLAMP);
        break;
      case Shimmer.Shape.RADIAL:
        shader =
            new RadialGradient(
                width / 2f,
                height / 2f,
                (float) (Math.max(width, height) / Math.sqrt(2)),
                mShimmer.colors,
                mShimmer.positions,
                Shader.TileMode.CLAMP);
        break;
    }

    mShimmerPaint.setShader(shader);
  }

  private void updateValueAnimator() {
    final boolean started;
    if (mValueAnimator != null) {
      started = mValueAnimator.isStarted();
      mValueAnimator.cancel();
      mValueAnimator.removeAllUpdateListeners();
    } else {
      started = false;
    }

    mValueAnimator =
        ValueAnimator.ofFloat(0f, 1f + (float) (mShimmer.repeatDelay / mShimmer.animationDuration));
    mValueAnimator.setRepeatMode(mShimmer.repeatMode);
    mValueAnimator.setRepeatCount(mShimmer.repeatCount);
    mValueAnimator.setDuration(mShimmer.animationDuration + mShimmer.repeatDelay);
    mValueAnimator.addUpdateListener(mUpdateListener);
    if (started) {
      mValueAnimator.start();
    }
  }

  private void maybeStartShimmer() {
    if (mValueAnimator != null && !mValueAnimator.isStarted() && mShimmer.autoStart) {
      mValueAnimator.start();
    }
  }
}
