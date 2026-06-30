package cn.imhtb.live.modules.live.agent;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpLangChainHttpClientBuilder implements HttpClientBuilder {

    private Duration connectTimeout;
    private Duration readTimeout;

    @Override
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public HttpClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public HttpClient build() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (connectTimeout != null) {
            builder.connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (readTimeout != null) {
            builder.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return new Client(builder.build());
    }

    private static class Client implements HttpClient {

        private final OkHttpClient client;

        private Client(OkHttpClient client) {
            this.client = client;
        }

        @Override
        public SuccessfulHttpResponse execute(HttpRequest request) {
            Request okRequest = toOkHttpRequest(request);
            try (Response response = client.newCall(okRequest).execute()) {
                ResponseBody body = response.body();
                String responseBody = body == null ? "" : body.string();
                if (!response.isSuccessful()) {
                    throw new HttpException(response.code(), responseBody);
                }
                return SuccessfulHttpResponse.builder()
                        .statusCode(response.code())
                        .headers(response.headers().toMultimap())
                        .body(responseBody)
                        .build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void execute(HttpRequest request,
                            ServerSentEventParser parser,
                            ServerSentEventListener listener) {
            listener.onError(new UnsupportedOperationException("Streaming chat is not used by PulseLive agent."));
        }

        private Request toOkHttpRequest(HttpRequest request) {
            Request.Builder builder = new Request.Builder().url(request.url());
            addHeaders(request, builder);

            if (HttpMethod.GET.equals(request.method())) {
                return builder.get().build();
            }
            if (HttpMethod.DELETE.equals(request.method())) {
                return builder.delete(buildBody(request)).build();
            }
            return builder.post(buildBody(request)).build();
        }

        private void addHeaders(HttpRequest request, Request.Builder builder) {
            for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
                String name = entry.getKey();
                if (!shouldForwardHeader(name)) {
                    continue;
                }
                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) {
                    continue;
                }
                for (String value : values) {
                    builder.addHeader(name, value);
                }
            }
        }

        private boolean shouldForwardHeader(String name) {
            if (name == null) {
                return false;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            return !"content-length".equals(lower) && !"host".equals(lower);
        }

        private RequestBody buildBody(HttpRequest request) {
            String body = request.body() == null ? "" : request.body();
            String contentType = firstHeader(request.headers(), "content-type");
            MediaType mediaType = MediaType.parse(contentType == null ? "application/json; charset=utf-8" : contentType);
            return RequestBody.create(body, mediaType);
        }

        private String firstHeader(Map<String, List<String>> headers, String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    List<String> values = entry.getValue();
                    return values == null || values.isEmpty() ? null : values.get(0);
                }
            }
            return null;
        }
    }
}
