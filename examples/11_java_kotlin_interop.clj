;; =============================================================================
;; Example 11 — Java / Kotlin usage (EpubBuilder)
;; =============================================================================
;;
;; clj-epub provides io.github.dokosho02.EpubBuilder for use from Java, Kotlin, and Scala.
;;
;; Note: EpubBuilder setter methods return Object (to avoid an AOT circular dependency).
;; For strongly-typed chaining, cast as needed.

;; --- Java usage ---
;;
;; import io.github.dokosho02.EpubBuilder;
;;
;; // Option 1: call methods one by one (no casting needed)
;; EpubBuilder builder = new EpubBuilder();
;; builder.title("My Book");
;; builder.language("en");
;; builder.author("Jane Smith");
;; builder.addChapter("ch01", "Chapter 1", "Text/ch01.xhtml",
;;                    "<h1>Chapter 1</h1><p>Content.</p>");
;; builder.writeToFile("output.epub");
;;
;; // Option 2: get byte array (for HTTP responses)
;; byte[] bytes = builder.toBytes();
;; response.getOutputStream().write(bytes);

;; --- Kotlin usage ---
;;
;; import io.github.dokosho02.EpubBuilder
;;
;; val builder = EpubBuilder()
;; builder.title("My Book")
;; builder.language("en")
;; builder.author("Jane Smith")
;; builder.addChapter("ch01", "Chapter 1", "Text/ch01.xhtml",
;;                    "<h1>Chapter 1</h1><p>Content.</p>")
;; builder.writeToFile("output.epub")
;;
;; // or get a byte array
;; val bytes: ByteArray = builder.toBytes() as ByteArray

;; --- Scala usage ---
;;
;; import io.github.dokosho02.EpubBuilder
;;
;; val builder = new EpubBuilder()
;; builder.title("My Book")
;; builder.language("en")
;; builder.addChapter("ch01", "Chapter 1", "Text/ch01.xhtml",
;;                    "<h1>Chapter 1</h1>")
;; builder.writeToFile("output.epub")

;; --- Maven / Gradle dependency ---
;;
;; Maven:
;; <dependency>
;;   <groupId>io.github.dokosho02</groupId>
;;   <artifactId>clj-epub</artifactId>
;;   <version>1.0.0</version>
;; </dependency>
;;
;; Gradle (Kotlin DSL):
;; implementation("io.github.dokosho02/clj-epub {:mvn/version "1.0.0"}")

;; --- Testing EpubBuilder from Clojure ---
(import 'io.github.dokosho02.EpubBuilder)

(let [builder (EpubBuilder.)]
  (.title    builder "Interop Test Book")
  (.language builder "en")
  (.author   builder "Test Author")
  (.addChapter builder
               "ch01"
               "Chapter 1"
               "Text/ch01.xhtml"
               "<h1>Chapter 1</h1><p>Hello from Java interop!</p>")
  (.writeToFile builder "11-java-interop.epub"))

(println "Generated 11-java-interop.epub via EpubBuilder")
