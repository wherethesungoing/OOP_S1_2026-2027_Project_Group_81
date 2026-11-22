#!/bin/bash
# Build and run I-ITSM. Needs JDK 17+.
#   bash run.sh          compile, run the console demo, then the 48 tests
#   bash run.sh test     compile and run the tests only
#   bash run.sh web      compile and start the web app at http://localhost:8080 (data kept in ./data)
#   bash run.sh reset    same, but wipe ./data first and start from the demo set-up
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
case "$1" in
  web)  java -cp out itsm.web.DashboardServer "${2:-8080}" ;;
  reset) java -cp out itsm.web.DashboardServer "${2:-8080}" --reset ;;
  test) java -cp out itsm.TestRunner ;;
  *)    echo "=== DEMO ==="; java -cp out itsm.Main; echo; echo "=== TESTS ==="; java -cp out itsm.TestRunner ;;
esac
