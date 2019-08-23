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
import com.overzealous.remark.util.MarkdownTable;
import com.overzealous.remark.util.MarkdownTableCell;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Phil DeJarnett
 */
public class Table extends AbstractNodeHandler {

	// Largest amount of cells found in a row
	private int maxRowCells;
	private boolean hasHeader;

	private static final Pattern STYLE_ALIGNMENT_PATTERN = Pattern.compile("text-align:\\s*([a-z]+)",
			Pattern.CASE_INSENSITIVE);


	public void handleNode(NodeHandler parent, Element node, DocumentConverter converter) {
		MarkdownTable table = new MarkdownTable();
		hasHeader = false;
		maxRowCells = 0;

		processTable(table, node, converter);

		if(!hasHeader) {
			// No header was created, need to insert an empty one for markdown to work
			insertEmptyRow(table.addHeaderRow(), maxRowCells);
		}

		// OK, now render this sucker
		Options.Tables opts = converter.options.getTables();
		converter.output.startBlock();
		table.renderTable(converter.output, opts.isColspanEnabled(), opts.isRenderedAsCode());
		converter.output.endBlock();
	}

	private void processTable(MarkdownTable table, Element node, DocumentConverter converter) {
		// loop over every direct child of the table node.
		for(final Element child : node.children()) {
			String childTagName = child.tagName();
			if(childTagName.equals("thead")) {
				hasHeader = true;
				// handle explicitly declared header sections
				for(final Element headerRow : child.children()) {
					processRow(table.addHeaderRow(), headerRow, converter);
				}

			} else if(childTagName.equals("tbody") || childTagName.equals("tfoot")) {
				// Chance there are headers in body/footer need to go inside to verify.
				processTable(table, child, converter);
			} else if (childTagName.equals("tr")) {
				// Hrm, a row was added outside a valid table body or header...
				if(!child.children().isEmpty()) {
					if(child.children().get(0).tagName().equals("th")) {
						if(hasHeader == true) {
							// already has header, treat this as a regular body row
							processRow(table.addBodyRow(), child, converter);	
						} else {
							hasHeader = true;
							// handle manual TH cells
							processRow(table.addHeaderRow(), child, converter);
						}
						
					} else {
						// OK, must be a table row.
						updateMaxRowCells(child);
						processRow(table.addBodyRow(), child, converter);
					}
				}
			}
		}
	}

	private void processRow(List<MarkdownTableCell> row, Element tableRow, DocumentConverter converter) {
		for(final Element cell : tableRow.children()) {
			String contents = converter.getInlineContent(this, cell, true);
			row.add(new MarkdownTableCell(contents, getAlignment(cell), getColspan(cell)));
		}
	}

	private void insertEmptyRow(List<MarkdownTableCell> row, int numberOfCells) {
		for(int i = 0; i < numberOfCells; i++) {
			row.add(new MarkdownTableCell("", MarkdownTable.Alignment.LEFT));
		}
	}

	private MarkdownTable.Alignment getAlignment(Element cell) {
		MarkdownTable.Alignment alignment = MarkdownTable.Alignment.LEFT;
		String alignmentString = null;
		if(cell.hasAttr("align")) {
			alignmentString = cell.attr("align").toLowerCase();
		} else if(cell.hasAttr("style")) {
			Matcher m = STYLE_ALIGNMENT_PATTERN.matcher(cell.attr("style"));
			if(m.find()) {
				alignmentString = m.group(1).toLowerCase();
			}
		}
		if(alignmentString != null) {
			if(alignmentString.equals("center")) {
				alignment = MarkdownTable.Alignment.CENTER;
			} else if(alignmentString.equals("right")) {
				alignment = MarkdownTable.Alignment.RIGHT;
			}
		}
		return alignment;
	}

	private int getColspan(Element cell) {
		int colspan = 1;
		if(cell.hasAttr("colspan")) {
			try {
				colspan = Integer.parseInt(cell.attr("colspan"));
			} catch(NumberFormatException ex) {
				// ignore invalid numbers
			}
		}
		return colspan;
	}

	/**
	* Updates maxRowCells with the value of the larger amount of cells
	* @param row
	*/
	private void updateMaxRowCells(Element row) {
		int currentRowCells = row.children().size();
		if(currentRowCells > maxRowCells) {
			maxRowCells = currentRowCells;
		}
	}
}
