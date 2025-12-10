package xyz.dnieln7.galleryex.core.domain.model

import android.webkit.MimeTypeMap
import java.io.File

sealed interface VolumeFile {
    val name: String

    data class Directory(val file: File) : VolumeFile {
        override val name: String
            get() = file.name

        val children: List<VolumeFile> by lazy {
            file.listFiles()?.map { fromFile(it) } ?: emptyList()
        }
    }

    data class Image(val file: File) : VolumeFile {
        override val name: String
            get() = file.nameWithoutExtension
    }

    data class Other(val file: File) : VolumeFile {
        override val name: String
            get() = file.nameWithoutExtension
    }

    companion object {
        fun fromFile(file: File): VolumeFile {
            if (file.isDirectory) {
                return Directory(file = file)
            }

            val extension = file.extension.lowercase()
            val isImage = MimeTypeMap
                .getSingleton()
                .getMimeTypeFromExtension(extension)
                ?.startsWith("image/")
                ?: false

            return if (isImage) {
                Image(file = file)
            } else {
                Other(file = file)
            }
        }
    }
}