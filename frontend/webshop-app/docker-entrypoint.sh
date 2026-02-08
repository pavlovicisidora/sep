#!/bin/sh
set -e

# Replace localhost URLs in compiled JS with actual URLs from environment variables
if [ -n "$WEBSHOP_API_URL" ]; then
  sed -i "s|https://localhost:8443|${WEBSHOP_API_URL}|g" /usr/share/nginx/html/*.js
fi

if [ -n "$PSP_API_URL" ]; then
  sed -i "s|https://localhost:8444|${PSP_API_URL}|g" /usr/share/nginx/html/*.js
fi

exec nginx -g 'daemon off;'
