package com.example.cookpal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private var userList: MutableList<User>,  // ✅ Mutable list for updates
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.usernameText.text = user.username

        // 🔹 Profile Image (Default for now)
        holder.profileImage.setImageResource(R.drawable.ic_profile)

        // 🔥 Clicking a user navigates to their profile
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount() = userList.size

    // ✅ Function to update list dynamically
    fun updateList(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}
