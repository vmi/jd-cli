#!/bin/bash

jar="$(cygpath -am "$0")"
args=()
for arg in "$@"; do
  if [ -e "$arg" ]; then
    args+=("$(cygpath -am "$arg")")
  else
    args+=("$arg")
  fi
done
exec java -jar "$jar" "${args[@]}"
exit 1
