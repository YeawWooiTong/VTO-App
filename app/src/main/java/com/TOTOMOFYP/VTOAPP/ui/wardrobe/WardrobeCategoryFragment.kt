package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.databinding.FragmentWardrobeCategoryBinding
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment

class WardrobeCategoryFragment : BaseFragment() {

    private var _binding: FragmentWardrobeCategoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var category: WardrobeCategory
    private lateinit var clothingAdapter: ClothingItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = WardrobeCategory.valueOf(it.getString(ARG_CATEGORY)!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWardrobeCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadItems()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            clothingAdapter = ClothingItemsAdapter(emptyList()) { item ->
                // Handle item click - show details
            }
            adapter = clothingAdapter
        }
    }

    private fun loadItems() {
        // In a real app, load items from database based on category
        val items = getDummyItems()
        
        if (items.isEmpty()) {
            showEmptyState()
        } else {
            showItems(items)
        }
    }

    private fun showEmptyState() {
        binding.recyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showItems(items: List<ClothingItem>) {
        binding.recyclerView.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        clothingAdapter.updateItems(items)
    }

    private fun getDummyItems(): List<ClothingItem> {
        // Return dummy items for now - in a real app, would come from database
        return listOf(
            // For demonstration, return empty list for some categories
            // and sample items for others
            if (category == WardrobeCategory.TOP) {
                ClothingItem(
                    id = "1",
                    category = WardrobeCategory.TOP,
                    imageUrl = "",
                    color = "Blue"
                )
            } else null
        ).filterNotNull()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: WardrobeCategory): WardrobeCategoryFragment {
            return WardrobeCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category.name)
                }
            }
        }
    }
} 