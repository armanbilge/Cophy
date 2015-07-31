/*
 * Particle.java
 *
 * Cophy: Cophylogenetics for BEAST
 *
 * Copyright (c) 2015 Arman Bilge <armanbilge@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cophy.particlefiltration;

import cophy.CophyUtils.RandomWeightedInteger;

/**
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class Particle<T extends Copyable> implements Copyable {

    private final T value;
    private double weight;

    protected Particle(final T value) {
        this(value, 1.0);
    }

    protected Particle(final T value, final double weight) {
        this.value = value;
        this.weight = weight;
    }

    public T getValue() {
        return value;
    }

    public double getWeight() {
        return weight;
    }

    public void resetWeight() {
        weight = 1.0;
    }

    public void multiplyWeight(final double value) {
        weight *= value;
    }

    @Override
    public Particle<T> copy() {
        return new Particle<T>((T) value.copy());
    }

    public static void resample(final Particle<?>[] particles) {

        final double[] weights = new double[particles.length];
        final Particle<?>[] particlesCopy = new Particle[particles.length];

        for (int i = 0; i < particles.length; ++i) {
            weights[i] = particles[i].getWeight();
            particlesCopy[i] = particles[i];
        }

        final RandomWeightedInteger rwi = new RandomWeightedInteger(weights);
        for (int i = 0; i < particles.length; ++i) {
            final int r = rwi.nextInt();
            particles[i] = particlesCopy[r].copy();
        }

    }

}
