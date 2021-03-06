/*
 * LeafHostSwitchOperator.java
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

package cophy.operation;

import cophy.model.Reconciliation;
import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.operators.OperatorFailedException;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.MathUtils;
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
public class LeafHostSwitchOperator extends SimpleMCMCOperator {

    private static final String LEAF_HOST_SWITCH_OPERATOR =
            "leafHostSwitchOperator";

    protected final Tree guestTree;
    protected final Tree hostTree;
    protected final Reconciliation reconciliation;

    public LeafHostSwitchOperator(final Tree guestTree,
                                  final Tree hostTree,
                                  final Reconciliation reconciliation,
                                  final double weight) {

        this.guestTree = guestTree;
        this.hostTree = hostTree;
        this.reconciliation = reconciliation;
        setWeight(weight);

    }

    @Override
    public String getPerformanceSuggestion() {
        return "No performance suggestion.";
    }

    @Override
    public String getOperatorName() {
        return LEAF_HOST_SWITCH_OPERATOR + "(" + reconciliation.getId() + ")";
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        final int r = MathUtils.nextInt(guestTree.getExternalNodeCount());
        final NodeRef guestNode = guestTree.getExternalNode(r);

        final NodeRef hostNode = reconciliation.getHost(guestNode);

        final int s = MathUtils.nextInt(hostTree.getExternalNodeCount());
        final NodeRef newHostNode = hostTree.getExternalNode(s);

        if (hostNode.equals(newHostNode))
            throw new OperatorFailedException("No change in state.");

        reconciliation.setHost(guestNode, hostNode);

        return 0.0;

    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String GUEST = "guest";
                private static final String HOST = "host";

                @Override
                public String getParserName() {
                    return LEAF_HOST_SWITCH_OPERATOR;
                }

                @Override
                public Object parseXMLObject(final XMLObject xo)
                        throws XMLParseException {

                    final Tree guestTree =
                            (Tree) xo.getChild(GUEST)
                            .getChild(MutableTree.class);
                    final Tree hostTree =
                            (Tree) xo.getChild(HOST).getChild(Tree.class);
                    final Reconciliation reconciliation =
                            (Reconciliation) xo.getChild(Reconciliation.class);
                    final double weight = xo.getDoubleAttribute(WEIGHT);

                    return new LeafHostSwitchOperator(guestTree,
                                                     hostTree,
                                                     reconciliation,
                                                     weight);
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(GUEST, Tree.class),
                        new ElementRule(HOST, Tree.class),
                        new ElementRule(Reconciliation.class),
                        AttributeRule.newDoubleRule(WEIGHT)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "An operator that switches the hosts of guests.";
                }

                @Override
                public Class<LeafHostSwitchOperator> getReturnType() {
                    return LeafHostSwitchOperator.class;
                }

    };


}
