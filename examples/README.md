# clj-epub Examples

From simple to complex — a progressive guide to using clj-epub.

## Example index

| File | Description |
|------|-------------|
| `01_minimal.clj` | Minimal usage: three fields, valid EPUB |
| `02_multiple_chapters.clj` | Multiple chapters + full metadata |
| `03_cover_and_titlepage.clj` | Cover image + cover page + title page |
| `04_custom_css.clj` | Custom CSS / disable CSS |
| `05_multiple_creators_and_series.clj` | Multiple creators + series metadata |
| `06_resources_and_fonts.clj` | Embedded fonts and images |
| `07_in_memory_http.clj` | In-memory generation + Ring HTTP response |
| `08_pre_rendered_xhtml.clj` | Pre-rendered XHTML (`:data` field) |
| `09_chinese_book.clj` | Chinese-language book with CJK typography |
| `10_full_production.clj` | Full production-grade configuration |
| `11_java_kotlin_interop.clj` | Java / Kotlin / Scala usage via EpubBuilder |

## How to run

Each file is a standalone Clojure script. Add the library to your `deps.edn` first:

```clojure
{:deps {io.github.dokosho02/clj-epub {:mvn/version "1.0.0"}}}
```

Then run:

```bash
clojure -M examples/01_minimal.clj
clojure -M examples/02_multiple_chapters.clj
# ...
```

Or evaluate interactively in a REPL:

```bash
clojure -M:dev
;; => (load-file "examples/01_minimal.clj")
```

## Key concepts at a glance

### Required fields

```clojure
{:metadata {:title    "Book Title"   ; required
            :language "en"}          ; required — BCP-47 code

 :chapters [{:id      "ch01"                ; required — manifest id
             :title   "Chapter 1"           ; required — shown in TOC
             :href    "Text/ch01.xhtml"      ; required — path inside ZIP
             :content "<h1>...</h1><p>...</p>"}]}  ; inner HTML body
```

### Two ways to supply chapter content

```clojure
;; Option 1: :content string (recommended)
;; The library wraps it into a complete XHTML document automatically.
{:content "<h1>Title</h1><p>Body</p>"}

;; Option 2: :data byte array
;; Pass a fully-rendered XHTML document; the library writes it as-is.
{:data (some-renderer chapter-source)}
```

### Cover page pattern

```clojure
;; 1. :cover field — the cover image (used for bookshelf thumbnails)
:cover {:image-href "Images/cover.jpg"
        :image-data cover-image-bytes}

;; 2. Cover chapter — linear=false keeps it out of the reading flow.
;;    The id can be anything; the library detects it via linear=false.
{:id "any-name-you-like"
 :linear false
 :content "..."}
```

### CSS options

```clojure
;; Use the built-in default CSS (recommended for getting started)
{:css nil}   ; or simply omit :css

;; Custom CSS string
{:css "body { font-family: sans-serif; }"}

;; No stylesheet at all
{:css false}
```
