# Cloud Landmark Detection System

A scalable, cloud system for automatic landmark detection in photos using Google Cloud Platform (GCP) services.

---

## Overview

This system enables users to:

- Submit images for landmark detection.
- Retrieve identified monuments with geographic coordinates and confidence levels.
- Request a static map image for a detected location.
- Query all successfully identified landmarks with a certainty above a given threshold.

All operations are handled through a gRPC API abstracting away the underlying cloud infrastructure.

---

## Cloud Architecture

This system is built entirely on GCP services with support for autoscaling and fault tolerance:

| Component          | Description                                                                 |
|--------------------|-----------------------------------------------------------------------------|
| **Cloud Storage**  | Stores submitted image files.                                               |
| **Firestore**      | Stores metadata and results from image analysis.                           |
| **Pub/Sub**        | Decouples image submission and processing (work queue pattern).            |
| **Compute Engine** | Runs gRPC server replicas and worker apps for landmark analysis.           |
| **Vision API**     | Detects landmarks in images.                                                |
| **Static Maps API**| Generates maps for given geolocations.                                     |
| **Cloud Functions**| Provides lookup service to discover active gRPC server IPs.                |

---

## Features

- **Elasticity**: Instance groups for both gRPC servers and landmark processing workers.
- **Reliability**: Stateless gRPC servers with shared cloud-based storage and messaging.
- **API-First Design**: Uses protocol buffers and gRPC for efficient, language-agnostic communication.
- **Decoupled Architecture**: Pub/Sub and Firestore enable asynchronous processing.

---

## How to Run

> Requires Google Cloud SDK and a GCP project with billing enabled.

1. **Enable the required APIs**:
    - Vision API
    - Static Maps API
    - Cloud Functions
    - Firestore
    - Cloud Storage
    - Pub/Sub
    - Compute Engine

2. **Deploy Cloud Function** for server IP lookup.

3. **Create instance groups** for:
    - gRPC Server VMs (min 1, max 3)
    - Worker VMs (min 0, max 2)

4. **Configure Firestore**, Pub/Sub topics/subscriptions, and buckets.

5. **Launch the system** from your client app using the IP provided by the lookup function.
