package xyz.dnieln7.galleryex.core.domain.model

import java.io.File

/**
 * A mounted storage volume that can appear on the Home screen or back a browsed directory.
 *
 * @property name Human-readable label reported by the platform for the volume.
 * @property file Root directory for the mounted volume.
 * @property isRemovable True when the volume can be detached from the device, such as SD cards or USB storage.
 */
data class Volume(
    val name: String,
    private val file: File,
    val isRemovable: Boolean,
) {
    /**
     * Root directory exposed as a [VolumeFile.Directory] so feature code can treat the volume like a browsable folder.
     */
    val root: VolumeFile.Directory
        get() {
            return VolumeFile.Directory(file = file)
        }
}
