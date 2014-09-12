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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import cophy.CophyUtils;
import cophy.model.AbstractCophylogenyLikelihood;
import cophy.model.Reconciliation;
import cophy.particlefiltration.AbstractParticle.TreeParticle;
import cophy.simulation.CophylogenySimulator;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.SimpleNode;
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
public class PFCophylogenyLikelihood extends AbstractCophylogenyLikelihood {

    private static final long serialVersionUID = -6527862383425163978L;

    final protected CophylogenySimulator<?> simulator;
    final protected TreeParticle[] particles;
    final int particleCount;

    @SuppressWarnings("unchecked")
    public PFCophylogenyLikelihood(final CophylogenySimulator<?> simulator,
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

        for (int i = 0; i < guestTree.getInternalNodeCount(); ++i) {
            final NodeRef node = guestTree.getInternalNode(i);
            final double height = guestTree.getNodeHeight(node);
            if (!heights2Nodes.containsKey(height))
                heights2Nodes.put(height, new HashSet<NodeRef>());
            heights2Nodes.get(height).add(node);
        }

        for (int i = 0; i < particles.length; ++i) {
            final Tree tree = simulator.createTree();
            particles[i] = new TreeParticle(tree);
        }

        final Queue<Double> speciationsQueue =
                new LinkedList<Double>(heights2Nodes.descendingKeySet());

        double previousUntil = model.getOriginHeight();
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
                    final NodeRef completeParent =
                            particle.getCompleteNode(speciatingNode);

                    final NodeRef head;
                    if (CophyUtils.isLeft(guestTree, speciatingNode))
                        head = tree.getChild(completeParent, 0);
                    else
                        head = tree.getChild(completeParent, 1);

                    final Set<NodeRef> potentialCompletes =
                            new LinkedHashSet<NodeRef>(Tree.Utils
                                    .getExternalNodes(guestTree, head));

                    for (final Iterator<NodeRef> iter =
                            potentialCompletes.iterator();
                            iter.hasNext();) {

                        final SimpleNode node = (SimpleNode) iter.next();
                        if (node.getAttribute(CophylogenySimulator.EXTINCT)
                                == null ||
                                !node.getAttribute(CophylogenySimulator.HOST)
                                .equals(speciatingNodeHost))
                            iter.remove();

                        final int size = potentialCompletes.size();
                        if (size == 0) {
                            particle.multiplyWeight(0.0);
                            break;
                        }

                        final NodeRef complete =
                                CophyUtils.getRandomElement(potentialCompletes);

                        final double w = simulator.s

                    }

                }

                totalWeight += particle.getWeight();

            }

            final double meanWeight = totalWeight / particles.length;
            logLikelihood += Math.log(meanWeight);

            CophyUtils.resample(particles);
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
                    final Tree guestTree = (Tree) xo.getChild(Tree.class);
                    final Reconciliation reconciliation =
                            (Reconciliation) xo.getChild(Reconciliation.class);
                    final int particleCount =
                            xo.getIntegerAttribute(PARTICLE_COUNT);

                    return new PFCophylogenyLikelihood(simulator,
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
                public Class<PFCophylogenyLikelihood> getReturnType() {
                    return PFCophylogenyLikelihood.class;
                }

    };

}
