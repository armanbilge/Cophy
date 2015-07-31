/*
 * CospeciationOperator.java
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
import dr.inference.model.Parameter;
import dr.inference.operators.OperatorFailedException;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.MathUtils;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

public class CospeciationOperator extends SimpleMCMCOperator {

    private final static String COSPECIATION_OPERATOR = "cospeciationOperator";

    protected final MutableTree guestTree;
    protected final Tree hostTree;
    protected final Reconciliation reconciliation;
    protected final Parameter originHeightParameter;

    public CospeciationOperator(final MutableTree guestTree,
                                final Tree hostTree,
                                final Reconciliation reconciliation,
                                final Parameter originHeightParameter,
                                final double weight) {
        this.guestTree = guestTree;
        this.hostTree = hostTree;
        this.reconciliation = reconciliation;
        this.originHeightParameter = originHeightParameter;
        setWeight(weight);
    }

    @Override
    public String getPerformanceSuggestion() {
        return "No performance suggestion.";
    }

    @Override
    public String getOperatorName() {
        return COSPECIATION_OPERATOR + "(" + guestTree.getId() + ")";
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        final double originHeight = originHeightParameter.getValue(0);

        final int r = MathUtils.nextInt(guestTree.getInternalNodeCount());
        final NodeRef guestNode = guestTree.getInternalNode(r);
        final NodeRef hostNode = reconciliation.getHost(guestNode);
        if (hostTree.isExternal(hostNode))
            throw new OperatorFailedException("No change in state.");
        final double hostHeight = hostTree.getNodeHeight(hostNode);

        final double leftChildHeight = guestTree.getNodeHeight(
                guestTree.getChild(guestNode, 0));
        final double rightChildHeight = guestTree.getNodeHeight(
                guestTree.getChild(guestNode, 1));
        if (hostHeight <= Math.max(leftChildHeight, rightChildHeight))
            throw new OperatorFailedException("No change in state.");

        final double upperHeightEmbedded = guestTree.isRoot(guestNode)
                ? originHeight :
                    guestTree.getNodeHeight(guestTree.getParent(guestNode));
        final double upperHeightHost = hostTree.isRoot(hostNode)
                ? originHeight :
                    hostTree.getNodeHeight(hostTree.getParent(hostNode));
        final double upperHeight =
                Math.min(upperHeightEmbedded, upperHeightHost);
        final double range = upperHeight - hostHeight;

        double newHeight;
        double logHastingsRatio = Math.log(range);
        if (guestTree.getNodeHeight(guestNode) == hostHeight) {
            newHeight = MathUtils.nextDouble() * range + hostHeight;
            logHastingsRatio *= -1;
        } else {
            newHeight = hostHeight;
        }

        guestTree.setNodeHeight(guestNode, newHeight);
        return logHastingsRatio;

    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String GUEST = "guest";
                private static final String HOST = "host";

                @Override
                public String getParserName() {
                    return COSPECIATION_OPERATOR;
                }

                @Override
                public Object parseXMLObject(final XMLObject xo)
                        throws XMLParseException {

                    final MutableTree guestTree =
                            (MutableTree) xo.getChild(GUEST)
                            .getChild(MutableTree.class);
                    final Tree hostTree =
                            (Tree) xo.getChild(HOST).getChild(Tree.class);
                    final Reconciliation reconciliation =
                            (Reconciliation) xo.getChild(Reconciliation.class);
                    final Parameter originHeightParameter =
                            (Parameter) xo.getChild(Parameter.class);
                    final double weight = xo.getDoubleAttribute(WEIGHT);

                    return new CospeciationOperator(guestTree,
                                                    hostTree,
                                                    reconciliation,
                                                    originHeightParameter,
                                                    weight);
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(GUEST, MutableTree.class),
                        new ElementRule(HOST, Tree.class),
                        new ElementRule(Reconciliation.class),
                        new ElementRule(Parameter.class),
                        AttributeRule.newDoubleRule(WEIGHT)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "An operator that adds/removes cospeciation events.";
                }

                @Override
                public Class<CospeciationOperator> getReturnType() {
                    return CospeciationOperator.class;
                }

    };

}
