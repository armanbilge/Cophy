/*
 * TrajectoryState.java
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

    public NodeRef getGuestLineageHost(final NodeRef guest) {
        return guestLineageHosts.get(guest);
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
