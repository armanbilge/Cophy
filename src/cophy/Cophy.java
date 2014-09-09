/**
 * Cophy.java
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

package cophy;

import java.util.HashSet;
import java.util.Set;

import cophy.dhsl.DHSLModel;
import cophy.dhsl.DHSLSimulator;
import cophy.model.Reconciliation;
import cophy.operation.CospeciationOperator;
import cophy.operation.HostSwitchOperator;
import cophy.operation.LeafHostSwitchOperator;
import cophy.particlefiltration.PFCophylogenyLikelihood;
import dr.app.plugin.Plugin;
import dr.xml.XMLObjectParser;

/**
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class Cophy implements Plugin {

    private final Set<XMLObjectParser> parsers;
    {
        parsers = new HashSet<XMLObjectParser>();

        // Model
        parsers.add(Reconciliation.PARSER);

        // Operation
        parsers.add(CospeciationOperator.PARSER);
        parsers.add(HostSwitchOperator.PARSER);
        parsers.add(LeafHostSwitchOperator.PARSER);

        // Particle Filtration
        parsers.add(PFCophylogenyLikelihood.PARSER);

        // DHSL Model
        parsers.add(DHSLModel.PARSER);
        parsers.add(DHSLSimulator.PARSER);

    }

    @Override
    public Set<XMLObjectParser> getParsers() {
        return parsers;
    }

}
