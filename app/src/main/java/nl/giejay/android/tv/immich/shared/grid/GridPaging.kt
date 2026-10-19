package nl.giejay.android.tv.immich.shared.grid

/**
 * Pure page-navigation maths for the vertical asset grids.
 *
 * GRID-007: Leanback's [androidx.leanback.widget.BaseGridView] does not implement page navigation
 * on its own, so KEYCODE_PAGE_DOWN / KEYCODE_PAGE_UP (and the channel up/down variants) are wired
 * up by [nl.giejay.android.tv.immich.shared.fragment.GridFragment] using this helper.
 *
 * Kept free of Android types so the clamping behaviour is unit-testable on the JVM.
 */
object GridPaging {

    /**
     * Position the grid should jump to when paging one screen in [forward] direction.
     *
     * A "page" is [columns] * [rowsPerPage] adapter slots, so paging lands on the same column in
     * the row one screen away. The result is clamped to `0..itemCount - 1`.
     *
     * @return [selected] unchanged for degenerate input ([itemCount] <= 0, [columns] <= 0 or
     *   [rowsPerPage] <= 0), otherwise the clamped target position.
     */
    fun pageTargetPosition(
        selected: Int,
        columns: Int,
        rowsPerPage: Int,
        itemCount: Int,
        forward: Boolean
    ): Int {
        if (itemCount <= 0 || columns <= 0 || rowsPerPage <= 0) {
            return selected
        }
        // Use Long so an absurd columns/rowsPerPage combination cannot overflow into a wrap-around.
        val pageSize = columns.toLong() * rowsPerPage.toLong()
        val raw = if (forward) selected.toLong() + pageSize else selected.toLong() - pageSize
        return raw.coerceIn(0L, (itemCount - 1).toLong()).toInt()
    }
}
