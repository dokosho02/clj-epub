(ns clj-epub.container
  "Generates META-INF/container.xml.")

(defn generate
  "Returns a UTF-8 byte array containing a valid container.xml.

  Options:
    :opf-path  - path to the OPF file relative to the ZIP root
                 (default: \"OEBPS/content.opf\")"
  ([] (generate {}))
  ([{:keys [opf-path] :or {opf-path "OEBPS/content.opf"}}]
   (.getBytes
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
         "<container version=\"1.0\"\n"
         "           xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
         "  <rootfiles>\n"
         "    <rootfile full-path=\"" opf-path "\"\n"
         "              media-type=\"application/oebps-package+xml\"/>\n"
         "  </rootfiles>\n"
         "</container>\n")
    "UTF-8")))
