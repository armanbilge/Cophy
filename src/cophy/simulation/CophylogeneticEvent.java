/**
 * CophylogeneticEvent.java
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

package cophy.simulation;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

/**
 * 
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class CophylogeneticEvent {

    protected final String eventName;
    protected final double eventHeight;
    
    public CophylogeneticEvent(final String eventName,
                               final double eventHeight) {
        this.eventName = eventName;
        this.eventHeight = eventHeight;
    }
    
    public String getName() {
        return eventName;
    }
    
    public double getHeight() {
        return eventHeight;
    }
    
    protected abstract void apply(final MutableCophylogeneticTrajectoryState state)
            throws CophylogeneticEventFailedException;
    
    public static final class CospeciationEvent extends CophylogeneticEvent {
        
        private static final String COSPECIATION_EVENT = "cospeciationEvent";
        protected final Tree tree;
        protected final NodeRef node;
        
        public CospeciationEvent(final Tree tree,
                                 final NodeRef node,
                                 final double eventHeight) {
            super(COSPECIATION_EVENT, eventHeight);
            this.tree = tree;
            this.node = node;
        }
        
        @Override
        public void apply(final MutableCophylogeneticTrajectoryState state) {
            
            final int guestCount = state.getGuestCountAtHost(node);
            state.removeHost(node);
            final NodeRef left = tree.getChild(node, 0);
            final NodeRef right = tree.getChild(node, 1);
            state.addHost(left, guestCount);
            state.addHost(right, guestCount);
            
        }

    }
    
    public static abstract class BirthEvent extends CophylogeneticEvent {

        final protected NodeRef node;
        
        public BirthEvent(final String eventName,
                          final double eventHeight,
                          final NodeRef node) {
            super(eventName, eventHeight);
            this.node = node;
        }
        
        @Override
        public void apply(final MutableCophylogeneticTrajectoryState state) {
            state.incrementGuestCountAtHost(node);
        }
        
    }

    public static abstract class DeathEvent extends CophylogeneticEvent {

        final protected NodeRef node;
        
        public DeathEvent(final String eventName,
                          final double eventHeight,
                          final NodeRef node) {
            super(eventName, eventHeight);
            this.node = node;
        }
        
        @Override
        public void apply(final MutableCophylogeneticTrajectoryState state)
                throws CophylogeneticEventFailedException {
            try {
                state.decrementGuestCountAtHost(node);
            } catch (CophylogeneticEventFailedException e) {
                throw new CophylogeneticEventFailedException(this);
            }
        }
        
    }
    
    public static class CophylogeneticEventFailedException
            extends RuntimeException {

        private static final long serialVersionUID = -1074026006660524662L;
        
        public CophylogeneticEventFailedException() {
            super("Event failed to make valid change to state");
        }
        
        public CophylogeneticEventFailedException(final
                                                  CophylogeneticEvent event) {
            super("Event " + event.getName()
                    + " failed to make valid change to state");
        }
        
    }
    
}
