package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import xyz.dnieln7.galleryex.core.domain.model.VolumeFile

internal data class ImageViewerRequest(
    val images: List<VolumeFile.Image>,
    val selectedIndex: Int,
)

internal data class VideoViewerRequest(
    val videos: List<VolumeFile.Video>,
    val selectedIndex: Int,
)

internal fun createImageViewerRequest(
    files: List<VolumeFile>,
    selectedImage: VolumeFile.Image,
): ImageViewerRequest {
    val images = files.filterIsInstance<VolumeFile.Image>()

    return ImageViewerRequest(
        images = images,
        selectedIndex = images.indexOf(selectedImage),
    )
}

internal fun createVideoViewerRequest(
    files: List<VolumeFile>,
    selectedVideo: VolumeFile.Video,
): VideoViewerRequest {
    val videos = files.filterIsInstance<VolumeFile.Video>()

    return VideoViewerRequest(
        videos = videos,
        selectedIndex = videos.indexOf(selectedVideo),
    )
}
