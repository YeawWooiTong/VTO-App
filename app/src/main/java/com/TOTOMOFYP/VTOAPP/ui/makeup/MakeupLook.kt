package com.TOTOMOFYP.VTOAPP.ui.makeup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class MakeupLook(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val dateCreated: Date = Date(),
    val isPublic: Boolean = false,
    val likesCount: Int = 0
) : Parcelable