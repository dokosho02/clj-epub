# Changelog

All notable changes to clj-epub will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [1.0.1] - 2026-03-21

### Changed
- Removed `bodymatter` from `default-landmarks` in `nav.clj`;
  modern readers use spine order to determine the opening position,
  not landmarks. The `body-href` parameter is also removed.

## [1.0.0] - 2026-03-20

### Added
- Initial release.
- `clj-epub.core/make-epub!` — write EPUB to file
- `clj-epub.core/make-epub-bytes` — build EPUB into memory
- `clj-epub.core/make-epub` — low-level entry-map pipeline
- `clj-epub.container` — generates `META-INF/container.xml`
- `clj-epub.opf` — generates `content.opf` (Package Document)
- `clj-epub.nav` — generates EPUB3 `nav.xhtml` with TOC, page-list, landmarks
- `clj-epub.ncx` — generates EPUB2-compatible `toc.ncx`
- `clj-epub.xhtml` — XHTML chapter wrapper, cover page, title page templates
- `clj-epub.zip` — OCF-compliant ZIP packaging (mimetype STORED, first)
- Default CSS stylesheet for good typographic defaults across readers
- Full Sigil directory convention (`Text/`, `Images/`, `Fonts/`, `Styles/`)
- Series metadata support (`belongs-to-collection`)
- Multiple creator support with OPF roles
- Nested TOC entries
- EPUB2 compatibility toggle (`:epub2-compat false` to omit NCX)
- 60+ unit and integration tests
