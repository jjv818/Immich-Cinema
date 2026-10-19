package nl.giejay.android.tv.immich.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.leanback.preference.LeanbackEditTextPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackListPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import arrow.core.Either
import kotlinx.coroutines.launch
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.shared.prefs.EnumByTitlePref
import nl.giejay.android.tv.immich.shared.prefs.EnumWithTitle
import nl.giejay.android.tv.immich.shared.prefs.PrefScreen
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager

/**
 * Host fragment for a settings screen (SET-001). Replaces the LeanbackSettingsFragmentCompat
 * header harness (the "right tab" layout) with a single pane that swaps
 * [LeanbackPreferenceFragmentCompat] screens: groups of iOS-style rows on the
 * `surface_base` pane, presented over the dimmed full-bleed artwork behind the dialog.
 *
 * Responsibilities migrated 1:1 from LeanbackSettingsFragmentCompat:
 *  - startPreferenceFragment: first screen is added, later screens replace + back-stack
 *  - OnPreferenceStartScreenCallback: drill-in with `ARG_PREFERENCE_ROOT`
 *  - OnPreferenceStartFragmentCallback: pref-declared fragments
 *  - OnPreferenceDisplayDialogCallback: the Leanback list/edit preference dialogs are
 *    started into the pane instead of mobile Material dialogs (they are plain Fragments
 *    styled as screens; they pop their own back-stack entry when chosen)
 */
abstract class SettingsScreenFragment : Fragment(),
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    abstract fun getFragment(): SettingsInnerFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.settings_screen_pane, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onPreferenceStartInitialScreen()
        // SET-004 backdrop: one random library asset behind the pane stack; flat surface
        // when the library has nothing or the user disabled background images.
        val backdrop = view.findViewById<ImageView>(R.id.settings_pane_backdrop)
        viewLifecycleOwner.lifecycleScope.launch {
            val url = SettingsBackdropLoader.fetchBackdropUrl()
            if (isAdded) {
                SettingsBackdropLoader.loadBackdrop(backdrop, url)
            }
        }
    }

    /** BACK routing toward the dialog: pop a drill-in / list dialog first, else dismiss. */
    fun onBackRequested(): Boolean {
        val fm = childFragmentManager
        // A transaction right after opening a list dialog may not be committed yet; sync so
        // the single-BACK semantics are correct while paging quickly.
        fm.executePendingTransactions()
        return if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            true
        } else {
            false
        }
    }

    private fun startPreferenceFragment(fragment: Fragment) {
        val fm = childFragmentManager
        val tx = fm.beginTransaction()
        val existing = fm.findFragmentByTag(PREFERENCE_FRAGMENT_TAG)
        if (existing != null) {
            tx.addToBackStack(null)
            tx.replace(R.id.settings_pane_body, fragment, PREFERENCE_FRAGMENT_TAG)
        } else {
            tx.add(R.id.settings_pane_body, fragment, PREFERENCE_FRAGMENT_TAG)
        }
        tx.commit()
    }

    private fun startImmersiveFragment(fragment: Fragment) {
        val fm = childFragmentManager
        val tx = fm.beginTransaction()
        val existing = fm.findFragmentByTag(PREFERENCE_FRAGMENT_TAG)
        if (existing != null && !existing.isRemoving) {
            tx.remove(existing)
        }
        tx.add(R.id.settings_screen_pane, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun onPreferenceStartInitialScreen() {
        val fragment = getFragment()
        fragment.arguments = arguments
        startPreferenceFragment(fragment)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val args = pref.extras
        val f = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader, pref.fragment
        )
        f.arguments = args
        f.setTargetFragment(caller, 0)
        if (f is PreferenceFragmentCompat || f is PreferenceDialogFragmentCompat) {
            startPreferenceFragment(f)
        } else {
            startImmersiveFragment(f)
        }
        return true
    }

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        val fragment = getFragment()
        val args = Bundle(1)
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.arguments = args
        startPreferenceFragment(fragment)
        return true
    }

    override fun onPreferenceDisplayDialog(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        if (childFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) return true
        val dialog: Fragment = when (pref) {
            is androidx.preference.ListPreference ->
                LeanbackListPreferenceDialogFragmentCompat.newInstanceSingle(pref.key)
            is MultiSelectListPreference ->
                LeanbackListPreferenceDialogFragmentCompat.newInstanceMulti(pref.key)
            is androidx.preference.EditTextPreference ->
                LeanbackEditTextPreferenceDialogFragmentCompat.newInstance(pref.key)
            else -> return false
        }
        dialog.setTargetFragment(caller, 0)
        startPreferenceFragment(dialog)
        return true
    }

    abstract class SettingsInnerFragment : LeanbackPreferenceFragmentCompat() {
        abstract fun getLayout(): Either<Int, PrefScreen>

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // after changing a pref, set the summary again for all listprefs with enumWithTitle
            getLayout().map { prefScreen ->
                val prefs = prefScreen.children.flatMap { it.children }
                prefs.filterIsInstance(EnumByTitlePref::class.java).forEach{
                    val value: EnumWithTitle = it.getValue(PreferenceManager.sharedPreference) as EnumWithTitle
                    findPreference<Preference>(it.key())!!.summary = value.getTitle()
                }
            }
        }

        abstract fun handlePreferenceClick(preference: Preference?): Boolean

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences_empty, rootKey)
            val layout = getLayout()
            layout.fold(
                { layoutId -> setPreferencesFromResource(layoutId, rootKey) },
                { prefScreen ->
                    preferenceScreen.title = prefScreen.title
                    prefScreen.children.forEach { category ->
                        // Unnamed PrefCategories render as pure spacing-free groups; titled
                        // categories (album ordering/filtering) get a section header row.
                        val group =
                            if (category.title.isNotBlank()) PreferenceCategory(requireContext()).apply {
                                title = category.title
                                preferenceScreen.addPreference(this)
                            } else {
                                preferenceScreen
                            }
                        category.children.forEach { child ->
                            val preference = child.createPreference(requireContext())
                            // Drill-in rows (child screens, pickers) get a trailing chevron;
                            // plain actions (start screensaver now, refresh, system settings)
                            // and boolean rows do not (SET-003). Keyed to the locked
                            // PrefScreen schema, so this stays valid presentation data.
                            if (preference.key in DRILL_IN_KEYS) {
                                preference.widgetLayoutResource = R.layout.settings_widget_chevron
                            }
                            group.addPreference(preference)
                        }
                    }
                })
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            val handled = handlePreferenceClick(preference!!)
            return if (handled) true else super.onPreferenceTreeClick(preference)
        }
    }

    companion object {
        private const val PREFERENCE_FRAGMENT_TAG =
            "nl.giejay.android.tv.immich.settings.PREFERENCE_FRAGMENT"
        private const val DIALOG_FRAGMENT_TAG =
            "nl.giejay.android.tv.immich.settings.PREFERENCE_DIALOG"

        /**
         * PrefScreen rows whose click opens another settings surface (child screen, album
         * picker or metadata customiser). These render a trailing chevron. Keys mirror the
         * locked `PrefScreen` schema in `shared/prefs/Preferences.kt` — an add there needs
         * the same key registered here (presentation layer only).
         */
        private val DRILL_IN_KEYS = setOf(
            // View > child screens
            "view_general",
            "view_ordering",
            "view_slideshow",
            "view_content",
            "view_slideshow_display",
            "view_slideshow_effects",
            // Album pickers (open the album selection screen)
            "screensaver_albums",
            "channel_albums",
            "exclude_assets_in_album",
            // Metadata customisers
            "screensaver_metadata_customizer",
            "slider_metadata_customizer"
        )
    }
}

