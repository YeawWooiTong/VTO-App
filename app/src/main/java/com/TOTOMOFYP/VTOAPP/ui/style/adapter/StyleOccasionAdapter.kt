package com.TOTOMOFYP.VTOAPP.ui.style.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.TOTOMOFYP.VTOAPP.databinding.ItemStyleOccasionBinding

class StyleOccasionAdapter : RecyclerView.Adapter<StyleOccasionAdapter.OccasionViewHolder>() {

    private val items: MutableList<String> = mutableListOf()
    private var onItemClickListener: ((String) -> Unit)? = null

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccasionViewHolder {
        val binding = ItemStyleOccasionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OccasionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OccasionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OccasionViewHolder(
        private val binding: ItemStyleOccasionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(items[position])
                }
            }
        }

        fun bind(occasion: String) {
            binding.occasionName.text = occasion
            
            // TODO: Set appropriate image based on occasion
            // For now, we'll leave the default image
        }
    }
} 