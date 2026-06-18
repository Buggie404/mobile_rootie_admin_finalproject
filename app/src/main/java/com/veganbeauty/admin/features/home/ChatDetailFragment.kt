package com.veganbeauty.admin.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.remote.ChatMessage
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.databinding.FragmentChatDetailBinding
import com.veganbeauty.admin.databinding.ItemChatMessageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ChatDetailFragment : RootieAdminFragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val firebaseService = FirebaseService()
    private val allMessages = mutableListOf<ChatMessage>()
    private val filteredMessages = mutableListOf<ChatMessage>()

    private lateinit var chatAdapter: ChatMessagesAdapter

    private var userId: String = ""
    private var username: String = ""
    private var avatar: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID, "")
            username = it.getString(ARG_USERNAME, "")
            avatar = it.getString(ARG_AVATAR, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Bind Header Info
        binding.txtHeaderTitle.text = username
        if (avatar.isNotEmpty()) {
            binding.imgHeaderAvatar.load(avatar) {
                crossfade(true)
                placeholder(R.drawable.imv_avatar)
                error(R.drawable.imv_avatar)
            }
        } else {
            binding.imgHeaderAvatar.setImageResource(R.drawable.imv_avatar)
        }

        // Back button
        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(HomeMessageFragment())
        }

        // Send button
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // IME Editor Action Send
        binding.edtInputMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        setupRecyclerView()
        loadConversation()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatMessagesAdapter(filteredMessages, avatar)
        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = true // Start showing from bottom of list
        binding.rvChatHistory.layoutManager = layoutManager
        binding.rvChatHistory.adapter = chatAdapter
    }

    private fun loadConversation() {
        lifecycleScope.launch {
            // Load local messages
            val localList = withContext(Dispatchers.IO) {
                loadLocalMessages()
            }
            allMessages.clear()
            allMessages.addAll(localList)

            // Mark incoming messages as read
            var updatedAny = false
            for (msg in allMessages) {
                if (msg.senderId == userId && msg.receiverId == "admin_rootie" && !msg.isRead) {
                    msg.isRead = true
                    updatedAny = true
                    // Sync unread status change to firebase
                    launch {
                        firebaseService.saveChatMessage(msg)
                    }
                }
            }

            if (updatedAny) {
                withContext(Dispatchers.IO) {
                    saveLocalMessages(allMessages)
                }
            }

            filterAndBindMessages()
        }
    }

    private fun filterAndBindMessages() {
        filteredMessages.clear()
        // Filter messages between admin and this specific user
        val filtered = allMessages.filter {
            (it.senderId == userId && it.receiverId == "admin_rootie") ||
                    (it.senderId == "admin_rootie" && it.receiverId == userId)
        }
        filteredMessages.addAll(filtered)
        chatAdapter.notifyDataSetChanged()
        if (filteredMessages.isNotEmpty()) {
            binding.rvChatHistory.scrollToPosition(filteredMessages.size - 1)
        }
    }

    private fun sendMessage() {
        val text = binding.edtInputMessage.text.toString().trim()
        if (text.isEmpty()) return

        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "admin_rootie",
            senderName = "Quản trị viên Rootie",
            senderAvatar = "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg",
            receiverId = userId,
            receiverName = username,
            receiverAvatar = avatar,
            content = text,
            timestamp = System.currentTimeMillis(),
            isRead = true // Sender automatically reads their own message
        )

        binding.edtInputMessage.text.clear()

        // Append to lists
        allMessages.add(newMessage)
        filteredMessages.add(newMessage)
        chatAdapter.notifyItemInserted(filteredMessages.size - 1)
        binding.rvChatHistory.scrollToPosition(filteredMessages.size - 1)

        lifecycleScope.launch {
            // Save locally
            withContext(Dispatchers.IO) {
                saveLocalMessages(allMessages)
            }
            // Save to Firebase
            val success = firebaseService.saveChatMessage(newMessage)
            if (!success) {
                Toast.makeText(context, "Lỗi kết nối Firebase, đã lưu ngoại tuyến", Toast.LENGTH_SHORT).show()
            }
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
                            id = obj.optString("id"),
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // RecyclerView Adapter for Chat Messages
    private class ChatMessagesAdapter(
        private val list: List<ChatMessage>,
        private val partnerAvatar: String
    ) : RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>() {

        private val VIEW_TYPE_INCOMING = 1
        private val VIEW_TYPE_OUTGOING = 2

        class ViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemViewType(position: Int): Int {
            val item = list[position]
            return if (item.senderId == "admin_rootie") VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val viewType = getItemViewType(position)

            if (viewType == VIEW_TYPE_OUTGOING) {
                holder.binding.layoutOutgoing.visibility = View.VISIBLE
                holder.binding.layoutIncoming.visibility = View.GONE
                holder.binding.txtOutgoingMessage.text = item.content
            } else {
                holder.binding.layoutOutgoing.visibility = View.GONE
                holder.binding.layoutIncoming.visibility = View.VISIBLE
                holder.binding.txtIncomingMessage.text = item.content

                if (partnerAvatar.isNotEmpty()) {
                    holder.binding.imgIncomingAvatar.load(partnerAvatar) {
                        crossfade(true)
                        placeholder(R.drawable.imv_avatar)
                        error(R.drawable.imv_avatar)
                    }
                } else {
                    holder.binding.imgIncomingAvatar.setImageResource(R.drawable.imv_avatar)
                }
            }
        }

        override fun getItemCount() = list.size
    }

    companion object {
        private const val ARG_USER_ID = "arg_user_id"
        private const val ARG_USERNAME = "arg_username"
        private const val ARG_AVATAR = "arg_avatar"

        fun newInstance(userId: String, username: String, avatar: String): ChatDetailFragment {
            val fragment = ChatDetailFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            args.putString(ARG_USERNAME, username)
            args.putString(ARG_AVATAR, avatar)
            fragment.arguments = args
            return fragment
        }
    }
}
