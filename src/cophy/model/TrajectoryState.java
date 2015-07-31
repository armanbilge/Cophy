/*
 * CophylogeneticTrajectoryState.java
 *
 * Cophy: Cophylogenetics for BEAST
 *
 * Copyright (C) 2014 Arman Bilge <armanbilge@gmail.com>
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

package cophy.model;

import cophy.particlefiltration.Copyable;
import dr.evolution.tree.NodeRef;

import java.util.*;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class TrajectoryState implements Copyable {

    private Map<NodeRef,Integer> guestCounts;
    private int guestCount;
    private Map<NodeRef,NodeRef> guestLineageHosts;
    private double height;

    public TrajectoryState(final double origin, final NodeRef guest, final NodeRef host) {
        setHeight(origin);
        increment(host);
        setGuestLineageHost(guest, host);
    }

    private TrajectoryState() {}

    public double getHeight() {
        return height;
    }

    public void setHeight(final double height) {
        this.height = height;
    }

    public void forwardTime(final double time) {
        height -= time;
    }

    public int getHostCount() {
        return guestCounts.size();
    }

    public Set<NodeRef> getHosts() {
        return guestCounts.keySet();
    }

    public int getGuestCount() {
        return guestCount;
    }

    public Map<NodeRef,Integer> getGuestCounts() {
        return Collections.unmodifiableMap(guestCounts);
    }

    public int getGuestCount(final NodeRef host) {
        return guestCounts.containsKey(host) ? guestCounts.get(host) : 0;
    }

    public void setGuestCount(final NodeRef host, final int count) {
        guestCounts.put(host, count);
        guestCount += count;
    }

    public int removeGuests(final NodeRef host) {
        final int n = getGuestCount(host);
        guestCount -= n;
        guestCounts.remove(host);
        return n;
    }

    public void increment(final NodeRef host) {
        final int n = getGuestCount(host);
        guestCounts.put(host, n+1);
        ++guestCount;
    }

    public void decrement(final NodeRef host) {
        final int n = getGuestCount(host);
        if (n == 0)
            throw new InvalidTrajectoryException("Cannot have a negative number of guests.");
        guestCounts.put(host, n - 1);
        --guestCount;
    }

    public int getGuestLineageCount(final NodeRef host) {
        int count = 0;
        for (final Map.Entry<NodeRef,NodeRef> entry : guestLineageHosts.entrySet()) {
            if (entry.getValue().equals(host))
                ++count;
        }
        return count;
    }

    public Set<NodeRef> getGuestLineages(final NodeRef host) {
        final Set<NodeRef> lineages = new HashSet<NodeRef>();
        for (final Map.Entry<NodeRef,NodeRef> entry : guestLineageHosts.entrySet()) {
            if (entry.getValue().equals(host))
                lineages.add(entry.getKey());
        }
        return lineages;
    }

    public void setGuestLineageHost(final NodeRef guest, final NodeRef host) {
        guestLineageHosts.put(guest, host);
    }

    @Override
    public TrajectoryState copy() {
        final TrajectoryState copy = new TrajectoryState();
        copy.guestCounts = new HashMap<NodeRef,Integer>(guestCounts);
        copy.guestCount = guestCount;
        copy.guestLineageHosts = new HashMap<NodeRef,NodeRef>(guestLineageHosts);
        copy.height = height;
        return copy;
    }

}
