package ladylib.networking;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class HTTPRequestHelper {
    private static final Gson GSON = new Gson();
    private static final Executor THREAD_POOL = new ThreadPoolExecutor(
            0,
            3,
            60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            runnable -> new Thread(runnable, "LadyLib HTTP Helper")
    );

    /**
     * Load JSON-encoded data from the server using a GET HTTP request.
     * <p>
     * The request will be done asynchronously. If a valid JSON representation is retrieved successfully,
     * the <code>success</code> callback will be called with the generated {@link JsonElement}.
     * <p>
     * Do note that the callback will be called on a different thread and can be called at any time after it
     * was passed to this method. There is also a chance that it may not be called at all, for a number of reasons,
     * such as 404s, I/O exceptions and unexpected input.
     * <p>
     * Request treatment starts during {@link FMLInitializationEvent mod initialization}. After that,
     * pending requests will be processed at regular intervals.
     * </p>
     *
     * @param url     A string containing the URL to which the request is sent.
     * @param success A callback function that is executed if the request succeeds.
     * @see #getJSON(URL)
     */
    public static void getJSON(@Nonnull String url, @Nonnull JsonCallback success) {
        try {
            getJSON(new URL(url)).thenAccept(success);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Load JSON-encoded data from the server using a GET HTTP request. <br>
     * <p>
     * The request will be done asynchronously. Processing of the result can be done using various
     * {@link CompletableFuture} methods such as {@link CompletableFuture#thenAccept(Consumer)} or {@link Future#get}.
     * Any exception thrown during the JSON retrieval will prevent the completion of the future.
     * You can handle those exceptions through {@link CompletableFuture#exceptionally(Function)}.
     * <br>
     * Example: <pre>{@code
     * HTTPRequestHelper.getJSON(myURL)
     *                  .thenApply(jsonElement -> GSON.fromJson(jsonElement, MyObject.class)
     *                  .exceptionally(t -> {
     *                      LOGGER.error("Error while retrieving information from the server", e);
     *                      return new MyObject());
     *                  }).thenAccept(MyObject::doSomething);
     * }</pre>
     * <p>
     * <p>
     * Request treatment starts during {@link FMLInitializationEvent mod initialization}. After that,
     * pending requests will be processed at regular intervals.
     * </p>
     *
     * @param url A string containing the URL to which the request is sent.
     * @return a CompletableFuture running the JSON GET request
     * @see #getJSON(URL)
     * @see CompletableFuture
     */
    public static CompletableFuture<JsonElement> getJSON(@Nonnull URL url) {
        return CompletableFuture.supplyAsync(() -> requestJSON(url), THREAD_POOL);
    }

    private static JsonElement requestJSON(URL url) {
        try {
            URLConnection rewardPage = url.openConnection();
            if (rewardPage instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) rewardPage).getResponseCode();
                if (code <= 200 || code > 299) {
                    throw new RuntimeException("Got a bad response code from the server (code " + code + ")");
                }
            }
            rewardPage.connect();
            try (Reader in = new InputStreamReader(rewardPage.getInputStream())) {
                return GSON.fromJson(in, JsonElement.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not connect to " + url + ". Maybe you're offline ?", e);
        } catch (JsonParseException e) {
            throw new RuntimeException("Bad json coming from " + url + ". This should be reported.", e);
        }
    }

    @FunctionalInterface
    public interface JsonCallback extends Consumer<JsonElement> {}

}
