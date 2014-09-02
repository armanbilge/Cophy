/**
 * AbstractCophylogenyModel.java
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

package cophy.model;

import dr.evolution.tree.Tree;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;

/**
 * 
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class AbstractCophylogenyModel extends AbstractModel {

    private static final long serialVersionUID = -2874567072654237379L;

    protected final Tree hostTree;
    
    public AbstractCophylogenyModel(final String name,
                                    final Tree hostTree) {
        super(name);
        this.hostTree = hostTree;
        if (hostTree instanceof Model)
            addModel((Model) hostTree);
    }

    public Tree getHostTree() {
        return hostTree;
    }
    
}
