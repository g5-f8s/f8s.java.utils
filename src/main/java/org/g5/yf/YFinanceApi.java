package org.g5.yf;

import org.g5.util.GZipper;
import org.g5.yf.http.HttpRequestProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class YFinanceApi {

    private static final String urlFormat = "https://query2.finance.yahoo.com/v8/finance/chart/$tic?period1=$start&period2=$end&interval=1m&includePrePost=true&events=div%7Csplit%7Cearn&lang=en-US&region=US&source=cosaic";

    private static final String cookie = "dflow=917; EuConsent=CQoXoQAQoXoQAAOACKENCqFgAAAAAAAAACiQAAAAAAAA.IMHtB9G7eTXFneTJ2YLskOYwX0VBJ4MAwBgCAAEABzBIUIBwGVmATJEyIICACGAIAIGBBIABtGBhAQEAAIIAVAABIAEkAIBAAIGAAACAIQABACAABAAAAMAAQgEAXIAQgmAYEAFoIQUhAkgAgAQAAAAAEAIgBCASAEAAAQAAACAAAgCgAggAAAAAAAAAEAFAIEQAAIAECAovgdgAQAAAAAAgIAAYACEABAAAABIAAAgCAAAAAAAAAAACAAAAAAABCAIAACA; GUC=AQABCAFqcgpqq0IfZgR6&s=AQAAAN3f1NRW&g=anDCng; A1S=d=AQABBJTCcGoCEOm73KmuZR4txAh5UaFwJ9wFEgABCAEKcmqravZ0rXYBAiAAAAcIkMJwagVT3NU&S=AQAAAuiwsi3MSi28mkJ8tAFxffY; A1=d=AQABBJTCcGoCEOm73KmuZR4txAh5UaFwJ9wFEgABCAEKcmqravZ0rXYBAiAAAAcIkMJwagVT3NU&S=AQAAAuiwsi3MSi28mkJ8tAFxffY; A3=d=AQABBJTCcGoCEOm73KmuZR4txAh5UaFwJ9wFEgABCAEKcmqravZ0rXYBAiAAAAcIkMJwagVT3NU&S=AQAAAuiwsi3MSi28mkJ8tAFxffY; cmp=t=1785778354&j=1&u=1---&v=143; PRF=t%3DAAPL%26dock-collapsed%3Dtrue";

    private final JsonMapper jsonParser = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Map<String, JsonNode> getData(String stockTicker) {
        try {
            Document document = Jsoup.connect("https://finance.yahoo.com/quote/" + stockTicker).get();
            Map<String, String> dataMap = document.select("script[data-sveltekit-fetched][data-url]").stream()
                    .filter(s -> {
                        String u = s.attr("data-url");
                        return u.contains("finance/timeseries") || u.contains("finance/quoteSummary");
                    })
                    .collect(Collectors.toMap(e -> e.attr("data-url"), Element::html));
            Map<String, JsonNode> jsonNodeMap = new HashMap<>();
            for (Map.Entry<String, String> row : dataMap.entrySet()) {
                jsonNodeMap.put(row.getKey(), jsonParser.readTree(row.getValue()));
            }
            return jsonNodeMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode getStockData(String stockTicker) {
        OffsetDateTime start = OffsetDateTime.now().minusMonths(6L);
        OffsetDateTime end = OffsetDateTime.now();
        String uri = URLDecoder.decode(urlFormat, StandardCharsets.UTF_8).formatted(stockTicker, start.toEpochSecond(), end.toEpochSecond())
                .replace("$tic", stockTicker)
                .replace("$start", Long.toString(start.toEpochSecond()))
                .replace("$end", Long.toString(end.toEpochSecond()))
                .replaceAll("\\|", "%7C");
        URI dataUri = URI.create(uri);
        try (InputStream responseStream =  new HttpRequestProcessor<InputStream>(httpClient)
                .execute(buildFetchRequest(dataUri), HttpResponse.BodyHandlers.ofInputStream())) {
            String responseData = GZipper.decompress(responseStream.readAllBytes());
            //Accept cookies to continue...
            return jsonParser.readTree(responseData);
        } catch (IOException | HttpRequestProcessor.RequestFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest buildFetchRequest(URI dataUri) {
        return HttpRequest.newBuilder()
                .GET()
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Accept-Language", "en-GB,en;q=0.9")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64)")
                .uri(URI.create("https://finance.yahoo.com/quote/AAPL"))
                .build();
    }

    public static void main(String[] args) {
        Map<String, JsonNode> aapl = new YFinanceApi().getData("AAPL");
    }
}
