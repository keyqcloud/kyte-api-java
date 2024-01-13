# Kyte API Java Client Library

[![Java CI with Gradle](https://github.com/keyqcloud/kyte-api-java/actions/workflows/gradle.yml/badge.svg)](https://github.com/keyqcloud/kyte-api-java/actions/workflows/gradle.yml)

## Overview
The Kyte API Java Client Library is a robust and easy-to-use Java interface for interacting with the Kyte API. It provides methods for secure HTTP requests, supporting operations like GET, POST, PUT, and DELETE. The library includes functionality for authentication, signature generation using HMAC SHA-256, and handling responses in JSON format.

## Features
- Easy to use interface for the Kyte API.
- Support for basic CRUD operations (Create, Read, Update, Delete).
- Secure authentication and signature generation.
- JSON response handling.

## Requirements
- Java 8 or later.
- Apache HttpClient (version 4.5.x).
- JSON library (e.g., org.json).

<!-- ## Installation
Include the following dependency in your `build.gradle` file:

```groovy
dependencies {
    implementation 'cloud.keyq:kyte-api-client:1.0.0'
}
```

Or, if using Maven, add this to your `pom.xml`:
```xml
<dependency>
    <groupId>cloud.keyq</groupId>
    <artifactId>kyte-api-client</artifactId>
    <version>1.0.0</version>
</dependency>
``` -->

# Usage
```java
package your.package;

import kyte.api.Client;
import org.json.JSONObject;
import org.json.JSONArray;

public class Example {
    public static void main(String[] args) {
        try {
            KyteClient kyte = new KyteClient(
                "your_public_key",
                "your_private_key",
                "your_kyte_account",
                "your_kyte_identifier",
                "https://api.kyte.endpoint.example.com",
                "your_kyte_app_id"
            );

            JSONObject response = kyte.get("Model", "field_name", "field_value", null);
            System.out.println(response.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
