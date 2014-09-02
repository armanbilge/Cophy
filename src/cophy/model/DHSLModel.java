/**
 * DHSLModel.java
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

import dr.evolution.tree.Tree;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
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
public class DHSLModel extends AbstractCophylogenyModel {

    private static final long serialVersionUID = 2041282129285374772L;

    private static final String DHSL_MODEL = "DHSLModel";
    
    protected final Parameter birthDiffRateParameter;
    protected final Parameter relativeDeathRateParameter;
    protected final Parameter hostSwitchProportionParameter;
    protected final Parameter originHeightParameter;

    public DHSLModel(final Tree hostTree,
                     final Parameter birthDiffRateParameter,
                     final Parameter relativeDeathRateParameter,
                     final Parameter hostSwitchProportionParameter,
                     final Parameter originHeightParameter) {
        
        super(DHSL_MODEL, hostTree);
        
        this.birthDiffRateParameter = birthDiffRateParameter;
        addVariable(birthDiffRateParameter);
        
        this.relativeDeathRateParameter = relativeDeathRateParameter;
        addVariable(relativeDeathRateParameter);
        
        this.hostSwitchProportionParameter = hostSwitchProportionParameter;
        addVariable(hostSwitchProportionParameter);
        
        this.originHeightParameter = originHeightParameter;
        addVariable(originHeightParameter);
    }

    public Tree getHostTree() {
        return hostTree;
    }

    public double getBirthDiffRate() {
        return birthDiffRateParameter.getValue(0);
    }

    public double getRelativeDeathRate() {
        return relativeDeathRateParameter.getValue(0);
    }

    public double getDuplicationProportion() {
        return 1 - getHostSwitchProportion();
    }
    
    public double getHostSwitchProportion() {
        return hostSwitchProportionParameter.getValue(0);
    }
    
    public double getBirthRate() {
        return getBirthDiffRate() / (1 - getRelativeDeathRate());
    }
    
    public double getDeathRate() {
        return getBirthRate() - getBirthDiffRate();
    }
    
    public double getDuplicationRate() {
        return getBirthRate() * getDuplicationProportion();
    }
        
    public double getHostSwitchRate() {
        return getBirthRate() * getHostSwitchProportion();
    }
    
    public double getLossRate() {
        return getDeathRate();
    }
    
    public double getOriginHeight() {
        return originHeightParameter.getValue(0);
    }
    
    @Override
    protected void handleModelChangedEvent(final Model model,
                                           final Object object,
                                           final int index) {
        this.fireModelChanged();
    }

    @Override
    protected void handleVariableChangedEvent(@SuppressWarnings("rawtypes")
                                              final Variable variable,
                                              final int index,
                                              final ChangeType type) {
        fireModelChanged();
    }

    @Override
    protected void storeState() {
        // Nothing to do
    }

    @Override
    protected void restoreState() {
        // Nothing to do
    }

    @Override
    protected void acceptState() {
        // Nothing to do
    }
    
    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

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
                    return DHSL_MODEL;
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
                    
                    return new DHSLModel(hostTree,
                                         birthDiffRateParameter,
                                         relativeDeathRateParameter,
                                         hostSwitchProportionParameter,
                                         originHeightParameter);
                    
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
                    return "The duplication, host-switch, loss cophylogeny" +
                            " model.";
                }

                @Override
                public Class<DHSLModel> getReturnType() {
                    return DHSLModel.class;
                }
    };
    
}
