#!/usr/bin/env bash
# Script to run all generative art pieces with the same seed

cd "$(dirname "$0")"

if [ $# -lt 1 ]; then
    echo "Usage: ./run_all.sh <seed>"
    echo ""
    echo "This will generate all 11 art pieces with the given seed."
    echo "Example: ./run_all.sh 42"
    exit 1
fi

SEED=$1

echo "Generating all artworks with seed $SEED..."
echo "This may take a while (especially reaction-diffusion and strange-attractor)..."
echo ""

PIECES=("flow-field" "circle-packing" "mondrian" "voronoi" "phyllotaxis" "reaction-diffusion" "strange-attractor" "triangular-mesh" "polar-geometry" "lsystem" "grid-deformation")

for PIECE in "${PIECES[@]}"; do
    echo "========================================="
    echo "Running: $PIECE"
    echo "========================================="
    clojure -M:run "$PIECE" "$SEED"
    echo ""
done

echo "========================================="
echo "All artworks generated!"
echo "========================================="
ls -lh output/*_seed_${SEED}.jpg
