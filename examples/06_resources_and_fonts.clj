;; =============================================================================
;; Example 06 — Extra resources: fonts and images
;; =============================================================================
;;
;; Demonstrates:
;;   - :resources field to embed font files (OTF/TTF)
;;   - :resources field to embed inline images
;;   - @font-face CSS declaration referencing embedded fonts
;;   - Relative image paths from chapter XHTML to Images/
;;
;; Directory conventions (Sigil-compatible):
;;   Fonts/   — font files
;;   Images/  — images
;;   Text/    — chapter XHTML
;;   Styles/  — CSS
;;   Image path from chapter: ../Images/xxx.png

(require '[clj-epub.core :as epub])

(defn read-bytes [path]
  (java.nio.file.Files/readAllBytes
   (java.nio.file.Paths/get path (into-array String []))))

(epub/make-epub!
 {:metadata {:title    "Resources Demo"
             :language "en"
             :author   "Demo Author"}

  :chapters [{:id      "ch01"
              :title   "Chapter 1: Typography"
              :href    "Text/ch01.xhtml"
              :content "<h1>Chapter 1: Typography</h1>
                        <p>This chapter uses a custom embedded font.</p>
                        <p>The font is loaded via CSS @font-face declaration.</p>"}

             {:id      "ch02"
              :title   "Chapter 2: Images"
              :href    "Text/ch02.xhtml"
              ;; Image path in chapter content uses relative path: Text/ to Images/ requires ../
              :content "<h1>Chapter 2: Images</h1>
                        <p>Below is an embedded figure:</p>
                        <figure>
                          <img src=\"../Images/diagram.jpg\" alt=\"Architecture Diagram\"/>
                          <figcaption>Figure 1: System Architecture</figcaption>
                        </figure>"}]

  ;; Custom CSS with @font-face declaration
  :css "@font-face {
  font-family: 'SourceSerif';
  src: url('../Fonts/Vollkorn-Regular.ttf') format('truetype');
  font-weight: normal;
}
body {
  font-family: 'SourceSerif', Georgia, serif;
  line-height: 1.7;
  margin: 6%;
}
h1 { font-size: 1.8em; margin-bottom: 0.8em; }
figure { text-align: center; margin: 1.5em 0; }
figcaption { font-size: 0.85em; color: #666; margin-top: 0.4em; }
img { max-width: 100%; height: auto; }"

  ;; Extra resources: fonts and images
  :resources [{:id         "font-regular"
               :href        "Fonts/Vollkorn-Regular.ttf"
               :media-type  "font/ttf"
               :data        (read-bytes "Vollkorn-Regular.ttf")}

              {:id         "img-diagram"
               :href        "Images/diagram.jpg"
               ;; :media-type  "image/jpg"
               :data        (read-bytes "diagram.jpg")}]}

 "06-resources-and-fonts.epub")
