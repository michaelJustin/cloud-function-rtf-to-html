# cloud-function-rtf-to-html

The Java class RtfToHtml implements a Google Cloud Function (HttpFunction) that converts an RTF document 
- sent as HTTP POST payload - to HTML using the ScroogeXHTML library.

The function reads the RTF file from the HTTP POST payload, converts it, and responds with the 
HTML or an error message.

# Usage

The cloud function is used in the demo currently located at https//www.scroogexhtml.com/index.hteml

