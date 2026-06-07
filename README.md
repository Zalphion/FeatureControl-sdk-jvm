# FeatureControl-sdk-jvm

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/maven-central/v/com.zalphion.featurecontrol/sdk-java)](https://mvnrepository.com/artifact/com.zalphion.featurecontrol)

> [!WARNING]
> Work in progress

Official JVM SDK for the Feature Control Platform.

## Distributions

Two distributions are available, based on the maturity of your application.

### [Kotlin (JVM)]()
For modern JVM applications.

- Highly typesafe interface
- Completely non-reflective
- Serverless-friendly
- Fake in-memory server for unparalleled test integration

### [Java]()
For legacy and/or Spring applications.

- Requires only java 8 and the oldest slf4j possible
- All other dependencies are carefully selected and shaded to be minimal and non-intrusive