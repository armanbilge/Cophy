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

package cophy.simulation;

import java.util.HashSet;
import java.util.Set;

import cophy.CophylogenyUtils;
import cophy.model.DHSLModel;
import cophy.simulation.CophylogeneticEvent.BirthEvent;
import cophy.simulation.CophylogeneticEvent.DeathEvent;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.SimpleNode;
import dr.evolution.tree.Tree;
import dr.math.MathUtils;
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
public class DHSLSimulator extends CophylogenySimulator<DHSLModel> {

    public DHSLSimulator(final DHSLModel model, final boolean complete) {

        super(model, complete);

    }

    @Override
    protected SimpleNode simulateSubtree(final SimpleNode guestNode,
                                         final NodeRef hostNode,
                                         double height,
                                         final double until) {

        final Tree hostTree = model.getHostTree();
        final double duplicationRate = model.getDuplicationRate();
        final double hostSwitchRate = model.getHostSwitchRate();
        final double lossRate = model.getLossRate();

        guestNode.setAttribute(HOST, hostNode);

        final SimpleNode left, right;

        final int event;
        if (hostTree.isRoot(hostNode)) { // No host-switching at root
            event = CophylogenyUtils.nextWeightedInteger(duplicationRate,
                                                         lossRate);
            height -= CophylogenyUtils.nextPoissonTime(duplicationRate,
                                                       lossRate);
        } else {
            event = CophylogenyUtils.nextWeightedInteger(duplicationRate,
                                                         lossRate,
                                                         hostSwitchRate);
            height -= CophylogenyUtils.nextPoissonTime(duplicationRate,
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
                        CophylogenyUtils.getLineagesAtHeight(hostTree, height);
                potentialHosts.remove(hostNode);
                hosts[1]  = CophylogenyUtils.getRandomElement(potentialHosts);

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

        guestNode.addChild(left);
        guestNode.addChild(right);

        return guestNode;

    }

    public void resumeSimulation(final CophylogeneticTrajectory trajectory,
                                 final double until) {

        final CophylogeneticTrajectoryState state =
                trajectory.getCurrentState();
        while (state.getHeight() > until) {

            final double nextCospeciationEventHeight =
                    trajectory.getNextCospeciationEvent().getHeight();
            while (state.getHeight() > until) {

                final int guestCount = state.getTotalGuestCount();
                final double normalizedDuplicationRate =
                        guestCount * model.getDuplicationRate();
                final double normalizedHostSwitchRate =
                        guestCount * model.getHostSwitchRate();
                final double normalizedLossRate =
                        guestCount * model.getLossRate();
                final double nextEventHeight;

                if (state.getHostCount() > 1) {

                    nextEventHeight = CophylogenyUtils
                            .nextPoissonTime(normalizedDuplicationRate,
                                             normalizedLossRate,
                                             normalizedHostSwitchRate);

                } else { // No host-switching possible

                    nextEventHeight = CophylogenyUtils
                            .nextPoissonTime(normalizedDuplicationRate,
                                             normalizedLossRate);

                }

                if (nextEventHeight <= nextCospeciationEventHeight)
                    break;

                final int nextEventType;
                if (state.getHostCount() > 1) {

                    nextEventType = CophylogenyUtils
                            .nextWeightedInteger(normalizedDuplicationRate,
                                                 normalizedLossRate,
                                                 normalizedHostSwitchRate);

                } else { // No host-switching possible

                    nextEventType = CophylogenyUtils
                            .nextWeightedInteger(normalizedDuplicationRate,
                                                 normalizedLossRate);

                }

                final NodeRef affectedHost = state.getRandomWeightedHost();

                final CophylogeneticEvent nextEvent;

                switch(nextEventType) {
                case 0: // Duplication event
                    nextEvent = new DuplicationEvent(nextEventHeight,
                                                     affectedHost);
                    break;
                case 1: // Loss event
                    nextEvent = new LossEvent(nextEventHeight, affectedHost);
                    break;
                case 2: // Host-switch event
                    final Set<NodeRef> potentialHosts =
                            new HashSet<NodeRef>(state.getHosts());
                    potentialHosts.remove(affectedHost);
                    final NodeRef newHost =
                            CophylogenyUtils.getRandomElement(potentialHosts);
                    nextEvent = new HostSwitchEvent(nextEventHeight,
                                                    affectedHost,
                                                    newHost);
                default: // Should not be needed
                    throw new RuntimeException("Undefined event.");
                }

                trajectory.addEvent(nextEvent);

            }

            trajectory.applyNextCospeciationEvent();

        }

    }

    @Override
    public double simulateBirthEvent(final CophylogeneticTrajectory trajectory,
                                     final double eventHeight,
                                     final NodeRef source) {

        final CophylogeneticTrajectoryState state =
                trajectory.getStateAtHeight(eventHeight);

        final int weight = state.getGuestCountAtHost(source);

        final int nextEventType;
        if (state.getHostCount() > 1) {

            nextEventType = CophylogenyUtils
                    .nextWeightedInteger(model.getDuplicationProportion(),
                                         model.getHostSwitchProportion());

        } else { // No host-switching possible

            nextEventType = 0; // Has to be a duplication

        }

        final CophylogeneticEvent nextEvent;

        switch(nextEventType) {
        case 0: // Duplication event
            nextEvent = new DuplicationEvent(eventHeight, source);
            break;
        case 1: // Host-switch event
            final Set<NodeRef> potentialHosts =
                    new HashSet<NodeRef>(state.getHosts());
            potentialHosts.remove(source);
            final NodeRef newHost =
                    CophylogenyUtils.getRandomElement(potentialHosts);
            nextEvent = new HostSwitchEvent(eventHeight, source, newHost);
        default: // Should not be needed
            throw new RuntimeException("Undefined event.");
        }

        trajectory.addEvent(nextEvent);

        return weight;

    }

    protected static class DuplicationEvent extends BirthEvent {
        private static final String DUPLICATION_EVENT = "duplicationEvent";
        public DuplicationEvent(final double height,
                                final NodeRef node) {
            super(DUPLICATION_EVENT, height, node, node);
        }
    }

    protected static class HostSwitchEvent extends BirthEvent {
        private static final String HOST_SWITCH_EVENT = "hostSwitchEvent";
        public HostSwitchEvent(final double height,
                               final NodeRef source,
                               final NodeRef destination) {
            super(HOST_SWITCH_EVENT, height, source, destination);
        }
    }

    protected static class LossEvent extends DeathEvent {
        private static final String LOSS_EVENT = "lossEvent";
        public LossEvent(final double height, final NodeRef node) {
            super(LOSS_EVENT, height, node);
        }
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String DHSL_SIMULATOR = "DHSLSimulator";
                private static final String COMPLETE_HISTORY =
                        "completeHistory";

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
