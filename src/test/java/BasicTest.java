/**
 * Created by zhangbohan on 15/10/23.
 */

import com.fit2cloud.sdk.Fit2CloudClient;
import com.google.gson.Gson;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by zhangbohan on 15/10/23.
 */
public class BasicTest extends TestCase {
    private  String f2cApiKey;
    private  String f2cApiSecret;
    private  String f2cRestApiEndpoint;
    private Gson gson;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        f2cApiKey = "";
        f2cApiSecret = "";
        f2cRestApiEndpoint = "https://console.fit2cloud.com:8443/rest/";
        gson = new Gson();
    }

    //@Test
    public void testEvent() throws Exception {

    }
}
