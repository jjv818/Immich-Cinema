package nl.giejay.android.tv.immich.timeline

import android.content.Context
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.api.model.Memory
import nl.giejay.android.tv.immich.api.util.ApiUtil
import java.text.SimpleDateFormat
import java.time.Year
import java.util.Date
import java.util.Locale

/**
 * Renders one "N years ago" memory card in the timeline's horizontal memories row.
 * Plain [Presenter] (not Leanback's [androidx.leanback.widget.ImageCardView]) since Immich's
 * design overlays the title directly on the cover image rather than in a region below it.
 */
class MemoryPresenter(
    private val context: Context,
    private val onMemoryClicked: (Memory) -> Unit,
    private val onMemoryFocused: (Memory) -> Unit = {}
) : Presenter() {

    private val cornerRadiusPx =
        context.resources.getDimension(R.dimen.memory_card_corner_radius)

    private val focusScale = 1f + context.resources.getFraction(R.fraction.focus_scale, 1, 1)
    private val motionUiMs = context.resources.getInteger(R.integer.motion_ui_ms).toLong()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        // Explicit outline so the cover ImageView is clipped to the same rounded rect as Immich.
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
        view.clipToOutline = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val memory = item as Memory
        val view = viewHolder.view
        val image = view.findViewById<ImageView>(R.id.memory_card_image)
        val title = view.findViewById<TextView>(R.id.memory_card_title)
        val date = view.findViewById<TextView>(R.id.memory_card_date)

        title.text = yearsAgoLabel(context, memory)
        date.text = formatMemoryDate(memory.memoryAt)

        val coverId = memory.assets.firstOrNull()?.id
        Glide.with(image)
            .load(ApiUtil.getThumbnailUrl(coverId, "thumbnail"))
            .centerCrop()
            .into(image)

        view.setTag(R.id.timeline_memory_id, memory.id)
        view.setOnClickListener { onMemoryClicked(memory) }
        view.setOnFocusChangeListener { v, hasFocus ->
            // Resting cards stay clipped inside end clearance; focused scale may cover the scrubber.
            findMemoriesGrid(v)?.clipToPadding = !hasFocus
            v.animate().scaleX(if (hasFocus) focusScale else 1f)
                .scaleY(if (hasFocus) focusScale else 1f)
                .setDuration(motionUiMs)
                .start()
            v.elevation = if (hasFocus) 8f else 0f
            v.foreground = if (hasFocus) context.getDrawable(R.drawable.focus_hairline) else null
            if (hasFocus) {
                onMemoryFocused(memory)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val image = viewHolder.view.findViewById<ImageView>(R.id.memory_card_image)
        Glide.with(image).clear(image)
    }

    private fun formatMemoryDate(date: Date): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault(Locale.Category.FORMAT)).format(date)

    companion object {
        fun yearsAgoLabel(context: Context, memory: Memory): String {
            val years = MemoryLabels.yearsAgo(Year.now().value, memory.data.year)
            return context.resources.getQuantityString(
                R.plurals.years_ago, years, years
            )
        }

        private fun findMemoriesGrid(view: View): HorizontalGridView? {
            var parent = view.parent
            while (parent != null) {
                if (parent is HorizontalGridView) return parent
                parent = parent.parent
            }
            return null
        }
    }
}
