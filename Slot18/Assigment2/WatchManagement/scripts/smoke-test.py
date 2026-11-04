"""Direct API checks for assignment steps 3 and 5; use a disposable seeded database."""
import argparse
import base64
import json
from urllib.request import Request, urlopen
from urllib.error import HTTPError

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--account-url", default="http://localhost:8081")
parser.add_argument("--brand-url", default="http://localhost:8082")
args = parser.parse_args()
checks = 0

def call(base, method, path, body=None, expected=200):
    global checks
    request = Request(base + path, data=json.dumps(body).encode() if body is not None else None,
                      headers={"Content-Type": "application/json"}, method=method)
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as ex:
        response = ex
    with response:
        payload = json.load(response)
        assert response.code == expected, (method, path, response.code, payload)
    checks += 1
    return payload

a, b = args.account_url, args.brand_url
login = call(a, "POST", "/api/auth/login", {"username": "adminsys", "password": "@1"})["data"]
assert login["role"] == 1 and login["tokenType"] == "Bearer"
claims = json.loads(base64.urlsafe_b64decode(login["accessToken"].split(".")[1] + "=="))
assert claims["sub"] == "1" and claims["role"] == 1 and claims["type"] == "access"
assert claims["exp"] - claims["iat"] == 900
for username in ("staff02", "member01"):
    call(a, "POST", "/api/auth/login", {"username": username, "password": "@1"})
for username in ("member02", "staff01"):
    call(a, "POST", "/api/auth/login", {"username": username, "password": "@1"}, 403)
call(a, "POST", "/api/auth/login", {"username": "adminsys", "password": "wrong"}, 401)
call(a, "POST", "/api/auth/login", {"password": "@1"}, 400)
call(a, "POST", "/api/auth/login", {"username": "", "password": ""}, 400)
call(a, "POST", "/api/auth/refresh", {"refreshToken": login["refreshToken"]})
call(a, "POST", "/api/auth/refresh", {"refreshToken": login["accessToken"]}, 401)
call(a, "POST", "/api/auth/refresh", {"refreshToken": "invalid"}, 401)
call(a, "POST", "/api/auth/refresh", {}, 401)
call(a, "POST", "/api/auth/logout", {})
brands = call(b, "GET", "/api/brands")["data"]
assert {x["brandName"]: x["watchCount"] for x in brands} == {
    "Rolex": 2, "Omega": 2, "Seiko": 2, "Casio": 3, "TAG Heuer": 1}
assert call(b, "GET", "/api/brands/1/watches")["data"]["watchIds"] == [1, 8]
# Reserved smoke-test ID; do not overwrite any existing assignment.
for brand in brands:
    assert 999999 not in call(b, "GET", f"/api/brands/{brand['brandId']}/watches")["data"]["watchIds"]
try:
    for _ in range(2):
        call(b, "PUT", "/internal/brands/1/watches/999999")
    ids = call(b, "GET", "/api/brands/1/watches")["data"]["watchIds"]
    assert ids.count(999999) == 1
    failure = call(b, "PUT", "/internal/brands/999/watches/999999", expected=404)
    assert failure["success"] is False and failure["message"] == "Brand not found with id: 999"
    assert call(b, "GET", "/api/brands/1/watches")["data"]["watchCount"] == 3
    call(b, "PUT", "/internal/brands/2/watches/999999")
    assert call(b, "GET", "/api/brands/1")["data"]["watchCount"] == 2
    assert call(b, "GET", "/api/brands/2")["data"]["watchCount"] == 3
    assert call(b, "DELETE", "/internal/brands/watches/999999")["data"]["action"] == "UNASSIGNED"
    assert call(b, "DELETE", "/internal/brands/watches/999999")["data"]["action"] == "NOTHING_TO_DO"
finally:
    call(b, "DELETE", "/internal/brands/watches/999999")
assert call(b, "GET", "/api/brands")["data"] == brands
call(b, "GET", "/api/brands/999", expected=404)
print(f"PASS: {checks} HTTP checks plus JWT, seed counts, assignment uniqueness and cleanup assertions")
