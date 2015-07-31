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
import cophy.model.TrajectoryState;
import dr.evolution.tree.NodeRef;
import dr.math.MathUtils;

import java.util.Set;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class CophylogeneticEvent {

    private final String name;
    private final double waitingTime;

    public CophylogeneticEvent(final String name, final double waitingTime) {
        this.name = name;
        this.waitingTime = waitingTime;
    }

    public String getName() {
        return name;
    }

    public boolean isSpeciation() {
        return false;
    }

    public double getWaitingTime() {
        return waitingTime;
    }

    public final double apply(final TrajectoryState state) {
        state.forwardTime(waitingTime);
        return mutateTrajectory(state);
    }

    protected abstract double mutateTrajectory(TrajectoryState state);

    public static abstract class SpeciationEvent extends CophylogeneticEvent {

        public SpeciationEvent(String eventName, double waitingTime) {
            super(eventName, waitingTime);
        }

        @Override
        public final boolean isSpeciation() {
            return true;
        }

    }

    public static class CospeciationEvent extends SpeciationEvent {

        private static final String COSPECIATION_EVENT = "cospeciationEvent";
        private final NodeRef host;
        private final NodeRef leftChild;
        private final NodeRef rightChild;
        private final double height;

        public CospeciationEvent(final NodeRef host, final NodeRef leftChild, final NodeRef rightChild, final double height) {
            super(COSPECIATION_EVENT, 0.0);
            this.host = host;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.height = height;
        }

        @Override
        public double mutateTrajectory(final TrajectoryState state) {
            state.setHeight(height);
            final Set<NodeRef> lineages = state.getGuestLineages(host);
            final int n = state.removeGuests(host);
            state.setGuestCount(leftChild, n);
            state.setGuestCount(rightChild, n);
            for (final NodeRef lineage : lineages)
                state.setGuestLineageHost(lineage, MathUtils.nextBoolean() ? leftChild : rightChild);
            return 1L << lineages.size(); // Premature optimization is the root of all evil!
        }

        public double getHeight() {
            return height;
        }

    }

    public static abstract class BirthEvent extends SpeciationEvent {

        final protected NodeRef sourceHost;
        final protected NodeRef destinationHost;

        public BirthEvent(final String eventName,
                          final double eventHeight,
                          final NodeRef sourceHost,
                          final NodeRef destinationHost) {
            super(eventName, eventHeight);
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
        public double mutateTrajectory(final TrajectoryState state) {
            state.increment(destinationHost);
            final Set<NodeRef> lineages = state.getGuestLineages(sourceHost);
            if (CophyUtils.nextBoolean(lineages.size() / (double) state.getGuestCount(sourceHost))) {
                final NodeRef affectedLineage = CophyUtils.getRandomElement(lineages);
                if (MathUtils.nextBoolean())
                    state.setGuestLineageHost(affectedLineage, destinationHost);
                return 2.0;
            } else {
                return 1.0;
            }
        }

    }

    public static abstract class DeathEvent extends CophylogeneticEvent {

        final protected NodeRef host;

        public DeathEvent(final String eventName,
                          final double waitingTime,
                          final NodeRef host) {
            super(eventName, waitingTime);
            this.host = host;
        }

        @Override
        public double mutateTrajectory(final TrajectoryState state) {
            if (CophyUtils.nextBoolean(state.getGuestLineageCount(host) / (double) state.getGuestCount(host)))
                return 0.0;
            state.decrement(host);
            return 1.0;
        }

    }

}
