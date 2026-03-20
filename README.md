# clj-epub

This library is in alpha now!

----

A Clojure library for generating **EPUB3** ebooks, with full EPUB2 backward compatibility and Epubcheck compatibility.

[![Clojure CI](https://img.shields.io/badge/clojure-1.12-blue)](https://clojure.org)
[![License](https://img.shields.io/badge/license-EPL--2.0-green)](LICENSE)
[![Maven Central](https://img.shields.io/badge/maven-1.0.0-orange)](https://central.sonatype.com)

---

## Features

- ✅ **EPUB3 compliant** — `package version="3.0"`, `nav.xhtml`, `dcterms:modified`
- ✅ **EPUB2 backward compatible** — `toc.ncx` included by default
- ✅ **Sigil compatible** — `Text/`, `Images/`, `Fonts/`, `Styles/` directory convention
- ✅ **Correct ZIP packaging** — `mimetype` is always first, always STORED
- ✅ **Data-driven API** — plain Clojure maps, no macros, no magic
- ✅ **Zero config** — sensible defaults (UUID, timestamps, CSS) auto-filled
- ✅ **Composable** — each namespace is independently usable

---

## Installation

### deps.edn

```clojure
{:deps {org.clojars.dokosho02/clj-epub {:mvn/version "1.0.1"}}}
```

### Leiningen / Boot

```clojure
[org.clojars.dokosho02/clj-epub "1.0.1"]
```

### Maven

```xml
<dependency>
  <groupId>org.clojars.dokosho02</groupId>
  <artifactId>clj-epub</artifactId>
  <version>1.0.1</version>
</dependency>
```

---

## Quick Start

```clojure
(require '[clj-epub.core :as epub])

;; Write to a file
(epub/make-epub!
  {:metadata {:title    "My First Book"
              :author   "Jane Smith"
              :language "en"}
   :chapters [{:id      "ch01"
               :title   "Chapter 1"
               :href    "Text/ch01.xhtml"
               :content "<h1>Chapter 1</h1><p>Hello, EPUB world!</p>"}
              {:id      "ch02"
               :title   "Chapter 2"
               :href    "Text/ch02.xhtml"
               :content "<h1>Chapter 2</h1><p>More content here.</p>"}]}
  "my-first-book.epub")
```

That's it. The output is a valid EPUB3 file that opens in iBooks, Calibre, Sigil, and any standard e-reader.

---

## API Reference

### `clj-epub.core`

#### `make-epub! [opts output-path]`

Build an EPUB and write it to disk. Returns the output path.

```clojure
(epub/make-epub! opts "book.epub")
```

#### `make-epub-bytes [opts]`

Build an EPUB and return raw bytes. Useful for HTTP responses or in-memory processing.

```clojure
(let [bytes (epub/make-epub-bytes opts)]
  (ring.util.response/response (java.io.ByteArrayInputStream. bytes)))
```

#### `make-epub [opts]`

Low-level: returns a seq of `{:path "..." :data <bytes>}` maps.
Pass these to `clj-epub.zip/write-epub!` or `clj-epub.zip/epub->bytes`.

---

## Options Reference

### `:metadata` (required)

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `:title` | string | **yes** | Book title |
| `:language` | string | **yes** | BCP-47 language code (`"en"`, `"zh"`, `"fr"`) |
| `:identifier` | string | no | URN/UUID — auto-generated if omitted |
| `:author` | string | no | Single author name |
| `:creators` | seq | no | Multiple creators: `[{:name "..." :role "aut" :file-as "..."}]` |
| `:publisher` | string | no | Publisher name |
| `:date` | string | no | ISO-8601 date (`"2024-01-15"`) |
| `:description` | string | no | Book synopsis |
| `:subject` | string | no | Single subject/tag |
| `:subjects` | seq | no | Multiple subjects |
| `:rights` | string | no | Copyright/license string |
| `:source` | string | no | Original source URL |
| `:series` | string | no | Series title |
| `:series-position` | int | no | Position in series |
| `:modified` | string | no | ISO-8601 datetime — auto-generated if omitted |

### `:chapters` (seq of maps)

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `:id` | string | **yes** | Unique manifest ID (no spaces, e.g. `"ch01"`) |
| `:title` | string | **yes** | Display title (used in TOC) |
| `:href` | string | **yes** | Path inside OEBPS (`"Text/ch01.xhtml"`) |
| `:content` | string | one of | Raw inner HTML/XHTML for body |
| `:data` | bytes | one of | Pre-rendered XHTML byte array (bypasses wrapping) |
| `:linear` | boolean | no | `false` to exclude from spine linear order (default `true`) |
| `:toc` | seq | no | Nested TOC entries: `[{:title "..." :href "...#id" :toc [...]}]` |

### `:cover` (optional)

```clojure
:cover {:image-href "Images/cover.jpg"   ; path inside OEBPS
        :image-data (slurp-bytes "cover.jpg")  ; raw image bytes
        :image-id   "cover-image"}        ; manifest id (default: "cover-image")
```

### `:css` (optional)

- `nil` or omitted — use the built-in default stylesheet
- a string — use as the stylesheet content
- `false` — include no stylesheet at all

### `:resources` (optional)

Extra files (images, fonts, audio, etc.):

```clojure
:resources [{:id         "font-regular"
             :href        "Fonts/MyFont.ttf"
             :media-type  "font/ttf"          ; auto-inferred if omitted
             :data        (slurp-bytes "MyFont.ttf")}]
```

### `:options` (optional)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `:opf-path` | string | `"OEBPS/content.opf"` | OPF path in ZIP root |
| `:nav-href` | string | `"nav.xhtml"` | Nav doc path relative to OEBPS |
| `:ncx-href` | string | `"toc.ncx"` | NCX path relative to OEBPS |
| `:css-href` | string | `"Styles/stylesheet.css"` | CSS path relative to OEBPS |
| `:epub2-compat` | boolean | `true` | Include `toc.ncx` for EPUB2 readers |
| `:guide` | seq | auto | Override OPF `<guide>` refs |

---

## Examples

### Full-featured book

```clojure
(epub/make-epub!
  {:metadata {:title       "The Art of Clojure"
              :language    "en"
              :creators    [{:name "Rich Hickey" :role "aut"}]
              :publisher   "REPL Press"
              :date        "2024-03-01"
              :description "A deep dive into Clojure's philosophy."
              :subjects    ["Programming" "Functional Programming"]
              :rights      "© 2024 REPL Press. All rights reserved."
              :series      "Clojure Mastery"
              :series-position 1}

   :cover {:image-href "Images/cover.jpg"
           :image-data (java.nio.file.Files/readAllBytes
                         (java.nio.file.Paths/get "cover.jpg" (into-array String [])))}

   :chapters [{:id      "cover-page"
               :title   "Cover"
               :href    "Text/cover.xhtml"
               :content "<div class=\"cover\"><img src=\"../Images/cover.jpg\" alt=\"Cover\"/></div>"
               :linear  false}
              {:id      "titlepage"
               :title   "Title Page"
               :href    "Text/titlepage.xhtml"
               :content "<div class=\"titlepage\">
                           <h1>The Art of Clojure</h1>
                           <p class=\"author\">Rich Hickey</p>
                         </div>"}
              {:id      "ch01"
               :title   "Part 1: Simplicity"
               :href    "Text/ch01.xhtml"
               :content "<h1>Part 1: Simplicity</h1>
                         <p>Simplicity is the ultimate sophistication.</p>"
               :toc     [{:title "What is Simplicity?"
                           :href  "Text/ch01.xhtml#s1"}]}]

   :css "body { font-family: Georgia, serif; line-height: 1.6; }"

   :resources [{:id   "font"
                :href "Fonts/SourceSerif.otf"
                :data (java.nio.file.Files/readAllBytes
                        (java.nio.file.Paths/get "SourceSerif.otf"
                                                  (into-array String [])))}]

   :options {:epub2-compat true}}

  "the-art-of-clojure.epub")
```

### In-memory (Ring handler)

```clojure
(defn epub-handler [request]
  (let [bytes (epub/make-epub-bytes my-book-opts)]
    {:status  200
     :headers {"Content-Type"        "application/epub+zip"
               "Content-Disposition" "attachment; filename=\"book.epub\""}
     :body    (java.io.ByteArrayInputStream. bytes)}))
```

### Using pre-built XHTML templates

```clojure
(require '[clj-epub.xhtml :as xhtml])

;; Auto-generate a cover page
(def cover-html
  (xhtml/cover-page {:image-href "../Images/cover.jpg"
                     :title      "My Book"
                     :css-href   "../Styles/stylesheet.css"}))

;; Auto-generate a title page
(def title-html
  (xhtml/title-page {:title     "My Book"
                     :author    "Author Name"
                     :publisher "Publisher"
                     :date      "2024"}))
```

### Accessing individual generators

Each sub-namespace is independently usable:

```clojure
(require '[clj-epub.container :as container])
(require '[clj-epub.opf       :as opf])
(require '[clj-epub.nav       :as nav])
(require '[clj-epub.ncx       :as ncx])
(require '[clj-epub.zip       :as zip])

;; Build individual XML components
(container/generate {:opf-path "Content/book.opf"})  ;=> bytes
(nav/generate {:toc [{:title "Ch1" :href "Text/ch01.xhtml"}]})  ;=> bytes

;; Custom ZIP assembly
(zip/write-epub! "book.epub"
  [{:path "META-INF/container.xml" :data (container/generate)}
   {:path "OEBPS/content.opf"      :data my-opf-bytes}
   ...])
```

---

## EPUB Directory Structure (Generated)

```
book.epub
├── mimetype                          ← "application/epub+zip", STORED, first
├── META-INF/
│   └── container.xml                 ← points to content.opf
└── OEBPS/
    ├── content.opf                   ← Package Document (metadata+manifest+spine)
    ├── nav.xhtml                     ← EPUB3 navigation (TOC, landmarks)
    ├── toc.ncx                       ← EPUB2 navigation (compatibility)
    ├── Styles/
    │   └── stylesheet.css
    ├── Text/
    │   ├── cover.xhtml
    │   ├── titlepage.xhtml
    │   └── ch01.xhtml ...
    ├── Images/
    │   └── cover.jpg ...
    └── Fonts/
        └── *.ttf / *.otf ...
```

---

## Development

### Run tests

```bash
clojure -M:test -m cognitect.test-runner
```

### Build JAR

```bash
clojure -T:build jar
```

### Install to local Maven

```bash
clojure -T:build install
```

### Deploy to Clojars / Maven Central

```bash
# Set credentials first:
# export CLOJARS_USERNAME=...
# export CLOJARS_PASSWORD=...
clojure -T:build deploy
```

---

## EPUB Validation

Use [EPUBCheck](https://github.com/w3c/epubcheck) to validate generated files:

```bash
java -jar epubcheck.jar my-book.epub
```

Or add it as a test dependency in `deps.edn`:

```clojure
{:aliases {:epubcheck {:extra-deps {org.w3c/epubcheck {:mvn/version "5.1.0"}}}}}
```

---

## Compatibility

| Reader | Tested | Notes |
|--------|--------|-------|
| **Sigil** | ✅ | Full edit/open compatibility |
| **Apple Books** | ✅ | iBooks on macOS |

---

## License

Copyright © 2026 dokosho02.

Distributed under the [Eclipse Public License 2.0](LICENSE).
