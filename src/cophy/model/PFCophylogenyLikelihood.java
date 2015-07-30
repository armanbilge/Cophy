/*
 * PFCophylogenyLikelihood.java
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

import cophy.CophyUtils;
import cophy.particlefiltration.PFLikelihood;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.model.CompoundModel;
import dr.inference.model.Model;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class PFCophylogenyLikelihood extends PFLikelihood {

    private static final long serialVersionUID = -3968617707110378864L;

    protected final CophylogenyModel cophylogenyModel;
    protected final Tree guestTree;
    protected final Tree hostTree;
    protected final Reconciliation reconciliation;

    public PFCophylogenyLikelihood(final
                                   CophylogenyModel cophylogenyModel,
                                   final Tree guestTree,
                                   final Reconciliation reconciliation) {

        super(new CompoundModel("CophylogenyModel"));

        final CompoundModel compoundModel = (CompoundModel) getWrappedModel();

        compoundModel.addModelListener(this);

        compoundModel.addModel(reconciliation);

        this.cophylogenyModel = cophylogenyModel;
        compoundModel.addModel(cophylogenyModel);

        this.guestTree = guestTree;
        if (guestTree instanceof Model)
            compoundModel.addModel((Model) guestTree);

        this.reconciliation = reconciliation;
        reconciliation.addModelListener(this);

        // Storing solely for convenience
        this.hostTree = cophylogenyModel.getHostTree();

    }

    @Override
    public final boolean evaluateEarly() {
        return true;
    }

    @Override
    public final double calculateLogLikelihood() {
        if (isValid())
            return calculateValidLogLikelihood();
        else
            return Double.NEGATIVE_INFINITY;
    }

    protected abstract double calculateValidLogLikelihood();

    private boolean isValid() {

        final double guestRootHeight =
                guestTree.getNodeHeight(guestTree.getRoot());
        final double hostRootHeight =
                hostTree.getNodeHeight(hostTree.getRoot());
        final double originHeight = cophylogenyModel.getOriginHeight();

        if (guestRootHeight >= originHeight || hostRootHeight >= originHeight)
            return false;

        for (int i = 0; i < guestTree.getNodeCount(); ++i) {
            final NodeRef guestNode = guestTree.getNode(i);
            final NodeRef hostNode = reconciliation.getHost(guestNode);
            final double height = guestTree.getNodeHeight(guestNode);
            final boolean valid =
                    CophyUtils.lineageExistedAtHeight(hostTree,
                                                      hostNode,
                                                      height);
            if (!valid) return false;
        }

        return true;
    }

}
