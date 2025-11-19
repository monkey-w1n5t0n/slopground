#!/usr/bin/env bash
# Script to run a specific generative art piece

cd "$(dirname "$0")"

if [ $# -lt 2 ]; then
    echo "Usage: ./run.sh <piece-name> <seed>"
    echo ""
    echo "Example: ./run.sh flow-field 12345"
    echo ""
    clojure -M:run
    exit 1
fi

PIECE=$1
SEED=$2

echo "Generating $PIECE artwork with seed $SEED..."
echo ""

clojure -M:run "$PIECE" "$SEED"

if [ -f "output/${PIECE}_seed_${SEED}.jpg" ]; then
    echo ""
    echo "Success! Artwork saved to: $(pwd)/output/${PIECE}_seed_${SEED}.jpg"
    ls -lh "output/${PIECE}_seed_${SEED}.jpg"
else
    echo "Error: Artwork file was not created"
    exit 1
fi
