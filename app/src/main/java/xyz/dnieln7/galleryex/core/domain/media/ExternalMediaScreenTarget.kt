package xyz.dnieln7.galleryex.core.domain.media

/**
 * Screen-level metadata used to monitor storage loss across resume, restoration, and process death.
 *
 * @property path Absolute path currently explored or viewed.
 * @property removableVolumeRootPath Root path of the removable volume that originally backed [path].
 * @property removableVolumeName User-visible volume label used in the redirect toast.
 */
data class ExternalMediaScreenTarget(
    val path: String,
    val removableVolumeRootPath: String? = null,
    val removableVolumeName: String? = null,
)
