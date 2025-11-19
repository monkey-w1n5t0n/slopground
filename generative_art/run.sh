#!/bin/bash
# Script to run the generative art sketch

cd "$(dirname "$0")"
echo "Generating artwork... This may take a few minutes."
echo "Canvas size: 3000x3000 pixels"
echo "Particles: 8000"
echo ""

clojure -M:run

if [ -f "output.jpg" ]; then
    echo ""
    echo "Success! Artwork saved to: $(pwd)/output.jpg"
    ls -lh output.jpg
else
    echo "Error: output.jpg was not created"
    exit 1
fi
