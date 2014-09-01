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

public class DHSLModel extends AbstractCophylogenyModel {

    private static final long serialVersionUID = 2041282129285374772L;

    private static final String DHSL_MODEL = "DHSLModel";
    
    protected final Tree hostTree;
    protected final Parameter birthDiffRateParameter;
    protected final Parameter hostSwitchProportionParameter;
    protected final Parameter relativeDeathRateParameter;
    protected final Parameter originHeightParameter;

    public DHSLModel(final Tree hostTree,
                     final Parameter birthDiffRateParameter,
                     final Parameter relativeDeathRateParameter,
                     final Parameter hostSwitchProportionParameter,
                     final Parameter originHeightParameter) {
        super(DHSL_MODEL);
        this.hostTree = hostTree;
        this.birthDiffRateParameter = birthDiffRateParameter;
        this.relativeDeathRateParameter = relativeDeathRateParameter;
        this.hostSwitchProportionParameter = hostSwitchProportionParameter;
        this.originHeightParameter = originHeightParameter;
    }

    @Override
    protected void handleModelChangedEvent(final Model model,
                                           final Object object,
                                           final int index) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index,
            ChangeType type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void storeState() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void restoreState() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void acceptState() {
        // TODO Auto-generated method stub
        
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
