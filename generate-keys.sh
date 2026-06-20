#!/bin/bash
set -e

SERVICE_KEYS=service-app/src/main/resources/keys
LEDGER_KEYS=ledger-app/src/main/resources/keys

mkdir -p "$SERVICE_KEYS" "$LEDGER_KEYS"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$SERVICE_KEYS/private.pem"
chmod 600 "$SERVICE_KEYS/private.pem"
openssl rsa -pubout -in "$SERVICE_KEYS/private.pem" -out "$SERVICE_KEYS/public.pem"
cp "$SERVICE_KEYS/public.pem" "$LEDGER_KEYS/public.pem"

echo "RSA 키 생성 완료"
echo "  private: $SERVICE_KEYS/private.pem"
echo "  public : $SERVICE_KEYS/public.pem (ledger-app에도 복사됨)"
