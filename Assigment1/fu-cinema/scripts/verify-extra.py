"""Run additional API checks after the Postman collection (standard library only)."""
import base64
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import hashlib
import hmac
import json
from pathlib import Path
import threading
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
environment = json.loads((ROOT / '.runtime/runner.environment.json').read_text(encoding='utf-8'))
variables = {value['key']: value['value'] for value in environment['values']}
gateway = variables['gateway']
results = []


def request(method, path, token=None, body=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = Request(gateway + path, headers=headers, method=method,
                  data=json.dumps(body).encode('utf-8') if body is not None else None)
    try:
        response = urlopen(req, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        return response.status, json.loads(raw) if raw else None


def check(name, condition):
    results.append({'check': name, 'passed': bool(condition)})
    print(('PASS ' if condition else 'FAIL ') + name, flush=True)
    assert condition, name


def signed_token(secret, issued, expires):
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value, separators=(',', ':')).encode()).rstrip(b'=')
    payload = encode({'alg': 'HS256', 'typ': 'JWT'}) + b'.' + encode({
        'sub': 'an@gmail.com', 'uid': 1, 'role': 'CUSTOMER', 'iat': issued, 'exp': expires})
    signature = base64.urlsafe_b64encode(hmac.new(secret.encode(), payload, hashlib.sha256).digest()).rstrip(b'=')
    return (payload + b'.' + signature).decode()


admin = variables['adminToken']
customer = variables['customerToken']
other_customer = variables['customer2Token']
now = int(time.time())
secret = 'fu-cinema-booking-system-secret-key-2026-mss301'
status, _ = request('GET', '/api/customers/me', signed_token(secret, now - 3600, now - 600))
check('Expired JWT returns 401', status == 401)
status, _ = request('GET', '/api/customers/me', signed_token(secret + '-wrong', now, now + 3600))
check('Wrong JWT signature returns 401', status == 401)

status, _ = request('POST', '/api/bookings', customer, {'items': [None]})
check('Null ticket rejected by validation', status == 400)

barrier = threading.Barrier(2)
body = {'items': [{'showtimeId': variables['showtimeId'], 'seatCode': 'D1'}]}


def competing_booking(token):
    barrier.wait()
    return request('POST', '/api/bookings', token, body)


with ThreadPoolExecutor(max_workers=2) as pool:
    pending = [pool.submit(competing_booking, token) for token in [customer, other_customer]]
    attempts = [attempt.result() for attempt in pending]
check('Simultaneous same-seat requests produce one 201 and one 409',
      sorted(status for status, _ in attempts) == [201, 409])
status, seat_map = request('GET', '/api/bookings/showtimes/' + variables['showtimeId'] + '/seats')
check('Seat map contains competing seat exactly once', status == 200 and seat_map['bookedSeats'].count('D1') == 1)
winner = next(response for status, response in attempts if status == 201)
status, _ = request('PUT', f"/api/bookings/{winner['bookingId']}/cancel", admin)
check('Admin cleanup releases competing seat', status == 200)

status, room = request('POST', '/api/rooms', admin, {
    'roomName': 'Deadline test ' + str(time.time_ns()), 'roomType': 'STANDARD',
    'seatRows': 2, 'seatsPerRow': 3, 'roomStatus': 'ACTIVE'})
check('Create isolated room for cancellation deadline', status == 201)
status, showtime = request('POST', '/api/showtimes', admin, {
    'movieId': variables['movieId'], 'roomId': room['roomId'],
    'startTime': (datetime.now(timezone(timedelta(hours=7))).replace(tzinfo=None)
                  + timedelta(minutes=30)).isoformat(timespec='seconds'), 'ticketPrice': 95000})
check('Create showtime within two-hour cancellation window', status == 201)
status, booking = request('POST', '/api/bookings', customer, {
    'items': [{'showtimeId': showtime['showtimeId'], 'seatCode': 'A1'}]})
check('Book upcoming showtime', status == 201)
status, _ = request('PUT', f"/api/bookings/{booking['bookingId']}/cancel", customer)
check('Customer cancellation within two hours rejected', status == 400)
status, _ = request('PUT', f"/api/bookings/{booking['bookingId']}/cancel", admin)
check('Admin overrides customer cancellation deadline', status == 200)

# Inspect CRUD responses as well as deletion guards tested in the collection.
status, genre = request('POST', '/api/genres', admin, {'genreName': 'Delete test ' + str(time.time_ns())})
check('Create unused genre', status == 201)
status, _ = request('GET', '/api/genres/' + genre['genreId'])
check('Read genre detail', status == 200)
status, _ = request('DELETE', '/api/genres/' + genre['genreId'], admin)
check('Delete unused genre', status == 204)
status, _ = request('GET', '/api/genres/' + genre['genreId'])
check('Deleted genre returns 404', status == 404)

status, unused_room = request('POST', '/api/rooms', admin, {
    'roomName': 'Unused room ' + str(time.time_ns()), 'roomType': 'IMAX',
    'seatRows': 2, 'seatsPerRow': 3, 'roomStatus': 'ACTIVE'})
check('Create unused room', status == 201)
status, _ = request('PUT', '/api/rooms/' + unused_room['roomId'], admin, {
    'roomName': unused_room['roomName'], 'roomType': 'THREE_D',
    'seatRows': 3, 'seatsPerRow': 4, 'roomStatus': 'ACTIVE'})
check('Update room capacity', status == 200)
status, updated_room = request('GET', '/api/rooms/' + unused_room['roomId'], admin)
check('Read updated room capacity', status == 200 and updated_room['totalSeats'] == 12)
status, _ = request('DELETE', '/api/rooms/' + unused_room['roomId'], admin)
check('Delete unused room', status == 204)

status, unused_movie = request('POST', '/api/movies', admin, {
    'title': 'Unused movie', 'durationMinutes': 90, 'ageRating': 'P',
    'genreId': variables['genreId'], 'movieStatus': 'COMING_SOON'})
check('Create movie without showtimes', status == 201)
status, _ = request('GET', '/api/movies/' + unused_movie['movieId'])
check('Read movie detail', status == 200)
status, _ = request('DELETE', '/api/movies/' + unused_movie['movieId'], admin)
check('Delete movie without showtimes', status == 204)

status, _ = request('PUT', '/api/showtimes/' + variables['showtimeId'], admin, {
    'movieId': variables['movieId'], 'roomId': variables['roomId'],
    'startTime': variables['futureStart'], 'ticketPrice': 95000})
check('Update showtime excludes itself from overlap query', status == 200)

(ROOT / '.runtime/extra-verification.json').write_text(json.dumps(results, indent=2) + '\n', encoding='utf-8')
print(f'{len(results)} additional checks passed.', flush=True)
