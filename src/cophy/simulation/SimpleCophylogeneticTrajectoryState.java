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
import cophy.CophyUtils;
import cophy.simulation.CophylogeneticEvent.CophylogeneticEventFailedException;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class SimpleCophylogeneticTrajectoryState
        implements MutableCophylogeneticTrajectoryState {

    protected final double originHeight;
    protected final Tree guestTree;
    protected final Tree hostTree;
    protected double height;
    protected final Table<NodeRef,NodeRef,Integer> state;

    static final NodeRef NULL_GUEST = new NodeRef() {
        final private int number = -1;
        @Override
        public int getNumber() {
            return number;
        }
        @Override
        public void setNumber(int n) {
            throw new RuntimeException("Cannot set number.");
        }
    };

    public SimpleCophylogeneticTrajectoryState(final double originHeight,
                                               final Tree guestTree,
                                               final Tree hostTree) {
        this.originHeight = originHeight;
        this.guestTree = guestTree;
        this.hostTree = hostTree;
        final int guestLeafCount = guestTree.getExternalNodeCount();
        final int hostLeafCount = hostTree.getExternalNodeCount();
        state = HashBasedTable.create(guestLeafCount, hostLeafCount);
        reset();
    }

    @Override
    public Object copy() {
        final SimpleCophylogeneticTrajectoryState copy =
                new SimpleCophylogeneticTrajectoryState(originHeight,
                                                         guestTree,
                                                         hostTree);
        copy.height = height;
        copy.state.putAll(state);
        return copy;
    }

    public void reset() {
        state.clear();
        final NodeRef guestRoot = guestTree.getRoot();
        final NodeRef hostRoot = hostTree.getRoot();
        setGuestCountAtHost(guestRoot, hostRoot, 1);
        setGuestCountAtHost(NULL_GUEST, hostRoot, 0);
        height = originHeight;
    }

    public void applyEvent(final CophylogeneticEvent event)
            throws CophylogeneticEventFailedException {

        height = event.getHeight();
        if (height < 0)
            throw new CophylogeneticEventFailedException(event);

        event.apply(this);

        final int lineageCount = CophyUtils
                .getLineageCountAtHeight(hostTree, height);
        if (lineageCount != state.size())
            throw new CophylogeneticEventFailedException(event);

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

    public Map<NodeRef,Integer> getGuestCountsAtHost(final NodeRef host) {
        return state.column(host);
    }

    @Override
    public int getHostCount() {
        return state.size();
    }

    public Set<NodeRef> getGuests() {
        return state.rowKeySet();
    }

    @Override
    public Set<NodeRef> getHosts() {
        return state.columnKeySet();
    }

    public void setGuestCountAtHost(final NodeRef guest,
                                    final NodeRef host,
                                    final int guestCount) {
        state.put(guest, host, guestCount);
    }

    public void setGuestCountsAtHost(final NodeRef host, final Map<NodeRef,Integer> guestCounts) {
        for (final NodeRef guest : guestCounts.keySet())
            state.put(guest, host, guestCounts.get(guest));
    }

    public void incrementGuestCountAtHost(final NodeRef guest,
                                          final NodeRef host) {
        setGuestCountAtHost(guest, host, getGuestCountAtHost(guest, host) + 1);
    }

    public void decrementGuestCountAtHost(final NodeRef guest,
                                          final NodeRef host)
            throws CophylogeneticEventFailedException {

        final int guestCount = getGuestCountAtHost(guest, host) - 1;
        if (guestCount < 0)
            throw new CophylogeneticEventFailedException();
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

}
