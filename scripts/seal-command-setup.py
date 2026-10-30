"""Seal Work credentials to a user-confirmed device request; never print the values.

Normal mode reads {material:{keystore,password},credentials:{...},settings:{...}}
from stdin. --ci-test uses only synthetic tokens with the approved CI signing key.
The public request must be supplied by the user and its phone code confirmed.
"""
import argparse
import base64
import hashlib
import json
import os
import secrets
import sys
import time
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.serialization import pkcs12


def b64(value):
    return base64.b64encode(value).decode("ascii")


def seal(request, material, credentials, settings):
    if request.get("version") != 1 or request.get("package") != "com.abdickers.realms" or request.get("realm") != "work":
        raise ValueError("Unsupported setup request")
    now = int(time.time() * 1000)
    if not now < request["expiresAt"] <= now + 24 * 60 * 60 * 1000 + 60000:
        raise ValueError("Setup request is expired or invalid")
    public_bytes = base64.b64decode(request["publicKeySpki"], validate=True)
    recipient = hashlib.sha256(public_bytes).hexdigest().upper()
    if recipient != request["recipientSha256"] or len(base64.b64decode(request["nonce"], validate=True)) != 32:
        raise ValueError("Setup recipient is invalid")
    private_key, certificate, _ = pkcs12.load_key_and_certificates(base64.b64decode(material["keystore"], validate=True), material["password"].encode())
    if not isinstance(private_key, rsa.RSAPrivateKey) or certificate.fingerprint(hashes.SHA256()).hex().upper() != request["appSigningSha256"]:
        raise ValueError("The request names another publisher signing certificate")
    if not credentials or set(credentials) - {"vercel", "jira", "slack", "celigo"} or set(settings) - {"jira_email", "vercel_team"}:
        raise ValueError("Unsupported connection fields")
    for value in [*credentials.values(), *settings.values()]:
        if not isinstance(value, str) or not value.strip() or len(value) > 16000 or "\r" in value or "\n" in value:
            raise ValueError("Invalid connection value")
    key, iv = secrets.token_bytes(32), secrets.token_bytes(12)
    payload = json.dumps({"realm": "work", "credentials": credentials, "settings": settings}, separators=(",", ":")).encode()
    aad = (request["package"] + "\n" + request["nonce"] + "\n" + recipient).encode()
    encrypted = AESGCM(key).encrypt(iv, payload, aad)
    public_key = serialization.load_der_public_key(public_bytes)
    if not isinstance(public_key, rsa.RSAPublicKey) or public_key.key_size < 3072:
        raise ValueError("Unsupported phone public key")
    wrapped = public_key.encrypt(key, padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None))
    body = {"package": request["package"], "nonce": request["nonce"], "recipientSha256": recipient, "expiresAt": request["expiresAt"], "wrappedKey": b64(wrapped), "iv": b64(iv), "ciphertext": b64(encrypted)}
    encoded = b64(json.dumps(body, separators=(",", ":")).encode())
    signature = private_key.sign(encoded.encode(), padding.PKCS1v15(), hashes.SHA256())
    return {"version": 1, "body": encoded, "signature": b64(signature)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--request", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--ci-test", action="store_true")
    parser.add_argument("--phone-code", help="Recipient code confirmed by the user on the phone")
    args = parser.parse_args()
    try:
        request = json.loads(Path(args.request).read_text(encoding="utf-8-sig"))
        if args.ci_test:
            material = {"keystore": b64(Path(os.environ["COMMAND_KEYSTORE_FILE"]).read_bytes()), "password": os.environ["COMMAND_KEYSTORE_PASSWORD"]}
            values = {"vercel": "EMULATOR-TEST-NOT-A-REAL-TOKEN"}
            settings = {"vercel_team": "team_TEST123"}
        else:
            if args.phone_code is None or args.phone_code.replace("-", "").upper() != request["recipientSha256"][:12]:
                raise ValueError("Confirm the recipient code shown on the phone first")
            data = json.load(sys.stdin)
            material, values, settings = data["material"], data["credentials"], data.get("settings", {})
        envelope = seal(request, material, values, settings)
        Path(args.output).write_text(json.dumps(envelope, separators=(",", ":")), encoding="utf-8")
        print("Encrypted setup file written. Recipient phone code: " + request["recipientSha256"][:12])
    except Exception:
        # Provider credentials and cryptographic material must never appear in errors.
        print("Setup could not be sealed. Check the request, signing identity and connection fields.", file=sys.stderr)
        sys.exit(1)
