# RTF to HTML Cloud Function

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Google Cloud Functions Gen 2](https://img.shields.io/badge/Google%20Cloud-Functions%20Gen%202-yellow.svg)](https://cloud.google.com/functions)
[![ScroogeXHTML 11.4.0](https://img.shields.io/badge/ScroogeXHTML-11.4.0-green.svg)](https://www.scroogexhtml.com/)
[![License: MPL 2.0](https://img.shields.io/github/license/michaelJustin/cloud-function-rtf-to-html.svg)](LICENSE)

RTF in, clean HTML out: a Java 21 Google Cloud Function that converts uploaded RTF documents into self-contained HTML, powered by the [ScroogeXHTML](https://www.scroogexhtml.com/) RTF converter library.

<img width="442" height="383" alt="Online demo: an uploaded RTF document shown as converted HTML" src="https://github.com/user-attachments/assets/f148ad65-2af3-410a-bdcc-2cc5dd3d6af0" />

## Features

- **Rich formatting** — fonts, paragraph indents and borders, tables with cell borders, hyperlinks, bookmarks and footnotes.
- **Self-contained output** — JPG, PNG and BMP pictures up to 256 KB are embedded as Base64 data URIs.
- **Safe by default** — link targets are allow-list filtered by the converter, every response carries a restrictive `Content-Security-Policy`, and the demo pages show the result in a sandboxed iframe.
- **Bounded resources** — uploads are limited to 1 MB and read with a hard cap.
- **Origin-restricted** — only requests from `https://www.scroogexhtml.com` are accepted (CORS).

## Requirements

- JDK 21+ and Maven 3.8+
- [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) (`gcloud`) for deployment
- ScroogeXHTML 11.4.0 and ScroogeXHTML-Pictures 11.4.0 in your Maven repository — see [scroogexhtml.com](https://www.scroogexhtml.com/)

## Quick start

Build the shaded JAR:

```console
mvn clean package
```

Run the function locally on `http://localhost:8080/`:

```console
mvn function:run
```

Convert a document:

```console
curl -H "Origin: https://www.scroogexhtml.com" -F "file=@document.rtf" http://localhost:8080/
```

> [!NOTE]
> Browsers send their own `Origin` header, which the function rejects unless it matches `ALLOWED_ORIGIN` in [`RtfToHtml.java`](src/main/java/demo/RtfToHtml.java). To try [`demo-local.html`](demo-local.html) in a browser, serve it from a local web server and set `ALLOWED_ORIGIN` to that server's origin.

## API

`POST /` with a `multipart/form-data` body. The first part whose file name ends in `.rtf` is converted; the form field name does not matter.

| Status | Meaning |
| --- | --- |
| `200` | Converted document (`text/html; charset=utf-8`) |
| `204` | CORS preflight (`OPTIONS`) from the allowed origin |
| `400` | Not `multipart/form-data`, malformed multipart body, or no `.rtf` file |
| `403` | Missing or disallowed `Origin` |
| `405` | Method other than `POST` or `OPTIONS` |
| `413` | File larger than 1 MB |

Errors are returned as `text/plain` messages.

## Deployment

Deploy the contents of `target/` as a 2nd gen function, or run [`deploy.cmd`](deploy.cmd) on Windows:

```console
gcloud functions deploy rtf-to-html-demo-11 \
  --gen2 \
  --runtime=java21 \
  --region=europe-west10 \
  --entry-point=demo.RtfToHtml \
  --source=./target \
  --trigger-http \
  --allow-unauthenticated \
  --max-instances=3
```

[`demo-prod.html`](demo-prod.html) calls the deployed endpoint.

Files over 1 MB are rejected before conversion. A file close to that limit can still take longer to convert than the default 60 s request timeout (seen with 1 MB of unformatted text on the default instance size); increase `--cpu` or `--timeout` if you need more headroom.

## Configuration

All settings are constants and converter options in [`RtfToHtml.java`](src/main/java/demo/RtfToHtml.java):

| Setting | Default |
| --- | --- |
| `ALLOWED_ORIGIN` | `https://www.scroogexhtml.com` |
| `MAX_SIZE_BYTES` | 1 MB |
| `CONTENT_SECURITY_POLICY` | `default-src 'none'`, inline styles and `data:` images allowed |
| Embedded picture limit | 256 KB (`MemoryPictureAdapterDataURI`) |
| Table cell borders | Taken from the RTF document (`setConvertCellStyles`, experimental) |

The experimental cell-style option logs a warning for every conversion. The converter options (tables, footnotes, hyperlinks, CSS, font size unit, …) are documented at [scroogexhtml.com](https://www.scroogexhtml.com/).

## Learn more

- [ScroogeXHTML RTF converter](https://www.scroogexhtml.com/) — features, documentation and downloads
- [RTF to HTML in the cloud](https://blog.habarisoft.com/2025/07/rtf-to-html-in-the-cloud/) — background article on this project
- [Google Cloud Functions for Java](https://cloud.google.com/functions/docs/create-deploy-http-java)

## License

[Mozilla Public License 2.0](LICENSE)
