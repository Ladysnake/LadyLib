package ladylib.networking.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

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
            15,
            60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            runnable -> new Thread(runnable, "LadyLib HTTP Helper")
    );

    private static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 20);

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
     * This helper method automatically swallows any exception thrown by the JSON retrieval or the callback.
     * For proper handling of those exceptions, use {@link #getJSON(URL) the alternative overload}.
     * </p>
     *
     * @param url     A string containing the URL to which the request is sent.
     * @param success A callback function that is executed if the request succeeds.
     * @see #getJSON(URL)
     */
    public static void getJSON(String url, JsonCallback success) {
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
     *
     * @param url A URL to which the request is sent.
     * @return a CompletableFuture running the JSON GET request
     * @see #getJSON(String, JsonCallback)
     * @see CompletableFuture
     */
    public static CompletableFuture<JsonElement> getJSON(URL url) {
        return CompletableFuture.supplyAsync(() -> requestJSON(url), THREAD_POOL);
    }

    private static JsonElement requestJSON(URL url) {
        try {
            URLConnection page = openUrlConnection(url);
            if (page instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) page).getResponseCode();
                if (code < 200 || code > 299) {
                    throw new RuntimeException("Got a bad response code from the server (code " + code + ")");
                }
            }
            page.connect();
            try (Reader in = new InputStreamReader(page.getInputStream())) {
                return GSON.fromJson(in, JsonElement.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not connect to " + url + ". Maybe you're offline ?", e);
        } catch (JsonParseException e) {
            throw new RuntimeException("Bad json coming from " + url + ". This should be reported.", e);
        }
    }

    /**
     * Opens a connection to given URL while following redirects
     *
     * @param url the URL to fetch
     * @return an opened connection that may have been redirected
     * @throws IOException if an I/O exception occurs or that there are too many redirects
     */
    public static URLConnection openUrlConnection(URL url) throws IOException {
        URL currentUrl = url;
        for (int redirects = 0; redirects < MAX_HTTP_REDIRECTS; redirects++) {
            URLConnection c = currentUrl.openConnection();
            if (c instanceof HttpURLConnection) {
                HttpURLConnection huc = (HttpURLConnection) c;
                huc.setInstanceFollowRedirects(false);
                int responseCode = huc.getResponseCode();
                if (responseCode >= 300 && responseCode <= 399) {
                    try {
                        String loc = huc.getHeaderField("Location");
                        currentUrl = new URL(currentUrl, loc);
                        continue;
                    } finally {
                        huc.disconnect();
                    }
                }
            }

            return c;
        }
        throw new IOException("Too many redirects while trying to fetch " + url);
    }

    @FunctionalInterface
    public interface JsonCallback extends Consumer<JsonElement> { }

}
