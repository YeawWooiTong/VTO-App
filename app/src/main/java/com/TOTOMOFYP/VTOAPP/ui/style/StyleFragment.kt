package com.TOTOMOFYP.VTOAPP.ui.style

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.databinding.FragmentStyleBinding
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.style.adapter.StyleOccasionAdapter

class StyleFragment : BaseFragment() {

    private var _binding: FragmentStyleBinding? = null
    private val binding get() = _binding!!

    private val occasionAdapter = StyleOccasionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecommendations()
        setupOccasions()
        setupColorPalette()
        setupStyleGenerator()
    }

    private fun setupRecommendations() {
        // Set up recommendation card with sample data
        binding.styleRecommendationTitle.text = "Casual Elegance"
        binding.styleRecommendationSubtitle.text = "Perfect for your body type and preferences"
        
        // Set up try on button
        binding.styleTryOnButton.setOnClickListener {
            // TODO: Navigate to try-on screen with this style
        }
    }

    private fun setupOccasions() {
        // Set up horizontal scrolling occasions
        binding.occasionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = occasionAdapter
        }

        // Add sample data
        val occasions = listOf(
            "Casual", "Work", "Party", "Formal", "Sport", "Date"
        )
        
        // Submit the list to the adapter
        occasionAdapter.submitList(occasions)
    }

    private fun setupColorPalette() {
        // Sample color palette - these would come from ML analysis
        val colors = listOf(
            "#3B5999", // Primary blue
            "#FF5A5F", // Accent pink
            "#5D4037", // Brown
            "#4CAF50", // Green
            "#FFC107", // Amber
            "#9E9E9E"  // Gray
        )

        // Dynamically create color swatches
        val container = binding.colorPaletteContainer
        val context = requireContext()
        
        for (colorHex in colors) {
            val colorView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.color_swatch_size),
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
                }
                setBackgroundColor(Color.parseColor(colorHex))
            }
            container.addView(colorView)
        }

        // Set up customize button
        binding.colorCustomizeButton.setOnClickListener {
            // TODO: Open color customization screen
        }
    }

    private fun setupStyleGenerator() {
        // Set up the style generator button
        binding.generateStyleButton.setOnClickListener {
            val prompt = binding.stylePromptInput.text.toString()
            if (prompt.isNotEmpty()) {
                generateStyleFromPrompt(prompt)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please describe your style",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun generateStyleFromPrompt(prompt: String) {
        // TODO: Use ML model to generate style based on prompt
        Toast.makeText(
            requireContext(),
            "Generating style based on: $prompt",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 