/*
 * CophyUtils.java
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

package cophy;

import cophy.model.Reconciliation;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.math.MathUtils;

import java.util.*;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public final class CophyUtils {

    private CophyUtils() {}

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
        } else if (!tree.isExternal(node)) {
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
        } else if (!tree.isExternal(node)) {
            return getLineageCountAtHeight(tree, tree.getChild(node, 0), height)
                + getLineageCountAtHeight(tree, tree.getChild(node, 1), height);
        } else {
            return 0;
        }

    }

    public static final int
            getGuestCountAtHostAtHeight(final Tree guestTree,
                                        final NodeRef hostNode,
                                        final double height,
                                        final String hostTraitName) {
        int count = 0;
        final Set<NodeRef> guestNodes =
                CophyUtils.getLineagesAtHeight(guestTree, height);
        for (final NodeRef guestNode : guestNodes) {
            final NodeRef actualHost = (NodeRef) guestTree
                    .getNodeAttribute(guestNode, hostTraitName);
            if (hostNode.equals(actualHost)) ++count;
        }
        return count;
    }

    public static final int
            getGuestCountAtHostAtHeight(final Tree guestTree,
                                        final NodeRef hostNode,
                                        final double height,
                                        final Reconciliation reconciliation) {
        int count = 0;
        final Set<NodeRef> guestNodes =
                CophyUtils.getLineagesAtHeight(guestTree, height);
        for (final NodeRef guestNode : guestNodes) {
            final NodeRef actualHost = reconciliation.getHost(guestNode);
            if (hostNode.equals(actualHost)) ++count;
        }
        return count;
    }


    public static final Set<NodeRef>
            getGuestsAtHostAtHeight(final Tree guestTree,
                                    final NodeRef hostNode,
                                    final double height,
                                    final String hostTraitName) {

        final Set<NodeRef> guestNodes =
                CophyUtils.getLineagesAtHeight(guestTree, height);
        for (final Iterator<NodeRef> iter = guestNodes.iterator();
             iter.hasNext();) {

            final NodeRef guestNode = iter.next();
            final NodeRef actualHost = (NodeRef) guestTree
                    .getNodeAttribute(guestNode, hostTraitName);
            if (!hostNode.equals(actualHost))
                iter.remove();

        }

        return guestNodes;

    }

    public static final int
            getChildNumber(final Tree tree, final NodeRef node) {

        if (tree.isRoot(node))
            throw new RuntimeException("Root is not a child.");
        final NodeRef parent = tree.getParent(node);
        for (int i = 0; i < tree.getChildCount(parent); ++i) {
            if (tree.getChild(parent, i).equals(node)) return i;
        }
        throw new RuntimeException(); // Should never be needed
    }

    public static final long
            extendedBinomialCoefficient(final int n, final int k) {
        if (n >= 0 && n < k)
            return 0;
        else
            return org.apache.commons.math.util
                    .MathUtils.binomialCoefficient(n, k);
    }

    public static final double
            extendedBinomialCoefficientDouble(final int n, final int k) {
        if (n >= 0 && n < k)
            return 0.0;
        else
            return org.apache.commons.math.util
                    .MathUtils.binomialCoefficientDouble(n, k);
    }


    public static final <T> T getRandomElement(final Collection<T> collection) {
        int i = 0;
        final int r = MathUtils.nextInt(collection.size());
        for (T element : collection)
            if (i++ == r) return element;
        throw new RuntimeException();
    }

    public static class RandomWeightedObject<T> {

        final Map<T,? extends Number> weights;
        final double sum;

        public RandomWeightedObject(Map<T,? extends Number> weights) {
            double sum = 0.0;
            for (T key : weights.keySet())
                sum += weights.get(key).doubleValue();
            this.sum = sum;
            this.weights = weights;
        }

        public final T nextObject() {
            double U = MathUtils.nextDouble() * sum;
            for (T key : weights.keySet()) {
                U -= weights.get(key).doubleValue();
                if (U < 0.0) return key;
            }
            throw new RuntimeException();
        }

    }

    public static final <T> T
            nextWeightedObject(final Map<T,? extends Number> weights) {

        return new RandomWeightedObject<T>(weights).nextObject();
    }

    public static final class RandomWeightedInteger {

        final double[] weights;
        final double sum;

        public RandomWeightedInteger(final double...weights) {
            sum = MathUtils.getTotal(weights);
            this.weights = weights;
        }

        public final int nextInt() {
            double U = MathUtils.nextDouble() * sum;
            int i;
            for (i = 0; i < weights.length; ++i) {
                U -= weights[i];
                if (U < 0.0) return i;
            }
            throw new RuntimeException();
        }

    }

    public static final int nextWeightedInteger(final double...weights) {
        return MathUtils.randomChoice(weights);
    }

    public static final double nextPoissonTime(final double...lambdas) {
        final double lambda = MathUtils.getTotal(lambdas);
        return MathUtils.nextExponential(lambda);
    }

    public static boolean nextBoolean(final double p) {
        return MathUtils.nextDouble() < p;
    }

}
