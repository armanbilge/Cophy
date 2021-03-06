/*
 * CophylogenySimulator.java
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

package cophy.simulation;

import cophy.model.CophylogenyModel;
import cophy.model.TrajectoryState;
import cophy.simulation.CophylogeneticEvent.BirthEvent;
import cophy.simulation.CophylogeneticEvent.CospeciationEvent;
import dr.evolution.tree.FlexibleNode;
import dr.evolution.tree.FlexibleTree;
import dr.evolution.tree.MutableTree;
import dr.evolution.tree.MutableTreeListener;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class CophylogenySimulator<M extends CophylogenyModel> {

    public static final String HOST = "host";
    public static final String EXTINCT = "extinct";

    private final M model;
    private final boolean complete;

    private final NavigableMap<Double,CospeciationEvent> cospeciationEvents;

    private boolean cospeciationsKnown = false;

    public CophylogenySimulator(final M model, final boolean complete) {
        this.model = model;
        this.complete = complete;
        final Tree hostTree = model.getHostTree();
        cospeciationEvents = new TreeMap<Double, CospeciationEvent>();
        if (hostTree instanceof MutableTree) {
            ((MutableTree) hostTree).addMutableTreeListener(new MutableTreeListener() {
                @Override
                public void treeChanged(final Tree hostTree) {
                    cospeciationsKnown = false;
                }
            });
        }
    }

    private void setupCospeciationEvents() {
        cospeciationEvents.clear();
        final Tree hostTree = model.getHostTree();
        for (int i = 0; i < hostTree.getInternalNodeCount(); ++i) {
            final NodeRef host = hostTree.getInternalNode(i);
            final double height = hostTree.getNodeHeight(host);
            final CospeciationEvent event = new CospeciationEvent(host, hostTree.getChild(host, 0), hostTree.getChild(host, 1), height);
            cospeciationEvents.put(height, event);
            cospeciationsKnown = true;
        }
    }

    public NavigableMap<Double,CospeciationEvent> getCospeciationEvents() {
        if (!cospeciationsKnown)
            setupCospeciationEvents();
        return cospeciationEvents;
    }

    protected final boolean isComplete() {
        return complete;
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

//        if (!complete)
//            throw new RuntimeException("Cannot resume incomplete simulation");

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


    public TrajectoryState createTrajectory(final Tree guest) {
        return new TrajectoryState(getModel().getOriginHeight(), guest.getRoot(), getModel().getHostTree().getRoot());
    }

    public double resumeSimulation(final TrajectoryState state, final double until) {

        double p = 1.0;
        CospeciationEvent nextCospeciationEvent = nextCospeciationEvent(state.getHeight());
        while (state.getHeight() > Math.max(until, nextCospeciationEvent != null ? nextCospeciationEvent.getHeight() : 0.0)) {

            final CophylogeneticEvent nextEvent = nextEvent(state);
            final double nextEventHeight = state.getHeight() - nextEvent.getWaitingTime();
            if (nextEventHeight <= until) {
                state.setHeight(until);
                break;
            } else if (nextCospeciationEvent != null && nextEventHeight <= nextCospeciationEvent.getHeight()) {
                p *= nextCospeciationEvent.apply(state);
                nextCospeciationEvent = nextCospeciationEvent(state.getHeight());
            } else {
                p *= nextEvent.apply(state);
            }

        }
        return p;

    }

    protected abstract CophylogeneticEvent nextEvent(final TrajectoryState state);

    protected CospeciationEvent nextCospeciationEvent(final double height) {
        final Map.Entry<Double,CospeciationEvent> entry = getCospeciationEvents().lowerEntry(height);
        return entry != null ? entry.getValue() : null;
    }


    public double
            simulateSpeciationEvent(final TrajectoryState state,
                                    final Tree tree,
                                    final Set<NodeRef> speciatingNodes,
                                    final double height,
                                    final NodeRef host) {

        final Tree hostTree = model.getHostTree();
        if (hostTree.getNodeHeight(host) == height) // Cospeciation event
            return simulateCospeciationEvent(state, tree, speciatingNodes, height);
        else // Birth event
            return simulateBirthEvent(state, tree, speciatingNodes, height, host);

    }

    protected double
            simulateCospeciationEvent(final TrajectoryState state,
                                      final Tree tree,
                                      final Set<NodeRef> speciatingNodes,
                                      final double height) {

        return getCospeciationEvents().get(height).apply(state, tree, speciatingNodes);
    }

    protected double
            simulateBirthEvent(TrajectoryState state,
                               Tree tree,
                               Set<NodeRef> speciatingNodes,
                               double height,
                               NodeRef host) {

        return getModel().getBirthRate() * createBirthEvent(state, height, host).apply(state, tree, speciatingNodes);
    }

    protected abstract BirthEvent createBirthEvent(TrajectoryState state, double height, NodeRef host);

    public M getModel() {
        return model;
    }

}
