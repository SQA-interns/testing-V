#!/bin/sh
# Writes /config.json from APP_WORKSHOPS ("id=name;id=name") at container start
# (docs/02_contracts/frontend-config.schema.json, AR-04), then starts nginx.
set -eu
: "${APP_WORKSHOPS:?APP_WORKSHOPS must be set}"

json_string() {
  printf '%s' "$1" | tr '\t\r\n' '   ' | sed -e 's/^ *//' -e 's/ *$//' -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

json='{"workshops":['
separator=''
set -f
old_ifs=$IFS
IFS=';'
for entry in $APP_WORKSHOPS; do
  case "$entry" in
    *=*) ;;
    *) continue ;;
  esac
  id=$(json_string "${entry%%=*}")
  name=$(json_string "${entry#*=}")
  if [ -n "$id" ] && [ -n "$name" ]; then
    json="$json$separator{\"id\":\"$id\",\"name\":\"$name\"}"
    separator=','
  fi
done
IFS=$old_ifs
set +f
printf '%s]}\n' "$json" > /tmp/confreg/config.json
exec "$@"
