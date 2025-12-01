#!/usr/bin/env bash

# set -x # set this for debugging to print all called commands
set -e # fail non non-zero exit status
set -u # fail on unset variable
set -o pipefail # fail if a pipe fails

cd "$(dirname '${0}')"

mvn versions:use-latest-releases
mvn versions:display-plugin-updates
