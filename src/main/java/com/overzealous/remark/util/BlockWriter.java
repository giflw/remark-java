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

package com.overzealous.remark.util;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * This is a customized subclass of BufferedWriter that handles working with Markdown block-level elements.
 * In the case of a non-block-level element occurring outside a block, it is automatically promoted.
 *
 * @author Phil DeJarnett
 */
public class BlockWriter extends PrintWriter {

	private int blockDepth = 0;

	private boolean autoStartedBlock = false;

	private boolean empty = true;

	private StringWriter sw = null;

	/**
	 * Creates a new, empty BlockWriter with a StringWriter as the buffer.
	 * To get the contents of the StringWriter, call BlockWriter.toString()
	 *
	 * @see #toString()
	 * @return new BlockWriter
	 */
	public static BlockWriter create() {
		StringWriter sw = new StringWriter();
		BlockWriter bw = new BlockWriter(new BufferedWriter(sw));
		bw.sw = sw;
		return bw;
	}

	/**
	 * Creates a new BlockWriter with an arbitrary output stream.
	 * @param out The output stream that is buffered and written to.
	 */
	public BlockWriter(Writer out) {
		super(out);
	}

    @Override
	public void write(int c) {
		testNewBlock();
		super.write(c);
    }

    @Override
	public void write(char cbuf[], int off, int len) {
		testNewBlock();
		super.write(cbuf, off, len);
    }

    @Override
	public void write(String s, int off, int len) {
		testNewBlock();
		super.write(s, off, len);
    }

	/**
	 * Tests to see if we need to forcibly start a new block.
	 * This is used in cases where an inline element is rendered outside a block element, such as:
	 *
	 * <blockquote>{@code <p>foo</p> <em>bar</em> <p>baz</p>}</blockquote>
	 *
	 * In this case, {@code bar} is promoted to it's own block element.
	 *
	 * Note: this only occurs at the top level.  Otherwise, once inside a block, it is rendered as
	 * part of whatever block is currently being rendered.
	 *
	 */
	private void testNewBlock(){
		if(blockDepth == 0) {
			startBlock();
			// keep track of automatically started blocks.  See startBlock below.
			autoStartedBlock = true;
		}
	}

	/**
	 * Starts a new block.  This is useful when streaming out content within a block.
	 * This method keeps track of the current block depth, so make sure that
	 * {@link #endBlock()} is called when the block is completed.
	 */
	public void startBlock() {
		if(autoStartedBlock) {
			// if following an auto-started block, don't increment the block depth
			// this is because these blocks will never be closed.
			autoStartedBlock = false;
		} else {
			// otherwise, increment block depth so we can keep track of how far down we've traveled.
			blockDepth++;
		}
		if(empty) {
			// if this is the first block printed, then don't actually do anything.
			empty = false;
		} else {
			// otherwise, print two lines, so an empty line occurs between the
			println();
			println();
		}
	}

	/**
	 * Ends a block.  The depth counter is decreased, so we know when we are back at the root.
	 */
	public void endBlock() {
		if(blockDepth > 0) {
			blockDepth--;
		}
	}

	/**
	 * Writes an entire block in one go.
	 * This method automatically handles starting and ending the block.
	 *
	 * @param blockText The text of the block.
	 */
	public void writeBlock(Object blockText) {
		startBlock();
		print(blockText);
		endBlock();
	}

	/**
	 * Alias for {@link #writeBlock(Object)}.
	 * @param blockText The text of the block.
	 */
	public void printBlock(Object blockText) {
		writeBlock(blockText);
	}

	/**
	 * Returns how deep the number of blocks is.
	 *
	 * {@code 0} means that no blocks are currently active.
	 *
	 * @return block depth
	 */
	public int getBlockDepth() {
		return blockDepth;
	}

	/**
	 * Returns true if nothing has been written to the stream yet.
	 * @return true if nothing has been written yet.
	 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * If this object has been created using {@link #create()}, returns the StringWriter output buffer.
	 *
	 * @return the buffer for this BlockWriter
	 */
	public StringWriter getBuffer() {
		return sw;
	}

	/**
	 * If this object has been created using {@link #create()}, this will return the contents
	 * of the StringWriter buffer.
	 *
	 * Otherwise, this returns the default Object.toString() method.
	 *
	 * @return The contents of the buffer, or a generic Object method.
	 */
	public String toString() {
		if(this.sw != null) {
			this.flush();
			return sw.toString();
		} else {
			return super.toString();
		}
	}

}
