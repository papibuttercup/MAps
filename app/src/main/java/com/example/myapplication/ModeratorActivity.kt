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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import android.util.Log
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.databinding.FragmentSellerVerificationBinding
import com.example.myapplication.databinding.ItemSellerVerificationBinding
import com.example.myapplication.databinding.FragmentUserManagementBinding
import com.example.myapplication.User
import com.example.myapplication.UserAdapter
import com.google.android.material.tabs.TabLayoutMediator

class ModeratorActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moderator)

        verifyModeratorAccess()
        setupBackPressHandler()
        initializeUI()
    }

    private fun verifyModeratorAccess() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("ModeratorActivity", "Not authenticated")
            showToast("Not authenticated")
            finish()
            return
        }

        Log.d("ModeratorActivity", "Checking moderator access for user: ${currentUser.uid}")
        
        db.collection("users")
            .whereEqualTo("email", currentUser.email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ModeratorActivity", "User document not found")
                    showToast("Error: User profile not found")
                    finish()
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0]
                val accountType = userDoc.getString("accountType")
                Log.d("ModeratorActivity", "Account type: $accountType")

                if (accountType != "moderator") {
                    Log.e("ModeratorActivity", "Access denied: User is not a moderator")
                    showToast("Access denied: Moderator privileges required")
                    finish()
                } else {
                    Log.d("ModeratorActivity", "Moderator access granted")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ModeratorActivity", "Error verifying moderator access", e)
                showToast("Error verifying moderator access: ${e.message}")
                finish()
            }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun initializeUI() {
        try {
            // Initialize ViewPager2
            val viewPager = findViewById<ViewPager2>(R.id.viewPager)
            viewPager.adapter = ModeratorPagerAdapter(this)

            // Initialize TabLayout
            val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Users"
                    1 -> "Verify Accounts"
                    else -> null
                }
            }.attach()

            // Setup logout button
            findViewById<FloatingActionButton>(R.id.fabLogout).setOnClickListener {
                showLogoutConfirmationDialog()
            }
        } catch (e: Exception) {
            Log.e("ModeratorActivity", "Error initializing UI", e)
            showToast("Error initializing UI: ${e.message}")
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
}

class ModeratorPagerAdapter(fragmentActivity: FragmentActivity) : 
    FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserManagementFragment()
            1 -> SellerVerificationFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}

class UserManagementFragment : Fragment() {
    private lateinit var binding: FragmentUserManagementBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: UserAdapter
    private val usersList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        setupRecyclerView()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(usersList) { user, action ->
            when (action) {
                "delete" -> deleteUser(user)
                "disable" -> toggleUserStatus(user)
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadUsers() {
        try {
            db.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("UserManagement", "Listen failed.", e)
                        Toast.makeText(context, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w("UserManagement", "Snapshot is null")
                        return@addSnapshotListener
                    }

                    usersList.clear()
                    snapshot.documents.forEach { doc ->
                        try {
                            val createdAt = when (val createdAtValue = doc.get("createdAt")) {
                                is Long -> createdAtValue
                                is Number -> createdAtValue.toLong()
                                is String -> createdAtValue.toLongOrNull() ?: 0L
                                else -> 0L
                            }
                            
                            val lastLogin = when (val lastLoginValue = doc.get("lastLogin")) {
                                is Long -> lastLoginValue
                                is Number -> lastLoginValue.toLong()
                                is String -> lastLoginValue.toLongOrNull() ?: 0L
                                else -> 0L
                            }

                            val user = User(
                                id = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                phone = doc.getString("phone") ?: "",
                                accountType = doc.getString("accountType") ?: "",
                                isDisabled = doc.getBoolean("isDisabled") ?: false,
                                createdAt = createdAt,
                                lastLogin = lastLogin
                            )
                            usersList.add(user)
                        } catch (e: Exception) {
                            Log.e("UserManagement", "Error converting document ${doc.id}", e)
                        }
                    }

                    if (usersList.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                    }
                }
        } catch (e: Exception) {
            Log.e("UserManagement", "Error in loadUsers", e)
            Toast.makeText(context, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteUser(user: User) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Delete User")
            setMessage("Delete ${user.email}?")
            setPositiveButton("Delete") { _, _ ->
                db.collection("users").document(user.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "User ${if (newStatus) "disabled" else "enabled"}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

class SellerVerificationFragment : Fragment() {
    private lateinit var binding: FragmentSellerVerificationBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: SellerVerificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSellerVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        setupRecyclerView()
        loadPendingSellers()
    }

    private fun setupRecyclerView() {
        adapter = SellerVerificationAdapter { seller, action ->
            when (action) {
                "approve" -> approveSeller(seller)
                "reject" -> rejectSeller(seller)
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadPendingSellers() {
        try {
            db.collection("sellers")
                .whereEqualTo("verificationStatus", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("SellerVerification", "Listen failed.", e)
                        Toast.makeText(context, "Error loading sellers: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w("SellerVerification", "Snapshot is null")
                        return@addSnapshotListener
                    }

                    val sellers = mutableListOf<Seller>()
                    snapshot.documents.forEach { doc ->
                        try {
                            val createdAt = when (val createdAtValue = doc.get("createdAt")) {
                                is Long -> createdAtValue
                                is Number -> createdAtValue.toLong()
                                is String -> createdAtValue.toLongOrNull() ?: 0L
                                else -> 0L
                            }

                            val seller = Seller(
                                id = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                phone = doc.getString("phone") ?: "",
                                shopName = doc.getString("shopName") ?: "",
                                shopLocation = doc.getString("shopLocation") ?: "",
                                verificationStatus = doc.getString("verificationStatus") ?: "pending",
                                createdAt = createdAt
                            )
                            sellers.add(seller)
                        } catch (e: Exception) {
                            Log.e("SellerVerification", "Error converting document ${doc.id}", e)
                        }
                    }

                    if (sellers.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.submitList(sellers)
                    }
                }
        } catch (e: Exception) {
            Log.e("SellerVerification", "Error in loadPendingSellers", e)
            Toast.makeText(context, "Error loading sellers: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun approveSeller(seller: Seller) {
        try {
            db.collection("sellers").document(seller.id)
                .update("verificationStatus", "approved")
                .addOnSuccessListener {
                    Toast.makeText(context, "Seller approved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("SellerVerification", "Error approving seller", e)
                    Toast.makeText(context, "Error approving seller: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("SellerVerification", "Error in approveSeller", e)
            Toast.makeText(context, "Error approving seller: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rejectSeller(seller: Seller) {
        try {
            db.collection("sellers").document(seller.id)
                .update("verificationStatus", "rejected")
                .addOnSuccessListener {
                    Toast.makeText(context, "Seller rejected", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("SellerVerification", "Error rejecting seller", e)
                    Toast.makeText(context, "Error rejecting seller: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("SellerVerification", "Error in rejectSeller", e)
            Toast.makeText(context, "Error rejecting seller: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

data class Seller(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val shopName: String = "",
    val shopLocation: String = "",
    val verificationStatus: String = "pending",
    val createdAt: Long = 0
)

class SellerVerificationAdapter(
    private val onActionClick: (Seller, String) -> Unit
) : ListAdapter<Seller, SellerVerificationAdapter.ViewHolder>(SellerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSellerVerificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSellerVerificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(seller: Seller) {
            binding.apply {
                sellerName.text = "${seller.firstName} ${seller.lastName}"
                sellerEmail.text = seller.email
                sellerPhone.text = seller.phone
                shopName.text = seller.shopName
                shopLocation.text = seller.shopLocation
                
                approveButton.setOnClickListener { onActionClick(seller, "approve") }
                rejectButton.setOnClickListener { onActionClick(seller, "reject") }
            }
        }
    }
}

class SellerDiffCallback : DiffUtil.ItemCallback<Seller>() {
    override fun areItemsTheSame(oldItem: Seller, newItem: Seller): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Seller, newItem: Seller): Boolean {
        return oldItem == newItem
    }
}