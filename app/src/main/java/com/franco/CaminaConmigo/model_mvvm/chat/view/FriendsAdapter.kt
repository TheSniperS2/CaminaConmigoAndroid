package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.chat.model.Friend

class FriendsAdapter(
    private var friends: List<Friend>,
    private val onFriendSelected: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val selectedFriends = mutableListOf<Friend>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.bind(friend)
        holder.itemView.setOnClickListener {
            if (selectedFriends.contains(friend)) {
                selectedFriends.remove(friend)
                holder.itemView.setBackgroundResource(android.R.color.transparent)
            } else {
                selectedFriends.add(friend)
                holder.itemView.setBackgroundResource(R.color.colorSelectedFriend)
            }
            onFriendSelected(friend)
        }
    }

    override fun getItemCount(): Int = friends.size

    fun updateFriends(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }

    fun getSelectedFriends(): List<Friend> = selectedFriends

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendImage: ImageView = itemView.findViewById(R.id.friendImage)

        fun bind(friend: Friend) {
            friendName.text = friend.name
            Glide.with(itemView.context)
                .load(friend.imageUrl)
                .placeholder(R.drawable.ic_imagen)
                .into(friendImage)
        }
    }
}