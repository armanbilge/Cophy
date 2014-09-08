/**
 * CophylogenyUtils.java
 *
 * Cophy: Cophylogenetics for BEAST
 *
 * Copyright (C) 2014 Arman D. Bilge <armanbilge@gmail.com>
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

package cophy;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cophy.model.Reconciliation;
import cophy.particlefiltration.AbstractParticle;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.math.MathUtils;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public final class CophylogenyUtils {

    private CophylogenyUtils() {}

    public static final boolean lineageExistedAtHeight(final Tree tree,
                                                       final NodeRef node,
                                                       final double height) {

        return tree.getNodeHeight(node) <= height &&
                (tree.isRoot(node)
                        || tree.getNodeHeight(tree.getParent(node)) > height);

    }

    public static final Set<NodeRef> getLineagesAtHeight(final Tree tree,
                                                         final double height) {

        Set<NodeRef> lineages = new HashSet<NodeRef>();
        getLineagesAtHeight(tree, tree.getRoot(), height, lineages);
        return lineages;

    }

    private static final void getLineagesAtHeight(final Tree tree,
                                                  final NodeRef node,
                                                  final double height,
                                                  final Set<NodeRef> lineages) {

        if (lineageExistedAtHeight(tree, node, height)) {
            lineages.add(node);
        } else {
            getLineagesAtHeight(tree, tree.getChild(node, 0), height, lineages);
            getLineagesAtHeight(tree, tree.getChild(node, 1), height, lineages);
        }

    }

    public static final int getLineageCountAtHeight(final Tree tree,
                                                    final double height) {

        return getLineageCountAtHeight(tree, tree.getRoot(), height);

    }

    private static final int getLineageCountAtHeight(final Tree tree,
                                                     final NodeRef node,
                                                     final double height) {

        if (lineageExistedAtHeight(tree, node, height)) {
            return 1;
        } else {
            return getLineageCountAtHeight(tree, tree.getChild(node, 0), height)
                + getLineageCountAtHeight(tree, tree.getChild(node, 1), height);
        }

    }

    public static final int
            getGuestCountAtHostAtHeight(final Tree guestTree,
                                        final Reconciliation reconciliation,
                                        final NodeRef hostNode,
                                        final double height) {
        int count = 0;
        final Set<NodeRef> guestNodes =
                CophylogenyUtils.getLineagesAtHeight(guestTree, height);
        for (final NodeRef guestNode : guestNodes) {
            if (hostNode.equals(reconciliation.getHost(guestNode)))
                ++count;
        }
        return count;
    }

    public static final <T> T getRandomElement(Collection<T> collection) {
        int i = 0;
        final int r = MathUtils.nextInt(collection.size());
        for (T element : collection)
            if (i++ == r) return element;
        throw new RuntimeException();
    }

    public static class RandomWeightedObject<T> {

        final LinkedHashMap<T,Double> weights;
        final double sum;

        public RandomWeightedObject(Map<T,? extends Number> weights) {
            double sum = 0.0;
            this.weights = new LinkedHashMap<T,Double>(weights.size());
            for (T key : weights.keySet()) {
                final double weight = weights.get(key).doubleValue();
                this.weights.put(key, weight);
                sum += weight;
            }
            this.sum = sum;
        }

        public final T nextObject() {
            final double r = (1 - MathUtils.nextDouble()) * sum;
            for (T key : weights.keySet()) {
                if (weights.get(key) >= r)
                    return key;
            }
            throw new RuntimeException();
        }

    }

    public static final <T> T
            nextWeightedObject(Map<T,? extends Number> weights) {

        return new RandomWeightedObject<T>(weights).nextObject();
    }

    public static final class RandomWeightedInteger {

        final double[] weights;
        final double sum;

        public RandomWeightedInteger(final double...weights) {
            this.weights = weights;
            sum = MathUtils.getTotal(weights);
        }

        public final int nextInt() {
            final double r = (1 - MathUtils.nextDouble()) * sum;
            int i;
            for (i = 0; i < weights.length && weights[i] < r; ++i);
            return i;
        }

    }

    public static final int nextWeightedInteger(final double...weights) {
        return new RandomWeightedInteger(weights).nextInt();
    }

    public static final double nextPoissonTime(final double...lambdas) {
        final double lambda = MathUtils.getTotal(lambdas);
        return MathUtils.nextExponential(lambda);
    }

    public static final void resample(AbstractParticle<?>[] particles) {

        final double[] weights = new double[particles.length];
        final AbstractParticle<?>[] particlesCopy = new AbstractParticle[particles.length];

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
