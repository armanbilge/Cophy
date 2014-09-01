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

import cophy.model.DHSLModel;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.SimpleNode;
import dr.evolution.tree.SimpleTree;
import dr.evolution.tree.Tree;
import dr.inference.model.Parameter;
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
public class DHSLSimulator {

    public static final String HOST = "host";
    private static final String EXTINCT = "extinct";
    
    protected final DHSLModel dhslModel;
    protected final boolean complete;
    
    public DHSLSimulator(final DHSLModel dhslModel, final boolean complete) {
        
        this.dhslModel = dhslModel;
        this.complete = complete;
        
    }
    
    public Tree simulateTree() {
        return simulateTree(0.0);
    }
    
    public Tree simulateTree(final double until) {
        
        final double originHeight = originHeightParameter.getValue(0);
        final SimpleNode root = simulateSubtree(hostTree.getRoot(),
                                                originHeight,
                                                until);
        return new SimpleTree(root);
    }
    
    private SimpleNode simulateSubtree(final NodeRef hostNode,
                                       final double height,
                                       final double until) {

        return simulateSubtree(new SimpleNode(), hostNode, height, until);
    
    }

    private SimpleNode simulateSubtree(final SimpleNode guestNode,
                                       final NodeRef hostNode,
                                       final double height,
                                       final double until) {

        final double lambda = birthDiffRateParameter.getParameterValue(0);
        
        guestNode.setAttribute(HOST, hostNode);
        
        final SimpleNode left;
        final SimpleNode right;
        
        final int event;
        if (hostTree.isRoot(hostNode)) { // No host-switching at root
//            event = CophylogenyUtils.nextWeightedInteger(lambda, mu);
        } else {
            
        }
        
        return null;

    }

    public double getR() {
        return birthDiffRateParameter.getParameterValue(0);
    }

    public double getA() {
        return relativeDeathRateParameter != null ? relativeDeathRateParameter.getParameterValue(0) : 0;
    }

    public double getR() {
        return birthDiffRateParameter.getParameterValue(0);
    }

    public double getA() {
        return relativeDeathRateParameter.getParameterValue(0);
    }

    
    protected double getBirthRate() {
        return birthDiffRateParameter.getValue(0) / 
    }
    
    protected double getLambda() {
        
    }
    
    protected double getTau() {
        
    }
    
    protected double getMu() {
        
    }
    
    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String DHSL_SIMULATOR = "DHSLSimulator";
                private static final String BIRTH_DIFF_RATE =
                        "birthMinusDeathRate";
                private static final String RELATIVE_DEATH_RATE =
                        "relativeDeathRate";
                private static final String HOST_SWITCH_PROPORTION =
                        "hostSwitchProportion";
                private static final String ORIGIN_HEIGHT = "originHeight";
                private static final String COMPLETE_HISTORY =
                        "completeHistory";
                
                @Override
                public String getParserName() {
                    return DHSL_SIMULATOR;
                }

                @Override
                public Object parseXMLObject(final XMLObject xo)
                        throws XMLParseException {
                    
                    final Tree hostTree = (Tree) xo.getChild(Tree.class);
                    final Parameter birthDiffRateParameter =
                            (Parameter) xo.getChild(BIRTH_DIFF_RATE)
                            .getChild(Parameter.class);
                    final Parameter relativeDeathRateParameter =
                            (Parameter) xo.getChild(RELATIVE_DEATH_RATE)
                            .getChild(Parameter.class);
                    final Parameter hostSwitchProportionParameter =
                            (Parameter) xo.getChild(HOST_SWITCH_PROPORTION)
                            .getChild(Parameter.class);
                    final Parameter originHeightParameter =
                            (Parameter) xo.getChild(ORIGIN_HEIGHT)
                            .getChild(Parameter.class);
                    final boolean complete =
                            xo.getBooleanAttribute(COMPLETE_HISTORY);
                    
                    return new DHSLSimulator(hostTree,
                                             birthDiffRateParameter,
                                             relativeDeathRateParameter,
                                             hostSwitchProportionParameter,
                                             originHeightParameter,
                                             complete);
                    
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(Tree.class),
                        new ElementRule(BIRTH_DIFF_RATE, new XMLSyntaxRule[]{
                                new ElementRule(Parameter.class)}),
                        new ElementRule(RELATIVE_DEATH_RATE, new XMLSyntaxRule[]{
                                new ElementRule(Parameter.class)}),
                        new ElementRule(HOST_SWITCH_PROPORTION,
                                new XMLSyntaxRule[]{new ElementRule(
                                        Parameter.class)}),
                        new ElementRule(ORIGIN_HEIGHT, new XMLSyntaxRule[]{
                                new ElementRule(Parameter.class)}),
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
