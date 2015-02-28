/*
 * This file provided by Facebook is for non-commercial testing and evaluation purposes only.
 * Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.shimmer.example;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.facebook.shimmer.ShimmerFrameLayout;

public class MainActivity extends Activity {

  private ShimmerFrameLayout mShimmerViewContainer;
  private Button[] mPresetButtons;
  private int mCurrentPreset = -1;
  private Toast mPresetToast;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);
    mShimmerViewContainer = (ShimmerFrameLayout) findViewById(R.id.shimmer_view_container);

    mPresetButtons = new Button[]{
        (Button) findViewById(R.id.preset_button0),
        (Button) findViewById(R.id.preset_button1),
        (Button) findViewById(R.id.preset_button2),
        (Button) findViewById(R.id.preset_button3),
        (Button) findViewById(R.id.preset_button4),
    };
    for (int i = 0; i < mPresetButtons.length; i++) {
      final int preset = i;
      mPresetButtons[i].setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          selectPreset(preset, true);
        }
      });
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    selectPreset(0, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    mShimmerViewContainer.startShimmerAnimation();
  }

  @Override
  public void onPause() {
    mShimmerViewContainer.stopShimmerAnimation();
    super.onPause();
  }

  /**
   * Select one of the shimmer animation presets.
   *
   * @param preset index of the shimmer animation preset
   * @param showToast whether to show a toast describing the preset, or not
   */
  private void selectPreset(int preset, boolean showToast) {
    if (mCurrentPreset == preset) {
      return;
    }
    if (mCurrentPreset >= 0) {
      mPresetButtons[mCurrentPreset].setBackgroundResource(R.color.preset_button_background);
    }
    mCurrentPreset = preset;
    mPresetButtons[mCurrentPreset].setBackgroundResource(R.color.preset_button_background_selected);

    // Save the state of the animation
    boolean isPlaying = mShimmerViewContainer.isAnimationStarted();

    // Reset all parameters of the shimmer animation
    mShimmerViewContainer.useDefaults();

    // If a toast is already showing, hide it
    if (mPresetToast != null) {
      mPresetToast.cancel();
    }

    switch (preset) {
      default:
      case 0:
        // Default
        mPresetToast = Toast.makeText(this, "Default", Toast.LENGTH_SHORT);
        break;
      case 1:
        // Slow and reverse
        mShimmerViewContainer.setDuration(5000);
        mShimmerViewContainer.setRepeatMode(ObjectAnimator.REVERSE);
        mPresetToast = Toast.makeText(this, "Slow and reverse", Toast.LENGTH_SHORT);
        break;
      case 2:
        // Thin, straight and transparent
        mShimmerViewContainer.setBaseAlpha(0.1f);
        mShimmerViewContainer.setDropoff(0.1f);
        mShimmerViewContainer.setTilt(0);
        mPresetToast = Toast.makeText(this, "Thin, straight and transparent", Toast.LENGTH_SHORT);
        break;
      case 3:
        // Sweep angle 90
        mShimmerViewContainer.setAngle(ShimmerFrameLayout.MaskAngle.CW_90);
        mPresetToast = Toast.makeText(this, "Sweep angle 90", Toast.LENGTH_SHORT);
        break;
      case 4:
        // Spotlight
        mShimmerViewContainer.setBaseAlpha(0);
        mShimmerViewContainer.setDuration(2000);
        mShimmerViewContainer.setDropoff(0.1f);
        mShimmerViewContainer.setIntensity(0.35f);
        mShimmerViewContainer.setMaskShape(ShimmerFrameLayout.MaskShape.RADIAL);
        mPresetToast = Toast.makeText(this, "Spotlight", Toast.LENGTH_SHORT);
        break;
    }

    // Show toast describing the chosen preset, if necessary
    if (showToast) {
      mPresetToast.show();
    }

    // Setting a value on the shimmer layout stops the animation. Restart it, if necessary.
    if (isPlaying) {
      mShimmerViewContainer.startShimmerAnimation();
    }
  }
}
