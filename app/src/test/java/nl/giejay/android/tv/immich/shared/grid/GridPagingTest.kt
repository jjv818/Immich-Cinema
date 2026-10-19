package nl.giejay.android.tv.immich.shared.grid

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GRID-007: page-key navigation maths. A page is columns * rowsPerPage slots, the target is
 * clamped to the valid adapter range, and degenerate inputs (empty grid / no columns / no rows)
 * must leave the selection alone rather than throwing or jumping to a nonsense index.
 */
class GridPagingTest {

    @Test
    fun `full page forward moves by columns times rows`() {
        assertEquals(
            12,
            GridPaging.pageTargetPosition(
                selected = 0,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 40,
                forward = true
            )
        )
    }

    @Test
    fun `full page back moves by columns times rows`() {
        assertEquals(
            0,
            GridPaging.pageTargetPosition(
                selected = 12,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 40,
                forward = false
            )
        )
    }

    @Test
    fun `forward keeps the same column within the row`() {
        // 3 + (4 * 2) = 11; 11 % 4 == 3, so the column is preserved.
        assertEquals(
            11,
            GridPaging.pageTargetPosition(
                selected = 3,
                columns = 4,
                rowsPerPage = 2,
                itemCount = 40,
                forward = true
            )
        )
    }

    @Test
    fun `forward clamps to the last item near the end`() {
        assertEquals(
            39,
            GridPaging.pageTargetPosition(
                selected = 30,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 40,
                forward = true
            )
        )
    }

    @Test
    fun `back clamps to the first item near the start`() {
        assertEquals(
            0,
            GridPaging.pageTargetPosition(
                selected = 5,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 40,
                forward = false
            )
        )
    }

    @Test
    fun `forward from the last item stays on the last item`() {
        assertEquals(
            39,
            GridPaging.pageTargetPosition(
                selected = 39,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 40,
                forward = true
            )
        )
    }

    @Test
    fun `back from the first item stays on the first item`() {
        assertEquals(
            0,
            GridPaging.pageTargetPosition(
                selected = 0,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 40,
                forward = false
            )
        )
    }

    @Test
    fun `single visible row pages by one row`() {
        assertEquals(
            4,
            GridPaging.pageTargetPosition(
                selected = 0,
                columns = 4,
                rowsPerPage = 1,
                itemCount = 40,
                forward = true
            )
        )
    }

    @Test
    fun `empty grid returns the selection unchanged`() {
        assertEquals(
            3,
            GridPaging.pageTargetPosition(
                selected = 3,
                columns = 4,
                rowsPerPage = 3,
                itemCount = 0,
                forward = true
            )
        )
    }

    @Test
    fun `non positive columns returns the selection unchanged`() {
        assertEquals(
            3,
            GridPaging.pageTargetPosition(
                selected = 3,
                columns = 0,
                rowsPerPage = 3,
                itemCount = 40,
                forward = true
            )
        )
    }

    @Test
    fun `non positive rows per page returns the selection unchanged`() {
        assertEquals(
            3,
            GridPaging.pageTargetPosition(
                selected = 3,
                columns = 4,
                rowsPerPage = 0,
                itemCount = 40,
                forward = true
            )
        )
        assertEquals(
            3,
            GridPaging.pageTargetPosition(
                selected = 3,
                columns = 4,
                rowsPerPage = -2,
                itemCount = 40,
                forward = false
            )
        )
    }
}
