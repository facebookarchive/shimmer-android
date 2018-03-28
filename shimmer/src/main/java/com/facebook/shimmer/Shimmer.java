/**
 * Copyright (c) 2015-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.shimmer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.Px;
import android.util.AttributeSet;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A Shimmer is an object detailing all of the configuration options available for {@link
 * ShimmerFrameLayout}
 */
public class Shimmer {
  private static final int COMPONENT_COUNT = 4;

  /** The shape of the shimmer's highlight. By default LINEAR is used. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({Shape.LINEAR, Shape.RADIAL})
  public @interface Shape {
    /** Linear gives a ray reflection effect. */
    int LINEAR = 0;
    /** Radial gives a spotlight effect. */
    int RADIAL = 1;
  }

  /** Direction of the shimmer's sweep. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    Direction.LEFT_TO_RIGHT,
    Direction.TOP_TO_BOTTOM,
    Direction.RIGHT_TO_LEFT,
    Direction.BOTTOM_TO_TOP
  })
  public @interface Direction {
    int LEFT_TO_RIGHT = 0;
    int TOP_TO_BOTTOM = 1;
    int RIGHT_TO_LEFT = 2;
    int BOTTOM_TO_TOP = 3;
  }

  final float[] positions = new float[COMPONENT_COUNT];
  final int[] colors = new int[COMPONENT_COUNT];
  final RectF bounds = new RectF();

  @Direction int direction = Direction.LEFT_TO_RIGHT;
  @ColorInt int highlightColor = Color.WHITE;
  @ColorInt int baseColor = 0x4cffffff;
  @Shape int shape = Shape.LINEAR;
  int fixedWidth = 0;
  int fixedHeight = 0;

  float widthRatio = 1f;
  float heightRatio = 1f;
  float intensity = 0f;
  float dropoff = 0.5f;
  float tilt = 20f;

  boolean clipToChildren = true;
  boolean autoStart = true;
  boolean alphaShimmer = true;

  int repeatCount = ValueAnimator.INFINITE;
  int repeatMode = ValueAnimator.RESTART;
  long animationDuration = 1000L;
  long repeatDelay;

  Shimmer() {}

  int width(int width) {
    return fixedWidth > 0 ? fixedWidth : Math.round(widthRatio * width);
  }

  int height(int height) {
    return fixedHeight > 0 ? fixedHeight : Math.round(heightRatio * height);
  }

  void updateColors() {
    switch (shape) {
      default:
      case Shape.LINEAR:
        colors[0] = baseColor;
        colors[1] = highlightColor;
        colors[2] = highlightColor;
        colors[3] = baseColor;
        break;
      case Shape.RADIAL:
        colors[0] = highlightColor;
        colors[1] = highlightColor;
        colors[2] = baseColor;
        colors[3] = baseColor;
        break;
    }
  }

  void updatePositions() {
    switch (shape) {
      default:
      case Shape.LINEAR:
        positions[0] = Math.max((1f - intensity - dropoff) / 2f, 0f);
        positions[1] = Math.max((1f - intensity - 0.001f) / 2f, 0f);
        positions[2] = Math.min((1f + intensity + 0.001f) / 2f, 1f);
        positions[3] = Math.min((1f + intensity + dropoff) / 2f, 1f);
        break;
      case Shape.RADIAL:
        positions[0] = 0f;
        positions[1] = Math.min(intensity, 1f);
        positions[2] = Math.min(intensity + dropoff, 1f);
        positions[3] = 1f;
        break;
    }
  }

  void updateBounds(int viewWidth, int viewHeight) {
    int magnitude = Math.max(viewWidth, viewHeight);
    double rad = Math.PI / 2f - Math.toRadians(tilt % 90f);
    double hyp = magnitude / Math.sin(rad);
    int padding = 3 * Math.round((float) (hyp - magnitude) / 2f);
    bounds.set(-padding, -padding, width(viewWidth) + padding, height(viewHeight) + padding);
  }

  public abstract static class Builder<T extends Builder<T>> {
    final Shimmer mShimmer = new Shimmer();

    // Gets around unchecked cast
    protected abstract T getThis();

    /** Applies all specified options from the {@link AttributeSet}. */
    public T consumeAttributes(Context context, AttributeSet attrs) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0);
      return consumeAttributes(a);
    }

    T consumeAttributes(TypedArray a) {
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_clip_to_children)) {
        setClipToChildren(
            a.getBoolean(
                R.styleable.ShimmerFrameLayout_shimmer_clip_to_children, mShimmer.clipToChildren));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_auto_start)) {
        setAutoStart(
            a.getBoolean(R.styleable.ShimmerFrameLayout_shimmer_auto_start, mShimmer.autoStart));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_base_alpha)) {
        setBaseAlpha(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_base_alpha, 0.3f));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_highlight_alpha)) {
        setHighlightAlpha(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_highlight_alpha, 1f));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_duration)) {
        setDuration(
            a.getInt(
                R.styleable.ShimmerFrameLayout_shimmer_duration, (int) mShimmer.animationDuration));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_repeat_count)) {
        setRepeatCount(
            a.getInt(R.styleable.ShimmerFrameLayout_shimmer_repeat_count, mShimmer.repeatCount));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_repeat_delay)) {
        setRepeatDelay(
            a.getInt(
                R.styleable.ShimmerFrameLayout_shimmer_repeat_delay, (int) mShimmer.repeatDelay));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_repeat_mode)) {
        setRepeatMode(
            a.getInt(R.styleable.ShimmerFrameLayout_shimmer_repeat_mode, mShimmer.repeatMode));
      }

      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_direction)) {
        int direction =
            a.getInt(R.styleable.ShimmerFrameLayout_shimmer_direction, mShimmer.direction);
        switch (direction) {
          default:
          case Direction.LEFT_TO_RIGHT:
            setDirection(Direction.LEFT_TO_RIGHT);
            break;
          case Direction.TOP_TO_BOTTOM:
            setDirection(Direction.TOP_TO_BOTTOM);
            break;
          case Direction.RIGHT_TO_LEFT:
            setDirection(Direction.RIGHT_TO_LEFT);
            break;
          case Direction.BOTTOM_TO_TOP:
            setDirection(Direction.BOTTOM_TO_TOP);
            break;
        }
      }

      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_shape)) {
        int shape = a.getInt(R.styleable.ShimmerFrameLayout_shimmer_shape, mShimmer.shape);
        switch (shape) {
          default:
          case Shape.LINEAR:
            setShape(Shape.LINEAR);
            break;
          case Shape.RADIAL:
            setShape(Shape.RADIAL);
            break;
        }
      }

      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_dropoff)) {
        setDropoff(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_dropoff, mShimmer.dropoff));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_fixed_width)) {
        setFixedWidth(
            a.getDimensionPixelSize(
                R.styleable.ShimmerFrameLayout_shimmer_fixed_width, mShimmer.fixedWidth));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_fixed_height)) {
        setFixedHeight(
            a.getDimensionPixelSize(
                R.styleable.ShimmerFrameLayout_shimmer_fixed_height, mShimmer.fixedHeight));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_intensity)) {
        setIntensity(
            a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_intensity, mShimmer.intensity));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_width_ratio)) {
        setWidthRatio(
            a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_width_ratio, mShimmer.widthRatio));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_height_ratio)) {
        setHeightRatio(
            a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_height_ratio, mShimmer.heightRatio));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_tilt)) {
        setTilt(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_tilt, mShimmer.tilt));
      }
      return getThis();
    }

    /** Sets the direction of the shimmer's sweep. See {@link Direction}. */
    public T setDirection(@Direction int direction) {
      mShimmer.direction = direction;
      return getThis();
    }

    /** Sets the shape of the shimmer. See {@link Shape}. */
    public T setShape(@Shape int shape) {
      mShimmer.shape = shape;
      return getThis();
    }

    /** Sets the fixed width of the shimmer, in pixels. */
    public T setFixedWidth(@Px int fixedWidth) {
      if (fixedWidth < 0) {
        throw new IllegalArgumentException("Given invalid width: " + fixedWidth);
      }
      mShimmer.fixedWidth = fixedWidth;
      return getThis();
    }

    /** Sets the fixed height of the shimmer, in pixels. */
    public T setFixedHeight(@Px int fixedHeight) {
      if (fixedHeight < 0) {
        throw new IllegalArgumentException("Given invalid height: " + fixedHeight);
      }
      mShimmer.fixedHeight = fixedHeight;
      return getThis();
    }

    /** Sets the width ratio of the shimmer, multiplied against the total width of the layout. */
    public T setWidthRatio(float widthRatio) {
      if (widthRatio < 0f) {
        throw new IllegalArgumentException("Given invalid width ratio: " + widthRatio);
      }
      mShimmer.widthRatio = widthRatio;
      return getThis();
    }

    /** Sets the height ratio of the shimmer, multiplied against the total height of the layout. */
    public T setHeightRatio(float heightRatio) {
      if (heightRatio < 0f) {
        throw new IllegalArgumentException("Given invalid height ratio: " + heightRatio);
      }
      mShimmer.heightRatio = heightRatio;
      return getThis();
    }

    /** Sets the intensity of the shimmer. A larger value causes the shimmer to be larger. */
    public T setIntensity(float intensity) {
      if (intensity < 0f) {
        throw new IllegalArgumentException("Given invalid intensity value: " + intensity);
      }
      mShimmer.intensity = intensity;
      return getThis();
    }

    /**
     * Sets how quickly the shimmer's gradient drops-off. A larger value causes a sharper drop-off.
     */
    public T setDropoff(float dropoff) {
      if (dropoff < 0f) {
        throw new IllegalArgumentException("Given invalid dropoff value: " + dropoff);
      }
      mShimmer.dropoff = dropoff;
      return getThis();
    }

    /** Sets the tilt angle of the shimmer in degrees. */
    public T setTilt(float tilt) {
      mShimmer.tilt = tilt;
      return getThis();
    }

    /**
     * Sets the base alpha, which is the alpha of the underlying children, amount in the range [0,
     * 1].
     */
    public T setBaseAlpha(@FloatRange(from = 0, to = 1) float alpha) {
      int intAlpha = (int) (clamp(0f, 1f, alpha) * 255f);
      mShimmer.baseColor = intAlpha << 24 | (mShimmer.baseColor & 0x00FFFFFF);
      return getThis();
    }

    /** Sets the shimmer alpha amount in the range [0, 1]. */
    public T setHighlightAlpha(@FloatRange(from = 0, to = 1) float alpha) {
      int intAlpha = (int) (clamp(0f, 1f, alpha) * 255f);
      mShimmer.highlightColor = intAlpha << 24 | (mShimmer.highlightColor & 0x00FFFFFF);
      return getThis();
    }

    /**
     * Sets whether the shimmer will clip to the childrens' contents, or if it will opaquely draw on
     * top of the children.
     */
    public T setClipToChildren(boolean status) {
      mShimmer.clipToChildren = status;
      return getThis();
    }

    /** Sets whether the shimmering animation will start automatically. */
    public T setAutoStart(boolean status) {
      mShimmer.autoStart = status;
      return getThis();
    }

    /**
     * Sets how often the shimmering animation will repeat. See {@link
     * android.animation.ValueAnimator#setRepeatCount(int)}.
     */
    public T setRepeatCount(int repeatCount) {
      mShimmer.repeatCount = repeatCount;
      return getThis();
    }

    /**
     * Sets how the shimmering animation will repeat. See {@link
     * android.animation.ValueAnimator#setRepeatMode(int)}.
     */
    public T setRepeatMode(int mode) {
      mShimmer.repeatMode = mode;
      return getThis();
    }

    /** Sets how long to wait in between repeats of the shimmering animation. */
    public T setRepeatDelay(long millis) {
      if (millis < 0) {
        throw new IllegalArgumentException("Given a negative repeat delay: " + millis);
      }
      mShimmer.repeatDelay = millis;
      return getThis();
    }

    /** Sets how long the shimmering animation takes to do one full sweep. */
    public T setDuration(long millis) {
      if (millis < 0) {
        throw new IllegalArgumentException("Given a negative duration: " + millis);
      }
      mShimmer.animationDuration = millis;
      return getThis();
    }

    public Shimmer build() {
      mShimmer.updateColors();
      mShimmer.updatePositions();
      return mShimmer;
    }

    private static float clamp(float min, float max, float value) {
      return Math.min(max, Math.max(min, value));
    }
  }

  public static class AlphaHighlightBuilder extends Builder<AlphaHighlightBuilder> {
    public AlphaHighlightBuilder() {
      mShimmer.alphaShimmer = true;
    }

    @Override
    protected AlphaHighlightBuilder getThis() {
      return this;
    }
  }

  public static class ColorHighlightBuilder extends Builder<ColorHighlightBuilder> {
    public ColorHighlightBuilder() {
      mShimmer.alphaShimmer = false;
    }

    /** Sets the highlight color for the shimmer. */
    public ColorHighlightBuilder setHighlightColor(@ColorInt int color) {
      mShimmer.highlightColor = color;
      return getThis();
    }

    /** Sets the base color for the shimmer. */
    public ColorHighlightBuilder setBaseColor(@ColorInt int color) {
      mShimmer.baseColor = (mShimmer.baseColor & 0xFF000000) | (color & 0x00FFFFFF);
      return getThis();
    }

    @Override
    ColorHighlightBuilder consumeAttributes(TypedArray a) {
      super.consumeAttributes(a);
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_base_color)) {
        setBaseColor(
            a.getColor(R.styleable.ShimmerFrameLayout_shimmer_base_color, mShimmer.baseColor));
      }
      if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_highlight_color)) {
        setHighlightColor(
            a.getColor(
                R.styleable.ShimmerFrameLayout_shimmer_highlight_color, mShimmer.highlightColor));
      }
      return getThis();
    }

    @Override
    protected ColorHighlightBuilder getThis() {
      return this;
    }
  }
}
