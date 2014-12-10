/*
 * NodeRefProvider.java
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
