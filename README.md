![Java](https://img.shields.io/badge/language-Java-blue.svg)
![Google Cloud](https://img.shields.io/badge/platform-Google%20Cloud-yellow.svg)
![License](https://img.shields.io/github/license/michaelJustin/cloud-function-rtf-to-html.svg)
![Last Commit](https://img.shields.io/github/last-commit/michaelJustin/cloud-function-rtf-to-html.svg)
![Open Issues](https://img.shields.io/github/issues/michaelJustin/cloud-function-rtf-to-html.svg)

## Table of Contents

- [About](#about)
- [Features](#features)
- [Quickstart](#quickstart)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Limitations](#limitations)
  
# About

The Java class RtfToHtml implements a Google Cloud Function (HttpFunction) that converts an RTF document to HTML using the [ScroogeXHTML RTF Converter](https://www.scroogexhtml.com/) library.

The function reads the RTF file from the HTTP POST payload, converts it, and responds with the HTML or an error message.

# Features

Complete Source Code and Maven project.
Deployment script included.
Demo HTML page included.

# Quickstart

To lauch the local test server enter this Maven command:

```console
mvn function:run
```

Output:

```console
[INFO] jetty-9.4.51.v20230217; built: 2023-02-17T08:19:37.309Z; git: b45c405e4544384de066f814ed42ae3dceacdd49; jvm 21.0.7+6-LTS
[INFO] Started o.e.j.s.ServletContextHandler@3caa4d85{/,null,AVAILABLE}
[INFO] Started ServerConnector@c0013b8{HTTP/1.1, (http/1.1)}{0.0.0.0:8080}
[INFO] Started @17722ms
Juli 27, 2025 11:46:53 AM com.google.cloud.functions.invoker.runner.Invoker logServerInfo
INFORMATION: Serving function...
Juli 27, 2025 11:46:53 AM com.google.cloud.functions.invoker.runner.Invoker logServerInfo
INFORMATION: Function: demo.RtfToHtml
Juli 27, 2025 11:46:53 AM com.google.cloud.functions.invoker.runner.Invoker logServerInfo
INFORMATION: URL: http://localhost:8080/
```

Now you can open the demo.html in your browser and use it to upload an RTF to the local test server.

Note: in case ScroogeXHTML is not installed, you still may use the demo. Just replace the conversion by a hard-coded result.

# Installation

# Usage

The cloud function is used in the demo currently located at https://www.scroogexhtml.com/index.html

# Configuration

The converter uses a fixed configuration. See the `convertRtfToHtml` method in [`src/main/java/demo/RtfToHtml.java`](src/main/java/demo/RtfToHtml.java#L128-L150).

# Deployment

# Limitations

- the maximum allowed RTF size is 64 KB.
- only HTTP requests from https://www.scroogexhtml.com are accepted. 

# Further Reading

- https://blog.habarisoft.com/scroogexhtml/rtf-to-html-in-the-cloud-deploying-a-java-conversion-api-on-google-cloud-run/

