#!/bin/sh
set -e

# Replace localhost URLs in compiled JS with actual URLs from environment variables
if [ -n "$BANK_API_URL" ]; then
  sed -i "s|https://localhost:8445|${BANK_API_URL}|g" /usr/share/nginx/html/*.js
fi

exec nginx -g 'daemon off;'
