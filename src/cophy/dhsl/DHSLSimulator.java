/*
 * DHSLSimulator.java
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

import cophy.CophyUtils;
import cophy.model.TrajectoryState;
import cophy.simulation.CophylogeneticEvent;
import cophy.simulation.CophylogeneticEvent.BirthEvent;
import cophy.simulation.CophylogeneticEvent.DeathEvent;
import cophy.simulation.CophylogenySimulator;
import dr.evolution.tree.FlexibleNode;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.math.MathUtils;
import dr.xml.*;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class DHSLSimulator extends CophylogenySimulator<DHSLModel> {

    public DHSLSimulator(final DHSLModel model, final boolean complete) {
        super(model, complete);
    }

    @Override
    protected FlexibleNode simulateSubtree(final FlexibleNode guestNode,
                                           final NodeRef hostNode,
                                           double height,
                                           final double until) {

        final Tree hostTree = getModel().getHostTree();
        final double duplicationRate = getModel().getDuplicationRate();
        final double hostSwitchRate = getModel().getHostSwitchRate();
        final double lossRate = getModel().getLossRate();

        guestNode.setAttribute(HOST, hostNode);

        final FlexibleNode left, right;

        final int event;
        if (hostTree.isRoot(hostNode)) { // No host-switching at root
            event = CophyUtils.nextWeightedInteger(duplicationRate,
                                                   lossRate);
            height -= CophyUtils.nextPoissonTime(duplicationRate,
                                                 lossRate);
        } else {
            event = CophyUtils.nextWeightedInteger(duplicationRate,
                                                   lossRate,
                                                   hostSwitchRate);
            height -= CophyUtils.nextPoissonTime(duplicationRate,
                                                 lossRate,
                                                 hostSwitchRate);
        }

        NodeRef leftHost = null;
        NodeRef rightHost = null;
        final double hostHeight = hostTree.getNodeHeight(hostNode);
        if (hostHeight > height) {

            height = hostHeight;
            if (hostTree.isExternal(hostNode)) {
                guestNode.setHeight(height);
                return guestNode;
            }

        } else if (until > height) {

            guestNode.setHeight(until);
            return guestNode;

        } else {

            switch(event) {
            case 0: // Duplication event
                leftHost = hostNode;
                rightHost = hostNode;
                break;
            case 1: // Loss event
                if (!isComplete()) return null;
                guestNode.setAttribute(EXTINCT, true);
                break;
            case 2: // Host-switch event
                final NodeRef[] hosts = new NodeRef[2];

                hosts[0] = hostNode;
                final Set<NodeRef> potentialHosts =
                        CophyUtils.getLineagesAtHeight(hostTree, height);
                potentialHosts.remove(hostNode);
                hosts[1]  = CophyUtils.getRandomElement(potentialHosts);

                final int r = MathUtils.nextInt(2);
                leftHost = hosts[r];
                rightHost = hosts[1 - r];
                break;
            default: // Should not be needed.
                throw new RuntimeException("Unknown event type");
            }
        }

        if (leftHost == null) {
            left = null;
            right = null;
        } else {
            left = simulateSubtree(leftHost, height, until);
            right = simulateSubtree(rightHost, height, until);
        }

        guestNode.setHeight(height);

        if (!isComplete()) {
            final boolean leftIsNull = (left == null);
            final boolean rightIsNull = (right == null);
            if (leftIsNull && rightIsNull) { // Entire lineage was lost
                return null;
            } else if (leftIsNull || rightIsNull) { // One child lineage is lost
                return left != null ? left : right;
            }
        }

        if (left != null) {
            guestNode.addChild(left);
            guestNode.addChild(right);
        }

        return guestNode;

    }

    @Override
    public double simulateBirthEvent(final Tree tree,
                                     final NodeRef node,
                                     final double height) {

        final DHSLModel model = getModel();
        final Tree hostTree = model.getHostTree();
        final FlexibleNode flexibleNode = (FlexibleNode) node;
        flexibleNode.setHeight(height);
        final NodeRef host = (NodeRef) flexibleNode.getAttribute(HOST);

        final int nextEventType;
        if (CophyUtils.getLineageCountAtHeight(hostTree, height) > 1) {

            nextEventType = CophyUtils
                    .nextWeightedInteger(model.getDuplicationProportion(),
                                         model.getHostSwitchProportion());

        } else { // No host-switching possible

            nextEventType = 0; // Has to be a duplication

        }

        final FlexibleNode child1 = new FlexibleNode();
        child1.setHeight(height);
        child1.setAttribute(HOST, host);
        final FlexibleNode child2 = new FlexibleNode();
        child2.setHeight(height);

        switch(nextEventType) {
        case 0: // Duplication event
            child2.setAttribute(HOST, host);
            break;
        case 1: // Host-switch event
            final Set<NodeRef> potentialHosts =
                    CophyUtils.getLineagesAtHeight(hostTree, height);
            potentialHosts.remove(host);
            final NodeRef newHost =
                    CophyUtils.getRandomElement(potentialHosts);
            child2.setAttribute(HOST, newHost);
        default: // Should not be needed
            throw new RuntimeException("Undefined event.");
        }

        final FlexibleNode left, right;
        if (MathUtils.nextBoolean()) {
            left = child1;
            right = child2;
        } else {
            left = child2;
            right = child1;
        }

        flexibleNode.insertChild(left, 0);
        flexibleNode.insertChild(right, 1);

        return model.getBirthRate();

    }


    @Override
    protected CophylogeneticEvent nextEvent(final TrajectoryState state) {

        final DHSLModel model = getModel();
        final int guestCount = state.getGuestCount();
        final double normalizedDuplicationRate = guestCount * model.getDuplicationRate();
        final double normalizedHostSwitchRate = guestCount * model.getHostSwitchRate();
        final double normalizedLossRate = guestCount * model.getLossRate();
        final double nextEventHeight;

        if (state.getHostCount() > 1) {

            nextEventHeight = CophyUtils
                    .nextPoissonTime(normalizedDuplicationRate,
                            normalizedLossRate,
                            normalizedHostSwitchRate);

        } else { // No host-switching possible

            nextEventHeight = CophyUtils
                    .nextPoissonTime(normalizedDuplicationRate,
                            normalizedLossRate);

        }

        final int nextEventType;
        if (state.getHostCount() > 1) {

            nextEventType = CophyUtils
                    .nextWeightedInteger(normalizedDuplicationRate,
                            normalizedLossRate,
                            normalizedHostSwitchRate);

        } else { // No host-switching possible

            nextEventType = CophyUtils
                    .nextWeightedInteger(normalizedDuplicationRate,
                            normalizedLossRate);

        }

//        final Map<NodeRef,Integer> guestCounts = state.getGuestCounts();
//        final NodeRef guest = CophyUtils.nextWeightedObject(guestCounts);
//        final NodeRef host = CophyUtils.nextWeightedObject(state.getGuestCountAtHosts(guest));

        final NodeRef host = CophyUtils.nextWeightedObject(state.getGuestCounts());

        final CophylogeneticEvent nextEvent;


        switch(nextEventType) {
            case 0: // Duplication event
                nextEvent = new DuplicationEvent(nextEventHeight, host);
                break;
            case 1: // Loss event
                nextEvent = new LossEvent(nextEventHeight, host);
                break;
            case 2: // Host-switch event
                final Set<NodeRef> potentialHosts = new HashSet<NodeRef>(state.getHosts());
                potentialHosts.remove(host);
                final NodeRef newHost = CophyUtils.getRandomElement(potentialHosts);
                nextEvent = new HostSwitchEvent(nextEventHeight, host, newHost);
                break;
            default: // Should not be needed
                throw new RuntimeException("Undefined event.");
        }

        return nextEvent;

    }

    @Override
    protected BirthEvent createBirthEvent(final TrajectoryState state,
                                          final double eventHeight,
                                          final NodeRef host) {

        final DHSLModel model = getModel();

        final int nextEventType;
        if (state.getHostCount() > 1) {

            nextEventType = CophyUtils
                    .nextWeightedInteger(model.getDuplicationProportion(),
                                         model.getHostSwitchProportion());

        } else { // No host-switching possible

            nextEventType = 0; // Has to be a duplication

        }

        switch(nextEventType) {
        case 0: // Duplication event
            return new DuplicationEvent(eventHeight, host);
        case 1: // Host-switch event
            final Set<NodeRef> potentialHosts =
                    new HashSet<NodeRef>(state.getHosts());
            potentialHosts.remove(host);
            final NodeRef newHost =
                    CophyUtils.getRandomElement(potentialHosts);
            return new HostSwitchEvent(eventHeight, host, newHost);
        default: // Should not be needed
            throw new RuntimeException("Undefined event.");
        }
    }

    protected static class DuplicationEvent extends BirthEvent {
        private static final String DUPLICATION_EVENT = "duplicationEvent";
        public DuplicationEvent(final double height,
                                final NodeRef host) {
            super(DUPLICATION_EVENT, height, host, host);
        }
    }

    protected static class HostSwitchEvent extends BirthEvent {
        private static final String HOST_SWITCH_EVENT = "hostSwitchEvent";
        public HostSwitchEvent(final double height,
                               final NodeRef sourceHost,
                               final NodeRef destinationHost) {
            super(HOST_SWITCH_EVENT, height, sourceHost, destinationHost);
        }
    }

    protected static class LossEvent extends DeathEvent {
        private static final String LOSS_EVENT = "lossEvent";
        public LossEvent(final double height, final NodeRef host) {
            super(LOSS_EVENT, height, host);
        }
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String DHSL_SIMULATOR = "dhslSimulator";
                private static final String COMPLETE_HISTORY = "completeHistory";

                @Override
                public String getParserName() {
                    return DHSL_SIMULATOR;
                }

                @Override
                public Object parseXMLObject(final XMLObject xo)
                        throws XMLParseException {

                    final DHSLModel model =
                            (DHSLModel) xo.getChild(DHSLModel.class);
                    final boolean complete =
                            xo.getBooleanAttribute(COMPLETE_HISTORY);

                    return new DHSLSimulator(model, complete);

                }

                private final XMLSyntaxRule[] rules = {
                        new ElementRule(DHSLModel.class),
                        AttributeRule.newBooleanRule(COMPLETE_HISTORY)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "Simulator for the duplication, host-switch, loss "
                            + "cophylogeny model.";
                }

                @Override
                public Class<DHSLSimulator> getReturnType() {
                    return DHSLSimulator.class;
                }

    };

}
