/*
 * Reconciliation.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cophy.CophyUtils;
import cophy.NodeRefTrait;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeTrait;
import dr.evolution.tree.TreeTraitProvider;
import dr.evolution.util.Taxon;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Variable;
import dr.inference.model.Variable.ChangeType;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class Reconciliation extends AbstractModel implements TreeTraitProvider {

    private static final long serialVersionUID = -8575817366502931528L;

    public static final String RECONCILIATION = "reconciliation";

    protected final Tree guestTree;
    protected final Tree hostTree;
    protected final String hostTraitName;
    protected boolean initialized = false;
    protected final boolean[] sampleable;
    protected int[] map;
    protected int[] storedMap;

    protected final TreeTraitProvider.Helper treeTraitProvider;

    {
        treeTraitProvider = new TreeTraitProvider.Helper();
    }

    public Reconciliation(final Tree guestTree,
                          final Tree hostTree,
                          final String hostTraitName) {

        super(RECONCILIATION);

        this.guestTree = guestTree;
        if (guestTree instanceof Model)
            addModel((Model) guestTree);

        this.hostTree = hostTree;
        if (hostTree instanceof Model)
            addModel((Model) hostTree);

        this.hostTraitName = hostTraitName;

        map = new int[guestTree.getNodeCount()];
        storedMap = new int[guestTree.getNodeCount()];

        sampleable = new boolean[guestTree.getNodeCount()];

        final NodeRefTrait trait = new NodeRefTrait(hostTraitName) {
            @Override
            public NodeRef getTrait(Tree tree, NodeRef node) {
                if (tree != guestTree)
                    throw new RuntimeException("Reconciliation " + getId()
                                               + " can be logged only on hostTree "
                                               + guestTree.getId() + ".");
                return getHost(node);
            }
        };

        treeTraitProvider.addTrait(trait);

    }

    public void initialize() {

        final Map<Taxon,NodeRef> hostTaxa2Nodes =
                new HashMap<Taxon,NodeRef>(hostTree.getTaxonCount());
        for (int i = 0; i < hostTree.getExternalNodeCount(); ++i) {
            final NodeRef hostNode = hostTree.getExternalNode(i);
            final Taxon hostTaxon = hostTree.getNodeTaxon(hostNode);
            hostTaxa2Nodes.put(hostTaxon, hostNode);
        }

        for (int i = 0; i < guestTree.getExternalNodeCount(); ++i) {
            final NodeRef guestNode = guestTree.getExternalNode(i);
            final Taxon guestTaxon = guestTree.getNodeTaxon(guestNode);
            final Taxon hostTaxon;
            try {
                hostTaxon = (Taxon) guestTaxon.getAttribute(hostTraitName);
            } catch (ClassCastException e) {
                throw new RuntimeException("Host for guest "
                                           + guestTaxon.getId()
                                           + " is not a taxon.");
            }

            if (hostTaxon == null)
                throw new RuntimeException("No host for guest "
                                            + guestTaxon.getId() + ".");

            final NodeRef hostNode = hostTaxa2Nodes.get(hostTaxon);
            setHost(guestNode, hostNode);
        }

        for (int i = 0; i < guestTree.getInternalNodeCount(); ++i) {
            final NodeRef guestNode = guestTree.getInternalNode(i);
            final double height = guestTree.getNodeHeight(guestNode);
            final Set<NodeRef> potentialHosts =
                    CophyUtils.getLineagesAtHeight(hostTree, height);
            final NodeRef hostNode =
                    CophyUtils.getRandomElement(potentialHosts);
            setHost(guestNode, hostNode);
        }

        for (int i = 0; i < sampleable.length; ++i)
            sampleable[i] = !guestTree.isExternal(guestTree.getNode(i));

        initialized = true;

    }

    public NodeRef getHost(final NodeRef guest) {
        return hostTree.getNode(map[guest.getNumber()]);
    }

    public void setHost(final NodeRef guest, final NodeRef host) {
        if (initialized && !sampleable[guest.getNumber()])
            throw new RuntimeException("Cannot set host for node "
                                       + guest + ".");
        map[guest.getNumber()] = host.getNumber();
        fireModelChanged();
    }

    @Override
    protected void handleModelChangedEvent(final Model model,
                                           final Object object,
                                           final int index) {
        // Nothing to do
    }

    @Override
    protected void handleVariableChangedEvent(@SuppressWarnings("rawtypes")
                                              final Variable variable,
                                              final int index,
                                              final ChangeType type) {
        // Nothing to do
    }

    @Override
    protected void storeState() {
        System.arraycopy(map, 0, storedMap, 0, map.length);
    }

    @Override
    protected void restoreState() {
        int[] temp = map;
        map = storedMap;
        storedMap = temp;
    }

    @Override
    protected void acceptState() {
        // Nothing to do
    }

    @SuppressWarnings("rawtypes")
    @Override
    public TreeTrait[] getTreeTraits() {
        return treeTraitProvider.getTreeTraits();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public TreeTrait getTreeTrait(String key) {
        return treeTraitProvider.getTreeTrait(key);
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String GUEST = "guest";
                private static final String HOST = "host";
                private static final String HOST_TRAIT_NAME = "hostTraitName";

                @Override
                public String getParserName() {
                    return RECONCILIATION;
                }

                @Override
                public Object parseXMLObject(final XMLObject xo)
                        throws XMLParseException {

                    final Tree guestTree =
                            (Tree) xo.getChild(GUEST).getChild(Tree.class);
                    final Tree hostTree =
                            (Tree) xo.getChild(HOST).getChild(Tree.class);
                    final String hostTraitName =
                            xo.getStringAttribute(HOST_TRAIT_NAME);

                    final Reconciliation reconciliation =
                            new Reconciliation(guestTree,
                                               hostTree,
                                               hostTraitName);

                    reconciliation.initialize();

                    return reconciliation;
                }


                private final XMLSyntaxRule[] rules = {
                        new ElementRule(GUEST, Tree.class),
                        new ElementRule(HOST, Tree.class),
                        AttributeRule.newStringRule(HOST_TRAIT_NAME)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "A mapping between two trees.";
                }

                @Override
                public Class<Reconciliation> getReturnType() {
                    return Reconciliation.class;
                }

    };

}
