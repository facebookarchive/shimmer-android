/**
 * Copyright (c) 2015-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.shimmer;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class ShimmerDrawable extends Drawable {
  private final ValueAnimator.AnimatorUpdateListener mUpdateListener =
      new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          invalidateSelf();
        }
      };

  private final Paint mShimmerPaint = new Paint();
  private final Rect mDrawRect = new Rect();
  private final Matrix mShaderMatrix = new Matrix();

  private @Nullable ValueAnimator mValueAnimator;

  private @Nullable Shimmer mShimmer;

  public ShimmerDrawable() {
    mShimmerPaint.setAntiAlias(true);
  }

  public void setShimmer(@Nullable Shimmer shimmer) {
    mShimmer = shimmer;
    if (mShimmer != null) {
      mShimmerPaint.setXfermode(
          new PorterDuffXfermode(
              mShimmer.alphaShimmer ? PorterDuff.Mode.DST_IN : PorterDuff.Mode.SRC_IN));
    }
    updateShader();
    updateValueAnimator();
    invalidateSelf();
  }

  /** Starts the shimmer animation. */
  public void startShimmer() {
    if (mValueAnimator != null && !isShimmerStarted() && getCallback() != null) {
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
  public void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    final int width = bounds.width();
    final int height = bounds.height();
    mDrawRect.set(0, 0, width, height);
    updateShader();
    maybeStartShimmer();
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (mShimmer == null || mShimmerPaint.getShader() == null) {
      return;
    }

    final float tiltTan = (float) Math.tan(Math.toRadians(mShimmer.tilt));
    final float translateHeight = mDrawRect.height() + tiltTan * mDrawRect.width();
    final float translateWidth = mDrawRect.width() + tiltTan * mDrawRect.height();
    final float dx;
    final float dy;
    final float animatedValue = mValueAnimator != null ? mValueAnimator.getAnimatedFraction() : 0f;
    switch (mShimmer.direction) {
      default:
      case Shimmer.Direction.LEFT_TO_RIGHT:
        dx = offset(-translateWidth, translateWidth, animatedValue);
        dy = 0;
        break;
      case Shimmer.Direction.RIGHT_TO_LEFT:
        dx = offset(translateWidth, -translateWidth, animatedValue);
        dy = 0f;
        break;
      case Shimmer.Direction.TOP_TO_BOTTOM:
        dx = 0f;
        dy = offset(-translateHeight, translateHeight, animatedValue);
        break;
      case Shimmer.Direction.BOTTOM_TO_TOP:
        dx = 0f;
        dy = offset(translateHeight, -translateHeight, animatedValue);
        break;
    }

    mShaderMatrix.reset();
    mShaderMatrix.setRotate(mShimmer.tilt, mDrawRect.width() / 2f, mDrawRect.height() / 2f);
    mShaderMatrix.postTranslate(dx, dy);
    mShimmerPaint.getShader().setLocalMatrix(mShaderMatrix);
    canvas.drawRect(mDrawRect, mShimmerPaint);
  }

  @Override
  public void setAlpha(int alpha) {
    // No-op, modify the Shimmer object you pass in instead
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    // No-op, modify the Shimmer object you pass in instead
  }

  @Override
  public int getOpacity() {
    return mShimmer != null && (mShimmer.clipToChildren || mShimmer.alphaShimmer)
        ? PixelFormat.TRANSLUCENT
        : PixelFormat.OPAQUE;
  }

  private float offset(float start, float end, float percent) {
    return start + (end - start) * percent;
  }

  private void updateValueAnimator() {
    if (mShimmer == null) {
      return;
    }

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

  void maybeStartShimmer() {
    if (mValueAnimator != null
        && !mValueAnimator.isStarted()
        && mShimmer != null
        && mShimmer.autoStart
        && getCallback() != null) {
      mValueAnimator.start();
    }
  }

  private void updateShader() {
    final Rect bounds = getBounds();
    final int boundsWidth = bounds.width();
    final int boundsHeight = bounds.height();
    if (boundsWidth == 0 || boundsHeight == 0 || mShimmer == null) {
      return;
    }
    final int width = mShimmer.width(boundsWidth);
    final int height = mShimmer.height(boundsHeight);

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
}
