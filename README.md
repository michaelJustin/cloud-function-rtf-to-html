![Java](https://img.shields.io/badge/language-Java-blue.svg)
![Google Cloud](https://img.shields.io/badge/platform-Google%20Cloud-yellow.svg)
![License](https://img.shields.io/github/license/michaelJustin/cloud-function-rtf-to-html.svg)
![Last Commit](https://img.shields.io/github/last-commit/michaelJustin/cloud-function-rtf-to-html.svg)
![Open Issues](https://img.shields.io/github/issues/michaelJustin/cloud-function-rtf-to-html.svg)

# RTF to HTML Converter Online 

The Java class RtfToHtml implements a Google Cloud Function (HttpFunction) that converts an RTF document to HTML using the [ScroogeXHTML RTF Converter](https://www.scroogexhtml.com/) library.

The function reads the RTF file from the HTTP POST payload, converts it, and responds with the HTML or an error message.

# Usage

The cloud function is used in the demo currently located at https://www.scroogexhtml.com/index.html

# Configuration

The converter uses a fixed configuration. See the `convertRtfToHtml` method in [`src/main/java/demo/RtfToHtml.java`](src/main/java/demo/RtfToHtml.java#L128-L150).

# Restrictions

- the maximum allowed RTF size is 64 KB.
- only HTTP requests from https://www.scroogexhtml.com are accepted. 

# Further Reading

- https://blog.habarisoft.com/scroogexhtml/rtf-to-html-in-the-cloud-deploying-a-java-conversion-api-on-google-cloud-run/

