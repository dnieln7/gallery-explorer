package xyz.dnieln7.galleryex.core.domain.model

import java.io.File

data class Volume(
    val name: String,
    private val file: File,
) {
    val root: VolumeFile.Directory
        get() {
            return VolumeFile.Directory(file = file)
        }
}
