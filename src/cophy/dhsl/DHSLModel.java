/*
 * DHSLModel.java
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

package cophy.dhsl;

import cophy.model.CophylogenyModel;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Units;
import dr.evoxml.util.XMLUnits;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
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
public class DHSLModel extends CophylogenyModel {

    private static final long serialVersionUID = 2041282129285374772L;

    private static final String DHSL_MODEL = "dhslModel";

    protected final Parameter birthDiffRateParameter;
    protected final Parameter relativeDeathRateParameter;
    protected final Parameter hostSwitchProportionParameter;
    protected final Parameter originHeightParameter;
    protected final Parameter samplingProbabilityParameter;

    public DHSLModel(final Tree hostTree,
                     final Parameter birthDiffRateParameter,
                     final Parameter relativeDeathRateParameter,
                     final Parameter hostSwitchProportionParameter,
                     final Parameter originHeightParameter,
                     final Parameter samplingProbabilityParameter,
                     final Units.Type units) {

        super(DHSL_MODEL, hostTree, units);

        this.birthDiffRateParameter = birthDiffRateParameter;
        addVariable(birthDiffRateParameter);

        this.relativeDeathRateParameter = relativeDeathRateParameter;
        addVariable(relativeDeathRateParameter);

        this.hostSwitchProportionParameter = hostSwitchProportionParameter;
        addVariable(hostSwitchProportionParameter);

        this.originHeightParameter = originHeightParameter;
        addVariable(originHeightParameter);

        this.samplingProbabilityParameter = samplingProbabilityParameter;
        addVariable(samplingProbabilityParameter);
    }

    public double getBirthDiffRate() {
        return birthDiffRateParameter.getParameterValue(0);
    }

    public double getRelativeDeathRate() {
        return relativeDeathRateParameter.getParameterValue(0);
    }

    public double getDuplicationProportion() {
        return 1 - getHostSwitchProportion();
    }

    public double getHostSwitchProportion() {
        return hostSwitchProportionParameter.getParameterValue(0);
    }

    @Override
    public double getBirthRate() {
        return getBirthDiffRate() / (1 - getRelativeDeathRate());
    }

    @Override
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

    @Override
    public double getSamplingProbability(final NodeRef host) {
        if (!hostTree.isExternal(host))
            throw new RuntimeException("No sampling rate for non-extant hosts");
        return samplingProbabilityParameter.getValue(host.getNumber());
    }

    @Override
    public double getOriginHeight() {
        return originHeightParameter.getParameterValue(0);
    }

    @Override
    protected void handleModelChangedEvent(final Model model,
                                           final Object object,
                                           final int index) {
        fireModelChanged();
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
                private static final String SAMPLING_PROBABILITY =
                        "samplingProbability";

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
                    final Parameter samplingProbabilityParameter =
                            (Parameter) xo.getChild(SAMPLING_PROBABILITY)
                            .getChild(Parameter.class);
                    final Units.Type units = XMLUnits.Utils.getUnitsAttr(xo);

                    return new DHSLModel(hostTree,
                                         birthDiffRateParameter,
                                         relativeDeathRateParameter,
                                         hostSwitchProportionParameter,
                                         samplingProbabilityParameter,
                                         originHeightParameter,
                                         units);

                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(Tree.class),
                        new ElementRule(BIRTH_DIFF_RATE, Parameter.class),
                        new ElementRule(RELATIVE_DEATH_RATE, Parameter.class),
                        new ElementRule(HOST_SWITCH_PROPORTION, Parameter.class),
                        new ElementRule(ORIGIN_HEIGHT, Parameter.class),
                        new ElementRule(SAMPLING_PROBABILITY, Parameter.class),
                        XMLUnits.UNITS_RULE
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "The duplication, host-switch, loss cophylogeny"
                            + " model.";
                }

                @Override
                public Class<DHSLModel> getReturnType() {
                    return DHSLModel.class;
                }
    };

}
