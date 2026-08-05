package org.g5.yf.http;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpRequestProcessor<T> {

    private final HttpClient httpClient;

    public HttpRequestProcessor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public T execute(HttpRequest httpRequest, HttpResponse.BodyHandler<T> responseHandler) throws RequestFailedException {
        try {
            HttpResponse<T> httpResponse = this.httpClient.send(httpRequest, responseHandler);
            //check status and report exception or return result
            HttpStatus responseStatus = HttpStatus.resolve(httpResponse.statusCode());
            if(responseStatus.isOkStatus()){
                return httpResponse.body();
            } else {
                throw new RequestFailedException(responseStatus, "failed");
            }
        } catch (IOException ioe) {
            throw new RequestFailedException(HttpStatus.BAD_GATEWAY, "Failed to get response!", ioe);
        } catch (InterruptedException e) {
            Thread.interrupted();//clear interrupt
        }
        return null;
    }

    public static final class RequestFailedException extends Exception {

        private final HttpStatus responseStatus;

        public RequestFailedException(HttpStatus responseStatus, String message) {
            this(responseStatus, message, null);
        }

        public RequestFailedException(HttpStatus responseStatus, String message, Throwable cause) {
            super(message, cause);
            this.responseStatus = responseStatus;
        }

        public HttpStatus getResponseStatus() {
            return responseStatus;
        }
    }
}
