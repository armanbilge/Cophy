/**
 * CophylogenySimulator.java
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

package cophy.simulation;

import cophy.model.AbstractCophylogenyModel;
import cophy.simulation.CophylogeneticEvent.CospeciationEvent;
import dr.evolution.tree.FlexibleNode;
import dr.evolution.tree.FlexibleTree;
import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class CophylogenySimulator<M extends AbstractCophylogenyModel> {

    public static final String HOST = "host";
    public static final String EXTINCT = "extinct";

    protected final M model;
    protected final boolean complete;

    protected final NavigableMap<Double,CospeciationEvent> cospeciationEvents;

    public CophylogenySimulator(final M model, final boolean complete) {
        this.model = model;
        this.complete = complete;
        final Tree hostTree = model.getHostTree();
        cospeciationEvents = new TreeMap<Double, CospeciationEvent>();
        for (int i = 0; i < hostTree.getInternalNodeCount(); ++i) {
            final NodeRef node = hostTree.getInternalNode(i);
            final double height = hostTree.getNodeHeight(node);
            final CospeciationEvent event = new CospeciationEvent(hostTree, node, height);
            cospeciationEvents.put(height, event);
        }
    }

    public FlexibleTree createTree() {
        final NodeRef hostRoot = model.getHostTree().getRoot();
        final double originHeight = model.getOriginHeight();
        final FlexibleNode root = new FlexibleNode();
        root.setHeight(originHeight);
        root.setAttribute(HOST, hostRoot);
        return new FlexibleTree(root);
    }

    public Tree simulateTree() {
        return simulateTree(0.0);
    }

    public Tree simulateTree(final double until) {

        final Tree tree = createTree();
        resumeSimulation(tree, until);
        return tree;

    }

    public void resumeSimulation(final Tree tree, final double until) {

        if (!(tree instanceof FlexibleTree))
            throw new RuntimeException("Tree not generated by a"
                    + " CophylogenySimulator.");

        if (!complete)
            throw new RuntimeException("Cannot resume incomplete simulation");

        for (int i = 0; i < tree.getExternalNodeCount(); ++i) {
            final FlexibleNode node = (FlexibleNode) tree.getExternalNode(i);
            if (node.getAttribute(EXTINCT) == null) {
                // Does not have EXTINCT attribute
                simulateSubtree(node,
                                (NodeRef) node.getAttribute(HOST),
                                node.getHeight(),
                                until);
            }
        }

        ((FlexibleTree) tree).adoptTreeModelOrdering();

    }

    protected FlexibleNode simulateSubtree(final NodeRef hostNode,
                                           final double height,
                                           final double until) {

        final FlexibleNode node = new FlexibleNode();
        simulateSubtree(node, hostNode, height, until);
        return node;

    }

    protected abstract FlexibleNode simulateSubtree(final FlexibleNode guestNode,
                                                    final NodeRef hostNode,
                                                    double height,
                                                    final double until);

    public double simulateSpeciationEvent(final Tree tree,
                                          final NodeRef node,
                                          final double height) {

        final Tree hostTree = model.getHostTree();
        final NodeRef hostNode = (NodeRef) tree.getNodeAttribute(node, HOST);

        final double w;
        if (hostTree.getNodeHeight(hostNode) == height) // Cospeciation event
            w = simulateCospeciationEvent(tree, node, height);
        else // Birth event
            w = simulateBirthEvent(tree, node, height);

        ((FlexibleTree) tree).adoptTreeModelOrdering();
        return w;
    }

    protected double
            simulateCospeciationEvent(final Tree tree,
                                      final NodeRef node,
                                      final double height) {

        final MutableTree mutableTree = (MutableTree) tree;

        final NodeRef host = (NodeRef) tree.getNodeAttribute(node, HOST);
        final FlexibleNode left = new FlexibleNode();
        left.setAttribute(HOST, host);
        mutableTree.addChild(node, left);
        final FlexibleNode right = new FlexibleNode();
        right.setAttribute(HOST, right);
        mutableTree.addChild(node, right);

        return 1.0; // Cospeciation events are always guaranteed
                    // i.e. occur with equal weight
}

    protected abstract double
            simulateBirthEvent(final Tree tree,
                               final NodeRef node,
                               final double height);


    public MutableCophylogeneticTrajectoryState createTrajectory(final Tree guest) {
        return new SimpleCophylogeneticTrajectoryState(model.getOriginHeight(), guest, model.getHostTree());
    }

//    public MutableCophylogeneticTrajectoryState simulateTrajectory(final Tree guest) {
//        return simulateTrajectory(0.0);
//    }
//
//    public MutableCophylogeneticTrajectoryState simulateTrajectory(final Tree guest, final double until) {
//
//        final MutableCophylogeneticTrajectoryState trajectory = createTrajectory(guest);
//        resumeSimulation(trajectory, until);
//        return trajectory;
//    }

    public void
            resumeSimulation(final MutableCophylogeneticTrajectoryState state,
                             final double until) {

        CophylogeneticEvent nextCospeciationEvent = nextCospeciationEvent(state.getHeight());
        while (state.getHeight() > Math.max(until, nextCospeciationEvent.getHeight())) {

            final CophylogeneticEvent nextEvent = nextEvent(state);
            final double nextEventHeight = nextEvent.getHeight();
            if (nextEventHeight <= until) {
                state.setHeight(until);
                break;
            } else if (nextEventHeight <= nextCospeciationEvent.getHeight()) {
                state.applyEvent(nextCospeciationEvent);
                nextCospeciationEvent = nextCospeciationEvent(state.getHeight());
            } else {
                state.applyEvent(nextEvent);
            }

        }

    }

    protected abstract CophylogeneticEvent nextEvent(final CophylogeneticTrajectoryState state);

    protected CospeciationEvent nextCospeciationEvent(final double height) {
        return cospeciationEvents.lowerEntry(height).getValue();
    }


    public double
            simulateSpeciationEvent(final MutableCophylogeneticTrajectoryState state,
                                    final double height,
                                    final NodeRef guest,
                                    final NodeRef host) {

        final Tree hostTree = model.getHostTree();
        if (hostTree.getNodeHeight(host) == height) // Cospeciation event
            return simulateCospeciationEvent(state, height, host);
        else // Birth event
            return simulateBirthEvent(state, height, guest, host);

    }

    protected double
            simulateCospeciationEvent(final MutableCophylogeneticTrajectoryState state,
                                      final double height,
                                      final NodeRef host) {

        final CospeciationEvent event =
                new CospeciationEvent(model.getHostTree(),
                                      host,
                                      height);
        state.applyEvent(event);

        return 1.0; // Cospeciation events are always guaranteed
                    // i.e. occur with equal weight
    }

    protected abstract double
            simulateBirthEvent(MutableCophylogeneticTrajectoryState state,
                               double height,
                               NodeRef guest,
                               NodeRef host);

    public M getModel() {
        return model;
    }
}
