package demo;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.scroogexhtml.ScroogeXHTML;
import com.scroogexhtml.css.LengthUnit;
import com.scroogexhtml.events.PostProcessEventObject;
import com.scroogexhtml.events.PostProcessListener;
import com.scroogexhtml.pictures.MemoryPictureAdapterDataURI;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * This Java class implements a Google Cloud Function (HttpFunction) that converts an RTF document 
 * (send as HTTP POST payload) to HTML using the ScroogeXHTML library. 
 * The function reads an RTF file from a query parameter URL, converts it, and responds with the 
 * HTML or usage instructions.
 */
public class RtfToHtml implements HttpFunction {

    private static final int MAX_SIZE_BYTES = 1024 * 1024; // 1024 KB
    private static final String DEFAULT_CSS = """
            body,p {
              margin: 0;
            }
            td {
              vertical-align: top;
              border: 1px solid #D3D3D3;
            }
            table {
              border-collapse: collapse;
            }
            """;
    
    /**
     * Handles HTTP requests to convert an RTF document to HTML.
     * Only requests coming from https://scroogexhtml.com are accepted.
     * This is intentional to limit the usage to code hosted on the ScroogeXHTML
     * website.
     *
     * @param request  The HTTP request containing query parameters.
     * @param response The HTTP response to write the HTML output or usage instructions.
     * @throws IOException If an I/O error occurs while reading the RTF file or writing the response.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {

        // Only accept POST
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatusCode(405); // Method Not Allowed
            return;
        }

        // Only accept requests from scroogexhtml.com
        String origin = request.getHeaders().get("origin") != null
                ? request.getHeaders().get("origin").getFirst()
                : null;

        String referer = request.getHeaders().get("referer") != null
                ? request.getHeaders().get("referer").getFirst()
                : null;

        boolean isValidOrigin = (origin != null && origin.equals("https://www.scroogexhtml.com")) ||
                (referer != null && referer.startsWith("https://www.scroogexhtml.com"));

        if (!isValidOrigin) {
            response.setStatusCode(403, "Forbidden: Invalid origin");
            return;
        }
        
        // Get content type and boundary
        String contentTypeHeader = request.getContentType().orElse("");
        if (!contentTypeHeader.startsWith("multipart/form-data")) {
            response.setStatusCode(400, "Invalid content type");
            return;
        }

        ContentType contentType = new ContentType(contentTypeHeader);
        String boundary = contentType.getParameter("boundary");
        if (boundary == null) {
            response.setStatusCode(400, "Missing boundary in multipart/form-data");
            return;
        }

        // Parse multipart content
        byte[] bodyBytes = IOUtils.toByteArray(request.getInputStream());
        ByteArrayDataSource dataSource = new ByteArrayDataSource(bodyBytes, contentTypeHeader);
        MimeMultipart mimeMultipart = new MimeMultipart(dataSource);

        byte[] rtfContent = null;
        String fileName = null;

        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            MimeBodyPart part = (MimeBodyPart) mimeMultipart.getBodyPart(i);
            if (part.getFileName() != null && part.getFileName().endsWith(".rtf")) {
                try (InputStream is = part.getInputStream()) {
                    rtfContent = IOUtils.toByteArray(is);
                    fileName = part.getFileName();
                    break;
                }
            }
        }

        if (rtfContent == null) {
            response.setStatusCode(400, "RTF file not found in request");
            return;
        }
        
        if (rtfContent.length > MAX_SIZE_BYTES) {
            response.setStatusCode(413, "File too large (max 1024 KB)");
            return;
        }

        // ✅ RTF to HTML conversion
        String htmlContent = convertRtfToHtml(rtfContent, fileName);

        // Return HTML content
        response.setStatusCode(200);
        response.appendHeader("Content-Type", "text/html; charset=utf-8");
        response.appendHeader("Access-Control-Allow-Origin", "https://www.scroogexhtml.com"); 
//        response.appendHeader("Access-Control-Allow-Origin", "*"); // Allow all origins for testing purposes
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
        }
    }

    private String convertRtfToHtml(byte[] rtfContent, String filename) {
     
        ScroogeXHTML converter = new ScroogeXHTML();

        converter.setAddOuterHTML(true);

        // Head options
        converter.getHtmlHeadConfig().setMetaAuthor("ScroogeXHTML RTF Converter - https://www.scroogexhtml.com");
        converter.getHtmlHeadConfig().setMetaDescription("Conversion of '%s'".formatted(filename));
        converter.getHtmlHeadConfig().setStyleSheetInclude(DEFAULT_CSS);
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
        converter.setConvertFootnotes(true);
        converter.setConvertHyperlinks(true);
        converter.setConvertPictures(true);
        converter.setConvertTables(true);
        
        // pictures
        converter.setConvertPictures(true);
        MemoryPictureAdapterDataURI pa = new MemoryPictureAdapterDataURI(256);
        converter.setPictureAdapter(pa);
        
        // footer
        converter.addPostProcessListener(new MyPostProcessListener(filename, rtfContent.length));
        
        return converter.convert(new ByteArrayInputStream(rtfContent));
    }

    private static class MyPostProcessListener implements PostProcessListener {
        private final String filename;
        private final int length;

        public MyPostProcessListener(String filename, int length) {
            this.filename = filename;
            this.length = length;
        }

        @Override
        public void postProcess(PostProcessEventObject eventObject) {
            Document doc = eventObject.getDocument();
            // Add a footer with the filename
            doc.getElementsByTagName("body").item(0).appendChild(
                    doc.createElement("footer")
            ).setTextContent(
                    "Converted from: %s (size: %d KB) - Powered by ScroogeXHTML - https://www.scroogexhtml.com"
                            .formatted(filename, length / 1024));
        }
    }
}