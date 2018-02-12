/**
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

package com.facebook.shimmer.sample

import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.android.synthetic.main.main.*

class MainActivity : Activity(), View.OnClickListener {
  private lateinit var shimmerViewContainer: ShimmerFrameLayout
  private lateinit var presetButtons: Array<Button>
  private var currentPreset = -1
  private var toast: Toast? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    shimmerViewContainer = shimmer_view_container
    presetButtons = arrayOf(
        preset_button0,
        preset_button1,
        preset_button2,
        preset_button3,
        preset_button4
    )
    presetButtons.forEach { it.setOnClickListener(this@MainActivity) }
    selectPreset(0, false)
  }

  override fun onClick(v: View) {
    selectPreset(presetButtons.indexOf(v as Button), true)
  }

  public override fun onResume() {
    super.onResume()
    shimmerViewContainer.startShimmerAnimation()
  }

  public override fun onPause() {
    shimmerViewContainer.stopShimmerAnimation()
    super.onPause()
  }

  private fun selectPreset(preset: Int, showToast: Boolean) {
    if (currentPreset == preset) {
      return
    }

    if (currentPreset >= 0) {
      presetButtons[currentPreset].setBackgroundResource(R.color.preset_button_background)
    }
    currentPreset = preset
    presetButtons[currentPreset].setBackgroundResource(R.color.preset_button_background_selected)

    // Save the state of the animation
    val isPlaying = shimmerViewContainer.isAnimationStarted

    // Reset all parameters of the shimmer animation
    shimmerViewContainer.useDefaults()

    // If a toast is already showing, hide it
    toast?.cancel()

    when (preset) {
      1 -> {
        // Slow and reverse
        shimmerViewContainer.duration = 5000
        shimmerViewContainer.repeatMode = ObjectAnimator.REVERSE
        toast = Toast.makeText(this, "Slow and reverse", Toast.LENGTH_SHORT)
      }
      2 -> {
        // Thin, straight and transparent
        shimmerViewContainer.baseAlpha = 0.1f
        shimmerViewContainer.dropoff = 0.1f
        shimmerViewContainer.tilt = 0f
        toast = Toast.makeText(this, "Thin, straight and transparent", Toast.LENGTH_SHORT)
      }
      3 -> {
        // Sweep angle 90
        shimmerViewContainer.angle = ShimmerFrameLayout.MaskAngle.CW_90
        toast = Toast.makeText(this, "Sweep angle 90", Toast.LENGTH_SHORT)
      }
      4 -> {
        // Spotlight
        shimmerViewContainer.baseAlpha = 0f
        shimmerViewContainer.duration = 2000
        shimmerViewContainer.dropoff = 0.1f
        shimmerViewContainer.intensity = 0.35f
        shimmerViewContainer.maskShape = ShimmerFrameLayout.MaskShape.RADIAL
        toast = Toast.makeText(this, "Spotlight", Toast.LENGTH_SHORT)
      }
      else -> toast = Toast.makeText(this, "Default", Toast.LENGTH_SHORT)
    }

    // Show toast describing the chosen preset, if necessary
    if (showToast) {
      toast?.show()
    }

    // Setting a value on the shimmer layout stops the animation. Restart it, if necessary.
    if (isPlaying) {
      shimmerViewContainer.startShimmerAnimation()
    }
  }
}
