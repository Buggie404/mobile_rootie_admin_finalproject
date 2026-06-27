import firebase_admin
from firebase_admin import credentials, firestore
import os
import sys

if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

possible_paths = [
    'serviceAccountKey.json',
    '../ROOTIE/mobile_rootie_finalproject/serviceAccountKey.json',
    '../../ROOTIE/mobile_rootie_finalproject/serviceAccountKey.json',
    'h:/HOCKY3NAM3/ROOTIE/mobile_rootie_finalproject/serviceAccountKey.json'
]

cred_path = None
for p in possible_paths:
    if os.path.exists(p):
        cred_path = p
        break

if cred_path:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    bookings = db.collection('bookings').where('status', 'in', ['Chờ xác nhận', 'pending', 'Sắp diễn ra', 'confirmed', 'upcoming']).get()
    print(f"Total active bookings found: {len(bookings)}")
    for b in bookings:
        data = b.to_dict()
        print(f"ID: {b.id} | Status: {data.get('status')} | Date: '{data.get('dateDisplay')}' | Time: '{data.get('time')}'")
else:
    print("No service account key found.")
