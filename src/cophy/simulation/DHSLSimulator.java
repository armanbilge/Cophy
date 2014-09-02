/**
 * DHSLSimulator.java
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

package cophy.simulation;

import java.util.Set;

import cophy.CophylogenyUtils;
import cophy.model.DHSLModel;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.SimpleNode;
import dr.evolution.tree.SimpleTree;
import dr.evolution.tree.Tree;
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
public class DHSLSimulator extends CophylogenySimulator<DHSLModel> {
        
    public DHSLSimulator(final DHSLModel model, final boolean complete) {
        
        super(model, complete);
        
    }
    
    @Override
    public Tree simulateTree() {
        return simulateTree(0.0);
    }
    
    @Override
    public Tree simulateTree(final double until) {
        
        final NodeRef hostRoot = model.getHostTree().getRoot();
        final double originHeight = model.getOriginHeight();
        final SimpleNode root = simulateSubtree(hostRoot, originHeight, until);
        return new SimpleTree(root);
    }
        
    @Override
    protected SimpleNode simulateSubtree(final SimpleNode guestNode,
                                         final NodeRef hostNode,
                                         double height,
                                         final double until) {

        final Tree hostTree = model.getHostTree();
        final double duplicationRate = model.getDuplicationRate();
        final double hostSwitchRate = model.getHostSwitchRate();
        final double lossRate = model.getLossRate();
        
        guestNode.setAttribute(HOST, hostNode);
        
        final SimpleNode left, right;
        
        final int event;
        if (hostTree.isRoot(hostNode)) { // No host-switching at root
            event = CophylogenyUtils.nextWeightedInteger(duplicationRate,
                                                         lossRate);
            height -= CophylogenyUtils.nextPoissonTime(duplicationRate,
                                                       lossRate);
        } else {
            event = CophylogenyUtils.nextWeightedInteger(duplicationRate,
                                                         lossRate,
                                                         hostSwitchRate);
            height -= CophylogenyUtils.nextPoissonTime(duplicationRate,
                                                       lossRate,
                                                       hostSwitchRate);
        }
        
        NodeRef leftHost = null;
        NodeRef rightHost = null;
        final double hostHeight = hostTree.getNodeHeight(hostNode);
        if (hostHeight > height) {
            
            height = hostHeight;
            if (hostTree.isExternal(hostNode)) {
                guestNode.setHeight(height);
                return guestNode;
            }
            
        } else if (until > height) {
            
            guestNode.setHeight(until);
            return guestNode;
            
        } else {
            
            switch(event) {
            case 0: // Duplication event
                leftHost = hostNode;
                rightHost = hostNode;
                break;
            case 1: // Loss event
                if (!complete) return null;
                guestNode.setAttribute(EXTINCT, true);
                break;
            case 2: // Host-switch event
                final NodeRef[] hosts = new NodeRef[2];
                
                hosts[0] = hostNode;
                final Set<NodeRef> potentialHosts =
                        CophylogenyUtils.getLineagesAtHeight(hostTree, height);
                potentialHosts.remove(hostNode);
                hosts[1]  = CophylogenyUtils.getRandomElement(potentialHosts);
                
                final int r = MathUtils.nextInt(2);
                leftHost = hosts[r];
                rightHost = hosts[1 - r];
                break;
            default: // Should not be needed.
                throw new RuntimeException("Unknown event type");
            }   
        }
        
        if (leftHost == null) {
            left = null;
            right = null;
        } else {
            left = simulateSubtree(leftHost, height, until);
            right = simulateSubtree(rightHost, height, until);
        }
        
        guestNode.setHeight(height);
        
        if (!complete) {
            final boolean leftIsNull = (left == null);
            final boolean rightIsNull = (right == null);
            if (leftIsNull && rightIsNull) { // Entire lineage was lost
                return null;
            } else if (leftIsNull || rightIsNull) { // One child lineage is lost
                return left != null ? left : right;
            }
        }
        
        guestNode.addChild(left);
        guestNode.addChild(right);
        
        return guestNode;

    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String DHSL_SIMULATOR = "DHSLSimulator";
                private static final String COMPLETE_HISTORY =
                        "completeHistory";
                
                @Override
                public String getParserName() {
                    return DHSL_SIMULATOR;
                }

                @Override
                public Object parseXMLObject(final XMLObject xo)
                        throws XMLParseException {
                    
                    final DHSLModel model =
                            (DHSLModel) xo.getChild(DHSLModel.class);
                    final boolean complete =
                            xo.getBooleanAttribute(COMPLETE_HISTORY);
                    
                    return new DHSLSimulator(model, complete);
                    
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(DHSLModel.class),
                        AttributeRule.newBooleanRule(COMPLETE_HISTORY)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "Simulator for the duplication, host-switch, loss "
                            + "cophylogeny model.";
                }

                @Override
                public Class<DHSLSimulator> getReturnType() {
                    return DHSLSimulator.class;
                }
        
    };
}
