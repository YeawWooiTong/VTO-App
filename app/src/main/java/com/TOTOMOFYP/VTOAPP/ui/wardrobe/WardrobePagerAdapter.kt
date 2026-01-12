package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WardrobePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val categories = listOf(
        WardrobeCategory.ALL,
        WardrobeCategory.TOP,
        WardrobeCategory.BOTTOM,
        WardrobeCategory.FULL_BODY
    )

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return WardrobeCategoryFragment.newInstance(categories[position])
    }
} 