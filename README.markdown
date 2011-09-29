# OverZealous Creations Remark

**Remark** is a library for taking (X)HTML input and outputting clean [Markdown][], [Markdown Extra][], or [MultiMarkdown][] compatible text.  The purpose of this conversion is mainly to allow for the use of client-side HTML GUI editors while retaining safe, mobile-device editable markdown text behind the scenes.  It is recommended that the markdown text is stored, to reduce XSS attacks by code injection.

## Example Usage Scenario

 * The user logs in from their desktop.
	 * Adding some text, the user inputs into a full-featured GUI, such as [Dojo's rich text editor][dojo_rte], or [any of these editors][other_rtes].
	 * The webserver takes the generated HTML, which may contain a lot of bad HTML, depending on the browser, and passes it to **Remark**.
		 * **Remark** passes the HTML to [jsoup][], to clean up the input text, which strips unsupported HTML tags (the text will remain).
		 * **Remark** walks the generated DOM tree, and outputs clean, structured markdown text.
		 * The markdown text is returned.
	 * The webserver stores this markdown text for future display.
 * The user chooses to re-edit the HTML text from their desktop.
	 * The webserver converts the Markdown back to HTML, and sends it to the client.
	 * Repeat the steps above to save it.
 * The user later logs in from their mobile device.
	 * Mobile devices often not support rich text editing through the web browser.
	 * So, instead, render a plain text field with the raw markdown text.
	 * Because markdown is relatively easy to read and edit, the user can make simple changes without struggling with hundreds of messy HTML tags.
	 * If necessary, **Remark** can accept this input as well, and strip any HTML tags for security.

## Advanced Features

**Remark** can be configured to output extra functionality beyond straight markdown.

 * [Markdown Extra tables][] or [Multimarkdown tables][] (which add column spanning support), including a best-guess attempt at alignment (based on style or align attributes)
 * Reversal of various smart HTML entities or unicode characters:
	 * `&ldquo;` (“) and `&rdquo;` (”) become `"`
	 * `&lsquo;` (‘), `&rsquo;` (’), and `&apos;` become `'`
	 * `&laquo;` («) becomes `<<`
	 * `&raquo;` (») becomes `>>`
	 * `&hellip;` (…) becomes `...`
	 * `&endash;` (–) becomes `--`
	 * `&emdash;` (—) becomes `---`
 * Simplified hardwraps — A `<br/>` is converted to just a single linebreak, instead of `(space)(space)(newline)`, common in most third-party markdown renderers
 * Autolinks — a link that has the same content as it's label (and starts with http or https) is simply rendered as is, like `http://www.overzealous.com`
 * [Markdown Extra definition lists][]
 * [Markdown Extra abbreviations][]
 * Fenced code blocks, using either [Markdown Extra's format][Markdown Extra fenced code block] using `~~~`, or [Github's format][Github fenced code block] using ` ``` `
 * Customization of allowed HTML tags - not really recommended.

The basic theory is that you match the extensions to your Markdown conversion library.

---

## A Note on Forking:

Want to fork this project?  *Great!*  However, please note that I use [hgflow][] to manage the develop-release cycle.  If you are uncomfortable with that, that's fine, too!  Just switch to the **develop** branch before working, or I won't be able to easily merge the changes back in.

Source code build is done via [Gradle][].

## License

**Remark** is released under the Apache 2.0 license.

	Copyright 2011 OverZealous Creations, LLC

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

[Markdown]: http://daringfireball.net/projects/markdown/
[Markdown Extra]: http://michelf.com/projects/php-markdown/extra/
[Markdown Extra tables]: http://michelf.com/projects/php-markdown/extra/#table
[Markdown Extra definition lists]: http://michelf.com/projects/php-markdown/extra/#def-list
[Markdown Extra fenced code block]: http://michelf.com/projects/php-markdown/extra/#fenced-code-blocks
[Markdown Extra abbreviations]: http://michelf.com/projects/php-markdown/extra/#abbr
[MultiMarkdown]: http://fletcherpenney.net/multimarkdown/
[MultiMarkdown tables]: http://fletcher.github.com/peg-multimarkdown/#tables
[Github fenced code block]: http://github.github.com/github-flavored-markdown/
[dojo_rte]: http://dojotoolkit.org/reference-guide/dijit/Editor.html
[other_rtes]: http://www.queness.com/post/212/10-jquery-and-non-jquery-javascript-rich-text-editors
[jsoup]: http://jsoup.org/
[hgflow]: https://bitbucket.org/yinwm/hgflow/wiki/Home
[Gradle]: http://gradle.org/