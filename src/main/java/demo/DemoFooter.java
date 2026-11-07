/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.OptionalInt;

/**
 * The footer appended to each converted document: converter version, conversion time,
 * source file and the HTML validation result.
 * <p>
 * The footer uses the website font (IBM Plex Sans, SIL Open Font License 1.1, see
 * {@code IBM-Plex-OFL-1.1.license}) and the HTML5 logo, both embedded as data URIs:
 * the demo shows the document in a sandboxed iframe, which can't load fonts from
 * the website, and the Content-Security-Policy only allows {@code data:} fonts and images.
 */
final class DemoFooter {

    // a family name of its own, so the RTF document's fonts are not affected
    private static final String FONT_FAMILY = "ScroogeXHTML Demo Footer";

    /** Style sheet rules for the footer, to be included in the document head. */
    static final String CSS = """
            @font-face {
              font-family: '%1$s';
              font-weight: 400;
              src: url(data:font/woff2;base64,%2$s) format('woff2');
            }
            @font-face {
              font-family: '%1$s';
              font-weight: 600;
              src: url(data:font/woff2;base64,%3$s) format('woff2');
            }
            footer.sx-demo-footer {
              display: flex;
              flex-wrap: wrap;
              align-items: center;
              gap: 0.5em 1.5em;
              margin-top: 2em;
              padding: 0.75em 1em;
              border-top: 4px solid #FB8C00;
              background-color: #143F39;
              color: #FFFFFF;
              font: 400 14px/1.4 '%1$s', sans-serif;
            }
            footer.sx-demo-footer strong {
              font-weight: 600;
            }
            footer.sx-demo-footer a {
              color: #FFFFFF;
              margin-left: auto;
            }
            footer.sx-demo-footer img {
              display: block;
              width: 28px;
              height: 28px;
            }
            footer.sx-demo-footer .sx-errors {
              padding: 0.1em 0.6em;
              border-radius: 1em;
              background-color: #FB8C00;
              color: #16302C;
              font-weight: 600;
            }
            """.formatted(FONT_FAMILY,
            base64("ibm-plex-sans-latin-regular.woff2"),
            base64("ibm-plex-sans-latin-600.woff2"));

    private static final String HTML5_LOGO = "data:image/svg+xml;base64," + base64("html5_logo.svg");

    private DemoFooter() {
    }

    /**
     * Builds the footer element.
     *
     * @param version          the converter version
     * @param conversionMillis the conversion time in milliseconds
     * @param fileName         the name of the RTF file
     * @param sizeBytes        the size of the RTF file
     * @param errors           the number of validation errors, empty if validation failed
     * @return the footer HTML
     */
    static String html(String version, long conversionMillis, String fileName, int sizeBytes,
                       OptionalInt errors) {
        StringBuilder sb = new StringBuilder("<footer class=\"sx-demo-footer\">");
        sb.append("<span>Converted by <strong>ScroogeXHTML ").append(escape(version))
                .append("</strong> in ").append(conversionMillis).append(" ms</span>");
        sb.append("<span>").append(escape(fileName))
                .append(" (").append(sizeBytes / 1024).append(" KB)</span>");
        if (errors.isPresent()) {
            int count = errors.getAsInt();
            if (count == 0) {
                sb.append("<img src=\"").append(HTML5_LOGO).append("\" alt=\"Valid HTML5\"")
                        .append(" title=\"Valid HTML5 (checked with the Nu Html Checker)\">");
            } else {
                sb.append("<span class=\"sx-errors\" title=\"Checked with the Nu Html Checker\">")
                        .append(count).append(count == 1 ? " validation error" : " validation errors")
                        .append("</span>");
            }
        }
        sb.append("<a href=\"https://www.scroogexhtml.com\" target=\"_blank\" rel=\"noopener\">")
                .append("www.scroogexhtml.com</a>");
        return sb.append("</footer>").toString();
    }

    /** Inserts the footer at the end of the document body. */
    static String insert(String document, String footer) {
        int end = document.lastIndexOf("</body>");
        if (end < 0) {
            return document + footer;
        }
        return document.substring(0, end) + footer + document.substring(end);
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String base64(String resource) {
        try (InputStream is = DemoFooter.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
