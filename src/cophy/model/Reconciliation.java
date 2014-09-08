/**
 * Reconciliation.java
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

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Variable;
import dr.inference.model.Variable.ChangeType;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class Reconciliation extends AbstractModel {

    private static final long serialVersionUID = -8575817366502931528L;

    public static final String RECONCILIATION = "reconciliation";

    protected final Tree guestTree;
    protected final Tree hostTree;
    protected final boolean[] sampleable;
    protected int[] map;
    protected int[] storedMap;

    public Reconciliation(final Tree guestTree,
                          final Tree hostTree) {

        super(RECONCILIATION);
        this.guestTree = guestTree;
        if (guestTree instanceof Model)
            addModel((Model) guestTree);
        this.hostTree = hostTree;
        if (hostTree instanceof Model)
            addModel((Model) hostTree);
        map = new int[guestTree.getNodeCount()];
        sampleable = new boolean[guestTree.getNodeCount()];
        for (int i = 0; i < sampleable.length; ++i)
            sampleable[i] = !guestTree.isExternal(guestTree.getNode(i));

    }

    public NodeRef getHost(final NodeRef guest) {
        return hostTree.getNode(map[guest.getNumber()]);
    }

    public void setHost(final NodeRef guest, final NodeRef host) {
        if (!sampleable[guest.getNumber()])
            throw new RuntimeException("Cannot set host for node "
                                                                + guest + "!");
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

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String GUEST = "guest";
                private static final String HOST = "host";

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

                    return new Reconciliation(guestTree, hostTree);
                }


                private final XMLSyntaxRule[] rules = {
                        new ElementRule(GUEST, new XMLSyntaxRule[]{
                                new ElementRule(Tree.class)}),
                        new ElementRule(HOST, new XMLSyntaxRule[]{
                                new ElementRule(Tree.class)})
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
