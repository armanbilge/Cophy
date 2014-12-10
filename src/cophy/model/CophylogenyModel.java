/*
 * CophylogenyModel.java
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
