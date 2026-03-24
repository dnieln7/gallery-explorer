package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import xyz.dnieln7.galleryex.core.domain.model.VolumeFile

internal data class ImageViewerRequest(
    val imagePaths: List<String>,
    val selectedIndex: Int,
)

internal data class VideoViewerRequest(
    val videoPaths: List<String>,
    val selectedIndex: Int,
)

internal fun createImageViewerRequest(
    files: List<VolumeFile>,
    selectedImage: VolumeFile.Image,
): ImageViewerRequest {
    val images = files.filterIsInstance<VolumeFile.Image>()

    return ImageViewerRequest(
        imagePaths = images.map { it.file.absolutePath },
        selectedIndex = images.indexOf(selectedImage),
    )
}

internal fun createVideoViewerRequest(
    files: List<VolumeFile>,
    selectedVideo: VolumeFile.Video,
): VideoViewerRequest {
    val videos = files.filterIsInstance<VolumeFile.Video>()

    return VideoViewerRequest(
        videoPaths = videos.map { it.file.absolutePath },
        selectedIndex = videos.indexOf(selectedVideo),
    )
}
