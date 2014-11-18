/**
 * SimpleCophylogeneticTrajectoryState.java
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import cophy.model.CophylogenyModel;
import cophy.simulation.CophylogeneticEvent.CophylogeneticEventFailedException;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class SimpleCophylogeneticTrajectoryState implements MutableCophylogeneticTrajectoryState {

    protected double height;
    protected final Table<NodeRef,NodeRef,Integer> state;
    private final Set<MutableCophylogeneticTrajectoryStateListener> listeners;

    public SimpleCophylogeneticTrajectoryState(final Tree guestTree, final CophylogenyModel model) {
        final int guestLeafCount = guestTree.getExternalNodeCount();
        final int hostLeafCount = model.getHostTree().getExternalNodeCount();
        state = HashBasedTable.create(guestLeafCount, hostLeafCount);
        reset(guestTree, model);
        listeners = new HashSet<MutableCophylogeneticTrajectoryStateListener>();
    }

    protected SimpleCophylogeneticTrajectoryState(final double height, final Table<NodeRef,NodeRef,Integer> state, final Set<MutableCophylogeneticTrajectoryStateListener> listeners) {
        this.height = height;
        this.state = state;
        this.listeners = listeners;
    }

    @Override
    public Object copy() {
        final Table<NodeRef,NodeRef,Integer> stateCopy =
                HashBasedTable.create(state.rowKeySet().size(), state.columnKeySet().size());
        stateCopy.putAll(state);
        return new SimpleCophylogeneticTrajectoryState(height, stateCopy, new HashSet<MutableCophylogeneticTrajectoryStateListener>(listeners));
    }

    @Override
    public void reset(Tree guestTree, CophylogenyModel model) {
        state.clear();
        final NodeRef guestRoot = guestTree.getRoot();
        final NodeRef hostRoot = model.getHostTree().getRoot();
        setGuestCountAtHost(guestRoot, hostRoot, 1);
        setGuestCountAtHost(NULL_GUEST, hostRoot, 0);
        height = model.getOriginHeight();
    }

    @Override
    public void applyEvent(final CophylogeneticEvent event)
            throws CophylogeneticEventFailedException {

        setHeight(event.getHeight());
        if (height < 0)
            throw new CophylogeneticEventFailedException(event);

        event.apply(this);

    }

    @Override
    public int getTotalGuestCount() {
        int total = 0;
        for (int count : state.values()) total += count;
        return total;
    }

    @Override
    public int getGuestCount(final NodeRef guest) {
        int count = 0;
        for (final NodeRef host : getHosts()) count += state.get(guest, host);
        return count;
    }

    @Override
    public Map<NodeRef,Integer> getGuestCountAtHosts(final NodeRef guest) {
        return state.row(guest);
    }

    @Override
    public Map<NodeRef,Integer> getGuestCounts() {
        final Map<NodeRef,Integer> counts = new HashMap<NodeRef, Integer>(getGuests().size());
        for (final NodeRef guest : getGuests())
            counts.put(guest, getGuestCount(guest));
        return counts;
    }

    @Override
    public int getGuestCountAtHost(final NodeRef guest, final NodeRef host) {
        return state.get(guest, host);
    }

    @Override
    public Map<NodeRef,Integer> getGuestCountsAtHost(final NodeRef host) {
        return state.column(host);
    }

    @Override
    public int getHostCount() {
        return state.size();
    }

    @Override
    public Set<NodeRef> getGuests() {
        return state.rowKeySet();
    }

    @Override
    public Set<NodeRef> getHosts() {
        return state.columnKeySet();
    }

    @Override
    public void setGuestCountAtHost(final NodeRef guest,
                                    final NodeRef host,
                                    final int guestCount) {
        state.put(guest, host, guestCount);
    }

    @Override
    public void setGuestCountsAtHost(final NodeRef host, final Map<NodeRef,Integer> guestCounts) {
        for (final NodeRef guest : guestCounts.keySet())
            state.put(guest, host, guestCounts.get(guest));
    }

    @Override
    public void incrementGuestCountAtHost(final NodeRef guest,
                                          final NodeRef host) {
        setGuestCountAtHost(guest, host, getGuestCountAtHost(guest, host) + 1);
    }

    @Override
    public void decrementGuestCountAtHost(final NodeRef guest,
                                          final NodeRef host)
            throws InvalidCophylogeneticTrajectoryStateException {

        final int guestCount = getGuestCountAtHost(guest, host) - 1;
        if (guestCount < 0)
            throw new InvalidCophylogeneticTrajectoryStateException("Cannot have a non-positive number of guests.");
        else
            setGuestCountAtHost(guest, host, guestCount);
    }

    @Override
    public void addGuest(final NodeRef guest) {
        for (final NodeRef host : getHosts())
            state.put(guest, host, 0);
    }

    @Override
    public void removeGuest(final NodeRef guest) {
        for (final NodeRef host : getHosts())
            state.remove(guest, host);
    }

    @Override
    public void addHost(final NodeRef host) {
        for (final NodeRef guest : getGuests())
            state.put(guest, host, 0);
    }

    @Override
    public void removeHost(final NodeRef host) {
        for (final NodeRef guest : getGuests())
            state.remove(guest, host);
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setHeight(final double height) {
        this.height = height;
    }

    @Override
    public void addListener(final MutableCophylogeneticTrajectoryStateListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final MutableCophylogeneticTrajectoryStateListener listener) {
        listeners.remove(listener);
    }

}
