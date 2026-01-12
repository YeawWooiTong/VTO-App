package com.TOTOMOFYP.VTOAPP.ui.home

import androidx.annotation.DrawableRes

data class QuickAction(
    @DrawableRes val iconRes: Int,
    val title: String,
    val action: () -> Unit
) 