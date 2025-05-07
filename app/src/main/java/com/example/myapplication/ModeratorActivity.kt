package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ModeratorActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { Firebase.firestore }
    private val usersList = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moderator)

        setupBackPressHandler()
        initializeUI()
        loadUsers()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun initializeUI() {
        adapter = UserAdapter(usersList) { user, action ->
            when (action) {
                "delete" -> deleteUser(user)
                "disable" -> toggleUserStatus(user)
            }
        }

        findViewById<MaterialButton>(R.id.btnManageReports).setOnClickListener {
            Toast.makeText(this, "Reports management coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnManageUsers).setOnClickListener {
            showUserManagementDialog()
        }

        findViewById<MaterialButton>(R.id.btnContentReview).setOnClickListener {
            Toast.makeText(this, "Content review coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<FloatingActionButton>(R.id.fabLogout).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun loadUsers() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                usersList.clear()
                usersList.addAll(result.map { it.toObject(User::class.java).copy(id = it.id) })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                showToast("Error loading users: ${e.message}")
            }
    }

    private fun showUserManagementDialog() {
        val dialog = MaterialAlertDialogBuilder(this).apply {
            setTitle("Manage Users")
            setView(createUserListView())
            setPositiveButton("Close", null)
        }.create()

        dialog.setOnShowListener {
            // Set maximum height to 80% of screen height
            val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxHeight
            )
        }

        dialog.show()
    }

    private fun createUserListView(): View {
        return LayoutInflater.from(this).inflate(R.layout.dialog_user_management, null).apply {
            findViewById<RecyclerView>(R.id.usersRecyclerView)?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@ModeratorActivity.adapter
                setHasFixedSize(true)
                visibility = View.VISIBLE
            }
        }
    }

    private fun deleteUser(user: User) {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Delete User")
            setMessage("Delete ${user.email}?")
            setPositiveButton("Delete") { _, _ ->
                db.collection("users").document(user.id).delete()
                    .addOnSuccessListener {
                        showToast("User deleted")
                        loadUsers()
                    }
                    .addOnFailureListener { e ->
                        showToast("Error: ${e.message}")
                    }
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun toggleUserStatus(user: User) {
        val newStatus = !user.isDisabled
        db.collection("users").document(user.id)
            .update("isDisabled", newStatus)
            .addOnSuccessListener {
                showToast("User ${if (newStatus) "disabled" else "enabled"}")
                loadUsers()
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Logout")
            setMessage("Are you sure you want to logout?")
            setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                showToast("Logged out successfully")
                finish()
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Exit")
            setMessage("Exit moderator panel?")
            setPositiveButton("Exit") { _, _ -> finish() }
            setNegativeButton("Stay", null)
            show()
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message.orEmpty(), Toast.LENGTH_SHORT).show()
    }

    data class User(
        val id: String = "",
        val email: String = "",
        val accountType: String = "",
        val isDisabled: Boolean = false
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
}