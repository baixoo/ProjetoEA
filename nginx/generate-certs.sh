#!/bin/sh
set -e

# Generate self-signed certificate if it doesn't exist
if [ ! -f /etc/nginx/certs/server.crt ]; then
    echo "Generating self-signed certificate..."
    mkdir -p /etc/nginx/certs
    
    # Check if openssl is installed, otherwise install it
    if ! command -v openssl >/dev/null 2>&1; then
        echo "Installing openssl..."
        apk add --no-cache openssl
    fi

    # Generate the certificate
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout /etc/nginx/certs/server.key \
        -out /etc/nginx/certs/server.crt \
        -subj "/C=PT/ST=Portugal/L=Portugal/O=Development/CN=localhost"
        
    echo "Self-signed certificate generated successfully."
else
    echo "Self-signed certificate already exists. Skipping generation."
fi