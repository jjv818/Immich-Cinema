package nl.giejay.android.tv.immich.card

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import nl.giejay.android.tv.immich.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards the artwork card binding (§5.3): the title is always shown, the secondary line only
 * appears when there is a description, and the image placeholder/error state is wired so a
 * slow/failed load never renders a flat grey tile.
 *
 * Cards are built with `thumbnailUrl = null` so Glide is never invoked — the test is about the
 * view binding, not the image pipeline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ArtworkCardPresenterTest {

    private val app = RuntimeEnvironment.getApplication()
    private val presenter = ArtworkCardPresenter(app)

    private fun bind(card: ICard): View {
        val parent = FrameLayout(app)
        val holder = presenter.onCreateViewHolder(parent)
        presenter.onBindViewHolder(holder, card)
        return holder.view
    }

    private fun card(
        title: String = "Photo",
        description: String? = null,
        thumbnailUrl: String? = null
    ): ICard = object : ICard {
        override val title: String = title
        override val description: String? = description
        override val id: String = "asset-1"
        override val thumbnailUrl: String? = thumbnailUrl
        override val backgroundUrl: String? = null
        override val selected: Boolean = false
    }

    @Test
    fun `title is always bound`() {
        val title = bind(card(title = "Oct 19, 2026"))
            .findViewById<TextView>(R.id.artwork_card_title)
        assertEquals("Oct 19, 2026", title.text.toString())
    }

    @Test
    fun `blank description hides the subtitle`() {
        val subtitle = bind(card(description = null))
            .findViewById<TextView>(R.id.artwork_card_subtitle)
        assertEquals(View.GONE, subtitle.visibility)
    }

    @Test
    fun `empty description hides the subtitle`() {
        val subtitle = bind(card(description = ""))
            .findViewById<TextView>(R.id.artwork_card_subtitle)
        assertEquals(View.GONE, subtitle.visibility)
    }

    @Test
    fun `description is shown as the subtitle`() {
        val subtitle = bind(card(description = "Golden Gate"))
            .findViewById<TextView>(R.id.artwork_card_subtitle)
        assertEquals(View.VISIBLE, subtitle.visibility)
        assertEquals("Golden Gate", subtitle.text.toString())
    }

    @Test
    fun `placeholder drawable resolves`() {
        // ArtworkCardPresenter wires this as both placeholder and error; if the resource is
        // missing, the card renders a flat grey tile on slow/failed loads.
        assertNotNull(app.resources.getDrawable(R.drawable.image_placeholder, null))
    }
}
