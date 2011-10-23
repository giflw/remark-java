/*
 * Copyright 2011 OverZealous Creations, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.overzealous.remark.convert;

import com.overzealous.remark.Options;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.regex.Pattern;

/**
 * Handles various inline styling (italics and bold), such as em, i, strong, b, span, and font tags.
 * @author Phil DeJarnett
 */
public class InlineStyle extends AbstractNodeHandler {

	private static final char ITALICS_WRAPPER = '*';
	private static final String BOLD_WRAPPER = "**";

	private static final Pattern ITALICS_PATTERN = Pattern.compile("font-style:\\s*italic", Pattern.CASE_INSENSITIVE);
	private static final Pattern BOLD_PATTERN = Pattern.compile("font-weight:\\s*bold", Pattern.CASE_INSENSITIVE);
	
	private static final Pattern INWORD_CHARACTER = Pattern.compile("\\w");

	private int italicDepth = 0;
	private int boldDepth = 0;

	/**
	 * Renders inline styling (bold, italics) for the given tag.  It handles implicit styling ({@code em}, {@code strong}) as
	 * well as explicit styling via the {@code style} attribute.
	 * <p>This object keeps track of the depth of the styling, to prevent recursive situations like this:</p>
	 *
	 * <blockquote>{@code <em>hello <em>world</em></em>}</blockquote>
	 *
	 * <p>A naive method would be render the example incorrectly (the output would be {@code *hello **world*})</p>
	 *
	 * @param parent The previous node walker, in case we just want to remove an element.
	 * @param node	  Node to handle
	 * @param converter Parent converter for this object.
	 */
	public void handleNode(NodeHandler parent, Element node, DocumentConverter converter) {
		InWord iw = checkInword(node, converter);
		if(iw.emphasisPreserved) {
			Style style = checkTag(node);

			if(style.bold || style.italics) {
				if(iw.addSpacing) {
					converter.output.write(' ');
				}
				start(style, converter);
				converter.walkNodes(this, node, converter.inlineNodes);
				end(style, converter);
				if(iw.addSpacing) {
					converter.output.write(' ');
				}
			} else {
				converter.walkNodes(this, node, converter.inlineNodes);
			}
		} else { // emphasis has been disabled for this section
			// mark as if emphasis was already processed
			italicDepth++;
			boldDepth++;
			converter.walkNodes(this, node, converter.inlineNodes);
			italicDepth--;
			boldDepth--;
		}
	}

	private class InWord {
		boolean emphasisPreserved = true;
		boolean addSpacing = false;
	}

	/**
	 * Handles the situation where InWordEmphasis needs to be manipulated.
	 *
	 * <p>This isn't a terribly intelligent check - it merely looks for the
	 * situation where a styled node is immediately <em>followed</em> by a
	 * text node, and that text node starts with a word character.</p>
	 *
	 * @param node The current node (should be an inline-styled node)
	 * @param converter The current converter
	 * @return flags for checking.
	 */
	private InWord checkInword(Element node, DocumentConverter converter) {
		InWord result = new InWord();
		Options.InWordEmphasis iwe = converter.options.getInWordEmphasis();
		if(!iwe.isEmphasisPreserved() || iwe.isAdditionalSpacingNeeded()) {
			// peek behind for inline styling
			Node n = node.previousSibling();
			if(n != null && n instanceof TextNode) {
				TextNode tn = (TextNode)n;
				String text = tn.text();
				if(INWORD_CHARACTER.matcher(text.substring(text.length()-1)).matches()) {
					result.emphasisPreserved = iwe.isEmphasisPreserved();
					result.addSpacing = iwe.isAdditionalSpacingNeeded();
				}
			}
			// peek ahead for inline styling
			n = node.nextSibling();
			if(n != null && n instanceof TextNode) {
				TextNode tn = (TextNode)n;
				if(INWORD_CHARACTER.matcher(tn.text().substring(0,1)).matches()) {
					result.emphasisPreserved = iwe.isEmphasisPreserved();
					result.addSpacing = iwe.isAdditionalSpacingNeeded();
				}
			}
		}
		return result;
	}

	private class Style {
		boolean italics = false;
		boolean bold = false;
	}

	private Style checkTag(Element node) {
		Style s = new Style();

		String tn = node.tagName();
		if(tn.equals("i") || tn.equals("em")) {
			s.italics = (italicDepth == 0);
		} else if(tn.equals("b") || tn.equals("strong")) {
			s.bold = (boldDepth == 0);
		} else {
			// check inline-style
			if(node.hasAttr("style")) {
				String style = node.attr("style");
				if(ITALICS_PATTERN.matcher(style).find()) {
					s.italics = (italicDepth == 0);
				}
				if(BOLD_PATTERN.matcher(style).find()) {
					s.bold = (boldDepth == 0);
				}
			}
		}

		return s;
	}

	private void start(Style style, DocumentConverter converter) {
		if(style.italics) {
			if(italicDepth == 0) {
				converter.output.write(ITALICS_WRAPPER);
			}
			italicDepth++;
		}
		if(style.bold) {
			if(boldDepth == 0) {
				converter.output.write(BOLD_WRAPPER);
			}
			boldDepth++;
		}
	}

	private void end(Style style, DocumentConverter converter) {
		if(style.bold) {
			boldDepth--;
			if(boldDepth == 0) {
				converter.output.write(BOLD_WRAPPER);
			}
		}
		if(style.italics) {
			italicDepth--;
			if(italicDepth == 0) {
				converter.output.write(ITALICS_WRAPPER);
			}
		}
	}
}
