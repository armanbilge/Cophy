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

package cophy.simulation;

import cophy.particlefiltration.Copyable;
import dr.evolution.tree.NodeRef;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public interface CophylogeneticTrajectoryState extends Copyable {

    int getGuestCountAtHost(NodeRef guest, NodeRef host);

    Map<NodeRef,Integer> getGuestCountsAtHost(NodeRef host);

    int getTotalGuestCount();

    int getGuestCount(NodeRef guest);

    Map<NodeRef,Integer> getGuestCountAtHosts(NodeRef guest);

    Map<NodeRef,Integer> getGuestCounts();

    int getHostCount();

    Set<NodeRef> getGuests();

    Set<NodeRef> getHosts();

    double getHeight();

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

}
