package nl.giejay.android.tv.immich.shared.motion

import android.provider.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * MOTION-005: the motion tokens must collapse to 0 when the system animator duration scale is 0,
 * so a user who disabled animations gets no transitions. Robolectric's ShadowSettings intercepts
 * `Settings.Global.putFloat/getFloat`, so the scale can be driven directly here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MotionTest {

    private val app = RuntimeEnvironment.getApplication()

    private fun setAnimatorScale(scale: Float) {
        Settings.Global.putFloat(
            app.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            scale
        )
    }

    @After
    fun resetAnimatorScale() {
        setAnimatorScale(1f)
    }

    @Test
    fun `full animator scale returns the motion token`() {
        setAnimatorScale(1f)
        assertTrue(Motion.animationsEnabled(app))
        assertEquals(150L, Motion.durationMs(app, 150))
        assertEquals(250L, Motion.durationMs(app, 250))
    }

    @Test
    fun `zero animator scale collapses durations to zero`() {
        setAnimatorScale(0f)
        assertFalse(Motion.animationsEnabled(app))
        assertEquals(0L, Motion.durationMs(app, 150))
        assertEquals(0L, Motion.durationMs(app, 250))
    }
}
