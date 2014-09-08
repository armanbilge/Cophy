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

import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;

import cophy.CophylogenyUtils;
import cophy.model.AbstractCophylogenyLikelihood;
import cophy.model.Reconciliation;
import cophy.particlefiltration.AbstractParticle.Particle;
import cophy.simulation.CophylogeneticEvent;
import cophy.simulation.CophylogeneticEvent.SpeciationEvent;
import cophy.simulation.CophylogeneticTrajectory;
import cophy.simulation.CophylogeneticTrajectoryState;
import cophy.simulation.CophylogenySimulator;
import dr.evolution.tree.NodeRef;
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
    final protected Particle<CophylogeneticTrajectory>[] particles;
    final int particleCount;
    
    @SuppressWarnings("unchecked")
    public PFCophylogenyLikelihood(final CophylogenySimulator<?> simulator,
                                   final Reconciliation reconciliation,
                                   final int particleCount) {
        
        super(simulator.getModel(),
              simulator.getModel().getHostTree(),
              reconciliation);
        this.simulator = simulator;
        this.particles = new Particle[particleCount];
        this.particleCount = particleCount;
    }

    @Override
    protected double calculateLogLikelihood() {
        
        if (!isValid()) return Double.NEGATIVE_INFINITY;
        
        final NavigableMap<Double,NodeRef> heights2Nodes =
                new TreeMap<Double,NodeRef>();
        
        for (int i = 0; i < guestTree.getInternalNodeCount(); ++i) {
            final NodeRef node = guestTree.getNode(i);
            final double height = guestTree.getNodeHeight(node);
            heights2Nodes.put(height, node);
        }
        
        for (int i = 0; i < particles.length; ++i) {
            CophylogeneticTrajectory trajectory = simulator.createTrajectory();
            particles[i] = new Particle<CophylogeneticTrajectory>(trajectory);
        }
        
        final Queue<Double> speciationsQueue =
                new LinkedList<Double>(heights2Nodes.descendingKeySet());
        
        double previousUntil = model.getOriginHeight();
        double logLikelihood = 0.0;
        while (!speciationsQueue.isEmpty()) {
            final double until = speciationsQueue.poll();
            
            double totalWeight = 0.0;
            
            for(final Particle<CophylogeneticTrajectory> particle : particles) {
                
                final CophylogeneticTrajectory trajectory = particle.getValue();
                simulator.resumeSimulation(trajectory, until);

                // Rewind and replay
                trajectory.getStateAtHeight(previousUntil);
                while (trajectory.hasNextEvent()) {
                    final CophylogeneticEvent event =
                            trajectory.pollNextEvent();
                    if (event.isSpeciation()) {
                        final CophylogeneticTrajectoryState state =
                                trajectory.getCurrentState();
                        final double p = ((SpeciationEvent) event)
                                .getProbabilityUnobserved(state,
                                                          guestTree,
                                                          reconciliation);
                        particle.multiplyWeight(p);
                        
                    }
                }
                
                final NodeRef speciatingNode = heights2Nodes.get(until);
                double p = simulator.simulateSpeciationEvent(trajectory,
                                                             until,
                                                             speciatingNode);
                particle.multiplyWeight(p);
                
                final SpeciationEvent event =
                        (SpeciationEvent) trajectory.getLastEvent();
                final CophylogeneticTrajectoryState state =
                        trajectory.getCurrentState();
                p = event.getProbabilityObserved(state,
                                                 guestTree,
                                                 reconciliation);
                particle.multiplyWeight(p);
                
                totalWeight += particle.getWeight();
                final double meanWeight = totalWeight / particles.length;
                logLikelihood += Math.log(meanWeight);
                
            }
            
            CophylogenyUtils.resample(particles);
            previousUntil = until;
            
        }
        
        return logLikelihood;
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
