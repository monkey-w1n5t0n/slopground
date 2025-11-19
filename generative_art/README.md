# Generative Art with Quil

A generative art project using Quil (Clojure) to create beautiful flow field visualizations.

## About

This project generates abstract art using Perlin noise-based flow fields. Particles follow vector fields created by noise, resulting in organic, flowing patterns.

## Running

To generate the artwork:

```bash
clojure -M:run
```

The artwork will be saved as `output.jpg` in high resolution (3000x3000 pixels).

## Dependencies

- Clojure 1.11.1
- Quil 4.3.1563

## Output

The sketch generates a high-resolution JPEG image featuring flowing particle trails guided by Perlin noise fields.
