package com.TOTOMOFYP.VTOAPP.ui.util

import android.content.Context
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.TOTOMOFYP.VTOAPP.R

/**
 * Extensions for optimized image loading
 */

/**
 * Creates an optimized image request for Coil
 */
fun createOptimizedImageRequest(context: Context, url: String, placeholderResId: Int = R.drawable.ic_outfit): ImageRequest {
    return ImageRequest.Builder(context)
        .data(url)
        .crossfade(true)
        .placeholder(placeholderResId)
        .error(placeholderResId)
        .fallback(placeholderResId)
        .scale(Scale.FILL)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}

/**
 * Composable function to get an optimized image request
 */
@Composable
fun optimizedImageRequest(url: String, placeholderResId: Int = R.drawable.ic_outfit): ImageRequest {
    val context = LocalContext.current
    return createOptimizedImageRequest(context, url, placeholderResId)
} 