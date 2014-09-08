/**
 * AbstractParticle.java
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

package cophy.particlefiltration;

import java.util.HashMap;
import java.util.Map;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

/**
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class AbstractParticle<T> implements Copyable {

    protected final T value;
    protected double weight;
        
    public AbstractParticle(final T value) {
        this(value, 1.0);
    }
    
    public AbstractParticle(final T value, final double weight) {
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
        
        public Particle(final T value, final double weight) {
            super(value, weight);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Particle<T> copy() {
            return new Particle<T>((T) value.copy(), weight);
        }
        
    }
    
    public static class TreeParticle extends AbstractParticle<Tree> {
        
        protected final Map<NodeRef,NodeRef> nodeMap;
        
        {
            nodeMap = new HashMap<NodeRef,NodeRef>();
        }
        
        public TreeParticle(final Tree value) {
            super(value);
        }
        
        public TreeParticle(final Tree value, final double weight) {
            super(value, weight);
        }
        
        public void setReconstructedNode(final NodeRef completeNode,
                                         final NodeRef reconstructedNode) {
            nodeMap.put(completeNode, reconstructedNode);
        }
        
        @Override
        public TreeParticle copy() {
            final TreeParticle copy = new TreeParticle(value.getCopy(), weight);
            copy.nodeMap.putAll(nodeMap);
            return copy;
        }
        
    }
    
}
