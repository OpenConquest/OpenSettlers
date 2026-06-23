package fr.opensettlers.service.mapgen;

import java.util.Random;

/**
 * Classic 2D Perlin noise generator with a seeded permutation table. The
 * {@link MapGenerator} uses it to produce smooth, natural-looking elevation and
 * moisture fields from which terrain types are derived.
 */
public class PerlinNoise {
    /** Permutation table, duplicated to 512 entries to avoid index wrapping. */
    private final int[] p = new int[512];

    /**
     * Builds the noise generator, shuffling the permutation table from the seed
     * so that the same seed always yields the same noise field.
     *
     * @param seed the seed driving the permutation shuffle
     */
    public PerlinNoise(long seed) {
        Random rand = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) permutation[i] = i;

        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int tmp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = tmp;
        }

        for (int i = 0; i < 256; i++) {
            p[i] = permutation[i];
            p[256 + i] = permutation[i];
        }
    }

    /**
     * Samples the noise field at a point.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return a smoothly varying value, conventionally in the range [-1, 1]
     */
    public double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        double u = fade(x);
        double v = fade(y);

        int A = p[X] + Y;
        int B = p[X + 1] + Y;

        return lerp(v, lerp(u, grad(p[A], x, y), grad(p[B], x - 1, y)),
                       lerp(u, grad(p[A + 1], x, y - 1), grad(p[B + 1], x - 1, y - 1)));
    }

    /**
     * Ken Perlin's smoothstep easing curve {@code 6t^5 - 15t^4 + 10t^3}, used to
     * interpolate smoothly between grid cells.
     *
     * @param t the interpolation parameter in [0, 1]
     * @return the eased value
     */
    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    /**
     * Linearly interpolates between two values.
     *
     * @param t the interpolation parameter in [0, 1]
     * @param a the value at {@code t == 0}
     * @param b the value at {@code t == 1}
     * @return the interpolated value
     */
    private double lerp(double t, double a, double b) { return a + t * (b - a); }

    /**
     * Computes the dot product of a pseudo-random gradient (selected from the
     * hash) with the distance vector to a grid corner.
     *
     * @param hash the permutation-table value selecting the gradient direction
     * @param x    the x distance from the grid corner
     * @param y    the y distance from the grid corner
     * @return the gradient contribution of that corner
     */
    private double grad(int hash, double x, double y) {
        int h = hash & 7;
        double u = h < 4 ? x : y;
        double v = h < 4 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? 2.0 * v : -2.0 * v);
    }
}