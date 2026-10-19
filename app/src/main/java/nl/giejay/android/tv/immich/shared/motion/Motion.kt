package nl.giejay.android.tv.immich.shared.motion

import android.content.Context
import android.provider.Settings

/**
 * Resolves the design system's motion tokens against the system animator duration scale
 * (MOTION-005, AGENTS.md §5.7).
 *
 * When the user turns animations off (Developer options → Animator duration scale → off, or a
 * reduced-motion accessibility setting), [durationMs] returns `0`. Callers must still apply the
 * end state: a zero-duration [android.view.ViewPropertyAnimator] applies its final value
 * immediately, so nothing gets stuck mid-state.
 */
object Motion {

    /**
     * [tokenMs] when animations run, or `0` when the system animator duration scale is 0.
     *
     * Reads [Settings.Global.ANIMATOR_DURATION_SCALE] directly instead of relying on
     * [android.animation.ValueAnimator.areAnimatorsEnabled], which is API 26+ (minSdk is 24).
     */
    fun durationMs(context: Context, tokenMs: Int): Long =
        if (animationsEnabled(context)) tokenMs.toLong() else 0L

    /** False when the system animator duration scale is 0; defaults to enabled. */
    fun animationsEnabled(context: Context): Boolean {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (e: SecurityException) {
            // A restricted profile may refuse the read; treat that as "animations on".
            1f
        }
        return scale != 0f
    }
}
