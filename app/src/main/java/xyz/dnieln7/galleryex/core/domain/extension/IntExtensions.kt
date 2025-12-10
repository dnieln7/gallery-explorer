package xyz.dnieln7.galleryex.core.domain.extension

fun Int.indexInc(size: Int): Int {
    val new = this + 1

    return if (new >= size) this else new
}

fun Int.indexDec(): Int {
    val new = this - 1

    return if (new < 0) this else new
}
