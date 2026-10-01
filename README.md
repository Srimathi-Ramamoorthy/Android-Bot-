# CRI ANDROID BOT

An integrated manufacturing intelligence solution combining a native Android monitoring app with an interactive Java bot. The system consumes real-time line metrics from a web API, displays live unit output, triggers automated receipt/slip printing, and provides on-demand hourly production breakdowns.

---

## Key Features

- **Real-Time Output Sync:** Polls production endpoints to fetch current manufacturing counts per operational line.
- **Automated Count Printing:** Interfaces directly with connected ESC/POS thermal printers (Bluetooth / USB / Network) to print physical production logs and shift slips.
- **Interactive Java Bot:** A lightweight conversational assistant enabling operators and supervisors to query line status, daily summaries, and hour-by-hour production trends via text commands.
- **Hourly Breakdown Reports:** Aggregates line metrics into hourly buckets to track cycle time variations, bottlenecks, and shift targets.
- **Failover Handling:** Caches the latest operational state locally to preserve count continuity during network latency or endpoint drops.

---
ech Stack
Android Application:

Language: Java (Android SDK)

Networking: Retrofit 2 / OkHttp (or Volley) for REST API communication

JSON Parsing: Gson / Jackson

Hardware I/O: Android USB Host / Bluetooth API for thermal printing

Bot Engine:

Language: Java 11+

Framework: Standalone Java Console

HTTP Client: Java HttpClient / Apache HttpComponents

Getting Started
Prerequisites
Android Studio: Hedgehog | Iguana or newer

JDK: OpenJDK 11 or 17+

Hardware: Android Device / Industrial Tablet (USB-OTG or Bluetooth enabled) and an ESC/POS-compatible thermal printer

API Endpoint: Access credentials and base URL for the factory production web server
