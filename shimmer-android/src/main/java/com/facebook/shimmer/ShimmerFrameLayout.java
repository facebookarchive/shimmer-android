// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.shimmer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class ShimmerFrameLayout extends FrameLayout {

  private static final String TAG = "ShimmerFrameLayout";
  private static final PorterDuffXfermode DST_IN_PORTER_DUFF_XFERMODE = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

  // enum specifying the shape of the highlight mask applied to the contained view
  public enum MaskShape {
    LINEAR,
    RADIAL
  }

  // enum controlling the angle of the highlight mask animation
  public enum MaskAngle {
    CW_0, // left to right
    CW_90, // top to bottom
    CW_180, // right to left
    CW_270, // bottom to top
  }

  // struct storing various mask related parameters, which are used to construct the mask bitmap
  private static class Mask {

    public MaskAngle angle;
    public float tilt;
    public float dropoff;
    public int fixedWidth;
    public int fixedHeight;
    public float intensity;
    public float relativeWidth;
    public float relativeHeight;
    public MaskShape shape;

    public int maskWidth(int width) {
      return fixedWidth > 0 ? fixedWidth : (int) (width * relativeWidth);
    }

    public int maskHeight(int height) {
      return fixedHeight > 0 ? fixedHeight : (int) (height * relativeHeight);
    }

    /**
     * Get the array of colors to be distributed along the gradient of the mask bitmap
     *
     * @return An array of black and transparent colors
     */
    public int[] getGradientColors() {
      switch (shape) {
        default:
        case LINEAR:
          return new int[]{Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT};
        case RADIAL:
          return new int[]{Color.BLACK, Color.BLACK, Color.TRANSPARENT};
      }
    }

    /**
     * Get the array of relative positions [0..1] of each corresponding color in the colors array
     *
     * @return A array of float values in the [0..1] range
     */
    public float[] getGradientPositions() {
      switch (shape) {
        default:
        case LINEAR:
          return new float[]{
              Math.max((1.0f - intensity - dropoff) / 2, 0.0f),
              Math.max((1.0f - intensity) / 2, 0.0f),
              Math.min((1.0f + intensity) / 2, 1.0f),
              Math.min((1.0f + intensity + dropoff) / 2, 1.0f)};
        case RADIAL:
          return new float[]{
              0.0f,
              Math.min(intensity, 1.0f),
              Math.min(intensity + dropoff, 1.0f)};
      }
    }
  }

  // struct for storing the mask translation animation values
  private static class MaskTranslation {

    public int fromX;
    public int fromY;
    public int toX;
    public int toY;

    public void set(int fromX, int fromY, int toX, int toY) {
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }
  }

  private Paint mAlphaPaint;
  private Paint mMaskPaint;

  private Mask mMask;
  private MaskTranslation mMaskTranslation;

  private Bitmap mRenderMaskBitmap;
  private Bitmap mRenderUnmaskBitmap;

  private boolean mAutoStart;
  private int mDuration;
  private int mRepeatCount;
  private int mRepeatDelay;
  private int mRepeatMode;

  private int mMaskOffsetX;
  private int mMaskOffsetY;

  private boolean mAnimationStarted;
  private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

  protected ValueAnimator mAnimator;
  protected Bitmap mMaskBitmap;

  public ShimmerFrameLayout(Context context) {
    this(context, null, 0);
  }

  public ShimmerFrameLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ShimmerFrameLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    setWillNotDraw(false);

    mMask = new Mask();
    mAlphaPaint = new Paint();
    mMaskPaint = new Paint();
    mMaskPaint.setAntiAlias(true);
    mMaskPaint.setDither(true);
    mMaskPaint.setFilterBitmap(true);
    mMaskPaint.setXfermode(DST_IN_PORTER_DUFF_XFERMODE);

    useDefaults();

    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0);
      try {
        if (a.hasValue(R.styleable.ShimmerFrameLayout_auto_start)) {
          setAutoStart(a.getBoolean(R.styleable.ShimmerFrameLayout_auto_start, false));
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_base_alpha)) {
          setBaseAlpha(a.getFloat(R.styleable.ShimmerFrameLayout_base_alpha, 0));
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_duration)) {
          setDuration(a.getInt(R.styleable.ShimmerFrameLayout_duration, 0));
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_repeat_count)) {
          setRepeatCount(a.getInt(R.styleable.ShimmerFrameLayout_repeat_count, 0));
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_repeat_delay)) {
          setRepeatDelay(a.getInt(R.styleable.ShimmerFrameLayout_repeat_delay, 0));
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_repeat_mode)) {
          setRepeatMode(a.getInt(R.styleable.ShimmerFrameLayout_repeat_mode, 0));
        }

        if (a.hasValue(R.styleable.ShimmerFrameLayout_angle)) {
          int angle = a.getInt(R.styleable.ShimmerFrameLayout_angle, 0);
          switch (angle) {
            default:
            case 0:
              mMask.angle = MaskAngle.CW_0;
              break;
            case 90:
              mMask.angle = MaskAngle.CW_90;
              break;
            case 180:
              mMask.angle = MaskAngle.CW_180;
              break;
            case 270:
              mMask.angle = MaskAngle.CW_270;
              break;
          }
        }

        if (a.hasValue(R.styleable.ShimmerFrameLayout_shape)) {
          int shape = a.getInt(R.styleable.ShimmerFrameLayout_shape, 0);
          switch (shape) {
            default:
            case 0:
              mMask.shape = MaskShape.LINEAR;
              break;
            case 1:
              mMask.shape = MaskShape.RADIAL;
              break;
          }
        }

        if (a.hasValue(R.styleable.ShimmerFrameLayout_dropoff)) {
          mMask.dropoff = a.getFloat(R.styleable.ShimmerFrameLayout_dropoff, 0);
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_fixed_width)) {
          mMask.fixedWidth = a.getDimensionPixelSize(R.styleable.ShimmerFrameLayout_fixed_width, 0);
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_fixed_height)) {
          mMask.fixedHeight = a.getDimensionPixelSize(R.styleable.ShimmerFrameLayout_fixed_height, 0);
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_intensity)) {
          mMask.intensity = a.getFloat(R.styleable.ShimmerFrameLayout_intensity, 0);
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_relative_width)) {
          mMask.relativeWidth = a.getFloat(R.styleable.ShimmerFrameLayout_relative_width, 0);
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_relative_height)) {
          mMask.relativeHeight = a.getFloat(R.styleable.ShimmerFrameLayout_relative_height, 0);
        }
        if (a.hasValue(R.styleable.ShimmerFrameLayout_tilt)) {
          mMask.tilt = a.getFloat(R.styleable.ShimmerFrameLayout_tilt, 0);
        }
      } finally {
        a.recycle();
      }
    }
  }

  /**
   * Resets the layout to its default state. Any parameters that were set or modified will be reverted back to their
   * original value. Also, stops the shimmer animation if it is currently playing.
   */
  public void useDefaults() {
    // Set defaults
    setAutoStart(false);
    setDuration(1000);
    setRepeatCount(ObjectAnimator.INFINITE);
    setRepeatDelay(0);
    setRepeatMode(ObjectAnimator.RESTART);

    mMask.angle = MaskAngle.CW_0;
    mMask.shape = MaskShape.LINEAR;
    mMask.dropoff = 0.5f;
    mMask.fixedWidth = 0;
    mMask.fixedHeight = 0;
    mMask.intensity = 0.0f;
    mMask.relativeWidth = 1.0f;
    mMask.relativeHeight = 1.0f;
    mMask.tilt = 20;

    mMaskTranslation = new MaskTranslation();

    setBaseAlpha(0.3f);

    resetAll();
  }

  /**
   * Is 'auto start' enabled for this layout. When auto start is enabled, the layout will start animating automatically
   * whenever it is attached to the current window.
   *
   * @return True if 'auto start' is enabled, false otherwise
   */
  public boolean isAutoStart() {
    return mAutoStart;
  }

  /**
   * Enable or disable 'auto start' for this layout. When auto start is enabled, the layout will start animating
   * automatically whenever it is attached to the current window.
   *
   * @param autoStart Whether auto start should be enabled or not
   */
  public void setAutoStart(boolean autoStart) {
    mAutoStart = autoStart;
    resetAll();
  }

  /**
   * Get the alpha currently used to render the base view i.e. the unhighlighted view over which the highlight is drawn.
   *
   * @return Alpha (opacity) of the base view
   */
  public float getBaseAlpha() {
    return (float) mAlphaPaint.getAlpha() / 0xff;
  }

  /**
   * Set the alpha to be used to render the base view i.e. the unhighlighted view over which the highlight is drawn.
   *
   * @param alpha Alpha (opacity) of the base view
   */
  public void setBaseAlpha(float alpha) {
    mAlphaPaint.setAlpha((int) (clamp(0, 1, alpha) * 0xff));
    resetAll();
  }

  /**
   * Get the duration of the current animation i.e. the time it takes for the highlight to move from one end
   * of the layout to the other. The default value is 1000 ms.
   *
   * @return Duration of the animation, in milliseconds
   */
  public int getDuration() {
    return mDuration;
  }

  /**
   * Set the duration of the animation i.e. the time it will take for the highlight to move from one end of the layout
   * to the other.
   *
   * @param duration Duration of the animation, in milliseconds
   */
  public void setDuration(int duration) {
    mDuration = duration;
    resetAll();
  }

  /**
   * Get the number of times of the current animation will repeat. The default value is -1, which means the animation
   * will repeat indefinitely.
   *
   * @return Number of times the current animation will repeat, or -1 for indefinite.
   */
  public int getRepeatCount() {
    return mRepeatCount;
  }

  /**
   * Set the number of times the animation should repeat. If the repeat count is 0, the animation stops after reaching
   * the end. If greater than 0, or -1 (for infinite), the repeat mode is taken into account.
   *
   * @param repeatCount Number of times the current animation should repeat, or -1 for indefinite.
   */
  public void setRepeatCount(int repeatCount) {
    mRepeatCount = repeatCount;
    resetAll();
  }

  /**
   * Get the delay after which the current animation will repeat. The default value is 0, which means the animation
   * will repeat immediately, unless it has ended.
   *
   * @return Delay after which the current animation will repeat, in milliseconds.
   */
  public int getRepeatDelay() {
    return mRepeatDelay;
  }

  /**
   * Set the delay after which the animation repeat, unless it has ended.
   *
   * @param repeatDelay Delay after which the animation should repeat, in milliseconds.
   */
  public void setRepeatDelay(int repeatDelay) {
    mRepeatDelay = repeatDelay;
    resetAll();
  }

  /**
   * Get what the current animation will do after reaching the end. One of
   * <a href="http://developer.android.com/reference/android/animation/ValueAnimator.html#REVERSE">REVERSE</a> or
   * <a href="http://developer.android.com/reference/android/animation/ValueAnimator.html#RESTART">RESTART</a>
   *
   * @return Repeat mode of the current animation
   */
  public int getRepeatMode() {
    return mRepeatMode;
  }

  /**
   * Set what the animation should do after reaching the end. One of
   * <a href="http://developer.android.com/reference/android/animation/ValueAnimator.html#REVERSE">REVERSE</a> or
   * <a href="http://developer.android.com/reference/android/animation/ValueAnimator.html#RESTART">RESTART</a>
   *
   * @param repeatMode Repeat mode of the animation
   */
  public void setRepeatMode(int repeatMode) {
    mRepeatMode = repeatMode;
    resetAll();
  }

  /**
   * Get the shape of the current animation's highlight mask. One of {@link MaskShape#LINEAR} or
   * {@link MaskShape#RADIAL}
   *
   * @return The shape of the highlight mask
   */
  public MaskShape getMaskShape() {
    return mMask.shape;
  }

  /**
   * Set the shape of the animation's highlight mask. One of {@link MaskShape#LINEAR} or {@link MaskShape#RADIAL}
   *
   * @param shape The shape of the highlight mask
   */
  public void setMaskShape(MaskShape shape) {
    mMask.shape = shape;
    resetAll();
  }

  /**
   * Get the angle at which the highlight mask is animated. One of:
   * <ul>
   * <li>{@link MaskAngle#CW_0} which animates left to right,</li>
   * <li>{@link MaskAngle#CW_90} which animates top to bottom,</li>
   * <li>{@link MaskAngle#CW_180} which animates right to left, or</li>
   * <li>{@link MaskAngle#CW_270} which animates bottom to top</li>
   * </ul>
   *
   * @return The {@link MaskAngle} of the current animation
   */
  public MaskAngle getAngle() {
    return mMask.angle;
  }

  /**
   * Set the angle of the highlight mask animation. One of:
   * <ul>
   * <li>{@link MaskAngle#CW_0} which animates left to right,</li>
   * <li>{@link MaskAngle#CW_90} which animates top to bottom,</li>
   * <li>{@link MaskAngle#CW_180} which animates right to left, or</li>
   * <li>{@link MaskAngle#CW_270} which animates bottom to top</li>
   * </ul>
   *
   * @param angle The {@link MaskAngle} of the new animation
   */
  public void setAngle(MaskAngle angle) {
    mMask.angle = angle;
    resetAll();
  }

  /**
   * Get the dropoff of the current animation's highlight mask. Dropoff controls the size of the fading edge of the
   * highlight.
   * <p/>
   * The default value of dropoff is 0.5.
   *
   * @return Dropoff of the highlight mask
   */
  public float getDropoff() {
    return mMask.dropoff;
  }

  /**
   * Set the dropoff of the animation's highlight mask, which defines the size of the highlight's fading edge.
   * <p/>
   * It is the relative distance from the center at which the highlight mask's opacity is 0 i.e it is fully transparent.
   * For a linear mask, the distance is relative to the center towards the edges. For a radial mask, the distance is
   * relative to the center towards the circumference. So a dropoff of 0.5 on a linear mask will create a band that
   * is half the size of the corresponding edge (depending on the {@link MaskAngle}), centered in the layout.
   *
   * @param dropoff
   */
  public void setDropoff(float dropoff) {
    mMask.dropoff = dropoff;
    resetAll();
  }

  /**
   * Get the fixed width of the highlight mask, or 0 if it is not set. By default it is 0.
   *
   * @return The width of the highlight mask if set, in pixels.
   */
  public int getFixedWidth() {
    return mMask.fixedWidth;
  }

  /**
   * Set the fixed width of the highlight mask, regardless of the size of the layout.
   *
   * @param fixedWidth The width of the highlight mask in pixels.
   */
  public void setFixedWidth(int fixedWidth) {
    mMask.fixedWidth = fixedWidth;
    resetAll();
  }

  /**
   * Get the fixed height of the highlight mask, or 0 if it is not set. By default it is 0.
   *
   * @return The height of the highlight mask if set, in pixels.
   */
  public int getFixedHeight() {
    return mMask.fixedHeight;
  }

  /**
   * Set the fixed height of the highlight mask, regardless of the size of the layout.
   *
   * @param fixedHeight The height of the highlight mask in pixels.
   */
  public void setFixedHeight(int fixedHeight) {
    mMask.fixedHeight = fixedHeight;
    resetAll();
  }

  /**
   * Get the intensity of the highlight mask, in the [0..1] range. The intensity controls the brightness of the
   * highlight; the higher it is, the greater is the opaque region in the highlight. The default value is 0.
   *
   * @return The intensity of the highlight mask
   */
  public float getIntensity() {
    return mMask.intensity;
  }

  /**
   * Set the intensity of the highlight mask, in the [0..1] range.
   * <p/>
   * Intensity is the point relative to the center where opacity starts dropping off, so an intensity of 0 would mean
   * that the highlight starts becoming translucent immediately from the center (the spread is controlled by 'dropoff').
   *
   * @param intensity The intensity of the highlight mask.
   */
  public void setIntensity(float intensity) {
    mMask.intensity = intensity;
    resetAll();
  }

  /**
   * Get the width of the highlight mask relative to the layout's width. The default is 1.0, meaning that the mask is
   * of the same width as the layout.
   *
   * @return Relative width of the highlight mask.
   */
  public float getRelativeWidth() {
    return mMask.relativeWidth;
  }

  /**
   * Set the width of the highlight mask relative to the layout's width, in the [0..1] range.
   *
   * @param relativeWidth Relative width of the highlight mask.
   */
  public void setRelativeWidth(int relativeWidth) {
    mMask.relativeWidth = relativeWidth;
    resetAll();
  }

  /**
   * Get the height of the highlight mask relative to the layout's height. The default is 1.0, meaning that the mask is
   * of the same height as the layout.
   *
   * @return Relative height of the highlight mask.
   */
  public float getRelativeHeight() {
    return mMask.relativeHeight;
  }

  /**
   * Set the height of the highlight mask relative to the layout's height, in the [0..1] range.
   *
   * @param relativeHeight Relative height of the highlight mask.
   */
  public void setRelativeHeight(int relativeHeight) {
    mMask.relativeHeight = relativeHeight;
    resetAll();
  }

  /**
   * Get the tilt angle of the highlight, in degrees. The default value is 20.
   *
   * @return The highlight's tilt angle, in degrees.
   */
  public float getTilt() {
    return mMask.tilt;
  }

  /**
   * Set the tile angle of the highlight, in degrees.
   *
   * @param tilt The highlight's tilt angle, in degrees.
   */
  public void setTilt(float tilt) {
    mMask.tilt = tilt;
    resetAll();
  }

  /**
   * Start the shimmer animation. If the 'auto start' property is set, this method is called automatically when the
   * layout is attached to the current window. Calling this method has no effect if the animation is already playing.
   */
  public void startShimmerAnimation() {
    if (mAnimationStarted) {
      return;
    }
    Animator animator = getShimmerAnimation();
    animator.start();
    mAnimationStarted = true;
  }

  /**
   * Stop the shimmer animation. Calling this method has no effect if the animation hasn't been started yet.
   */
  public void stopShimmerAnimation() {
    if (mAnimator != null) {
      mAnimator.end();
      mAnimator.removeAllUpdateListeners();
      mAnimator.cancel();
    }
    mAnimator = null;
    mAnimationStarted = false;
  }

  /**
   * Whether the shimmer animation is currently underway.
   *
   * @return True if the shimmer animation is playing, false otherwise.
   */
  public boolean isAnimationStarted() {
    return mAnimationStarted;
  }

  /**
   * Translate the mask offset horizontally. Used by the animator.
   *
   * @param maskOffsetX Horizontal translation offset of the mask
   */
  private void setMaskOffsetX(int maskOffsetX) {
    if (mMaskOffsetX == maskOffsetX) {
      return;
    }
    mMaskOffsetX = maskOffsetX;
    invalidate();
  }

  /**
   * Translate the mask offset vertically. Used by the animator.
   *
   * @param maskOffsetY Vertical translation offset of the mask
   */
  private void setMaskOffsetY(int maskOffsetY) {
    if (mMaskOffsetY == maskOffsetY) {
      return;
    }
    mMaskOffsetY = maskOffsetY;
    invalidate();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (mGlobalLayoutListener == null) {
      mGlobalLayoutListener = getLayoutListener();
    }
    getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
  }

  private ViewTreeObserver.OnGlobalLayoutListener getLayoutListener() {
    return new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        boolean animationStarted = mAnimationStarted;
        resetAll();
        if (mAutoStart || animationStarted) {
          startShimmerAnimation();
        }
      }
    };
  }

  @Override
  protected void onDetachedFromWindow() {
    stopShimmerAnimation();
    if (mGlobalLayoutListener != null) {
      getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener);
      mGlobalLayoutListener = null;
    }
    super.onDetachedFromWindow();
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (!mAnimationStarted || getWidth() <= 0 || getHeight() <= 0) {
      super.dispatchDraw(canvas);
      return;
    }
    dispatchDrawUsingBitmap(canvas);
  }

  private static float clamp(float min, float max, float value) {
    return Math.min(max, Math.max(min, value));
  }

  /**
   * Draws and masks the children using a Bitmap.
   *
   * @param canvas Canvas that the masked children will end up being drawn to.
   */
  private boolean dispatchDrawUsingBitmap(Canvas canvas) {
    Bitmap unmaskBitmap = tryObtainRenderUnmaskBitmap();
    Bitmap maskBitmap = tryObtainRenderMaskBitmap();
    if (unmaskBitmap == null || maskBitmap == null) {
      return false;
    }
    // First draw a desaturated version
    drawUnmasked(new Canvas(unmaskBitmap));
    canvas.drawBitmap(unmaskBitmap, 0, 0, mAlphaPaint);

    // Then draw the masked version
    drawMasked(new Canvas(maskBitmap));
    canvas.drawBitmap(maskBitmap, 0, 0, null);

    return true;
  }

  private Bitmap tryObtainRenderUnmaskBitmap() {
    if (mRenderUnmaskBitmap == null) {
      mRenderUnmaskBitmap = tryCreateRenderBitmap();
    }
    return mRenderUnmaskBitmap;
  }

  private Bitmap tryObtainRenderMaskBitmap() {
    if (mRenderMaskBitmap == null) {
      mRenderMaskBitmap = tryCreateRenderBitmap();
    }
    return mRenderMaskBitmap;
  }

  private Bitmap tryCreateRenderBitmap() {
    int width = getWidth();
    int height = getHeight();
    try {
      return createBitmapAndGcIfNecessary(width, height);
    } catch (OutOfMemoryError e) {
      String logMessage = "ShimmerFrameLayout failed to create working bitmap";
      StringBuilder logMessageStringBuilder = new StringBuilder(logMessage);
      logMessageStringBuilder.append(" (width = ");
      logMessageStringBuilder.append(width);
      logMessageStringBuilder.append(", height = ");
      logMessageStringBuilder.append(height);
      logMessageStringBuilder.append(")\n\n");
      for (StackTraceElement stackTraceElement :
          Thread.currentThread().getStackTrace()) {
        logMessageStringBuilder.append(stackTraceElement.toString());
        logMessageStringBuilder.append("\n");
      }
      logMessage = logMessageStringBuilder.toString();
      Log.d(TAG, logMessage);
    }
    return null;
  }

  // Draws the children without any mask.
  private void drawUnmasked(Canvas renderCanvas) {
    renderCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
    super.dispatchDraw(renderCanvas);
  }

  // Draws the children and masks them on the given Canvas.
  private void drawMasked(Canvas renderCanvas) {
    Bitmap maskBitmap = getMaskBitmap();
    if (maskBitmap == null) {
      return;
    }

    renderCanvas.clipRect(
        mMaskOffsetX,
        mMaskOffsetY,
        mMaskOffsetX + maskBitmap.getWidth(),
        mMaskOffsetY + maskBitmap.getHeight());
    renderCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
    super.dispatchDraw(renderCanvas);

    renderCanvas.drawBitmap(maskBitmap, mMaskOffsetX, mMaskOffsetY, mMaskPaint);
  }

  private void resetAll() {
    stopShimmerAnimation();
    resetMaskBitmap();
    resetRenderedView();
  }

  // If a mask bitmap was created, it's recycled and set to null so it will be recreated when needed.
  private void resetMaskBitmap() {
    if (mMaskBitmap != null) {
      mMaskBitmap.recycle();
      mMaskBitmap = null;
    }
  }

  // If a working bitmap was created, it's recycled and set to null so it will be recreated when needed.
  private void resetRenderedView() {
    if (mRenderUnmaskBitmap != null) {
      mRenderUnmaskBitmap.recycle();
      mRenderUnmaskBitmap = null;
    }

    if (mRenderMaskBitmap != null) {
      mRenderMaskBitmap.recycle();
      mRenderMaskBitmap = null;
    }
  }

  // Return the mask bitmap, creating it if necessary.
  private Bitmap getMaskBitmap() {
    if (mMaskBitmap != null) {
      return mMaskBitmap;
    }

    int width = mMask.maskWidth(getWidth());
    int height = mMask.maskHeight(getHeight());

    mMaskBitmap = createBitmapAndGcIfNecessary(width, height);
    Canvas canvas = new Canvas(mMaskBitmap);
    Shader gradient;
    switch (mMask.shape) {
      default:
      case LINEAR: {
        int x1, y1;
        int x2, y2;
        switch (mMask.angle) {
          default:
          case CW_0:
            x1 = 0;
            y1 = 0;
            x2 = width;
            y2 = 0;
            break;
          case CW_90:
            x1 = 0;
            y1 = 0;
            x2 = 0;
            y2 = height;
            break;
          case CW_180:
            x1 = width;
            y1 = 0;
            x2 = 0;
            y2 = 0;
            break;
          case CW_270:
            x1 = 0;
            y1 = height;
            x2 = 0;
            y2 = 0;
            break;
        }
        gradient =
            new LinearGradient(
                x1, y1,
                x2, y2,
                mMask.getGradientColors(),
                mMask.getGradientPositions(),
                Shader.TileMode.REPEAT);
        break;
      }
      case RADIAL: {
        int x = width / 2;
        int y = height / 2;
        gradient =
            new RadialGradient(
                x,
                y,
                (float) (Math.max(width, height) / Math.sqrt(2)),
                mMask.getGradientColors(),
                mMask.getGradientPositions(),
                Shader.TileMode.REPEAT);
        break;
      }
    }
    canvas.rotate(mMask.tilt, width / 2, height / 2);
    Paint paint = new Paint();
    paint.setShader(gradient);
    // We need to increase the rect size to account for the tilt
    int padding = (int) (Math.sqrt(2) * Math.max(width, height)) / 2;
    canvas.drawRect(-padding, -padding, width + padding, height + padding, paint);

    return mMaskBitmap;
  }

  // Get the shimmer <a href="http://developer.android.com/reference/android/animation/Animator.html">Animator</a>
  // object, which is responsible for driving the highlight mask animation.
  private Animator getShimmerAnimation() {
    if (mAnimator != null) {
      return mAnimator;
    }
    int width = getWidth();
    int height = getHeight();
    switch (mMask.shape) {
      default:
      case LINEAR:
        switch (mMask.angle) {
          default:
          case CW_0:
            mMaskTranslation.set(-width, 0, width, 0);
            break;
          case CW_90:
            mMaskTranslation.set(0, -height, 0, height);
            break;
          case CW_180:
            mMaskTranslation.set(width, 0, -width, 0);
            break;
          case CW_270:
            mMaskTranslation.set(0, height, 0, -height);
            break;
        }
    }
    mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f + (float) mRepeatDelay / mDuration);
    mAnimator.setDuration(mDuration + mRepeatDelay);
    mAnimator.setRepeatCount(mRepeatCount);
    mAnimator.setRepeatMode(mRepeatMode);
    mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float value = Math.max(0.0f, Math.min(1.0f, (Float) animation.getAnimatedValue()));
        setMaskOffsetX((int) (mMaskTranslation.fromX * (1 - value) + mMaskTranslation.toX * value));
        setMaskOffsetY((int) (mMaskTranslation.fromY * (1 - value) + mMaskTranslation.toY * value));
      }
    });
    return mAnimator;
  }

  /**
   * Creates a bitmap with the given width and height.
   * <p/>
   * If it fails with an OutOfMemory error, it will force a GC and then try to create the bitmap
   * one more time.
   *
   * @param width  width of the bitmap
   * @param height height of the bitmap
   */
  protected static Bitmap createBitmapAndGcIfNecessary(int width, int height) {
    try {
      return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    } catch (OutOfMemoryError e) {
      System.gc();
      return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
  }
}
