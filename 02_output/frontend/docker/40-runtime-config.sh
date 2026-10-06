#!/bin/sh
# Writes /config.js from APP_WORKSHOPS ("id=title;id=title") at container start (D-14, AR-04).
# Values are JSON-escaped and "<" ">" written as < > so they cannot leave the script.
set -eu

escape() {
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/</\\u003c/g' -e 's/>/\\u003e/g'
}

trim() {
  printf '%s' "$1" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

items=""
while read -r entry; do
  case "$entry" in
    *=*) id=$(trim "${entry%%=*}"); title=$(trim "${entry#*=}") ;;
    *) id=$(trim "$entry"); title=$id ;;
  esac
  if [ -n "$id" ]; then
    items="${items}${items:+,}{\"id\":\"$(escape "$id")\",\"title\":\"$(escape "$title")\"}"
  fi
done <<EOF
$(printf '%s\n' "${APP_WORKSHOPS:-}" | tr ';' '\n')
EOF

printf 'window.APP_CONFIG = {"workshops":[%s]};\n' "$items" > "${CONFIG_JS:-/usr/share/nginx/html/config.js}"
