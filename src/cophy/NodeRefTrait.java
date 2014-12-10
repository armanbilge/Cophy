/*
 * NodeRefTrait.java
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

package cophy;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeTrait;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class NodeRefTrait implements TreeTrait<NodeRef> {

    final String traitName;
    public NodeRefTrait(final String traitName) {
        this.traitName = traitName;
    }

    @Override
    public String getTraitName() {
        return traitName;
    }

    @Override
    public TreeTrait.Intent getIntent() {
        return Intent.NODE;
    }

    @Override
    public Class<NodeRef> getTraitClass() {
        return NodeRef.class;
    }

    @Override
    public String getTraitString(Tree tree, NodeRef node) {
        return Integer.toString(getTrait(tree, node).getNumber());
    }

    @Override
    public boolean getLoggable() {
        return true;
    }

}
