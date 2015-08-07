/*
 * CophylogeneticEvent.java
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

import cophy.CophyUtils;
import cophy.model.TrajectoryState;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
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

        public final double apply(final TrajectoryState state, final Tree tree, final Set<NodeRef> speciatingNodes) {
            state.forwardTime(getWaitingTime());
            return mutateTrajectory(state, tree, speciatingNodes);
        }

        public abstract double mutateTrajectory(TrajectoryState state, Tree tree, Set<NodeRef> speciatingNodes);

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

        @Override
        public double mutateTrajectory(final TrajectoryState state, final Tree tree, final Set<NodeRef> speciatingNodes) {
            state.setHeight(height);
            final int n = state.removeGuests(host);
            state.setGuestCount(leftChild, n);
            state.setGuestCount(rightChild, n);
            for (final NodeRef guest : speciatingNodes) {
                final int i = MathUtils.nextInt(2);
                final NodeRef leftGuest = tree.getChild(guest, i);
                final NodeRef rightGuest = tree.getChild(guest, 1 - i);
                state.setGuestLineageHost(leftGuest, leftChild);
                state.setGuestLineageHost(rightGuest, rightChild);
            }
            return 1.0;
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
            final Set<NodeRef> lineages = state.getGuestLineages(sourceHost);
            final double lineageAffected = lineages.size() / (double) state.getGuestCount(sourceHost);
            state.increment(destinationHost);
            if (CophyUtils.nextBoolean(lineageAffected)) {
                final NodeRef affectedLineage = CophyUtils.getRandomElement(lineages);
                if (MathUtils.nextBoolean())
                    state.setGuestLineageHost(affectedLineage, destinationHost);
                return 2.0;
            } else {
                return 1.0;
            }
        }

        @Override
        public double mutateTrajectory(final TrajectoryState state, final Tree tree, final Set<NodeRef> speciatingNodes) {
            state.increment(destinationHost);
            final int i = MathUtils.nextInt(2);
            final NodeRef speciatingNode = speciatingNodes.iterator().next();
            final NodeRef leftGuest = tree.getChild(speciatingNode, i);
            final NodeRef rightGuest = tree.getChild(speciatingNode, 1 - i);
            state.setGuestLineageHost(leftGuest, sourceHost);
            state.setGuestLineageHost(rightGuest, destinationHost);
            return 1.0;
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
