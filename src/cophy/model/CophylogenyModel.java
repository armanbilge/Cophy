/*
 * CophylogenyModel.java
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

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Units;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class CophylogenyModel extends AbstractModel implements Units {

    private static final long serialVersionUID = -2874567072654237379L;

    protected final Tree hostTree;
    protected Units.Type units;

    public CophylogenyModel(final String name,
                            final Tree hostTree,
                            final Units.Type units) {
        super(name);
        this.hostTree = hostTree;
        if (hostTree instanceof Model)
            addModel((Model) hostTree);
        this.units = units;
    }

    public Tree getHostTree() {
        return hostTree;
    }

    @Override
    public Units.Type getUnits() {
        return units;
    }

    @Override
    public void setUnits(Units.Type units) {
        this.units = units;
    }

    public abstract double getOriginHeight();

    public double getSamplingProbability(final NodeRef host) {
        return 1.0;
    }

}
