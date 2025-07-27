# RTF to HTML Converter Online 

The Java class RtfToHtml implements a Google Cloud Function (HttpFunction) that converts an RTF document to HTML using the [ScroogeXHTML RTF Converter](https//www.scroogexhtml.com/) library.

The function reads the RTF file from the HTTP POST payload, converts it, and responds with the HTML or an error message.

# Usage

The cloud function is used in the demo currently located at https://www.scroogexhtml.com/index.hteml

# Configuration

The converter uses a fixed configuration.

# Restrictions

- the maximum allowed RTF size is 64 BB.
- only HTTP requests from https//www.scroogexhtml.com are accepted. 


