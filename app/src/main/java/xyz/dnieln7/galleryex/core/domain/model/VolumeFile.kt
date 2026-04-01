package xyz.dnieln7.galleryex.core.domain.model

import android.webkit.MimeTypeMap
import java.io.File
import java.net.URLConnection

sealed interface VolumeFile {
    val name: String
    val file: File

    data class Directory(override val file: File) : VolumeFile {
        override val name: String
            get() = file.name

        val children: List<VolumeFile> by lazy {
            file.listFiles()
                ?.map { fromFile(it) }
                ?: emptyList()
        }

        val images: List<Image> by lazy {
            file.listFiles()
                ?.map { fromFile(it) }
                ?.filterIsInstance<Image>()
                ?: emptyList()
        }
    }

    data class Image(override val file: File) : VolumeFile {
        override val name: String
            get() = file.nameWithoutExtension
    }

    data class Video(override val file: File) : VolumeFile {
        override val name: String
            get() = file.nameWithoutExtension
    }

    data class Other(override val file: File) : VolumeFile {
        override val name: String
            get() = file.nameWithoutExtension
    }

    companion object {
        fun fromFile(file: File): VolumeFile {
            if (file.isDirectory) {
                return Directory(file = file)
            }

            val mimeType = resolveMimeType(file)
            val isImage = mimeType?.startsWith("image/") == true
            val isVideo = mimeType?.startsWith("video/") == true

            return if (isImage) {
                Image(file = file)
            } else if (isVideo) {
                Video(file = file)
            } else {
                Other(file = file)
            }
        }

        private fun resolveMimeType(file: File): String? {
            val extension = file.extension.lowercase()

            return URLConnection.guessContentTypeFromName(file.name)
                ?: runCatching {
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(extension)
                }.getOrNull()
        }
    }
}
