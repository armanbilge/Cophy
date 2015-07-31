/*
 * PFCophylogenyLikelihood.java
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
