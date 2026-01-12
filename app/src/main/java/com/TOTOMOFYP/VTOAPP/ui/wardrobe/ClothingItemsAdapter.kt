package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.TOTOMOFYP.VTOAPP.databinding.ItemClothingBinding

class ClothingItemsAdapter(
    private var items: List<ClothingItem>,
    private val onItemClick: (ClothingItem) -> Unit
) : RecyclerView.Adapter<ClothingItemsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClothingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ClothingItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemClothingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }

        fun bind(item: ClothingItem) {
            binding.apply {
                // Hide text views or set default values since name and brand are removed
                itemName.visibility = android.view.View.GONE
                itemBrand.visibility = android.view.View.GONE
                
                // Hide the color dot completely
                itemColor.visibility = android.view.View.GONE
                
                // Set favorite status - removed since isFavorite property no longer exists
                favoriteIcon.visibility = android.view.View.GONE

                // Load image - would normally use a library like Glide or Coil
                // For example: Glide.with(itemImage).load(item.imageUrl).into(itemImage)
                // For now, use a placeholder
                itemImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        private fun parseColor(colorName: String): Int {
            // In a real app, map color names to actual color values
            // This is a simplified version
            return when (colorName.lowercase()) {
                "red" -> 0xFFFF0000.toInt()
                "blue" -> 0xFF0000FF.toInt()
                "green" -> 0xFF00FF00.toInt()
                "black" -> 0xFF000000.toInt()
                "white" -> 0xFFFFFFFF.toInt()
                else -> 0xFF888888.toInt() // Default gray
            }
        }
    }
} 