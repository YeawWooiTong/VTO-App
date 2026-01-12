package com.TOTOMOFYP.VTOAPP.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.TOTOMOFYP.VTOAPP.databinding.ItemQuickActionBinding

class QuickActionsAdapter(
    private val actions: List<QuickAction>
) : RecyclerView.Adapter<QuickActionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickActionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(actions[position])
    }

    override fun getItemCount() = actions.size

    class ViewHolder(
        private val binding: ItemQuickActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(action: QuickAction) {
            binding.actionIcon.setImageResource(action.iconRes)
            binding.actionTitle.text = action.title
            binding.root.setOnClickListener { action.action() }
        }
    }
} 