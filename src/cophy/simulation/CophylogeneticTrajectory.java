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

import cophy.particlefiltration.Copyable;
import cophy.simulation.CophylogeneticEvent.CospeciationEvent;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
@Deprecated
public class CophylogeneticTrajectory implements Copyable {

    protected final Tree guestTree;
    protected final Tree hostTree;
    protected final SimpleCophylogeneticTrajectoryState state;
    protected final TreeMap<Double,CophylogeneticEvent> events;

    public CophylogeneticTrajectory(final double originHeight,
                                    final Tree guestTree,
                                    final Tree hostTree) {
        this.guestTree = guestTree;
        this.hostTree = hostTree;
        state = new SimpleCophylogeneticTrajectoryState(originHeight, guestTree, hostTree);
        events = new TreeMap<Double,CophylogeneticEvent>();
        for (int i = 0; i < hostTree.getInternalNodeCount(); ++i) {
            final NodeRef node = hostTree.getInternalNode(i);
            final double height = hostTree.getNodeHeight(node);
            addEvent(new CospeciationEvent(hostTree, node, height));
        }
    }

    public void addEvent(final CophylogeneticEvent event) {
        events.put(event.getHeight(), event);
    }

    public void addAndApplyEvent(final CophylogeneticEvent event) {
        addEvent(event);
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
        double height = state.getHeight();
        Entry<Double,CophylogeneticEvent> entry = events.lowerEntry(height);
        while (entry != null) {
            if (entry.getValue() instanceof CospeciationEvent)
                return (CospeciationEvent) entry.getValue();
            height = entry.getKey();
            entry = events.lowerEntry(height);
        }
        return null;
    }

    public void applyNextCospeciationEvent() {
        final CospeciationEvent event = getNextCospeciationEvent();
        if (event == null) throw new RuntimeException("No more cospeciations.");
        state.applyEvent(event);
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
        return events.lowerKey(state.getHeight()) != null;
    }

    @Override
    public CophylogeneticTrajectory copy() {
        final CophylogeneticTrajectory copy =
                new CophylogeneticTrajectory(state.originHeight, guestTree, hostTree);
        copy.events.putAll(events);
        return copy;
    }

}
