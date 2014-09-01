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

import java.util.HashSet;
import java.util.Set;

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

    
    public static final <T> T getRandomElement(Set<T> set) {
        int i = 0;
        final int r = MathUtils.nextInt(set.size());
        for (T element : set)
            if (i++ == r) return element;
        throw new RuntimeException();
    }
    
    public static final int nextWeightedInteger(final double...weights) {
        
        final double sum = MathUtils.getTotal(weights);
        
        final double[] normalizedCDF = new double[weights.length - 1];
        normalizedCDF[0] = weights[0] / sum;
        for (int i = 1; i < normalizedCDF.length; ++i)
            normalizedCDF[i] = weights[i] / sum + normalizedCDF[i - 1];
        
        final double U = 1 - MathUtils.nextDouble();        
        int i;
        for (i = 0; i < normalizedCDF.length && normalizedCDF[i] < U; ++i);
        return i;
        
    }
    
    public static final double nextPoissonTime(final double...lambdas) {
        final double lambda = MathUtils.getTotal(lambdas);
        return MathUtils.nextExponential(lambda);
    }
    
//    public static final <T> void resample(Particle<T>[] particles) {
//        
//        final double[] weightsCDF = new double[particles.length - 1];
//        @SuppressWarnings("unchecked")
//        final Particle<T>[] particlesCopy = new Particle[particles.length];
//        
//        double sum = 0.0;
//        for (int i = 0; i < particles.length - 1; ++i) {
//            sum += particles[i].getWeight();
//            weightsCDF[i] = sum;
//            particlesCopy[i] = particles[i];
//        }
//        
//        for (int i = 0; i < particles.length; ++i) {
//            final double U = 1 - Randomizer.nextDouble();        
//            int j;
//            for (j = 0; j < weightsCDF.length && weightsCDF[j] < U; ++j);
//            particles[i] = particlesCopy[j];
//        }
//                
//    }

    
}
