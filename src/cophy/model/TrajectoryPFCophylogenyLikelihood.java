/**
 * TrajectoryPFCophylogenyLikelihood.java
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

package cophy.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import cophy.CophyUtils;
import cophy.particlefiltration.AbstractParticle.Particle;
import cophy.simulation.CophylogeneticEvent;
import cophy.simulation.CophylogeneticEvent.SpeciationEvent;
import cophy.simulation.CophylogeneticTrajectory;
import cophy.simulation.CophylogeneticTrajectoryState;
import cophy.simulation.CophylogenySimulator;
import cophy.simulation.MutableCophylogeneticTrajectoryState;
import cophy.simulation.MutableCophylogeneticTrajectoryStateListener;
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
public class TrajectoryPFCophylogenyLikelihood extends PFCophylogenyLikelihood implements MutableCophylogeneticTrajectoryStateListener {

    private static final long serialVersionUID = -6527862383425163978L;

    final protected CophylogenySimulator<?> simulator;
    final protected Particle<MutableCophylogeneticTrajectoryState>[] particles;
    final protected Map<CophylogeneticTrajectoryState,Particle<CophylogeneticTrajectoryState>> states2Particles;
    final int particleCount;

    private boolean listening;
    private Particle<MutableCophylogeneticTrajectoryState> currentParticle;

    @SuppressWarnings("unchecked")
    public TrajectoryPFCophylogenyLikelihood(final
                                             CophylogenySimulator<?> simulator,
                                             final Tree guestTree,
                                             final
                                             Reconciliation reconciliation,
                                             final int particleCount) {

        super(simulator.getModel(), guestTree, reconciliation);
        this.simulator = simulator;
        this.particles = new Particle[particleCount];
        states2Particles = new HashMap<CophylogeneticTrajectoryState, Particle<CophylogeneticTrajectoryState>>(particleCount);
        this.particleCount = particleCount;
        for (int i = 0; i < particles.length; ++i) {
            final MutableCophylogeneticTrajectoryState state = simulator.createTrajectory(guestTree);
            state.addListener(this);
            particles[i] = new Particle<MutableCophylogeneticTrajectoryState>(state);
        }
    }

    @Override
    protected double calculateLogLikelihood() {

        if (!isValid()) return Double.NEGATIVE_INFINITY;

        final NavigableMap<Double,Set<NodeRef>> heights2Nodes =
                new TreeMap<Double,Set<NodeRef>>();

        for (int i = 0; i < guestTree.getInternalNodeCount(); ++i) {
            final NodeRef node = guestTree.getInternalNode(i);
            final double height = guestTree.getNodeHeight(node);
            if (!heights2Nodes.containsKey(height))
                heights2Nodes.put(height, new HashSet<NodeRef>());
            heights2Nodes.get(height).add(node);
        }

        for (int i = 0; i < particles.length; ++i)
            particles[i].getValue().reset(guestTree, cophylogenyModel);

        final Queue<Double> speciationsQueue =
                new LinkedList<Double>(heights2Nodes.descendingKeySet());

        double logLikelihood = 0.0;
        while (!speciationsQueue.isEmpty()) {
            final double until = speciationsQueue.poll();

            double totalWeight = 0.0;

            for (final Particle<MutableCophylogeneticTrajectoryState> particle : particles) {

                currentParticle = particle;
                final MutableCophylogeneticTrajectoryState trajectory = particle.getValue();
                listening = true;
                simulator.resumeSimulation(trajectory, until);
                listening = false;

                final Set<NodeRef> speciatingNodes = heights2Nodes.get(until);
                for (final NodeRef speciatingNode : speciatingNodes) {

                    final NodeRef speciatingNodeHost =
                            reconciliation.getHost(speciatingNode);
                    double p = simulator.simulateSpeciationEvent(trajectory, speciatingNode, until);
                    particle.multiplyWeight(p);

                    final SpeciationEvent event =
                            (SpeciationEvent) trajectory.getLastEvent();
                    final CophylogeneticTrajectoryState state =
                            trajectory.getCurrentState();
                    p = event.getProbabilityObserved(state,
                                                     guestTree,
                                                     reconciliation);
                    particle.multiplyWeight(p);

                    // TODO Proper handling of weights for multiple nodes

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

    @Override
    public void stateChangedEvent(final MutableCophylogeneticTrajectoryState state, final CophylogeneticEvent event) {
        if (listening && event.isSpeciation()) {
            final double weight = ((SpeciationEvent) event).getProbabilityUnobserved(state, guestTree, reconciliation);
            currentParticle.multiplyWeight(weight);
        }
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

                    return new TrajectoryPFCophylogenyLikelihood(simulator,
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
                public Class<TrajectoryPFCophylogenyLikelihood>
                        getReturnType() {
                    return TrajectoryPFCophylogenyLikelihood.class;
                }

    };

}
