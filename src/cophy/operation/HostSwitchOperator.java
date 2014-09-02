/**
 * HostSwitchOperator.java
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

package cophy.operation;

import java.util.Set;

import cophy.CophylogenyUtils;
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
                CophylogenyUtils.getLineagesAtHeight(hostTree, newHeight);
        final NodeRef newHost =
                CophylogenyUtils.getRandomElement(potentialHosts);
        
        guestTree.setNodeHeight(guestNode, newHeight);
        reconciliation.setHost(guestNode, newHost);

        final int inversePotentialHostCount =
                CophylogenyUtils.getLineageCountAtHeight(hostTree, oldHeight);
        
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
                        new ElementRule(GUEST, new XMLSyntaxRule[]{
                                new ElementRule(MutableTree.class)}),
                        new ElementRule(HOST, new XMLSyntaxRule[]{
                                new ElementRule(Tree.class)}),
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
                public Class<HostSwitchOperator> getReturnType() {
                    return HostSwitchOperator.class;
                }
        
    };

    
}
