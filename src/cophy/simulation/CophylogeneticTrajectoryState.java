/**
 * CophylogeneticTrajectoryState.java
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
import dr.evolution.tree.NodeRef;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public interface CophylogeneticTrajectoryState extends Copyable {

    public int getGuestCountAtHost(NodeRef guest, NodeRef host);

    public Map<NodeRef,Integer> getGuestCountsAtHost(NodeRef host);

    public int getTotalGuestCount();

    public int getGuestCount(NodeRef guest);

    public Map<NodeRef,Integer> getGuestCounts();

    public int getHostCount();

    public Set<NodeRef> getGuests();

    public Set<NodeRef> getHosts();

    public double getHeight();

    public NodeRef getRandomWeightedHost();

}
