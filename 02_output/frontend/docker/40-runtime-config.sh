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
old_ifs=$IFS
IFS=';'
for entry in ${APP_WORKSHOPS:-}; do
  IFS=$old_ifs
  case "$entry" in
    *=*) id=$(trim "${entry%%=*}"); title=$(trim "${entry#*=}") ;;
    *) id=$(trim "$entry"); title=$id ;;
  esac
  if [ -n "$id" ]; then
    items="${items}${items:+,}{\"id\":\"$(escape "$id")\",\"title\":\"$(escape "$title")\"}"
  fi
  IFS=';'
done
IFS=$old_ifs

printf 'window.APP_CONFIG = {"workshops":[%s]};\n' "$items" > /usr/share/nginx/html/config.js
