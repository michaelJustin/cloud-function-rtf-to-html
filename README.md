# RTF to HTML Google Cloud Function

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Google Cloud Functions Gen 2](https://img.shields.io/badge/Google%20Cloud-Functions%20Gen%202-yellow.svg)](https://cloud.google.com/functions)
[![ScroogeXHTML](https://img.shields.io/badge/ScroogeXHTML-11.3.0-green.svg)](https://www.scroogexhtml.com/)
[![License](https://img.shields.io/github/license/michaelJustin/cloud-function-rtf-to-html.svg)](LICENSE)

A high-performance Google Cloud Function (`HttpFunction`) in Java 21 that converts Rich Text Format (RTF) documents to clean, self-contained HTML using the [ScroogeXHTML RTF Converter](https://www.scroogexhtml.com/) library.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture & Conversion Flow](#architecture--conversion-flow)
- [Prerequisites](#prerequisites)
- [Quickstart: Local Development](#quickstart-local-development)
- [HTTP API Reference](#http-api-reference)
- [Configuration](#configuration)
- [Build and Deployment](#build-and-deployment)
- [Limitations & Guardrails](#limitations--guardrails)
- [Further Reading](#further-reading)

---

## Overview

The `RtfToHtml` cloud function accepts RTF document uploads via HTTP `POST` multipart requests, converts the RTF document structure, formatting, tables, and images into HTML, and returns the resulting HTML document as a self-contained payload.

It is designed to power serverless document conversion workflows with bounded memory consumption and origin-scoped security.

---

## Key Features

- **Serverless RTF Conversion**: Fast, on-demand conversion without maintaining dedicated server infrastructure.
- **Embedded Inline Images**: Converts embedded images (PNG, JPEG, etc. up to 256 KB) directly to inline Base64 Data URIs via `MemoryPictureAdapterDataURI`, producing self-contained HTML documents without external storage dependencies.
- **Comprehensive Document Formatting**: Preserves hyperlinks, bookmarks, tables, borders, margins, indentation, font styling, and footnotes.
- **Resource & Abuse Protection**: Enforces payload size limits (up to 1024 KB), bounded stream reading, and origin validation.
- **Automated Metadata Attribution**: Appends document provenance and conversion metadata to the HTML output footer.
- **Local Testing & Demos Included**: Pre-configured with the Google Cloud Functions Framework Maven plugin and sample HTML test clients.

---

## Architecture & Conversion Flow

```
[ Client Browser / Website ]
           │
           │ HTTP POST (multipart/form-data)
           ▼
[ Google Cloud Function: RtfToHtml ]
     ├── 1. CORS & Origin Validation (https://www.scroogexhtml.com)
     ├── 2. Size Check & Bounded Multipart Parsing (Max 1024 KB)
     ├── 3. ScroogeXHTML Conversion Engine
     │       ├── Apply CSS styles & typography
     │       ├── Convert structure (tables, bookmarks, links)
     │       └── Inline images as Data URIs
     ├── 4. Post-processing: Append document footer & metadata
     └── 5. Return HTTP 200 (text/html; charset=utf-8)
```

---

## Prerequisites

To build, test, and deploy this function locally, ensure you have:

- **Java Development Kit (JDK)**: Version 21 or higher
- **Apache Maven**: Version 3.8+
- **Google Cloud CLI (`gcloud`)**: Required for Cloud Function deployment
- **ScroogeXHTML Library**: Version 11.3.0 or compatible

---

## Quickstart: Local Development

### 1. Start the Local Function Server

Run the Cloud Functions Framework local invoker using Maven:

```console
mvn function:run
```

Once running, the function will be available at `http://localhost:8080/`.

### 2. Test with the Local Demo Client

Open `demo-local.html` directly in your browser:
- Select any `.rtf` file (under 1024 KB).
- Click **Convert**.
- The converted HTML will be rendered immediately within the preview frame.

> **Note**: For local testing without strict origin restrictions, adjust or mock `ALLOWED_ORIGIN` in `RtfToHtml.java` if sending requests from custom browser origins or test tools.

---

## HTTP API Reference

### Endpoint Details

- **Method**: `POST` (or `OPTIONS` for CORS preflight)
- **Path**: `/` (or deployed Cloud Function URL)
- **Content-Type**: `multipart/form-data`

### Request Headers

| Header | Description | Required |
| :--- | :--- | :--- |
| `Origin` | Request origin (must match `https://www.scroogexhtml.com`) | Yes |
| `Content-Type` | `multipart/form-data; boundary=...` | Yes |

### Multipart Form Fields

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `file` (or any part) | File (`*.rtf`) | The RTF document to convert (maximum 1024 KB). |

### HTTP Status Responses

| Status Code | Description |
| :--- | :--- |
| `200 OK` | Document converted successfully. Returns `text/html; charset=utf-8`. |
| `204 No Content` | CORS preflight request (`OPTIONS`) handled successfully. |
| `400 Bad Request` | Invalid content type, missing multipart boundary, or no `.rtf` file found in request. |
| `403 Forbidden` | Request origin header missing or does not match `ALLOWED_ORIGIN`. |
| `405 Method Not Allowed` | HTTP method is not `POST` or `OPTIONS`. |
| `413 Payload Too Large` | Uploaded file or payload exceeds the 1024 KB limit. |

---

## Configuration

RTF-to-HTML conversion parameters are configured in [`src/main/java/demo/RtfToHtml.java`](src/main/java/demo/RtfToHtml.java):

```java
ScroogeXHTML converter = new ScroogeXHTML();
converter.setAddOuterHTML(true);

// Header & styling configuration
converter.getHtmlHeadConfig().setStyleSheetInclude(DEFAULT_CSS);
converter.getCharPropConvConfig().setFontSizeUnit(LengthUnit.POINT);

// Structural elements
converter.setConvertBookmarks(true);
converter.setConvertHyperlinks(true);
converter.setConvertTables(true);
converter.setConvertFootnotes(true);
converter.setConvertParagraphBorders(true);

// Image handling: Base64 Data URI adapter
converter.setConvertPictures(true);
converter.setPictureAdapter(new MemoryPictureAdapterDataURI(256));
```

---

## Build and Deployment

### 1. Package the Function

Build the shaded JAR file containing all runtime dependencies:

```console
mvn clean package
```

### 2. Deploy to Google Cloud Functions (Gen 2)

Deploy the packaged JAR using Google Cloud CLI or run the provided `deploy.cmd` script on Windows:

```console
gcloud functions deploy rtf-to-html-demo-11 \
  --gen2 \
  --entry-point=demo.RtfToHtml \
  --runtime=java21 \
  --region=europe-west10 \
  --source=./target \
  --trigger-http \
  --allow-unauthenticated
```

### 3. Production Testing

Open `demo-prod.html` in your browser (configured to call the production Cloud Function endpoint) to verify live deployments.

---

## Limitations & Guardrails

- **File Size Limit**: Maximum allowed RTF file size is **1024 KB (1 MB)** to ensure optimal serverless performance and predictable memory utilization.
- **Allowed Origin**: Restricted by default to requests originating from `https://www.scroogexhtml.com`.
- **Image Size Limit**: Individual embedded images are bounded to 256 KB when generating Data URIs.

---

## Further Reading

- [ScroogeXHTML Official Documentation](https://www.scroogexhtml.com/)
- [Google Cloud Functions Java Framework](https://cloud.google.com/functions/docs/create-deploy-http-java)
- [RTF to HTML in the Cloud (Habari Blog)](https://blog.habarisoft.com/2025/07/rtf-to-html-in-the-cloud/)
