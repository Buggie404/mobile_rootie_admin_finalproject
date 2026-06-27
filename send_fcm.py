#!/usr/bin/env python3
import firebase_admin
from firebase_admin import credentials, messaging
import sys
import argparse
import os

def main():
    # 1. Path to Firebase Service Account Credentials
    cred_path = 'serviceAccountKey.json'
    if not os.path.exists(cred_path):
        print(f"Lỗi: Không tìm thấy file {cred_path} ở thư mục gốc!")
        print("Vui lòng tải file serviceAccountKey.json từ Firebase Console và đặt vào đây.")
        sys.exit(1)

    # 2. Parse arguments
    parser = argparse.ArgumentParser(description='Gửi thông báo FCM test tới thiết bị Admin.')
    parser.add_argument('--token', required=True, help='FCM Registration Token của thiết bị nhận')
    parser.add_argument('--title', default='Tin nhắn mới', help='Tiêu đề thông báo')
    parser.add_argument('--body', default='Khách hàng vừa gửi cho bạn một tin nhắn mới!', help='Nội dung thông báo')
    parser.add_argument('--sender', default='user_123', help='Sender ID giả lập')

    args = parser.parse_args()

    # 3. Khởi tạo Firebase Admin SDK
    try:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        print("Đã khởi tạo Firebase Admin SDK thành công.")
    except Exception as e:
        print(f"Lỗi khởi tạo Firebase Admin: {e}")
        sys.exit(1)

    # 4. Tạo message gửi đi (bao gồm cả data payload để app tự vẽ/xử lý hoặc hệ thống tự hiển thị)
    message = messaging.Message(
        notification=messaging.Notification(
            title=args.title,
            body=args.body,
        ),
        data={
            'title': args.title,
            'body': args.body,
            'sender_id': args.sender,
            'type': 'new_chat_message'
        },
        token=args.token,
    )

    # 5. Gửi tin nhắn qua FCM
    try:
        print(f"Đang gửi thông báo tới token: {args.token[:15]}...{args.token[-15:]}")
        response = messaging.send(message)
        print(f"Đã gửi thông báo thành công! Message ID: {response}")
    except Exception as e:
        print(f"Gửi thông báo thất bại: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
