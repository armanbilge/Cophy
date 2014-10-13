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

import java.util.Set;

import cophy.particlefiltration.Copyable;
import dr.evolution.tree.NodeRef;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public interface CophylogeneticTrajectoryState extends Copyable {

    public int getGuestCountAtHost(final NodeRef guest, final NodeRef host);

    public int getTotalGuestCount();

    public int getHostCount();

    public Set<NodeRef> getHosts();

    public double getHeight();

    public NodeRef getRandomWeightedHost();

}
