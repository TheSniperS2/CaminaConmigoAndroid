package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemFriendBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Friend

class FriendsAdapter(private var friends: List<Friend>, private val onFriendSelected: (Friend) -> Unit) :
    RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val selectedFriends = mutableSetOf<Friend>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.bind(friend)
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(private val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(friend: Friend) {
            binding.friendName.text = friend.name
            binding.root.setOnClickListener {
                if (selectedFriends.contains(friend)) {
                    selectedFriends.remove(friend)
                    binding.root.setBackgroundResource(android.R.color.transparent)
                } else {
                    selectedFriends.add(friend)
                    binding.root.setBackgroundResource(android.R.color.holo_blue_light)
                }
                onFriendSelected(friend)
            }
        }
    }

    fun getSelectedFriends(): List<Friend> = selectedFriends.toList()

    fun updateFriends(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }
}