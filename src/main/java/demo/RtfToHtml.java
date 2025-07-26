package demo;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.scroogexhtml.ScroogeXHTML;
import com.scroogexhtml.css.LengthUnit;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * This Java class implements a Google Cloud Function (HttpFunction) that converts an RTF document 
 * (send as HTTP POST payload) to HTML using the ScroogeXHTML library. 
 * The function reads an RTF file from a query parameter URL, converts it, and responds with the 
 * HTML or usage instructions.
 */
public class RtfToHtml implements HttpFunction {

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
        ByteArrayDataSource ds = new ByteArrayDataSource(bodyBytes, contentTypeHeader);
        MimeMultipart mimeMultipart = new MimeMultipart(ds);

        String rtfContent = null;

        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            MimeBodyPart part = (MimeBodyPart) mimeMultipart.getBodyPart(i);
            if (part.getFileName() != null && part.getFileName().endsWith(".rtf")) {
                try (InputStream is = part.getInputStream()) {
                    rtfContent = IOUtils.toString(is, StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (rtfContent == null) {
            response.setStatusCode(400, "RTF file not found in request");
            return;
        }

        final int MAX_SIZE_BYTES = 64 * 1024; // 64 KB
        
        if (rtfContent.length() > MAX_SIZE_BYTES) {
            response.setStatusCode(413, "File too large (max 64 KB)");
            return;
        }

        // ✅ RTF to HTML conversion
        String htmlContent = convertRtfToHtml(rtfContent);

        // Return HTML content
        response.setStatusCode(200);
        response.appendHeader("Content-Type", "text/html; charset=utf-8");
        response.appendHeader("Access-Control-Allow-Origin", "https://www.scroogexhtml.com"); // if needed
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
        }
    }

    private String convertRtfToHtml(String rtfContent) {
        final String DEFAULT_CSS = """
            body,p {
              margin: 0;
            }""";
     
        ScroogeXHTML converter = new ScroogeXHTML();

        converter.setAddOuterHTML(true);

        // Head options
        converter.getHtmlHeadConfig().setMetaAuthor("https://www.scroogexhtml.com/");
        converter.getHtmlHeadConfig().setStyleSheetInclude(DEFAULT_CSS);
        converter.getHtmlHeadConfig().setIncludeDefaultFontStyle(true);
        converter.setDefaultLanguage("en");

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
        
        return converter.convert(rtfContent);
    }
}