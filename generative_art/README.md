# Generative Art with Quil

A collection of 11 generative art pieces created with Quil (Clojure wrapper for Processing), exploring diverse algorithmic techniques and aesthetics.

## Features

- **Seeded Randomness**: All pieces use reproducible random seeds, allowing you to regenerate the exact same artwork
- **High Resolution**: Most pieces render at 3000x3000 pixels for high-quality output
- **Diverse Techniques**: Each piece explores a different generative algorithm or aesthetic approach
- **genartlib Integration**: Utilizes Tyler Hobbs' genartlib for geometric and mathematical utilities

## Art Pieces

### 1. Flow Field
**Technique**: Perlin noise-based flow fields
**Description**: 8,000 particles flow across the canvas following smooth vector fields created by noise, resulting in organic, flowing patterns with dynamic HSB coloring.

### 2. Circle Packing
**Technique**: Poisson disc sampling + iterative growth
**Description**: Circles grow from evenly distributed seed points until they touch, creating densely packed organic patterns with varied sizes.

### 3. Mondrian
**Technique**: Recursive subdivision
**Description**: Canvas recursively splits into rectangles, colored in Mondrian-inspired primary colors and white, creating balanced geometric compositions.

### 4. Voronoi
**Technique**: Voronoi diagram / cellular patterns
**Description**: Space divided into regions based on distance to seed points, creating cellular organic patterns with smooth color gradients.

### 5. Phyllotaxis
**Technique**: Golden angle spiral (fibonacci)
**Description**: 5,000 elements arranged in spirals following the golden angle (~137.5°), mimicking patterns found in sunflowers and pinecones.

### 6. Reaction-Diffusion
**Technique**: Gray-Scott model simulation
**Description**: Chemical reaction simulation creating organic, coral-like or maze-like patterns through diffusion and reaction of two virtual chemicals.

### 7. Strange Attractor
**Technique**: Lorenz attractor / chaotic systems
**Description**: 1 million points trace the path of a chaotic system, creating the iconic butterfly-shaped Lorenz attractor with flowing color gradients.

### 8. Triangular Mesh
**Technique**: Poisson disc sampling + triangulation
**Description**: Points connected into triangles with noise-based coloring, creating low-poly geometric landscapes with organic color variation.

### 9. Polar Geometry
**Technique**: Radial symmetry + Chaikin curves
**Description**: Concentric shapes with noise-based deformations, smoothed with Chaikin curve algorithm, creating mandala-like radial patterns.

### 10. L-System Fractal
**Technique**: Lindenmayer systems
**Description**: Recursive plant-like structures generated from simple rules, creating branching fractal trees with depth and variation.

### 11. Grid Deformation
**Technique**: Noise-based mesh deformation
**Description**: Regular grid warped by Perlin noise and smoothed with Chaikin curves, creating flowing organic distortions of geometric order.

## Installation

Requires Clojure and Java to be installed.

```bash
# Install Clojure (if not already installed)
# See: https://clojure.org/guides/install_clojure

# The project will automatically download dependencies on first run
```

## Usage

### Run a Single Piece

```bash
cd generative_art
./run.sh <piece-name> <seed>
```

Examples:
```bash
./run.sh flow-field 12345
./run.sh phyllotaxis 42
./run.sh mondrian 2024
```

Or using Clojure directly:
```bash
clojure -M:run <piece-name> <seed>
```

### Run All Pieces

Generate all 11 pieces with the same seed:

```bash
./run_all.sh 42
```

Note: This will take some time, especially for reaction-diffusion and strange-attractor.

### List Available Pieces

```bash
./run.sh
# or
clojure -M:run
```

## Available Pieces

- `flow-field` - Flowing particle trails
- `circle-packing` - Densely packed circles
- `mondrian` - Recursive rectangles
- `voronoi` - Cellular patterns
- `phyllotaxis` - Golden spiral
- `reaction-diffusion` - Organic textures
- `strange-attractor` - Chaotic systems
- `triangular-mesh` - Geometric triangulation
- `polar-geometry` - Radial symmetry
- `lsystem` - Fractal trees
- `grid-deformation` - Warped grids

## Output

All artworks are saved to the `output/` directory with filenames that include the seed:

```
output/flow_field_seed_12345.jpg
output/phyllotaxis_seed_42.jpg
```

This naming convention makes it easy to recreate specific artworks by noting their seed value.

## Dependencies

- **Clojure 1.11.1**
- **Quil 4.3.1563** - Processing wrapper for Clojure
- **genartlib** - Tyler Hobbs' generative art utilities (included as submodule)
- **Apache Commons Math3** - Mathematical utilities

## Project Structure

```
generative_art/
├── src/
│   └── generative_art/
│       ├── core.clj              # Main entry point
│       └── pieces/               # Individual art pieces
│           ├── flow_field.clj
│           ├── circle_packing.clj
│           ├── mondrian.clj
│           ├── voronoi.clj
│           ├── phyllotaxis.clj
│           ├── reaction_diffusion.clj
│           ├── strange_attractor.clj
│           ├── triangular_mesh.clj
│           ├── polar_geometry.clj
│           ├── lsystem.clj
│           └── grid_deformation.clj
├── genartlib/                    # Submodule with utilities
├── output/                       # Generated artworks
├── deps.edn                      # Dependencies
├── run.sh                        # Run single piece
└── run_all.sh                    # Run all pieces

```

## Tips

- **Experiment with seeds**: Different seeds produce completely different results
- **Performance**: Some pieces (reaction-diffusion, strange-attractor) take several minutes to render
- **Resolution**: Edit the `canvas-width` and `canvas-height` constants in individual piece files to adjust output size
- **Parameters**: Each piece has configurable parameters at the top of its file - feel free to experiment!

## Credits

- Built with [Quil](https://github.com/quil/quil)
- Uses [genartlib](https://github.com/thobbs/genartlib) by Tyler Hobbs
- Inspired by various generative art techniques and algorithms

## License

See LICENSE file.
