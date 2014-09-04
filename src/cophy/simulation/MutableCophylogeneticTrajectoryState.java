/**
 * MutableCophylogeneticTrajectoryState.java
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

import java.util.HashMap;
import java.util.Map;

import cophy.CophylogenyUtils;
import cophy.simulation.CophylogeneticEvent.CophylogeneticEventFailedException;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

/**
 * 
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class MutableCophylogeneticTrajectoryState
        implements CophylogeneticTrajectoryState {

    protected final double originHeight;
    protected final Tree hostTree;
    protected double currentHeight;
    protected final Map<NodeRef,Integer> currentState;
    
    public MutableCophylogeneticTrajectoryState(final double originHeight,
                                                final Tree hostTree) {
        this.originHeight = originHeight;
        this.hostTree = hostTree;
        final int leafCount = hostTree.getExternalNodeCount();
        currentState = new HashMap<NodeRef,Integer>(leafCount);
    }
    
    public void reset() {
        currentState.clear();
        setGuestCountAtHost(hostTree.getRoot(), 1);
        currentHeight = originHeight;
    }
    
    public void applyEvent(final CophylogeneticEvent event)
            throws CophylogeneticEventFailedException {
        
        currentHeight = event.getHeight();
        if (currentHeight < 0)
            throw new CophylogeneticEventFailedException(event);

        event.apply(this);
        
        final int lineageCount = CophylogenyUtils
                .getLineageCountAtHeight(hostTree, currentHeight);
        if (lineageCount != currentState.size())
            throw new CophylogeneticEventFailedException(event);
        
    }
 
    @Override
    public int getGuestCountAtHost(final NodeRef host) {
        return currentState.get(host);
    }
    
    public void setGuestCountAtHost(final NodeRef host, final int guestCount) {
        currentState.put(host, guestCount);
    }
    
    public void incrementGuestCountAtHost(final NodeRef host) {
        setGuestCountAtHost(host, getGuestCountAtHost(host) + 1);
    }
    
    public void decrementGuestCountAtHost(final NodeRef host)
            throws CophylogeneticEventFailedException {
        
        final int guestCount = getGuestCountAtHost(host) - 1;
        if (guestCount < 0)
            throw new CophylogeneticEventFailedException();
        else
            setGuestCountAtHost(host, guestCount);
    }
    
    public void removeHost(final NodeRef host) {
        currentState.remove(host);
    }
    
    public void addHost(final NodeRef host, final int guestCount) {
        setGuestCountAtHost(host, guestCount);
    }
    
    @Override
    public double getCurrentHeight() {
        return currentHeight;
    }
    
}
