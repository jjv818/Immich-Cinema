package nl.giejay.android.tv.immich.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter
import nl.giejay.android.tv.immich.R

/**
 * Text-first settings hub row (SET-002): surface_raised chip, Body title, trailing chevron.
 * No icon tile. Clicks resolve through the row's SettingsCard.
 */
class SettingsHubRowPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_hub_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val card = item as SettingsCard
        viewHolder.view.findViewById<TextView>(R.id.settings_hub_row_title).text = card.title
        viewHolder.view.setOnClickListener { card.onClick() }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        viewHolder.view.setOnClickListener(null)
    }
}

/**
 * Group header above a settings hub row group.
 */
class SettingsHubHeaderPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_hub_header, parent, false)
        view.isFocusable = false
        view.isClickable = false
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val header = item as SettingsHubHeader
        viewHolder.view.findViewById<TextView>(R.id.settings_hub_header_title).text = header.title
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
