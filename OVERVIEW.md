# Overview: RTF to HTML Cloud Function

## Executive Summary

The `RtfToHtml` cloud function is a Google Cloud Function (`HttpFunction`) designed to convert RTF (Rich Text Format) documents into self-contained HTML documents using the [ScroogeXHTML](https://www.scroogexhtml.com) library. It is designed to serve web conversion requests originating strictly from the ScroogeXHTML website (`https://www.scroogexhtml.com`).

---

## Architecture & Implementation Details

### 1. Request Lifecycle & Access Control
- **CORS Handling**: Automatically emits `Vary: Origin` headers and handles HTTP `OPTIONS` preflight requests, allowing `POST, OPTIONS` methods with cached preflight parameters (`Access-Control-Max-Age: 3600`).
- **Origin Whitelisting**: Restricts execution strictly to `https://www.scroogexhtml.com`. Requests from other origins are rejected with `403 Forbidden`.
- **Method Validation**: Only HTTP `POST` requests are processed for document conversion; other methods return `405 Method Not Allowed`.

### 2. Payload Validation & Resource Guardrails
- **Early Size Check**: Rejects payloads exceeding the maximum limit (`request.getContentLength() > MAX_SIZE_BYTES + 256`) with `413 File is too large` before parsing multipart bodies.
- **Bounded Stream Reading**: Uses `readLimited(InputStream)` to enforce a strict 1 MB (`1024 * 1024` bytes) cap during stream reading to protect serverless memory from unbounded streams.
- **Multipart Parsing**: Validates `multipart/form-data` content headers, parses boundaries via `jakarta.mail.internet.ContentType`, and extracts files ending in `.rtf`.

### 3. RTF to HTML Conversion (`ScroogeXHTML`)
- **Document Structure Preservation**: Retains bookmarks, hyperlinks, tables, borders, indentation, empty paragraphs, and footnotes.
- **Styling Configuration**: Uses standard typography defaults (font sizes in points) and embeds default stylesheet rules (`DEFAULT_CSS`).
- **Inline Image Handling**: Uses `MemoryPictureAdapterDataURI(256)` to embed image assets directly as base64 Data URIs, producing fully self-contained HTML without external storage dependencies.
- **Post-Processing & Provenance**: Implements `MyPostProcessListener` to append a `<footer>` containing original filename, size in KB, and service attribution to the HTML `<body>`.

### 4. Deterministic HTTP Error Signaling
- `200 OK`: Successful conversion returning `text/html; charset=utf-8`.
- `204 No Content`: Successful CORS preflight (`OPTIONS`).
- `400 Bad Request`: Missing/invalid multipart headers, missing boundary, or missing `.rtf` payload.
- `403 Forbidden`: Unauthorized request origin.
- `405 Method Not Allowed`: Unsupported HTTP methods.
- `413 Payload Too Large`: Upload exceeds size limits.

---

## Key Strengths

1. **Abuse Prevention & Cost Control**: Whitelisting origins and enforcing dual-layer upload limits (Content-Length header check + bounded stream reading) protects cloud infrastructure from resource exhaustion and unauthorized third-party exploitation.
2. **Self-Contained Artifacts**: Inlining pictures as Data URIs eliminates external asset storage requirements, broken image links, and extra network roundtrips.
3. **Structured Attribution**: Post-processing guarantees transparent metadata and source file tracking in converted outputs.

---

## Recommendations & Optimization Opportunities

1. **Memory & Stream Optimization**:
   - Currently, RTF content is buffered entirely into a `byte[]` via `readLimited` and wrapped in `ByteArrayInputStream`. Streaming directly into `converter.convert(InputStream)` with a counting/limiting filter stream can cut peak heap allocations per invocation in half.
2. **Multipart Stream Drainage**:
   - The parser terminates the multipart loop immediately after reading the first `.rtf` part. Properly consuming or closing remaining parts avoids lingering connection resources.
3. **Output Sanitization**:
   - If downstream consumers render the generated HTML directly in browser contexts, adding an HTML sanitizer (e.g., OWASP Java HTML Sanitizer) provides defense-in-depth against malicious payloads embedded inside RTF structures.
