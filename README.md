# FeatureControl-sdk-java

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/maven-central/v/com.zalphion.featurecontrol/sdk-java)](https://mvnrepository.com/artifact/com.zalphion.featurecontrol)

Official Java SDK for the Feature Control Platform.

## Requirements

- Java 8+
- Slf4j 1.4+

All other dependencies are carefully selected and shaded to be minimal and non-intrusive across a wide range of legacy dependency graphs.

## Quickstart

```kotlin
dependencies {
    implementation("com.zalphion.featurecontrol:sdk-java:<version>")
}
```

```java
public class Quickstart {

    public static void main(String[] args) throws IOException, InterruptedException {
        /*
         * Build a FeatureFlags instance from the Feature Control: Canada region.
         * The pre-fetching wrapper will cache the latest data and periodically refresh it.
         */
        final ApplicationSource source = FeatureControl.canada()
                .toFeatureSource(System.getenv("FEATURE_CONTROL_SDK_KEY"))
                .preFetching();

        /*
         * You can define a property or flag on init and share it within your application;
         * this is the idiomatic way to use FeatureControl.
         */
        ApplicationProperty<String> greetingProperty = source.stringProperty("greeting", "hello");
        FeatureFlag myFeatureFlag = source.flag("my-feature", "off");

        /*
         * Wait for the application to start.
         * This gives the preFetcher time to download the latest ApplicationBundle.
         *
         * If your application is synchronous (e.g. a CLI tool), don't use the preFetcher.
         */
        Thread.sleep(2000);  // for illustrative purposes only; simulating server startup

        /*
         * Get the latest property value.
         * If a bundle isn't ready in time, returns the default value.
         */
        System.out.println(greetingProperty.getValue());

        /*
         * Evaluate the flag for a recipient.
         * Different recipients may result in different variants, based on your remote configuration.
         * You must always provide a default in case the flag is not defined.
         */
        switch(myFeatureFlag.getVariant("user1")) {
            case "on":
                System.out.println("Do cool thing");
                break;
            case "both":
                System.out.println("Do cool thing");
            case "off":
                System.out.println("Don't do cool thing");
                break;
            default:
                System.out.println("Unknown variant");
        }
    }
}
```

## Examples

- [Examples](https://github.com/Zalphion/FeatureControl-sdk-java/tree/main/src/main/java)
- [Testing](https://github.com/Zalphion/FeatureControl-sdk-java/tree/main/src/test/java)