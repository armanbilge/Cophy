/*
 * CophylogeneticEvent.java
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

package cophy.simulation;

import cophy.CophyUtils;
import cophy.model.Reconciliation;
import cophy.simulation.MutableCophylogeneticTrajectoryState.InvalidCophylogeneticTrajectoryStateException;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import org.apache.commons.math.util.MathUtils;

import java.util.Map;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class CophylogeneticEvent {

    protected final String name;
    protected final double height;

    public CophylogeneticEvent(final String name,
                               final double height) {
        this.name = name;
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public double getHeight() {
        return height;
    }

    public boolean isSpeciation() {
        return false;
    }

    public final void apply(final MutableCophylogeneticTrajectoryState state) throws CophylogeneticEventFailedException {
        state.setHeight(height);
        mutateTrajectory(state);
    }

    protected abstract void mutateTrajectory(MutableCophylogeneticTrajectoryState state)
            throws CophylogeneticEventFailedException;

    public static abstract class SpeciationEvent extends CophylogeneticEvent {

        public SpeciationEvent(String eventName, double eventHeight) {
            super(eventName, eventHeight);
        }

        @Override
        public final boolean isSpeciation() {
            return true;
        }

        public final double
                getProbabilityUnobserved(final
                                         CophylogeneticTrajectoryState state,
                                         final Tree guestTree,
                                         final Reconciliation reconciliation) {

            if (state.getHeight() != height)
                throw new RuntimeException("State incompatible with event.");

            return calculateProbabilityUnobserved(state,
                                                  guestTree,
                                                  reconciliation);
        }

        protected abstract double
                calculateProbabilityUnobserved(final
                                               CophylogeneticTrajectoryState
                                               state,
                                               final Tree guestTree,
                                               final
                                               Reconciliation reconciliation);

        public final double
                getProbabilityObserved(final
                                       CophylogeneticTrajectoryState state,
                                       final Tree guestTree,
                                       final Reconciliation reconciliation) {

            if (state.getHeight() != height)
                throw new RuntimeException("State incompatible with event.");

            return calculateProbabilityObserved(state,
                                                guestTree,
                                                reconciliation);

}

        protected abstract double
                calculateProbabilityObserved(final
                                             CophylogeneticTrajectoryState
                                             state,
                                             final Tree guestTree,
                                             final
                                             Reconciliation reconciliation);


    }

    public static class CospeciationEvent extends SpeciationEvent {

        private static final String COSPECIATION_EVENT = "cospeciationEvent";
        protected final Tree hostTree;
        protected final NodeRef host;

        public CospeciationEvent(final Tree hostTree,
                                 final NodeRef host,
                                 final double eventHeight) {
            super(COSPECIATION_EVENT, eventHeight);
            this.hostTree = hostTree;
            this.host = host;
        }

        @Override
        public void mutateTrajectory(final MutableCophylogeneticTrajectoryState state) {
            final Map<NodeRef,Integer> guestCounts = state.getGuestCountsAtHost(host);
            state.removeHost(host);
            final NodeRef left = hostTree.getChild(host, 0);
            final NodeRef right = hostTree.getChild(host, 1);
            state.addHost(left);
            state.addHost(right);
            state.setGuestCountsAtHost(left, guestCounts);
            state.setGuestCountsAtHost(right, guestCounts);
        }

        @Override
        protected double
                calculateProbabilityUnobserved(final
                                               CophylogeneticTrajectoryState
                                               state,
                                               final Tree guestTree,
                                               final
                                               Reconciliation reconciliation) {

            final int observedGuestCount = 1;
            final int completeGuestCount = state.getGuestCountAtHost(host);
            // TODO Replace factorial division with a simplified form for
            // increased numerical stability
            final double invalidCombinations =
                    MathUtils.factorialDouble(observedGuestCount);
            final double totalCombinations =
                    MathUtils.factorialDouble(completeGuestCount);

            return (totalCombinations - invalidCombinations)
                    / totalCombinations;
        }

        @Override
        protected double
                calculateProbabilityObserved(final
                                             CophylogeneticTrajectoryState
                                             state,
                                             final Tree guestTree,
                                             final
                                             Reconciliation reconciliation) {

            final int completeGuestCount = state.getGuestCountAtHost(host);
            final double totalCombinations = MathUtils.factorialDouble(completeGuestCount);
            return 1.0 / totalCombinations;
        }

    }

    public static abstract class BirthEvent extends SpeciationEvent {

        final protected NodeRef guest;
        final protected NodeRef sourceHost;
        final protected NodeRef destinationHost;

        public BirthEvent(final String eventName,
                          final double eventHeight,
                          final NodeRef guest,
                          final NodeRef sourceHost,
                          final NodeRef destinationHost) {
            super(eventName, eventHeight);
            this.guest = guest;
            this.sourceHost = sourceHost;
            this.destinationHost = destinationHost;
        }

        public NodeRef getSource() {
            return sourceHost;
        }

        public NodeRef getDestination() {
            return destinationHost;
        }

        @Override
        public void mutateTrajectory(final MutableCophylogeneticTrajectoryState state) {
            state.incrementGuestCountAtHost(guest, destinationHost);
        }

        @Override
        protected double
                calculateProbabilityUnobserved(final
                                               CophylogeneticTrajectoryState
                                               state,
                                               final Tree guestTree,
                                               final
                                               Reconciliation reconciliation) {

            return 1.0;
        }

        @Override
        protected double
                calculateProbabilityObserved(final
                                             CophylogeneticTrajectoryState
                                             state,
                                             final Tree guestTree,
                                             final
                                             Reconciliation reconciliation) {

            final int completeGuestCountSource =
                    state.getGuestCountAtHost(guest, sourceHost);

            final long totalCombinations;
            if (sourceHost.equals(destinationHost)) {
                totalCombinations = CophyUtils
                        .extendedBinomialCoefficient(completeGuestCountSource,
                                                     2);
            } else {
                final int completeGuestCountDestination =
                        state.getGuestCountAtHost(guest, destinationHost);

                totalCombinations = completeGuestCountSource
                        * completeGuestCountDestination;

            }

            return 1.0 / totalCombinations;
        }

    }

    public static abstract class DeathEvent extends CophylogeneticEvent {

        final protected NodeRef guest;
        final protected NodeRef host;

        public DeathEvent(final String eventName,
                          final double eventHeight,
                          final NodeRef guest,
                          final NodeRef host) {
            super(eventName, eventHeight);
            this.guest = guest;
            this.host = host;
        }

        @Override
        public void mutateTrajectory(final MutableCophylogeneticTrajectoryState state)
                throws CophylogeneticEventFailedException {
            try {
                state.decrementGuestCountAtHost(guest, host);
            } catch (final InvalidCophylogeneticTrajectoryStateException e) {
                throw new CophylogeneticEventFailedException(this);
            }
        }

    }

    public static class CophylogeneticEventFailedException
            extends RuntimeException {

        private static final long serialVersionUID = -1074026006660524662L;

        public CophylogeneticEventFailedException(final CophylogeneticEvent event) {
            super(event.getName() + " failed to make valid change to state");
        }

    }

}
