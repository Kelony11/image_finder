# ImageFinder — Multithreaded Web Crawler & Image URL Extractor

A Java 8 & Maven web app that crawls a user-provided URL, discovers same-domain subpages, and returns a JSON array of image URLs found across the crawl. Built for the Eulerity Hackathon Challenge.

---

## Features

### Core requirements
- ✅ Crawl a starting URL and extract **all image URLs** from discovered pages
- ✅ Crawl **sub-pages** to find additional images
- ✅ **Multi-threaded crawling** to process multiple pages concurrently
- ✅ **Same-domain enforcement** (won’t crawl outside the input URL’s domain)
- ✅ **No duplicate visits** (tracks visited pages to avoid re-crawling)

---

## Tech Stack
- **Java 8** `required`
- **Maven 3.5+**
- **Jetty (mvn jetty:run)**
- **Jsoup** `HTML parsing + crawling`

---

## Getting Started

### Requirements

Make sure you have:
- **Java 8 (exactly)** — Java 9+ will fail the build
- **Maven 3.5+**

Check versions:
```bash
java -version
mvn -version
```

- **Build**
From the project root:
```bash
mvn package
```
To clean build artifacts:
```bash
mvn clean
```

- **RUN**
Start the server:
```bash
mvn clean test package jetty:run
```

**Note:** Open the local host and test it with the links in `test-links.txt` file.

---

> **Disclaimer** Educational/demo project, not intended as production-grade web scraping infrastructure.

---

### Author
`Kelvin Ihezue`

