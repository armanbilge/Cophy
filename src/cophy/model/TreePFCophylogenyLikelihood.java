/**
 * TreePFCophylogenyLikelihood.java
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import cophy.CophyUtils;
import cophy.particlefiltration.AbstractParticle.TreeParticle;
import cophy.simulation.CophylogenySimulator;
import dr.evolution.tree.FlexibleNode;
import dr.evolution.tree.FlexibleTree;
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
public class TreePFCophylogenyLikelihood extends PFCophylogenyLikelihood {

    private static final long serialVersionUID = -6527862383425163978L;

    final protected CophylogenySimulator<?> simulator;
    final protected TreeParticle[] particles;
    final int particleCount;

    public TreePFCophylogenyLikelihood(final CophylogenySimulator<?> simulator,
                                   final Tree guestTree,
                                   final Reconciliation reconciliation,
                                   final int particleCount) {

        super(simulator.getModel(), guestTree, reconciliation);
        this.simulator = simulator;
        this.particles = new TreeParticle[particleCount];
        this.particleCount = particleCount;
    }

    @Override
    protected double calculateLogLikelihood() {

        if (!isValid()) return Double.NEGATIVE_INFINITY;

        final NavigableMap<Double,Set<NodeRef>> heights2Nodes =
                new TreeMap<Double,Set<NodeRef>>();

        for (int i = 0; i < guestTree.getNodeCount(); ++i) {
            final NodeRef node = guestTree.getNode(i);
            final double height = guestTree.getNodeHeight(node);
            if (!heights2Nodes.containsKey(height))
                heights2Nodes.put(height, new HashSet<NodeRef>());
            heights2Nodes.get(height).add(node);
        }

        for (int i = 0; i < particles.length; ++i) {
            final FlexibleTree tree = simulator.createTree();
            particles[i] = new TreeParticle(tree);
        }

        final Queue<Double> speciationsQueue =
                new LinkedList<Double>(heights2Nodes.descendingKeySet());

        double logLikelihood = 0.0;
        while (!speciationsQueue.isEmpty()) {
            final double until = speciationsQueue.poll();

            double totalWeight = 0.0;

            for (final TreeParticle particle : particles) {

                final Tree tree = particle.getValue();
                simulator.resumeSimulation(tree, until);

                final Set<NodeRef> speciatingNodes = heights2Nodes.get(until);
                for (final NodeRef speciatingNode : speciatingNodes) {

                    final NodeRef speciatingNodeHost =
                            reconciliation.getHost(speciatingNode);

                    final NodeRef head;
                    if (!guestTree.isRoot(speciatingNode)) {
                        final NodeRef speciatingParent =
                                guestTree.getParent(speciatingNode);
                        final NodeRef completeParent =
                                particle.getCompleteNode(speciatingParent);

                        final int childNumber =
                                CophyUtils.getChildNumber(guestTree,
                                                          speciatingNode);
                        head = tree.getChild(completeParent, childNumber);
                    } else {
                        head = tree.getRoot();
                    }

                    final Set<NodeRef> potentialCompletes =
                            new LinkedHashSet<NodeRef>(Tree.Utils
                                    .getExternalNodes(tree, head));

                    for (final Iterator<NodeRef> iter =
                            potentialCompletes.iterator();
                            iter.hasNext();) {

                        final FlexibleNode node = (FlexibleNode) iter.next();
                        if (node.getAttribute(CophylogenySimulator.EXTINCT)
                                != null ||
                                !node.getAttribute(CophylogenySimulator.HOST)
                                .equals(speciatingNodeHost))
                            iter.remove();

                    }

                    final int size = potentialCompletes.size();
                    if (size == 0) {
                        particle.multiplyWeight(0.0);
                        break;
                    }

                    final NodeRef complete =
                            CophyUtils.getRandomElement(potentialCompletes);
                    particle.setCompleteNode(speciatingNode, complete);


                    if (until > 0.0) {
                        final double w = simulator
                                .simulateSpeciationEvent(tree,
                                                         complete,
                                                         until);
                        particle.multiplyWeight(w);
                    }

                }

                totalWeight += particle.getWeight();

            }

            final double meanWeight = totalWeight / particles.length;
            logLikelihood += Math.log(meanWeight);

            CophyUtils.resample(particles);

        }

        double totalWeight = 0.0;
        for (final TreeParticle particle : particles) {

            final Tree tree = particle.getValue();

            double rho = 1.0;
            for (int i = 0; i < hostTree.getExternalNodeCount(); ++i) {

                final NodeRef host = hostTree.getExternalNode(i);
                final double height = hostTree.getNodeHeight(host);
                final double samplingProbability =
                        cophylogenyModel.getSamplingProbability(host);

                final int completeCount =
                        getGuestCountAtHostAtHeight(tree, host, height);
                final int reconstructedCount = CophyUtils
                        .getGuestCountAtHostAtHeight(guestTree,
                                                     host,
                                                     height,
                                                     reconciliation);

                rho *= CophyUtils
                        .extendedBinomialCoefficientDouble(completeCount,
                                                           reconstructedCount);
                if (rho == 0.0) break;
                rho *= Math.pow(samplingProbability, reconstructedCount);
                rho *= Math.pow(1 - samplingProbability,
                        completeCount - reconstructedCount);

                if (rho == 0.0) break;

            }

            particle.multiplyWeight(rho);

            totalWeight += particle.getWeight();

        }

        final double meanWeight = totalWeight / particles.length;
        logLikelihood += Math.log(meanWeight);

        return logLikelihood;
    }

    protected static int getGuestCountAtHostAtHeight(final Tree guestTree,
                                                     final NodeRef hostNode,
                                                     final double height) {
        int count = 0;
        final Set<NodeRef> guestNodes =
                CophyUtils.getLineagesAtHeight(guestTree, height);
        for (final NodeRef guestNode : guestNodes) {
            final NodeRef actualHost = (NodeRef) guestTree
                    .getNodeAttribute(guestNode, CophylogenySimulator.HOST);
            final boolean isExtinct = guestTree
                    .getNodeAttribute(guestNode, CophylogenySimulator.EXTINCT)
                    != null;
            if (hostNode.equals(actualHost) && !isExtinct) ++count;
        }
        return count;
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String TREE_PF_COPHYLOGENY_LIKELIHOOD =
                        "treePFCophylogenyLikelihood";
                private static final String PARTICLE_COUNT = "particleCount";

                @Override
                public String getParserName() {
                    return TREE_PF_COPHYLOGENY_LIKELIHOOD;
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

                    return new TreePFCophylogenyLikelihood(simulator,
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
                    return "Approximates the cophylogenetic likelihood using a"
                            + " particle filtering method.";
                }

                @Override
                public Class<TreePFCophylogenyLikelihood> getReturnType() {
                    return TreePFCophylogenyLikelihood.class;
                }

    };

}
