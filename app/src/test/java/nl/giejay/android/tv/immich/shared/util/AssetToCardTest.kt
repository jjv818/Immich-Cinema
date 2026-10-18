package nl.giejay.android.tv.immich.shared.util

import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.api.model.AssetExifInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Card titles must never leak a raw filename (§5.3): prefer the exif description, then the
 * capture date, then a neutral Photo/Video label.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssetToCardTest {

    private fun asset(
        fileName: String? = "DSC_0001.jpg",
        description: String? = null,
        createdAt: Date? = null,
        type: String = "IMAGE"
    ) = Asset(
        id = "id1",
        type = type,
        deviceAssetId = null,
        exifInfo = AssetExifInfo(
            description = description,
            orientation = null,
            exifImageWidth = null,
            exifImageHeight = null,
            city = null,
            country = null,
            dateTimeOriginal = null,
            make = null,
            model = null
        ),
        fileCreatedAt = createdAt,
        fileModifiedAt = null,
        albumName = null,
        people = null,
        tags = null,
        originalPath = null,
        originalFileName = fileName,
        isFavorite = false
    )

    @Test
    fun `title never uses the raw filename`() {
        assertFalse(asset(fileName = "DSC_0001.jpg").toCard().title.contains("DSC_0001"))
    }

    @Test
    fun `prefers the exif description`() {
        assertEquals("A lovely photo", asset(description = "A lovely photo").toCard().title)
    }

    @Test
    fun `falls back to the capture date`() {
        val title = asset(createdAt = Date(1730000000000L)).toCard().title
        assertFalse(title.contains("DSC_0001"))
        assertFalse(title.isBlank())
    }

    @Test
    fun `falls back to Photo or Video label`() {
        assertEquals("Photo", asset(type = "IMAGE").toCard().title)
        assertEquals("Video", asset(type = "VIDEO").toCard().title)
    }
}
