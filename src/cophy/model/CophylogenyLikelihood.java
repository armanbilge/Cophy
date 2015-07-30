/*
 * TrajectoryPFCophylogenyLikelihood.java
 *
 * Cophy: Cophylogenetics for BEAST
 *
 * Copyright (C) 2014 Arman Bilge <armanbilge@gmail.com>
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

import java.util.*;

import cophy.CophyUtils;
import cophy.simulation.CophylogeneticEvent.SpeciationEvent;
import cophy.simulation.CophylogeneticTrajectoryState;
import cophy.simulation.CophylogenySimulator;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
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
public class CophylogenyLikelihood extends PFCophylogenyLikelihood {

    private static final long serialVersionUID = -6527862383425163978L;

    final protected CophylogenySimulator<?> simulator;
    final protected Particle<CophylogeneticTrajectoryState>[] particles;
    final int particleCount;

    @SuppressWarnings("unchecked")
    public CophylogenyLikelihood(final
                                 CophylogenySimulator<?> simulator,
                                 final Tree guestTree,
                                 final
                                 Reconciliation reconciliation,
                                 final int particleCount) {

        super(simulator.getModel(), guestTree, reconciliation);
        this.simulator = simulator;
        this.particles = new TrajectoryParticle[particleCount];
        this.particleCount = particleCount;
        for (int i = 0; i < particles.length; ++i) {
            final MutableCophylogeneticTrajectoryState state = simulator.createTrajectory(guestTree);
            particles[i] = new TrajectoryParticle(state);
        }
    }

    @Override
    protected double calculateValidLogLikelihood() {

        final NavigableMap<Double,Set<NodeRef>> heightsToNodes =
                new TreeMap<Double,Set<NodeRef>>();

        for (int i = 0; i < guestTree.getInternalNodeCount(); ++i) {
            final NodeRef node = guestTree.getInternalNode(i);
            final double height = guestTree.getNodeHeight(node);
            if (!heightsToNodes.containsKey(height))
                heightsToNodes.put(height, new HashSet<NodeRef>());
            final Set<NodeRef> representatives = heightsToNodes.get(height);
            final NodeRef host = reconciliation.getHost(node);
            boolean represented = false;
            for (final NodeRef rep : representatives) {
                if (reconciliation.getHost(rep).equals(host)) {
                    represented = true;
                    break;
                }
            }
            if (!represented)
                heightsToNodes.get(height).add(node);
        }

        for (final TrajectoryParticle particle : particles)
            particle.getValue().reset(guestTree, cophylogenyModel);

        final Queue<Double> speciationsQueue =
                new LinkedList<Double>(heightsToNodes.descendingKeySet());

        double logLikelihood = 0.0;
        while (!speciationsQueue.isEmpty()) {
            final double until = speciationsQueue.poll();

            double totalWeight = 0.0;

            for (final TrajectoryParticle particle : particles) {

                final MutableCophylogeneticTrajectoryState trajectory = particle.getValue();
                particle.setListening(true);
                simulator.resumeSimulation(trajectory, until);
                particle.setListening(false);

                final Set<NodeRef> speciatingNodes = heightsToNodes.get(until);
                for (final NodeRef speciatingNode : speciatingNodes) {

                    final NodeRef speciatingNodeHost = reconciliation.getHost(speciatingNode);
                    final SpeciationEvent event = simulator.simulateSpeciationEvent(trajectory, until, speciatingNode, speciatingNodeHost);

                    final double p = event.getProbabilityObserved(trajectory, guestTree, reconciliation);
                    particle.multiplyWeight(p);

                }

                double rho = 1.0;
                for (int i = 0; i < hostTree.getExternalNodeCount(); ++i) {
                    final NodeRef host = hostTree.getExternalNode(i);
                    final double samplingProbability = cophylogenyModel.getSamplingProbability(host);
                    final double noSamplingProbability = 1 - samplingProbability;
                    for (int j = 0; j < guestTree.getExternalNodeCount(); ++j) {
                        final NodeRef guest = guestTree.getExternalNode(j);
                        final int completeCount = trajectory.getGuestCountAtHost(guest, host);
                        rho *= completeCount;
                        rho *= samplingProbability;
                        rho *= Math.pow(noSamplingProbability, completeCount - 1);
                    }
                    final int extinctGuestCount =
                            trajectory.getGuestCountAtHost(CophylogeneticTrajectoryState.NULL_GUEST, host);
                    rho *= Math.pow(noSamplingProbability, extinctGuestCount);
                }

                particle.multiplyWeight(rho);

                totalWeight += particle.getWeight();

            }

            final double meanWeight = totalWeight / particles.length;
            logLikelihood += Math.log(meanWeight);

            CophyUtils.resample(particles);

        }

        return logLikelihood;
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String
                        TRAJECTORY_PF_COPHYLOGENY_LIKELIHOOD =
                        "trajectoryPFCophylogenyLikelihood";
                private static final String PARTICLE_COUNT = "particleCount";

                @Override
                public String getParserName() {
                    return TRAJECTORY_PF_COPHYLOGENY_LIKELIHOOD;
                }

                @Override
                public Object parseXMLObject(XMLObject xo)
                        throws XMLParseException {

                    final CophylogenySimulator<?> simulator =
                            (CophylogenySimulator<?>) xo
                            .getChild(CophylogenySimulator.class);
                    final Tree guestTree = (Tree) xo.getChild(Tree.class);
                    final Reconciliation reconciliation =
                            (Reconciliation) xo.getChild(Reconciliation.class);
                    final int particleCount =
                            xo.getIntegerAttribute(PARTICLE_COUNT);

                    return new CophylogenyLikelihood(simulator,
                                                       guestTree,
                                                       reconciliation,
                                                       particleCount);
                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(CophylogenySimulator.class),
                        new ElementRule(Tree.class),
                        new ElementRule(Reconciliation.class),
                        AttributeRule.newIntegerRule(PARTICLE_COUNT)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "Approximates the cophylogenetic likelihood using a particle filter.";
                }

                @Override
                public Class<CophylogenyLikelihood>
                        getReturnType() {
                    return CophylogenyLikelihood.class;
                }

    };

}
