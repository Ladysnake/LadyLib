package ladylib.networking;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import ladylib.LadyLib;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class HTTPRequestHelper extends Thread {
    private static final Gson GSON = new Gson();
    public static final HTTPRequestHelper INSTANCE = new HTTPRequestHelper();

    /**
     * Load JSON-encoded data from the server using a GET HTTP request. <br>
     *
     * The request will be done asynchronously. If a valid JSON representation is retrieved successfully,
     * the <code>success</code> callback will be called with the generated {@link JsonElement}.
     * <p>
     * Do note that the callback will be called on a different thread and can be called at any time after it
     * was passed to this method. There is also a chance that it may not be called at all, for a number of reasons,
     * such as 404s, I/O exceptions and unexpected input.
     * </p>
     *
     * <p>
     * Request treatment starts during {@link FMLInitializationEvent mod initialization}. After that,
     * pending requests will be processed at regular intervals.
     * </p>
     *
     * @param url A string containing the URL to which the request is sent.
     * @param success A callback function that is executed if the request succeeds.
     */
    public static void getJSON(@Nonnull String url, @Nonnull JsonCallback success) {
        try {
            getJSON(new URL(url), success);
        } catch (MalformedURLException e) {
            LadyLib.LOGGER.error("{} is not a valid url representation, request ignored.", e);
        }
    }

    /**
     * @param url the URL to which the request is sent.
     * @param success A callback function that is executed if the request succeeds.
     *
     * @see #getJSON(String, JsonCallback)
     */
    public static void getJSON(@Nonnull URL url, @Nonnull JsonCallback success) {
        INSTANCE.requests.add(new PendingRequest(url, success));
    }

    private final Queue<PendingRequest> requests = new ConcurrentLinkedQueue<>();

    private volatile boolean shouldRun = true;

    @Override
    public void run() {
        while (shouldRun) {
            while (true) {
                PendingRequest request = requests.poll();
                if (request == null) break;
                JsonElement json = getJSON(request.url);
                if (json != null) {
                    request.callback.accept(json);
                }
            }
            try {
                sleep(10_000);
            } catch (InterruptedException e) {
                LadyLib.LOGGER.info("Sleep ended prematurely", e);
            }
        }
    }

    public void shutdown() {
        shouldRun = false;
        interrupt();
    }

    private JsonElement getJSON(URL url) {
        try {
            URLConnection rewardPage = url.openConnection();
            rewardPage.connect();
            StringBuilder jsonString;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(rewardPage.getInputStream()))) {
                String inputLine;
                jsonString = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    jsonString.append(inputLine);
                }
            }
            return GSON.fromJson(jsonString.toString(), JsonElement.class);
        } catch (IOException e) {
            LadyLib.LOGGER.warn("Could not connect to " + url + ". Maybe you're offline ?", e);
        } catch (JsonParseException e) {
            LadyLib.LOGGER.error("Bad json coming from " + url + ". This should be reported.", e);
        }
        return null;
    }

    public interface JsonCallback extends Consumer<JsonElement> {}

    public static class PendingRequest {
        private final URL url;
        private final JsonCallback callback;

        public PendingRequest(URL url, JsonCallback callback) {
            this.url = url;
            this.callback = callback;
        }
    }
}
