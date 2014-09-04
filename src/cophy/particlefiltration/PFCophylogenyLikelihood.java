/**
 * PFCophylogenyLikelihood.java
 * 
 * Cophy: Cophylogenetics for BEAST 2
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

package cophy.particlefiltration;

import cophy.model.AbstractCophylogenyLikelihood;
import cophy.model.Reconciliation;
import cophy.particlefiltration.Particle.TreeParticle;
import cophy.simulation.CophylogenySimulator;
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
public class PFCophylogenyLikelihood extends AbstractCophylogenyLikelihood {

    private static final long serialVersionUID = -6527862383425163978L;

    final protected CophylogenySimulator<?> simulator;
    final protected TreeParticle[] particles;
    final int particleCount;
    
    public PFCophylogenyLikelihood(final CophylogenySimulator<?> simulator,
                                   final Reconciliation reconciliation,
                                   final int particleCount) {
        
        super(simulator.getModel(), reconciliation);
        this.simulator = simulator;
        this.particles = new TreeParticle[particleCount];
        this.particleCount = particleCount;
    }

    @Override
    protected double calculateLogLikelihood() {
        // TODO Auto-generated method stub
        return 0;
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String PF_COPHYLOGENY_LIKELIHOOD =
                        "pfCophylogenyLikelihood";
                private static final String PARTICLE_COUNT = "particleCount";
        
                @Override
                public String getParserName() {
                    return PF_COPHYLOGENY_LIKELIHOOD;
                }

                @Override
                public Object parseXMLObject(XMLObject xo)
                        throws XMLParseException {
                    
                    final CophylogenySimulator<?> simulator =
                            (CophylogenySimulator<?>) xo
                            .getChild(CophylogenySimulator.class);
                    final Reconciliation reconciliation =
                            (Reconciliation) xo.getChild(Reconciliation.class);
                    final int particleCount =
                            xo.getIntegerAttribute(PARTICLE_COUNT);
                    
                    return new PFCophylogenyLikelihood(simulator,
                                                     reconciliation,
                                                     particleCount);
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(CophylogenySimulator.class),
                        new ElementRule(Reconciliation.class),
                        AttributeRule.newIntegerRule(PARTICLE_COUNT)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "Approximates the cophylogenetic likelihood using a"
                            + " particle filtering method.";
                }

                @Override
                public Class<PFCophylogenyLikelihood> getReturnType() {
                    return PFCophylogenyLikelihood.class;
                }
        
    };
    
}
