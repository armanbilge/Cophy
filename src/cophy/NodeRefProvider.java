/*
 * NodeRefProvider.java
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

package cophy;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeTraitProvider;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

/**
 *
 * @author Arman D. Bilge <armanbilge@gmail.com>
 *
 */
public class NodeRefProvider extends TreeTraitProvider.Helper {

    public NodeRefProvider(final String traitName) {

        final NodeRefTrait trait = new NodeRefTrait(traitName) {
            @Override
            public NodeRef getTrait(Tree tree, NodeRef node) {
                return node;
            }
        };

        addTrait(trait);
    }

    public static final AbstractXMLObjectParser PARSER =
            new AbstractXMLObjectParser() {

                private static final String NODE_REF_PROVIDER =
                        "nodeRefProvider";
                private static final String TRAIT_NAME = "traitName";

                @Override
                public String getParserName() {
                    return NODE_REF_PROVIDER;
                }

                @Override
                public Object parseXMLObject(XMLObject xo)
                        throws XMLParseException {
                    xo.getAttribute(TRAIT_NAME);
                    return null;
                }

                private final XMLSyntaxRule[] rules = {
                        AttributeRule.newStringRule(TRAIT_NAME)
                };
                @Override
                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                @Override
                public String getParserDescription() {
                    return "Log node numbers to nodes in the tree.";
                }

                @Override
                public Class<NodeRefProvider> getReturnType() {
                    return NodeRefProvider.class;
                }

    };

}
