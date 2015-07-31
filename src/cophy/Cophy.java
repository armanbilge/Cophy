/*
 * Cophy.java
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

package cophy;

import cophy.dhsl.DHSLModel;
import cophy.dhsl.DHSLSimulator;
import cophy.model.Reconciliation;
import cophy.model.CophylogenyLikelihood;
import cophy.operation.CospeciationOperator;
import cophy.operation.HostSwitchOperator;
import cophy.operation.LeafHostSwitchOperator;
import dr.app.plugin.Plugin;
import dr.xml.XMLObjectParser;

import java.util.HashSet;
import java.util.Set;

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
        parsers.add(CophylogenyLikelihood.PARSER);

        // DHSL Model
        parsers.add(DHSLModel.PARSER);
        parsers.add(DHSLSimulator.PARSER);

    }

    @Override
    public Set<XMLObjectParser> getParsers() {
        return parsers;
    }

}
