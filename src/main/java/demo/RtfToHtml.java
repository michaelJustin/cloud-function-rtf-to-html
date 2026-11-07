/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package demo;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.scroogexhtml.ScroogeXHTML;
import com.scroogexhtml.css.LengthUnit;
import com.scroogexhtml.pictures.MemoryPictureAdapterDataURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

/**
 * This Java class implements a Google Cloud Function (HttpFunction) that converts an RTF document
 * to HTML using the ScroogeXHTML library.
 * The RTF file is sent as a {@code multipart/form-data} HTTP POST upload; the function responds
 * with the converted HTML document, or with a plain-text error message.
 */
public class RtfToHtml implements HttpFunction {

    private static final int MAX_SIZE_BYTES = 1024 * 1024; // 1024 KB
    // Allowance for the multipart framing around the file (boundaries, part headers
    // including the file name); the file itself is limited by readLimited()
    private static final int MAX_MULTIPART_OVERHEAD_BYTES = 16 * 1024;
    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; " +
            "img-src 'self' data:; " +
            "style-src 'unsafe-inline'; " +
            "font-src 'self' data:; " +
            "base-uri 'none'; " +
            "frame-ancestors 'none'; " +
            "form-action 'none';";
    private static final String DEFAULT_CSS = """
            body,p {
              margin: 0;
            }
            td {
              vertical-align: top;
            }
            table {
              border-collapse: collapse;
            }
            """;
    private static final String ALLOWED_ORIGIN = "https://www.scroogexhtml.com";
    private static final Logger log = LoggerFactory.getLogger(RtfToHtml.class);

    static {
        // The HTML validator brings Saxon-HE, which registers itself as the JAXP
        // TransformerFactory, but rejects the secure-processing properties ScroogeXHTML
        // sets. Keep the JDK's built-in factory for JAXP (vnu calls Saxon directly).
        System.setProperty("javax.xml.transform.TransformerFactory",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    }

    /**
     * Handles HTTP requests to convert an RTF document to HTML.
     * Only requests coming from https://www.scroogexhtml.com are accepted.
     * This is intentional to limit the usage to code hosted on the ScroogeXHTML
     * website.
     *
     * @param request  The HTTP request with the RTF file as a multipart/form-data upload.
     * @param response The HTTP response to write the HTML output or an error message to.
     * @throws Exception If writing the response or converting the document fails.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String origin = request.getFirstHeader("Origin").orElse(null);

        // CORS and CSP headers on EVERY response (including errors)
        response.appendHeader("Vary", "Origin");
        response.appendHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        if (ALLOWED_ORIGIN.equals(origin)) {
            response.appendHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
        }

        // Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            if (ALLOWED_ORIGIN.equals(origin)) {
                response.appendHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
                response.appendHeader("Access-Control-Allow-Headers", "Content-Type");
                response.appendHeader("Access-Control-Max-Age", "3600");
                response.setStatusCode(204);
            } else {
                response.setStatusCode(403);
            }
            return;
        }

        if (request.getContentLength() > MAX_SIZE_BYTES + MAX_MULTIPART_OVERHEAD_BYTES) {
            error(response,413, "File is too large");
            return;
        }
        
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatusCode(405);
            return;
        }

        if (!ALLOWED_ORIGIN.equals(origin)) {
            error(response,403, "Forbidden: Invalid origin");
            return;
        }
        
        // Get content type
        String contentTypeHeader = request.getContentType().orElse("");
        log.info("Content-Length: {}", request.getContentLength());
        
        if (!contentTypeHeader.startsWith("multipart/form-data")) {
            error(response,400, "Invalid content type");
            return;
        }

        // Parse multipart content (the framework throws unchecked exceptions
        // for a missing or mismatched boundary and for truncated bodies)
        Map<String, HttpRequest.HttpPart> parts;
        try {
            parts = request.getParts();
        } catch (RuntimeException e) {
            log.warn("Invalid multipart/form-data body", e);
            error(response, 400, "Invalid multipart/form-data body");
            return;
        }

        byte[] rtfContent = null;
        String fileName = null;

        for (HttpRequest.HttpPart part : parts.values()) {
            String uploadedName = part.getFileName().orElse(null);

            if (uploadedName == null ||
                    !uploadedName.toLowerCase(Locale.ROOT).endsWith(".rtf")) {
                continue;
            }

            try (InputStream is = part.getInputStream()) {
                rtfContent = readLimited(is);
                fileName = uploadedName;
            } catch (UploadTooLargeException e) {
                error(response, 413, "File too large (max 1024 KB)");
                return;
            } catch (IOException e) {
                log.warn("Could not read uploaded file", e);
                error(response, 400, "Could not read uploaded file");
                return;
            }

            break;
        }

        if (rtfContent == null) {
            error(response,400, "RTF file not found in request");
            return;
        }

        // ✅ RTF to HTML conversion
        String htmlContent = convertRtfToHtml(rtfContent, fileName);

        // Return HTML content
        response.setStatusCode(200);
        response.appendHeader("Content-Type", "text/html; charset=utf-8");
        
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;

        while ((count = input.read(buffer)) != -1) {
            if (count > RtfToHtml.MAX_SIZE_BYTES - total) {
                throw new UploadTooLargeException();
            }

            output.write(buffer, 0, count);
            total += count;
        }

        return output.toByteArray();
    }
    
    private static void error(HttpResponse response, int status, String message) throws IOException {
        response.setStatusCode(status);
        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().write(message);
    }
    
    private String convertRtfToHtml(byte[] rtfContent, String filename) {
     
        ScroogeXHTML converter = new ScroogeXHTML();

        converter.setAddOuterHTML(true);

        // Head options
        converter.getHtmlHeadConfig().setMetaAuthor("ScroogeXHTML RTF Converter - https://www.scroogexhtml.com");
        converter.getHtmlHeadConfig().setMetaDescription("Conversion of '%s'".formatted(filename));
        converter.getHtmlHeadConfig().setStyleSheetInclude(DEFAULT_CSS + DemoFooter.CSS);
        converter.getHtmlHeadConfig().setIncludeDefaultFontStyle(true);
        converter.setDefaultLanguage("");

        // Font (character) Formatting Properties
        converter.getCharPropConvConfig().setConvertLanguage(true);
        converter.getCharPropConvConfig().setFontSizeUnit(LengthUnit.POINT); // default is em

        // Paragraphs
        converter.getParaPropConvConfig().setConvertIndent(true);
        converter.getParaPropConvConfig().setConvertParagraphBorders(true);

        // Special options
        converter.setConvertBookmarks(true);
        converter.setConvertEmptyParagraphs(true);
        converter.setConvertHyperlinks(true);
        converter.setConvertTables(true);

        // Tables (cell borders from the RTF document)
        converter.getTablePropConvConfig().setConvertCellStyles(true);

        // pictures
        converter.setConvertPictures(true);
        MemoryPictureAdapterDataURI pa = new MemoryPictureAdapterDataURI(256);
        converter.setPictureAdapter(pa);

        long start = System.nanoTime();
        String html = converter.convert(new ByteArrayInputStream(rtfContent));
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        // validate the converter output, then append the footer (which is not validated)
        long validationStart = System.nanoTime();
        OptionalInt errors = HtmlValidator.countErrors(html);
        log.info("Converted in {} ms, validated in {} ms, validation errors: {}", millis,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - validationStart),
                errors.isPresent() ? errors.getAsInt() : "n/a");

        return DemoFooter.insert(html,
                DemoFooter.html(converter.getVersion(), millis, filename, rtfContent.length, errors));
    }

    private static class UploadTooLargeException extends IOException {
        UploadTooLargeException() {
            super("Upload too large");
        }
    }

}
