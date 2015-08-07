/*
 * CophylogenyLikelihood.java
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

package cophy.model;

import cophy.particlefiltration.Particle;
import cophy.simulation.CophylogenySimulator;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.model.Model;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class CophylogenyLikelihood extends PFCophylogenyLikelihood {

    private static final long serialVersionUID = -6527862383425163978L;

    private final CophylogenySimulator<?> simulator;
    private final Particle<TrajectoryState>[] particles;
    private final NavigableMap<Double,Set<NodeRef>> heightsToNodes = new TreeMap<Double, Set<NodeRef>>();
    private boolean heightsToNodesKnown = false;
    private final int particleCount;

    @SuppressWarnings("unchecked")
    public CophylogenyLikelihood(final
                                 CophylogenySimulator<?> simulator,
                                 final Tree guestTree,
                                 final
                                 Reconciliation reconciliation,
                                 final int particleCount) {

        super(simulator.getModel(), guestTree, reconciliation);
        this.simulator = simulator;
        this.particles = new Particle[particleCount];
        this.particleCount = particleCount;
    }

    @Override
    protected double calculateValidLogLikelihood() {

        for (int i = 0; i < particles.length; ++i) {
            final TrajectoryState state = simulator.createTrajectory(guestTree);
            particles[i] = new Particle<TrajectoryState>(state);
        }

        if (!heightsToNodesKnown) {
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
            heightsToNodesKnown = true;
        }

        final Queue<Double> speciationsQueue =
                new LinkedList<Double>(heightsToNodes.descendingKeySet());

        double logLikelihood = 0.0;
        while (!speciationsQueue.isEmpty()) {
            final double until = speciationsQueue.poll();

            double totalWeight = 0.0;

            for (final Particle<TrajectoryState> particle : particles) {

                final TrajectoryState trajectory = particle.getValue();
                particle.multiplyWeight(simulator.resumeSimulation(trajectory, until));

                if (particle.getWeight() == 0.0)
                    break;

                final Set<NodeRef> speciatingNodes = heightsToNodes.get(until);

                for (final NodeRef speciatingNode : speciatingNodes) {
                    if (!speciatingNode.equals(trajectory.getGuestLineageHost(speciatingNode))) {
                        particle.multiplyWeight(0.0);
                        break;
                    }
                }

                if (particle.getWeight() == 0.0)
                    break;

                final NodeRef host = reconciliation.getHost(speciatingNodes.iterator().next());
                particle.multiplyWeight(simulator.simulateSpeciationEvent(trajectory, guestTree, speciatingNodes, until, host));

                totalWeight += particle.getWeight();

            }

            final double meanWeight = totalWeight / particleCount;
            logLikelihood += Math.log(meanWeight);

            Particle.resample(particles);

        }

        final CophylogenyModel model = simulator.getModel();

        double totalWeight = 0.0;
        double weight2 = 0.0;
        for (final Particle<TrajectoryState> particle : particles) {

            final TrajectoryState trajectory = particle.getValue();
            particle.multiplyWeight(simulator.resumeSimulation(trajectory, 0.0));

            weight2 += particle.getWeight();

            trajectory.setHeight(0.0);

            final int[] lineageCounts = new int[hostTree.getExternalNodeCount()];
            for (int i = 0; i < guestTree.getExternalNodeCount(); ++i) {
                final NodeRef guest = guestTree.getExternalNode(i);
                final NodeRef host = reconciliation.getHost(guest);
                ++lineageCounts[host.getNumber()];
                if (!host.equals(trajectory.getGuestLineageHost(guest))) {
                    particle.multiplyWeight(0.0);
                    break;
                } else {
                    particle.multiplyWeight(model.getSamplingProbability(host));
                }
            }

            if (particle.getWeight() > 0.0) {
                for (int i = 0; i < hostTree.getExternalNodeCount(); ++i) {
                    final NodeRef host = hostTree.getExternalNode(i);
                    final double rho = model.getSamplingProbability(host);
                    final int count = trajectory.getGuestCount(host) - lineageCounts[host.getNumber()];
                    particle.multiplyWeight(Math.pow(1 - rho, count));
                    if (particle.getWeight() == 0)
                        break;
                }
            }

            totalWeight += particle.getWeight();

        }

        final double meanWeight = totalWeight / particleCount;
        System.out.println(weight2 / particleCount);
        System.out.println(meanWeight);
        logLikelihood += Math.log(meanWeight);

        return logLikelihood;
    }

    @Override
    protected void handleModelChangedEvent(final Model model, final Object object, final int index) {
        if (model == guestTree)
            heightsToNodesKnown = false;
        super.handleModelChangedEvent(model, object, index);
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String
                        TRAJECTORY_PF_COPHYLOGENY_LIKELIHOOD =
                        "cophylogenyLikelihood";
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
