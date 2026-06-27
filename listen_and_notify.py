#!/usr/bin/env python3
import firebase_admin
from firebase_admin import credentials, firestore, messaging
import time
import os
import sys

def main():
    cred_path = 'serviceAccountKey.json'
    if not os.path.exists(cred_path):
        print(f"Lỗi: Không tìm thấy file {cred_path} ở thư mục gốc!")
        sys.exit(1)

    try:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        db = firestore.client()
        print("Đã khởi tạo Firebase Admin SDK và đang lắng nghe tin nhắn...")
    except Exception as e:
        print(f"Lỗi khởi tạo Firebase: {e}")
        sys.exit(1)

    # Dictionary to keep track of last message ID to avoid duplicates
    last_processed_ids = {}

    def on_snapshot(col_snapshot, changes, read_time):
        # Read current admin token
        try:
            admin_doc = db.collection('admin_metadata').document('fcm').get()
            if not admin_doc.exists:
                print("Chưa có Admin token trên Firestore. Hãy mở Admin app để tự động tải token lên!")
                return
            admin_token = admin_doc.to_dict().get('token')
            if not admin_token:
                print("Admin token trên Firestore bị trống!")
                return
        except Exception as e:
            print(f"Không thể đọc Admin token từ Firestore: {e}")
            return

        for change in changes:
            doc = change.document
            doc_data = doc.to_dict()
            conv_id = doc.id
            
            members = doc_data.get('members', [])
            if 'rootie_vn' not in members:
                continue
                
            messages = doc_data.get('messages', [])
            if not messages:
                continue
                
            # Get the latest message
            try:
                sorted_messages = sorted(messages, key=lambda x: x.get('sent_at', ''))
            except Exception:
                sorted_messages = messages

            latest_msg = sorted_messages[-1]
            sender_id = latest_msg.get('sender_id')
            text = latest_msg.get('text', '')
            sent_at = latest_msg.get('sent_at', '')
            msg_id = latest_msg.get('id', '')

            # If sender is not admin (i.e. customer sent it)
            if sender_id and sender_id != 'rootie_vn':
                # Avoid notifying about the same message multiple times
                if last_processed_ids.get(conv_id) == msg_id:
                    continue
                
                # Check if this is a very old message (only process new updates while the listener is running)
                if conv_id not in last_processed_ids:
                    last_processed_ids[conv_id] = msg_id
                    continue
                
                last_processed_ids[conv_id] = msg_id
                
                # Get sender name from member_info
                member_info = doc_data.get('member_info', {})
                sender_info = member_info.get(sender_id, {})
                sender_name = sender_info.get('name', 'Khách hàng')

                print(f"Phát hiện tin nhắn mới từ {sender_name}: '{text}' (sent_at: {sent_at})")
                
                # Send push notification to admin token
                send_notification(admin_token, sender_name, text, sender_id)

    def send_notification(token, sender_name, text, sender_id):
        title = f"Tin nhắn mới từ {sender_name}"
        body = text if len(text) < 100 else text[:97] + "..."
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data={
                'title': title,
                'body': body,
                'sender_id': sender_id,
                'type': 'new_chat_message'
            },
            token=token,
        )
        try:
            response = messaging.send(message)
            print(f"Đã gửi thông báo thành công tới Admin! ID: {response}")
        except Exception as e:
            print(f"Gửi thông báo thất bại: {e}")

    # Watch the collection
    col_query = db.collection('community_message')
    query_watch = col_query.on_snapshot(on_snapshot)

    # Keep script running
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nĐang dừng listener...")

if __name__ == '__main__':
    main()
