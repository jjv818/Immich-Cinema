package nl.giejay.android.tv.immich.card

import android.content.Context
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import nl.giejay.android.tv.immich.R

/**
 * Renders a grid card as image-only artwork: full-bleed cover, a bottom [R.drawable.scrim_bottom]
 * gradient, and a title + secondary line overlaid bottom-left. No footer bar.
 *
 * Focus is a restrained ~1.03 scale + 8dp lift + a white hairline ([R.drawable.focus_hairline]) —
 * colour is never the only cue.
 */
class ArtworkCardPresenter(private val context: Context) : Presenter() {

    private val cornerRadiusPx = context.resources.getDimension(R.dimen.radius_card)
    private val cardWidthPx = context.resources.getDimension(R.dimen.artwork_card_width).toInt()
    private val motionUiMs = context.resources.getInteger(R.integer.motion_ui_ms)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.artwork_card, parent, false)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
        view.clipToOutline = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val card = item as ICard
        val view = viewHolder.view
        val image = view.findViewById<ImageView>(R.id.artwork_card_image)
        val title = view.findViewById<TextView>(R.id.artwork_card_title)
        val subtitle = view.findViewById<TextView>(R.id.artwork_card_subtitle)

        title.text = card.title
        if (card.description.isNullOrBlank()) {
            subtitle.visibility = View.GONE
        } else {
            subtitle.visibility = View.VISIBLE
            subtitle.text = card.description
        }

        card.thumbnailUrl?.let { url ->
            if (url.startsWith("http")) {
                Glide.with(image)
                    .load(url)
                    .apply(
                        RequestOptions()
                            .centerCrop()
                            .override(cardWidthPx)
                            .placeholder(R.drawable.image_placeholder)
                            .error(R.drawable.image_placeholder)
                    )
                    .into(image)
            } else {
                image.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val resId = context.resources
                    .getIdentifier(url, "drawable", "nl.giejay.android.tv.immich")
                image.setImageResource(resId)
            }
        }

        view.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.05f else 1f)
                .scaleY(if (hasFocus) 1.05f else 1f)
                .setDuration(motionUiMs.toLong())
                .start()
            v.elevation = if (hasFocus) 8f else 0f
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val image = viewHolder.view.findViewById<ImageView>(R.id.artwork_card_image)
        Glide.with(image).clear(image)
    }
}
