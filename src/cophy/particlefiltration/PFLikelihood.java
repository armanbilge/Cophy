/*
 * PFLikelihood.java
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

package cophy.particlefiltration;

import dr.inference.model.AbstractModelLikelihood;
import dr.inference.model.Model;
import dr.inference.model.Variable;
import dr.inference.model.Variable.ChangeType;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public abstract class PFLikelihood extends AbstractModelLikelihood {

    private static final long serialVersionUID = 6497579271787679306L;

    private final Model model;

    private double logLikelihood;
    private double storedLogLikelihood;
    private boolean likelihoodKnown;
    private boolean storedLikelihoodKnown;

    public PFLikelihood(final Model model) {
        super("CachingLikelihood");
        this.model = model;
        addModel(model);
        model.addModelListener(this);
    }

    @Override
    public Model getModel() {
        return this;
    }

    public Model getWrappedModel() {
        return model;
    }

    @Override
    public double getLogLikelihood() {
        if (!isLikelihoodKnown()) {
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    protected abstract double calculateLogLikelihood();

    protected boolean isLikelihoodKnown() {
        return likelihoodKnown;
    }

    @Override
    public void makeDirty() {
        // Do nothing
    }

    protected void trulyMakeDirty() {
        likelihoodKnown = false;
    }

    @Override
    protected void handleModelChangedEvent(final Model model,
                                           final Object object,
                                           final int index) {
        trulyMakeDirty();
    }

    @Override
    protected void handleVariableChangedEvent(@SuppressWarnings("rawtypes")
                                              final Variable variable,
                                              final int index,
                                              final ChangeType type) {
        trulyMakeDirty();
    }

    @Override
    protected void storeState() {
        storedLogLikelihood = logLikelihood;
        storedLikelihoodKnown = likelihoodKnown;
    }

    @Override
    protected void restoreState() {
        logLikelihood = storedLogLikelihood;
        likelihoodKnown = storedLikelihoodKnown;
    }

    @Override
    protected void acceptState() {
        // Nothing to do
    }

}
