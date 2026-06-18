import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
import time

# 1. Khoi tao Firebase
cred_path = 'serviceAccountKey.json'
if not os.path.exists(cred_path):
    print("Loi: Khong tim thay file serviceAccountKey.json o thu muc goc!")
    print("Vui long tai file serviceAccountKey.json tu Firebase Console va dat vao day.")
    exit()

cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)
db = firestore.client()

# 2. Cau hinh thu muc du lieu cua Rootie Admin
raw_dir = 'app/src/main/assets'

# Danh sach cac file va ten collection tuong ung
file_mapping = {
    'categories.json': 'categories',
    'products.json': 'products',
    'orders.json': 'orders',
    'users.json': 'users',
    'vouchers.json': 'vouchers',
    'skin_bookings.json': 'bookings',
    'admins.json': 'admins',
    'chat_message.json': 'chats'
}

def upload_file(filename, collection_name):
    file_path = os.path.join(raw_dir, filename)
    if not os.path.exists(file_path):
        print(f"Khong tim thay file: {file_path}")
        return

    print(f"\n--- Dang tai file: {filename} vao collection: {collection_name} ---")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except Exception as e:
            print(f"Loi doc file {filename}: {e}")
            return
        
        items = []
        if isinstance(data, list):
            items = data
        elif isinstance(data, dict):
            for key in data:
                if isinstance(data[key], list):
                    items = data[key]
                    break
        
        if not items:
            print(f"Khong tim thay danh sach du lieu trong {filename}")
            return
 
        batch_size = 50 
        batch = db.batch()
        count = 0
        
        for item in items:
            # Xac dinh ID tai lieu phu hop cho tung loai doi tuong
            raw_id = str(item.get('id') or item.get('_id') or item.get('orderId') or item.get('userId') or item.get('username') or '').strip()
            doc_id = raw_id.replace('/', '-') 
            
            if doc_id and doc_id != 'None':
                doc_ref = db.collection(collection_name).document(doc_id)
            else:
                doc_ref = db.collection(collection_name).document()
            
            batch.set(doc_ref, item)
            
            count += 1
            if count % batch_size == 0:
                try:
                    batch.commit()
                    print(f"Da tai duoc {count}/{len(items)} items...")
                    batch = db.batch()
                    time.sleep(1) # Tranh gioi han rate limit cua Firestore
                except Exception as e:
                    print(f"Loi khi commit batch tai item {count}: {e}")
                    batch = db.batch()
                    time.sleep(2)

        try:
            batch.commit()
            print(f"Hoan tat! Tong cong {count} items.")
        except Exception as e:
            print(f"Loi commit cuoi cung: {e}")

# Chay tai toan bo du lieu mau len Firestore
for filename, coll_name in file_mapping.items():
    upload_file(filename, coll_name)

print("\n--- TAT CA DU LIEU DA DUOC TAI LEN FIREBASE THANH CONG ---")
