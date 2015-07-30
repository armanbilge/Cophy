/*
 * AbstractParticle.java
 *
 * Cophy: Cophylogenetics for BEAST
 *
 * Copyright (C) 2014 Arman Bilge <armanbilge@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
