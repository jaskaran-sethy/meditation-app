package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentMenuBinding
import com.example.myapplication.databinding.CardLayoutBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardDataList = listOf(
            CardData(R.drawable.default_breath, "Card 1 Title"),
            CardData(R.drawable.default_breath, "Card 2 Title"),
            CardData(R.drawable.default_breath, "Card 3 Title"),
            CardData(R.drawable.default_breath, "Card 4 Title"),
            CardData(R.drawable.default_breath, "Card 5 Title")
            // Add more card data as needed
        )

        val layoutManager = GridLayoutManager(requireContext(), 2) // 2 columns
        binding.cardRecyclerView.layoutManager = layoutManager

        val adapter = MyAdapter(cardDataList) { cardData ->
            openMeditationTimerFragment(cardData)
        }
        binding.cardRecyclerView.adapter = adapter
    }

    inner class MyAdapter(
        private val cardDataList: List<CardData>,
        private val clickListener: (CardData) -> Unit
    ) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = CardLayoutBinding.inflate(inflater, parent, false)
            return MyViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val cardData = cardDataList[position]
            holder.bind(cardData, clickListener)
        }

        override fun getItemCount(): Int = cardDataList.size

        inner class MyViewHolder(private val binding: CardLayoutBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(cardData: CardData, clickListener: (CardData) -> Unit) {
                binding.cardImage.setImageResource(cardData.imageResId)
                binding.cardTitle.text = cardData.title
                binding.root.setOnClickListener { clickListener(cardData) }
            }
        }
    }

    data class CardData(
        val imageResId: Int,
        val title: String,
        val inhaleTimeInMilliseconds: Long = 3000,
        val inhaleHoldTimeInMilliseconds: Long = 0,
        val exhaleTimeInMilliseconds: Long = 5500,
        val exhaleHoldTimeInMilliseconds: Long = 0
    )

    private fun openMeditationTimerFragment(cardData: CardData) {
        val action = MenuFragmentDirections.actionMenuFragmentToMeditationTimerFragment(
            cardData.inhaleTimeInMilliseconds,
            cardData.inhaleHoldTimeInMilliseconds,
            cardData.exhaleTimeInMilliseconds,
            cardData.exhaleHoldTimeInMilliseconds
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}