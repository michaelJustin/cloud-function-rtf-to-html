/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package demo;

import nu.validator.validation.SimpleDocumentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

/**
 * Validates an HTML document with the Nu Html Checker (vnu), the checker behind
 * https://validator.w3.org/nu/. Runs locally, the document is not sent anywhere.
 */
final class HtmlValidator {

    // the schema the vnu command line client uses for HTML documents
    private static final String SCHEMA_URL = "http://s.validator.nu/html5-all.rnc";
    private static final Logger log = LoggerFactory.getLogger(HtmlValidator.class);

    private HtmlValidator() {
    }

    /**
     * Counts the validation errors (warnings and info messages are not counted).
     *
     * @param html the HTML document
     * @return the number of errors, or empty if the validator itself failed
     */
    static OptionalInt countErrors(String html) {
        CountingErrorHandler handler = new CountingErrorHandler();
        try {
            // a validator instance is not thread-safe, so use one per document;
            // the parsed schemas are cached across instances
            SimpleDocumentValidator validator = new SimpleDocumentValidator(true, false, false);
            validator.setUpMainSchema(SCHEMA_URL, new FailingErrorHandler());
            validator.setUpValidatorAndParsers(handler, false, false);
            InputSource source = new InputSource(
                    new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
            source.setEncoding("UTF-8");
            validator.checkHtmlInputSource(source);
        } catch (Exception e) {
            // a fatal parse error ends the check, but is counted
            if (handler.fatalErrors == 0) {
                // only the exception type: parser messages can quote document text
                log.warn("HTML validation failed: {}", e.getClass().getName());
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(handler.errors + handler.fatalErrors);
    }

    /** Counts only; the messages are not logged, because they can quote document text. */
    private static final class CountingErrorHandler implements ErrorHandler {
        private int errors;
        private int fatalErrors;

        @Override
        public void warning(SAXParseException e) {
            // not counted
        }

        @Override
        public void error(SAXParseException e) {
            errors++;
        }

        @Override
        public void fatalError(SAXParseException e) {
            fatalErrors++;
        }
    }

    /** For the schema setup: any problem there is a validator failure, not a document error. */
    private static final class FailingErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException e) {
            // ignored
        }

        @Override
        public void error(SAXParseException e) throws SAXParseException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXParseException {
            throw e;
        }
    }
}
