package com.veganbeauty.admin.features.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.remote.ChatMessage
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.databinding.FragmentHomeMessageBinding
import com.veganbeauty.admin.databinding.ItemChatThreadBinding
import com.veganbeauty.admin.databinding.ItemStoryUserBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HomeMessageFragment : RootieAdminFragment() {

    private var _binding: FragmentHomeMessageBinding? = null
    private val binding get() = _binding!!

    private val firebaseService = FirebaseService()
    private val allMessages = mutableListOf<ChatMessage>()
    private val chatThreads = mutableListOf<ChatThread>()
    private val filteredThreads = mutableListOf<ChatThread>()

    private lateinit var storiesAdapter: StoriesAdapter
    private lateinit var chatsAdapter: ChatsAdapter

    private var currentFilter = "all" // all, unread, pending
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Back Button click
        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.let { mainAct ->
                BottomNavHelper.navigate(mainAct, R.id.nav_home)
            }
        }

        // Notification Button click
        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Mở thông báo", Toast.LENGTH_SHORT).show()
        }

        // Setup Dropdown Title Menu
        binding.layoutTitleDropdown.setOnClickListener {
            showFilterMenu()
        }

        // Setup Search
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFiltersAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupRecyclerViews()
        loadAndSyncMessages()
    }

    private fun setupRecyclerViews() {
        // 1. Stories Horizontal List
        storiesAdapter = StoriesAdapter(getMockStories()) { story ->
            // Open chat with selected user
            openChatDetail(story.userId, story.name, story.avatar)
        }
        binding.rvStories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvStories.adapter = storiesAdapter

        // 2. Chat Threads Vertical List
        chatsAdapter = ChatsAdapter(filteredThreads) { thread ->
            openChatDetail(thread.userId, thread.username, thread.avatar)
        }
        binding.rvChats.layoutManager = LinearLayoutManager(context)
        binding.rvChats.adapter = chatsAdapter
    }

    private fun loadAndSyncMessages() {
        lifecycleScope.launch {
            // 1. Load local messages
            val localList = withContext(Dispatchers.IO) {
                loadLocalMessages()
            }

            allMessages.clear()
            allMessages.addAll(localList)
            updateChatThreads()

            // 2. Load Firestore remote messages and merge
            val remoteList = firebaseService.fetchChatMessages()

            // If some local messages are missing in remote, upload them (e.g. initial launch with empty Firestore)
            val missingInRemote = allMessages.filter { local -> remoteList.none { it.id == local.id } }
            if (missingInRemote.isNotEmpty()) {
                for (msg in missingInRemote) {
                    firebaseService.saveChatMessage(msg)
                }
            }

            // Merge local and remote messages, then sync back to local storage
            val merged = mergeMessages(allMessages, remoteList)
            allMessages.clear()
            allMessages.addAll(merged)

            withContext(Dispatchers.IO) {
                saveLocalMessages(allMessages)
            }

            updateChatThreads()
        }
    }

    private fun loadLocalMessages(): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        try {
            val localFile = File(requireContext().filesDir, "chat_message.json")
            val jsonString = if (localFile.exists()) {
                localFile.readText()
            } else {
                requireContext().assets.open("chat_message.json").bufferedReader().use { it.readText() }
            }

            if (jsonString.isNotBlank()) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ChatMessage(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            senderId = obj.optString("senderId"),
                            senderName = obj.optString("senderName"),
                            senderAvatar = obj.optString("senderAvatar"),
                            receiverId = obj.optString("receiverId"),
                            receiverName = obj.optString("receiverName"),
                            receiverAvatar = obj.optString("receiverAvatar"),
                            content = obj.optString("content"),
                            timestamp = obj.optLong("timestamp"),
                            isRead = obj.optBoolean("isRead", false)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveLocalMessages(messages: List<ChatMessage>) {
        try {
            val localFile = File(requireContext().filesDir, "chat_message.json")
            val jsonArray = JSONArray()
            for (msg in messages) {
                val obj = JSONObject()
                obj.put("id", msg.id)
                obj.put("senderId", msg.senderId)
                obj.put("senderName", msg.senderName)
                obj.put("senderAvatar", msg.senderAvatar)
                obj.put("receiverId", msg.receiverId)
                obj.put("receiverName", msg.receiverName)
                obj.put("receiverAvatar", msg.receiverAvatar)
                obj.put("content", msg.content)
                obj.put("timestamp", msg.timestamp)
                obj.put("isRead", msg.isRead)
                jsonArray.put(obj)
            }
            localFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mergeMessages(local: List<ChatMessage>, remote: List<ChatMessage>): List<ChatMessage> {
        val map = (local + remote).associateBy { it.id }.toMutableMap()
        // Sort chronologically
        return map.values.sortedBy { it.timestamp }
    }

    private fun updateChatThreads() {
        chatThreads.clear()

        // Group messages by chat partner (the user who is not admin_rootie)
        val grouped = allMessages.groupBy {
            if (it.senderId == "admin_rootie") it.receiverId else it.senderId
        }

        for ((partnerId, messages) in grouped) {
            if (partnerId == "admin_rootie" || partnerId.isBlank()) continue

            // Find last message
            val lastMsg = messages.maxByOrNull { it.timestamp } ?: continue
            val partnerName = if (lastMsg.senderId == "admin_rootie") lastMsg.receiverName else lastMsg.senderName
            val partnerAvatar = if (lastMsg.senderId == "admin_rootie") lastMsg.receiverAvatar else lastMsg.senderAvatar

            val unreadCount = messages.count { it.receiverId == "admin_rootie" && !it.isRead }

            // online status (mocking green dot matching screenshot)
            val isActive = partnerId == "48228004" || partnerId == "quynh_nhu_user"

            chatThreads.add(
                ChatThread(
                    userId = partnerId,
                    username = partnerName,
                    avatar = partnerAvatar,
                    lastMessage = lastMsg.content,
                    lastMessageTime = formatRelativeTime(lastMsg.timestamp),
                    unreadCount = unreadCount,
                    isActive = isActive,
                    timestamp = lastMsg.timestamp
                )
            )
        }

        // Sort threads by latest message timestamp descending
        chatThreads.sortByDescending { it.timestamp }

        applyFiltersAndSearch()
    }

    private fun applyFiltersAndSearch() {
        filteredThreads.clear()

        var result = chatThreads.toList()

        // 1. Category Filter from dropdown
        result = when (currentFilter) {
            "unread" -> result.filter { it.unreadCount > 0 }
            "pending" -> emptyList() // Mock: no pending messages for now
            else -> result
        }

        // 2. Search Query
        if (searchQuery.isNotEmpty()) {
            result = result.filter {
                it.username.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }

        filteredThreads.addAll(result)
        chatsAdapter.notifyDataSetChanged()

        // 3. Show/Hide Empty State
        if (filteredThreads.isEmpty()) {
            binding.rvChats.visibility = View.GONE
            binding.lblMessages.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvChats.visibility = View.VISIBLE
            binding.lblMessages.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun showFilterMenu() {
        val popup = PopupMenu(requireContext(), binding.layoutTitleDropdown, Gravity.CENTER)
        popup.menu.add(0, 1, 0, "Tất cả")
        popup.menu.add(0, 2, 0, "Chưa đọc")
        popup.menu.add(0, 3, 0, "Tin nhắn đang chờ")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    currentFilter = "all"
                    binding.txtTitle.text = "Message"
                }
                2 -> {
                    currentFilter = "unread"
                    binding.txtTitle.text = "Chưa đọc"
                }
                3 -> {
                    currentFilter = "pending"
                    binding.txtTitle.text = "Tin nhắn đang chờ"
                }
            }
            applyFiltersAndSearch()
            true
        }
        popup.show()
    }

    private fun openChatDetail(userId: String, username: String, avatar: String) {
        val chatDetail = ChatDetailFragment.newInstance(userId, username, avatar)
        (activity as? MainActivity)?.loadFragment(chatDetail)
    }

    private fun getMockStories(): List<StoryUser> {
        return listOf(
            StoryUser("admin_rootie", "Tin của bạn", "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg", isMe = true),
            StoryUser("48228004", "nguyen_bao", "https://i.pinimg.com/736x/ab/32/b1/ab32b13edefed48f94d93ee4b6f12f6b.jpg", isActive = true),
            StoryUser("68751659", "khanh_xun", "https://i1-c.pinimg.com/736x/4d/fe/b7/4dfeb7f781432e75e270d3bf70f494e4.jpg", isActive = true),
            StoryUser("87962440", "bin_khanh", "https://i1-c.pinimg.com/736x/9e/12/94/9e1294132dbb8f12c70f31058b98bdb1.jpg", isActive = true),
            StoryUser("85097162", "mei_anh", "https://i.pinimg.com/736x/87/1c/91/871c91ffb39c0fc6a44c77f0a905a396.jpg", isActive = true)
        )
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút"
            hours < 24 -> "$hours giờ"
            days < 7 -> "$days ngày"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Models
    data class ChatThread(
        val userId: String,
        val username: String,
        val avatar: String,
        val lastMessage: String,
        val lastMessageTime: String,
        val unreadCount: Int,
        val isActive: Boolean,
        val timestamp: Long
    )

    data class StoryUser(
        val userId: String,
        val name: String,
        val avatar: String,
        val isActive: Boolean = false,
        val isMe: Boolean = false
    )

    // RecyclerView Adapter for Stories (Horizontal)
    private class StoriesAdapter(
        private val list: List<StoryUser>,
        private val onClick: (StoryUser) -> Unit
    ) : RecyclerView.Adapter<StoriesAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemStoryUserBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStoryUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.txtUsername.text = item.name

            if (item.avatar.isNotEmpty()) {
                holder.binding.imgAvatar.load(item.avatar) {
                    crossfade(true)
                    placeholder(R.drawable.imv_avatar)
                    error(R.drawable.imv_avatar)
                }
            } else {
                holder.binding.imgAvatar.setImageResource(R.drawable.imv_avatar)
            }

            if (item.isMe) {
                holder.binding.layoutPlus.visibility = View.VISIBLE
                holder.binding.viewActiveDot.visibility = View.GONE
            } else {
                holder.binding.layoutPlus.visibility = View.GONE
                holder.binding.viewActiveDot.visibility = if (item.isActive) View.VISIBLE else View.GONE
            }

            holder.itemView.setOnClickListener {
                if (!item.isMe) {
                    onClick(item)
                }
            }
        }

        override fun getItemCount() = list.size
    }

    // RecyclerView Adapter for Chat Threads (Vertical)
    private class ChatsAdapter(
        private val list: List<ChatThread>,
        private val onClick: (ChatThread) -> Unit
    ) : RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemChatThreadBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemChatThreadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.txtName.text = item.username

            // Bold styling if unread messages exist
            val isUnread = item.unreadCount > 0
            if (isUnread) {
                holder.binding.txtName.textStyleBold()
                holder.binding.txtMessageSnippet.textStyleBold()
                holder.binding.txtMessageSnippet.text = "${item.unreadCount}+ tin nhắn mới"
                holder.binding.viewUnreadDot.visibility = View.VISIBLE
            } else {
                holder.binding.txtName.textStyleNormal()
                holder.binding.txtMessageSnippet.textStyleNormal()
                holder.binding.txtMessageSnippet.text = item.lastMessage
                holder.binding.viewUnreadDot.visibility = View.GONE
            }

            holder.binding.txtTime.text = item.lastMessageTime
            holder.binding.viewActiveDot.visibility = if (item.isActive) View.VISIBLE else View.GONE

            if (item.avatar.isNotEmpty()) {
                holder.binding.imgAvatar.load(item.avatar) {
                    crossfade(true)
                    placeholder(R.drawable.imv_avatar)
                    error(R.drawable.imv_avatar)
                }
            } else {
                holder.binding.imgAvatar.setImageResource(R.drawable.imv_avatar)
            }

            holder.itemView.setOnClickListener {
                onClick(item)
            }
        }

        override fun getItemCount() = list.size

        private fun android.widget.TextView.textStyleBold() {
            this.setTypeface(this.typeface, android.graphics.Typeface.BOLD)
        }

        private fun android.widget.TextView.textStyleNormal() {
            this.setTypeface(android.graphics.Typeface.create(this.typeface, android.graphics.Typeface.NORMAL))
        }
    }
}
