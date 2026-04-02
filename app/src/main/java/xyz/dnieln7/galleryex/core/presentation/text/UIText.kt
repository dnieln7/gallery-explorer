package xyz.dnieln7.galleryex.core.presentation.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UIText {
    data class FromString(val string: String) : UIText

    data class FromResource(@StringRes val id: Int) : UIText

    data class FromResourceWithArgs(@StringRes val id: Int, val args: Array<Any>) : UIText {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FromResourceWithArgs

            if (id != other.id) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + args.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "FromResourceWithArgs(id=$id, args=${args.contentToString()})"
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is FromString -> string
            is FromResource -> stringResource(id)
            is FromResourceWithArgs -> stringResource(id, *args)
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is FromString -> string
            is FromResource -> context.getString(id)
            is FromResourceWithArgs -> context.getString(id, *args)
        }
    }
}
