/**
 * DHSLSimulator.java
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

package cophy.dhsl;

import cophy.CophyUtils;
import cophy.simulation.CophylogeneticEvent;
import cophy.simulation.CophylogeneticEvent.BirthEvent;
import cophy.simulation.CophylogeneticEvent.DeathEvent;
import cophy.simulation.CophylogeneticTrajectoryState;
import cophy.simulation.CophylogenySimulator;
import cophy.simulation.MutableCophylogeneticTrajectoryState;
import dr.evolution.tree.FlexibleNode;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.math.MathUtils;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

import java.util.HashSet;
import java.util.Map;
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

        final Tree hostTree = model.getHostTree();
        final double duplicationRate = model.getDuplicationRate();
        final double hostSwitchRate = model.getHostSwitchRate();
        final double lossRate = model.getLossRate();

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
                if (!complete) return null;
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

        if (!complete) {
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
    protected CophylogeneticEvent nextEvent(final CophylogeneticTrajectoryState state) {

        final int guestCount = state.getTotalGuestCount();
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

        final Map<NodeRef,Integer> guestCounts = state.getGuestCounts();
        final NodeRef guest = CophyUtils.nextWeightedObject(guestCounts);
        final NodeRef host = CophyUtils.nextWeightedObject(state.getGuestCountAtHosts(guest));

        final CophylogeneticEvent nextEvent;


        switch(nextEventType) {
            case 0: // Duplication event
                nextEvent = new DuplicationEvent(nextEventHeight, guest, host);
                break;
            case 1: // Loss event
                nextEvent = new LossEvent(nextEventHeight, guest, host);
                break;
            case 2: // Host-switch event
                final Set<NodeRef> potentialHosts = new HashSet<NodeRef>(state.getHosts());
                potentialHosts.remove(host);
                final NodeRef newHost = CophyUtils.getRandomElement(potentialHosts);
                nextEvent = new HostSwitchEvent(nextEventHeight, guest, host, newHost);
            default: // Should not be needed
                throw new RuntimeException("Undefined event.");
        }

        return nextEvent;

    }

    @Override
    public double simulateBirthEvent(final MutableCophylogeneticTrajectoryState state,
                                     final double eventHeight,
                                     final NodeRef guest,
                                     final NodeRef host) {

        final int weight = state.getGuestCountAtHost(guest, host);

        final int nextEventType;
        if (state.getHostCount() > 1) {

            nextEventType = CophyUtils
                    .nextWeightedInteger(model.getDuplicationProportion(),
                                         model.getHostSwitchProportion());

        } else { // No host-switching possible

            nextEventType = 0; // Has to be a duplication

        }

        final CophylogeneticEvent nextEvent;

        switch(nextEventType) {
        case 0: // Duplication event
            nextEvent = new DuplicationEvent(eventHeight, guest, host);
            break;
        case 1: // Host-switch event
            final Set<NodeRef> potentialHosts =
                    new HashSet<NodeRef>(state.getHosts());
            potentialHosts.remove(host);
            final NodeRef newHost =
                    CophyUtils.getRandomElement(potentialHosts);
            nextEvent = new HostSwitchEvent(eventHeight, guest, host, newHost);
        default: // Should not be needed
            throw new RuntimeException("Undefined event.");
        }

        return weight;

    }

    protected static class DuplicationEvent extends BirthEvent {
        private static final String DUPLICATION_EVENT = "duplicationEvent";
        public DuplicationEvent(final double height,
                                final NodeRef guest,
                                final NodeRef host) {
            super(DUPLICATION_EVENT, height, guest, host, host);
        }
    }

    protected static class HostSwitchEvent extends BirthEvent {
        private static final String HOST_SWITCH_EVENT = "hostSwitchEvent";
        public HostSwitchEvent(final double height,
                               final NodeRef guest,
                               final NodeRef sourceHost,
                               final NodeRef destinationHost) {
            super(HOST_SWITCH_EVENT, height, guest, sourceHost, destinationHost);
        }
    }

    protected static class LossEvent extends DeathEvent {
        private static final String LOSS_EVENT = "lossEvent";
        public LossEvent(final double height, final NodeRef guest, final NodeRef host) {
            super(LOSS_EVENT, height, guest, host);
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
