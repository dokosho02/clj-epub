(ns clj-epub.zip
  "Low-level EPUB ZIP packaging.

  The EPUB OCF specification (§3.3) mandates:

    1. The 'mimetype' file MUST be the first entry in the ZIP archive.
    2. The 'mimetype' file MUST be stored uncompressed (STORE, not DEFLATE).
    3. The 'mimetype' file MUST NOT contain extra fields in its ZIP header.
    4. The file content MUST be exactly:  application/epub+zip

  All other files may (and should) use DEFLATE compression.

  This namespace wraps java.util.zip.ZipOutputStream to enforce these rules."
  (:import [java.io
            ByteArrayOutputStream
            File
            FileOutputStream
            OutputStream]
           [java.util.zip
            CRC32
            ZipEntry
            ZipOutputStream]))

(def ^:private mimetype-content
  "The exact bytes required by the OCF spec."
  (.getBytes "application/epub+zip" "US-ASCII"))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(defn- crc32 ^long [^bytes data]
  (let [crc (CRC32.)]
    (.update crc data)
    (.getValue crc)))

(defn- add-stored-entry!
  "Add a file entry using STORE (no compression).
   size, compressedSize, and crc must be set before writing data — this is
   required by ZipOutputStream when using STORED method."
  [^ZipOutputStream zos name ^bytes data]
  (let [entry (ZipEntry. name)]
    (.setMethod entry ZipEntry/STORED)
    (.setSize   entry (alength data))
    (.setCompressedSize entry (alength data))
    (.setCrc    entry (crc32 data))
    ;; Eliminate the extra field that Java adds by default (for STORED entries
    ;; the spec says there must be no extra field in the local header).
    (.setExtra  entry (byte-array 0))
    (.putNextEntry zos entry)
    (.write zos data)
    (.closeEntry zos)))

(defn- add-deflated-entry!
  "Add a file entry using DEFLATE compression (standard)."
  [^ZipOutputStream zos name ^bytes data]
  (let [entry (ZipEntry. name)]
    (.setMethod entry ZipEntry/DEFLATED)
    (.putNextEntry zos entry)
    (.write zos data)
    (.closeEntry zos)))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn write-epub!
  "Write an EPUB archive to `output` (a File, path String, or OutputStream).

  `entries` is an ordered seq of maps:
    {:path  \"OEBPS/content.opf\"   ; path inside the ZIP
     :data  <byte-array>}          ; file contents

  The mimetype entry is always written first automatically — do NOT include
  it in `entries`.

  Returns nil (side-effectful)."
  [output entries]
  (let [^OutputStream out (cond
                            (instance? OutputStream output) output
                            (instance? File output) (FileOutputStream. ^File output)
                            (string? output)        (FileOutputStream. ^String output)
                            :else (throw (ex-info "Invalid output type"
                                                  {:type (type output)})))
        zos (ZipOutputStream. out)]
    ;; 1. mimetype — MUST be first, STORED
    (add-stored-entry! zos "mimetype" mimetype-content)
    ;; 2. All other entries — DEFLATED
    (doseq [{:keys [path data]} entries]
      (add-deflated-entry! zos path data))
    (.finish zos)
    (when-not (instance? OutputStream output)
      (.close zos))))

(defn epub->bytes
  "Build an EPUB and return its raw bytes (useful for in-memory / HTTP).

  Same `entries` contract as `write-epub!`."
  ^bytes [entries]
  (let [baos (ByteArrayOutputStream.)]
    (write-epub! baos entries)
    (.toByteArray baos)))
