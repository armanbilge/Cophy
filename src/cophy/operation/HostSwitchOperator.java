/*
 * HostSwitchOperator.java
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

import cophy.CophyUtils;
import cophy.model.Reconciliation;
import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.model.Parameter;
import dr.inference.operators.OperatorFailedException;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.MathUtils;
import dr.xml.*;

import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class HostSwitchOperator extends SimpleMCMCOperator {

    protected static final String HOST_SWITCH_OPERATOR = "hostSwitchOperator";

    protected final MutableTree guestTree;
    protected final Tree hostTree;
    protected final Reconciliation reconciliation;
    protected final Parameter originHeightParameter;

    public HostSwitchOperator(final MutableTree guestTree,
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
        return HOST_SWITCH_OPERATOR + "(" + guestTree.getId() + ")";
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        final double originHeight = originHeightParameter.getValue(0);

        final int r = MathUtils.nextInt(guestTree.getInternalNodeCount());
        final NodeRef guestNode = guestTree.getInternalNode(r);

        if (guestTree.getNodeHeight(guestNode)
                == hostTree.getNodeHeight(reconciliation.getHost(guestNode)))
            throw new OperatorFailedException("No change in state.");

        final double leftChildHeight = guestTree.getNodeHeight(
                guestTree.getChild(guestNode, 0));
        final double rightChildHeight = guestTree.getNodeHeight(
                guestTree.getChild(guestNode, 1));
        final double lower = Math.max(leftChildHeight, rightChildHeight);
        final double upper = guestTree.isRoot(guestNode)  ? originHeight :
                    guestTree.getNodeHeight(guestTree.getParent(guestNode));
        final double range = upper - lower;

        final double oldHeight = guestTree.getNodeHeight(guestNode);
        final double newHeight = MathUtils.nextDouble() * range + lower;

        final Set<NodeRef> potentialHosts =
                CophyUtils.getLineagesAtHeight(hostTree, newHeight);
        final NodeRef newHost =
                CophyUtils.getRandomElement(potentialHosts);

        guestTree.setNodeHeight(guestNode, newHeight);
        reconciliation.setHost(guestNode, newHost);

        final int inversePotentialHostCount =
                CophyUtils.getLineageCountAtHeight(hostTree, oldHeight);

        return Math.log(potentialHosts.size())
                - Math.log(inversePotentialHostCount);

    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String GUEST = "guest";
                private static final String HOST = "host";

                @Override
                public String getParserName() {
                    return HOST_SWITCH_OPERATOR;
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

                    return new HostSwitchOperator(guestTree,
                                                  hostTree,
                                                  reconciliation,
                                                  originHeightParameter,
                                                  weight);
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(GUEST, Tree.class),
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
                    return "An operator that switches the hosts of guests.";
                }

                @Override
                public Class<HostSwitchOperator> getReturnType() {
                    return HostSwitchOperator.class;
                }

    };


}
