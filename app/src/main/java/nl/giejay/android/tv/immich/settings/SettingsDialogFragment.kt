package nl.giejay.android.tv.immich.settings

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.DialogFragment
import nl.giejay.android.tv.immich.R


/**
 * Settings dialog surface (SET-001..SET-005). Window: transparent background over the
 * underlying page's artwork, with a dim (`SettingsDialog` style) so the artwork stays
 * visible, un-blurred and lightly dimmed behind the floating settings pane
 * (R.layout.fragment_dialog / `fragment_settings_holder`).
 *
 * BACK handling: the dialog's key listener first lets the settings pane ([SettingsScreenFragment])
 * pop drill-in/list-dialog entries before dismissing.
 */
class SettingsDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val args = SettingsDialogFragmentArgs.fromBundle(requireArguments())
        val settingsType = args.settingsType
        val v: View = inflater.inflate(R.layout.fragment_dialog, container, false)
        val fragment = when (settingsType) {
            "album_details" -> {
                val frag = AlbumDetailsSettingsFragment()
                frag.arguments = requireArguments()
                frag
            }
            "generic_asset_settings" -> {
                GenericAssetsSettingsFragment()
            }
            "meta_data_item" -> {
                // todo refactor this, make MetaDataItemCustomizerFragment a child of SettingsDialogFragment and include it in the navGraph
                val frag = MetaDataItemCustomizerFragment()
                frag.arguments = requireArguments()
                frag
            }
            "meta_data_customizer" -> {
                val frag = MetaDataCustomizerFragment()
                frag.arguments = requireArguments()
                frag
            }
            else -> {
                val frag = PrefSettingsFragment()
                frag.arguments = requireArguments()
                frag
            }
        }
        if (args.fullscreen) {
            val holder = v.findViewById<View>(R.id.fragment_settings_holder)
            val params = holder.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            holder.layoutParams = params
        }
        childFragmentManager.beginTransaction().add(R.id.fragment_settings_holder, fragment)
            .commit()
        return v
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Window shell only: transparent window, dimming behind comes from the SettingsDialog
        // theme (backgroundDimEnabled/Amount) so the artwork stays visible and lightly
        // dimmed; the pane layout in onCreateView provides the floating settings column.
        val root = RelativeLayout(activity)
        val dialog = Dialog(requireContext(), R.style.SettingsDialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(root)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                when (val child = childFragmentManager.fragments.lastOrNull()) {
                    is SettingsScreenFragment -> child.onBackRequested()
                    else -> false
                }
            } else {
                false
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Always full-bleed: the pane keeps its column geometry from the layout regardless.
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
