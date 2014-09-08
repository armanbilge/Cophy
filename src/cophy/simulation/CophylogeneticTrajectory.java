/**
 * CophylogeneticTrajectory.java
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

import java.util.TreeMap;

import cophy.particlefiltration.Copyable;
import cophy.simulation.CophylogeneticEvent.CospeciationEvent;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class CophylogeneticTrajectory implements Copyable {

    protected final Tree hostTree;
    protected final MutableCophylogeneticTrajectoryState state;
    protected final TreeMap<Double,CophylogeneticEvent> events;

    public CophylogeneticTrajectory(final double originHeight,
                                    final Tree hostTree) {
        this.hostTree = hostTree;
        state = new MutableCophylogeneticTrajectoryState(originHeight, hostTree);
        events = new TreeMap<Double,CophylogeneticEvent>();
        for (int i = 0; i < hostTree.getInternalNodeCount(); ++i) {
            final NodeRef node = hostTree.getInternalNode(i);
            final double height = hostTree.getNodeHeight(node);
            addEvent(new CospeciationEvent(hostTree, node, height));
        }
    }

    public void addEvent(final CophylogeneticEvent event) {
        events.put(event.getHeight(), event);
        state.applyEvent(event);
    }

    public CophylogeneticTrajectoryState getCurrentState() {
        return state;
    }

    public CophylogeneticTrajectoryState getStateAtHeight(final double height) {

        state.reset();
        for (final double eventHeight : events.descendingKeySet()) {
            if (eventHeight < height) break;
            state.applyEvent(events.get(eventHeight));
        }

        return state;

    }

    public CospeciationEvent getNextCospeciationEvent() {
        final double height = state.getHeight();
        return (CospeciationEvent) events.lowerEntry(height).getValue();
    }

    public void applyNextCospeciationEvent() {
        state.applyEvent(getNextCospeciationEvent());
    }

    public CophylogeneticEvent peekNextEvent() {
        return events.lowerEntry(state.getHeight()).getValue();
    }

    public CophylogeneticEvent pollNextEvent() {
        final CophylogeneticEvent event = peekNextEvent();
        state.applyEvent(event);
        return event;
    }

    public CophylogeneticEvent getLastEvent() {
        return events.get(state.getHeight());
    }

    public boolean hasNextEvent() {
        return peekNextEvent() != null;
    }

    @Override
    public CophylogeneticTrajectory copy() {
        final CophylogeneticTrajectory copy =
                new CophylogeneticTrajectory(state.originHeight, hostTree);
        copy.events.putAll(events);
        return copy;
    }

}
