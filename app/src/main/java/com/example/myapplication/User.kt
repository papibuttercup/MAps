package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val accountType: String = "",
    val isDisabled: Boolean = false,
    val createdAt: Long = 0,
    val lastLogin: Long = 0
)

class UserAdapter(
    private val users: List<User>,
    private val onActionClicked: (User, String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emailTextView: TextView = itemView.findViewById(R.id.userEmail)
        private val typeTextView: TextView = itemView.findViewById(R.id.userType)
        private val disableButton: MaterialButton = itemView.findViewById(R.id.disableButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(user: User) {
            emailTextView.text = user.email
            typeTextView.text = buildString {
                append("Type: ${user.accountType} • ")
                append(if (user.isDisabled) "Disabled" else "Active")
            }
            disableButton.apply {
                text = if (user.isDisabled) "Enable" else "Disable"
                setOnClickListener { onActionClicked(user, "disable") }
            }
            deleteButton.setOnClickListener { onActionClicked(user, "delete") }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
    )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size
} 