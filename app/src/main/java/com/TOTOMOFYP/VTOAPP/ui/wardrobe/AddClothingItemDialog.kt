package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.databinding.DialogAddClothingItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddClothingItemDialog : DialogFragment() {

    private var _binding: DialogAddClothingItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddClothingItemBinding.inflate(layoutInflater)
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Item")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                saveItem()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddClothingItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply inset handling
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            
            // Apply padding to the top level view
            v.updatePadding(
                top = statusBarInsets.top
            )
            
            insets
        }
        
        setupCategoryDropdown()
        setupColorDropdown()
        setupImageSelection()
    }

    private fun setupCategoryDropdown() {
        val categories = WardrobeCategory.values().map { 
            it.name.lowercase().capitalize() 
        }.toTypedArray()
        
        val adapter = ArrayAdapter(
            requireContext(), 
            android.R.layout.simple_dropdown_item_1line, 
            categories
        )
        binding.categoryDropdown.setAdapter(adapter)
    }

    private fun setupColorDropdown() {
        val colors = arrayOf("Red", "Blue", "Green", "Black", "White", "Yellow", "Purple", "Pink", "Orange", "Gray")
        val adapter = ArrayAdapter(
            requireContext(), 
            android.R.layout.simple_dropdown_item_1line, 
            colors
        )
        binding.colorDropdown.setAdapter(adapter)
    }

    private fun setupImageSelection() {
        binding.addImageButton.setOnClickListener {
            // Show image picker - in a real app would request permissions and launch image picker intent
            // For now, just show a toast
            // Toast.makeText(requireContext(), "Image selection not implemented in demo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveItem() {
        // Validate input
        val name = binding.nameEditText.text.toString().trim()
        val brand = binding.brandEditText.text.toString().trim()
        val category = binding.categoryDropdown.text.toString().trim()
        val color = binding.colorDropdown.text.toString().trim()
        
        if (name.isEmpty() || category.isEmpty()) {
            return
        }

        // In a real app, would save to database
        // For now, just close the dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "AddClothingItemDialog"
    }
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return if (isNotEmpty()) this[0].uppercase() + substring(1) else this
} 