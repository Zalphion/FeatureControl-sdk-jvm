import com.zalphion.featurecontrol.FeatureControl;
import com.zalphion.featurecontrol.source.ApplicationSource;

import java.net.URI;

public class Main {
    public static void main(String[] args) throws Exception {

        /*
         * Step 1: Init FeatureSource
         * Using an embedded API key to pull features from a self-hosted Feature Control instance.
         * Pre-fetching and periodically refreshing for non-blocking operation.
         */
        final ApplicationSource source = new FeatureControl(URI.create(System.getenv("FEATURE_CONTROL_HOST")))
                .toFeatureSource(System.getenv("FEATURE_CONTROL_SDK_KEY"))
                .preFetching();

        /*
         * Step 2: Configure your Application
         * Create feature flags and property references.
         * Inject them into your application; they will always be up to date with the FeatureSource.
         */
        final BusinessModule module = new BusinessModule(
                source.flag("dashboard", "off"),
                source.property("excitement", Integer::parseInt, 1)
        );

        /*
         * Step 3: Start Application
         */
        // start server
        while(true) {
            System.out.println(module.renderIndex("user1"));

            Thread.sleep(2000);
        }
    }
}
