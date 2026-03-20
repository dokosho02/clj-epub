(ns clj-epub.xhtml
  "Utilities for generating and wrapping XHTML content for EPUB chapters.

  EPUB requires well-formed XHTML5 (not HTML5).  Key differences:
    - Must be valid XML (self-closing tags: <br/> not <br>)
    - Root element must declare XHTML and EPUB namespaces
    - Must include xml:lang attribute
    - Encoding declaration is recommended

  This namespace provides:
    1. `wrap-chapter`  — wraps raw HTML body content in a full XHTML doc
    2. `->bytes`       — convenience: string → UTF-8 byte array
    3. A set of helpers for common structural elements (cover, title page)")

(defn ->bytes
  "Convert a string to UTF-8 bytes."
  ^bytes [^String s]
  (.getBytes s "UTF-8"))

;; ---------------------------------------------------------------------------
;; Core wrapper
;; ---------------------------------------------------------------------------

(defn wrap-chapter
  "Wrap HTML body content in a complete, valid EPUB3 XHTML document.

  Options:
    :title      - <title> element text (default: \"\")
    :css-href   - relative path from this file to the stylesheet
                  (e.g. \"../Styles/stylesheet.css\")
    :lang       - xml:lang value (default: \"en\")
    :epub-type  - epub:type for the <section> element (default: \"chapter\")
    :body-id    - id attribute on <body> (optional)
    :extra-head - additional raw HTML string inserted in <head>

  `content` should be the inner HTML for the chapter body (a string of
  well-formed XHTML fragments, e.g. \"<h1>Title</h1><p>Text</p>\")."
  [content {:keys [title css-href lang epub-type body-id extra-head]
            :or   {title "" lang "en" epub-type "chapter"}}]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       "<!DOCTYPE html>\n"
       "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n"
       "      xmlns:epub=\"http://www.idpf.org/2007/ops\"\n"
       "      xml:lang=\"" lang "\">\n"
       "<head>\n"
       "  <meta charset=\"UTF-8\"/>\n"
       "  <title>" title "</title>\n"
       (when css-href
         (str "  <link rel=\"stylesheet\" type=\"text/css\" href=\"" css-href "\"/>\n"))
       extra-head
       "</head>\n"
       "<body" (when body-id (str " id=\"" body-id "\"")) ">\n"
       "  <section epub:type=\"" epub-type "\">\n"
       content "\n"
       "  </section>\n"
       "</body>\n"
       "</html>\n"))

(defn wrap-chapter-bytes
  "Like `wrap-chapter` but returns a UTF-8 byte array."
  ^bytes [content opts]
  (->bytes (wrap-chapter content opts)))

;; ---------------------------------------------------------------------------
;; Common page templates
;; ---------------------------------------------------------------------------

(defn cover-page
  "Generate a simple cover page XHTML document.

  Options:
    :image-href - relative path to cover image (required)
    :title      - alt text / title (default: \"Cover\")
    :css-href   - optional stylesheet
    :lang       - xml:lang (default: \"en\")"
  [{:keys [image-href title css-href lang]
    :or   {title "Cover" lang "en"}}]
  (wrap-chapter
   (str "<div class=\"cover\">\n"
        "  <img src=\"" image-href "\" alt=\"" title "\"/>\n"
        "</div>")
   {:title     title
    :css-href  css-href
    :lang      lang
    :epub-type "cover"
    :body-id   "cover"}))

(defn title-page
  "Generate a simple title page XHTML document.

  Options:
    :title       - book title (required)
    :author      - author name (optional)
    :publisher   - publisher (optional)
    :date        - publication date string (optional)
    :css-href    - optional stylesheet
    :lang        - xml:lang (default: \"en\")"
  [{:keys [title author publisher date css-href lang]
    :or   {lang "en"}}]
  (wrap-chapter
   (str "<div class=\"titlepage\">\n"
        "  <h1 class=\"title\">" title "</h1>\n"
        (when author   (str "  <p class=\"author\">"    author    "</p>\n"))
        (when publisher (str "  <p class=\"publisher\">" publisher "</p>\n"))
        (when date      (str "  <p class=\"date\">"      date      "</p>\n"))
        "</div>")
   {:title     title
    :css-href  css-href
    :lang      lang
    :epub-type "titlepage"}))

;; ---------------------------------------------------------------------------
;; Default stylesheet
;; ---------------------------------------------------------------------------

(def default-stylesheet
  "A minimal, reader-friendly CSS stylesheet compatible with most EPUB readers."
  "/* clj-epub default stylesheet */

body {
  margin: 5%;
  font-family: serif;
  font-size: 1em;
  line-height: 1.5;
  color: #1a1a1a;
}

h1, h2, h3, h4, h5, h6 {
  font-family: sans-serif;
  font-weight: bold;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  line-height: 1.2;
}

h1 { font-size: 1.8em; }
h2 { font-size: 1.5em; }
h3 { font-size: 1.2em; }

p {
  margin-top: 0;
  margin-bottom: 0.8em;
  text-align: justify;
  -webkit-hyphens: auto;
  hyphens: auto;
}

p + p { text-indent: 1.5em; }

blockquote {
  margin: 1em 2em;
  padding: 0.5em 1em;
  border-left: 3px solid #ccc;
  color: #555;
}

img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1em auto;
}

/* Cover page */
#cover { text-align: center; padding: 0; margin: 0; }
#cover img { width: 100%; height: auto; }

/* Title page */
.titlepage {
  text-align: center;
  margin-top: 3em;
}
.titlepage .title  { font-size: 2em; margin-bottom: 0.3em; }
.titlepage .author { font-size: 1.3em; margin: 0.5em 0; }
.titlepage .publisher, .titlepage .date { color: #666; }

/* Table of contents nav */
nav#toc ol { list-style: none; padding-left: 1em; }
nav#toc a  { text-decoration: none; color: inherit; }
nav#toc a:hover { text-decoration: underline; }

/* Code */
pre, code {
  font-family: monospace;
  font-size: 0.9em;
  background: #f5f5f5;
  border-radius: 3px;
}
pre  { padding: 0.8em; overflow-x: auto; }
code { padding: 0.1em 0.3em; }

/* Footnotes */
aside[epub|type='footnote'] {
  font-size: 0.85em;
  color: #555;
  border-top: 1px solid #ddd;
  margin-top: 2em;
  padding-top: 0.5em;
}
")
