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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

/**
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class AbstractParticle<T> implements Copyable {

    private final T value;
    private double weight;

    protected AbstractParticle(final T value) {
        this(value, 1.0);
    }

    protected AbstractParticle(final T value, final double weight) {
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
    public abstract AbstractParticle<T> copy();

    public static class Particle<T extends Copyable>
            extends AbstractParticle<T> {

        public Particle(final T value) {
            super(value);
        }

        private Particle(final T value, final double weight) {
            super(value, weight);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Particle<T> copy() {
            return new Particle<T>((T) getValue().copy(), getWeight());
        }

    }

}
