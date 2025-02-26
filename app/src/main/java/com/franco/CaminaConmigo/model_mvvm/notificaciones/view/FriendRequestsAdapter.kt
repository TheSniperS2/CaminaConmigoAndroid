package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemFriendRequestBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.FriendRequest

class FriendRequestsAdapter(
    private val onAcceptClicked: (FriendRequest) -> Unit,
    private val onRejectClicked: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.FriendRequestViewHolder>() {

    private var friendRequests: List<FriendRequest> = emptyList()

    fun submitList(newList: List<FriendRequest>) {
        friendRequests = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        holder.bind(friendRequests[position])
    }

    override fun getItemCount() = friendRequests.size

    inner class FriendRequestViewHolder(private val binding: ItemFriendRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(friendRequest: FriendRequest) {
            binding.tvRequestTitle.text = friendRequest.fromUserName
            binding.tvRequestMessage.text = friendRequest.fromUserEmail

            binding.btnAccept.setOnClickListener { onAcceptClicked(friendRequest) }
            binding.btnReject.setOnClickListener { onRejectClicked(friendRequest) }
        }
    }
}