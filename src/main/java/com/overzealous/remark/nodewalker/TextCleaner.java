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

package com.overzealous.remark.nodewalker;

import com.overzealous.remark.Options;
import com.overzealous.remark.util.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to clean up plain text fields based on the selected set of options.
 * It optionally escapes certain special characters, as well as replacing various
 * HTML and Unicode entities with their plaintext equivalents.
 *
 * @author Phil DeJarnett
 */
public class TextCleaner {

	/**
	 * Internal class simply used to hold the various escape regexes.
	 */
	private class Escape {
		Pattern pattern;
		String replacement;
		public Escape(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}
	}

	/** Used to track the replacements based on matched groups. */
	private Map<String, String> replacements;
	/** Compiled replacement pattern. */
	private Pattern replacementsPattern;
	/** List of possible escapes */
	private List<Escape> escapes;

	private static final Pattern LINEBREAK_REMOVER = Pattern.compile("(\\s*\\n)+");

	/**
	 * Create a new TextCleaner based on the configured options.
	 * @param options Options that will affect what is cleaned.
	 */
	public TextCleaner(Options options) {
		setupReplacements(options);
		setupEscapes(options);
	}

	/**
	 * Configures the basic replacements based on the configured options.
	 * @param options Options that will affect what is replaced.
	 */
	private void setupReplacements(Options options) {
		this.replacements = new HashMap<String, String> ();

		// Note: the ampersand is special, and must be replaced with care!!
		addRepl("&amp;", "&");
		addRepl("&lt;", "<");
		addRepl("&gt;", ">");
		addRepl("&quot;", "\"");
		if(options.reverseHtmlSmartQuotes) {
			addRepl("&ldquo;", "\"");
			addRepl("&rdquo;", "\"");
			addRepl("&lsquo;", "\'");
			addRepl("&rsquo;", "\'");
			addRepl("&apos;", "\'");
			addRepl("&laquo;", "<<");
			addRepl("&raquo;", ">>");
		}
		if(options.reverseUnicodeSmartQuotes) {
			addRepl("\u201c", "\""); // left double quote: “
			addRepl("\u201d", "\""); // right double quote: ”
			addRepl("\u2018", "\'"); // left single quote: ‘
			addRepl("\u2019", "\'"); // right single quote: ’
			addRepl("\u00ab", "<<"); // left angle quote: «
			addRepl("\u00bb", ">>"); // right angle quote: »
		}
		if(options.reverseHtmlSmartPunctuation) {
			addRepl("&ndash;", "--");
			addRepl("&mdash;", "---");
			addRepl("&hellip;", "...");
		}
		if(options.reverseUnicodeSmartPunctuation) {
			addRepl("\u2013", "--"); // en-dash: –
			addRepl("\u2014", "---"); // em-dash: —
			addRepl("\u2026", "..."); // ellipsis: …
		}
		// build replacement regex
		StringBuilder sb = new StringBuilder(replacements.size()*5);
		// this is a special case for double-encoded HTML entities.
		sb.append("&amp;([#a-z0-9]+;)");
		for(Map.Entry<String, String> rep : replacements.entrySet()) {
			sb.append('|');
			sb.append(Pattern.quote(rep.getKey()));
		}
		
		replacementsPattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
	}

	/**
	 * Utility method to make the code above easier to read.
	 * @param original Original character or string.
	 * @param replacement Replacement character or string.
	 */
	private void addRepl(String original, String replacement) {
		replacements.put(original, replacement);
	}

	/**
	 * Configures the basic escapes based on the configured options.
	 * @param options Options that will affect what is escaped.
	 */
	private void setupEscapes(Options options) {
		escapes = new ArrayList<Escape>();

		// confusingly, this replaces single backslashes with double backslashes.
		// Man, I miss Groovy's slashy strings in these moments...
		escapes.add(new Escape("\\\\", "\\\\"));

		// creates an set of characters that are universally escaped.
		// these characters are wrapped in \Q...\E to ensure they aren't treated as special characters.
		StringBuilder chars = new StringBuilder("([\\Q`*_{}[]#");
		if(options.tables.isConvertedToText() && !options.tables.isRenderedAsCode()) {
			chars.append('|');
		}
		chars.append("\\E])");
		escapes.add(new Escape(chars.toString(), "\\\\$1"));

		// finally, escape certain characters only if they are leading characters
		StringBuilder leadingChars = new StringBuilder("^( ?)([\\Q-+");
		if(options.definitionLists) {
			leadingChars.append(':');
		}
		leadingChars.append("\\E])");
		escapes.add(new Escape(leadingChars.toString(), "$1\\\\$2"));
	}

	/**
	 * Clean the given input text based on the original configuration Options.
	 * Newlines are also replaced with a single space.
	 *
	 * @param input The text to be cleaned.
	 * @return The cleaned text.
	 */
	public String clean(String input) {
		return clean(input, true);
	}

	/**
	 * Clean the given input text based on the original configuration Options.
	 * The text is treat as code, so it is not escaped, and newlines are preserved.
	 *
	 * @param input The text to be cleaned.
	 * @return The cleaned text.
	 */
	public String cleanCode(String input) {
		return clean(input, false);
	}

	/**
	 * Clean the given input text based on the original configuration Options.
	 * Optionally, don't escape special characters.
	 *
	 * @param input The text to be cleaned.
	 * @param normalText If false, don't escape special characters.  This is usually only used for
	 * 					 inline code or code blocks, because they don't need to be escaped.
	 * @return The cleaned text.
	 */
	protected String clean(String input, boolean normalText) {
		if(normalText) {
			// For non-code text, newlines are _never_ allowed.
			// Replace one or more set of whitespace chars followed by a newline with a single space.
			input = LINEBREAK_REMOVER.matcher(input).replaceAll(" ");

			// now escape special characters.
			for(Escape rep : escapes) {
				input = rep.pattern.matcher(input).replaceAll(rep.replacement);
			}
			StringBuffer output = new StringBuffer();
			// if we aren't escaping, don't bother replacing smart quotes, either.
			Matcher m = replacementsPattern.matcher(input);
			while (m.find()) {
				String repString;
				if(replacements.containsKey(m.group().toLowerCase())) {
					repString = replacements.get(m.group().toLowerCase());
				} else {
					// special case for ampersands
					repString = "\\\\&$1";
				}
				m.appendReplacement(output, repString);
			}
			m.appendTail(output);

			return output.toString();
		} else {
			// we have to revert ALL HTML entities for code, because they will end up
			// double-encoded by markdown
			// we also don't need to worry about escaping anything
			// note: we have to manually replace &apos; because it is ignored by StringEscapeUtils for some reason.
			return StringEscapeUtils.unescapeHtml4(input.replace("&apos;", "'"));
		}
	}

	/**
	 * Method to clean inline code, and, if necessary, add spaces to make sure that internal, leading, or
	 * trailing {@code '`'} characters don't break the inline code.
	 * Newlines are also replaced with spaces.
	 *
	 * This method also adds the leading and trailing {@code '`'} or {@code '```'} as necessary.
	 *
	 * @param input String to clean
	 * @return The cleaned text.
	 */
	public String cleanInlineCode(String input) {
		String output = clean(input, false).replace('\n', ' ');
		if(output.indexOf('`') != -1) {
			String prepend = "";
			if(output.charAt(0) == '`') {
				prepend = " ";
			}
			String append = "";
			if(output.charAt(output.length()-1) == '`') {
				append = " ";
			}
			String delim = getDelimiter(output);
			output = String.format("%s%s%s%s%s", delim, prepend, output, append, delim);
		} else {
			output = String.format("`%s`", output);
		}
		return output;
	}

	protected String getDelimiter(String input) {
		int max = 0;
		int counter = 0;
		for(int i=0; i<input.length(); i++) {
			if(input.charAt(i) == '`') {
				counter++;
			} else {
				max = Math.max(max, counter);
				counter = 0;
			}
		}
		// check in case the last tick was at the end.
		max = Math.max(max, counter);
		return StringUtils.multiply('`', max+1);
	}

}
