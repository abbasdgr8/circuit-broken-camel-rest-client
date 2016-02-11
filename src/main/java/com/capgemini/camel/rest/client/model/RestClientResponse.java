package com.capgemini.camel.rest.client.model;

import org.apache.http.Header;

/**
 * Class to model a response from a Rest call.
 *
 * @author Nick Walter
 */
public class RestClientResponse {

    private final String jsonResponse;
    private final int httpResponseCode;
    private Header[] responseHeaders;

    public RestClientResponse(String jsonResponse, int httpResponseCode) {
        this.jsonResponse = jsonResponse;
        this.httpResponseCode = httpResponseCode;
    }

    public RestClientResponse(String jsonResponse, int httpResponseCode, Header[] responseHeaders) {
        this.jsonResponse = jsonResponse;
        this.httpResponseCode = httpResponseCode;
        this.responseHeaders = responseHeaders;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public Header[] getResponseHeaders() { return responseHeaders; }

    @Override
    public String toString() {
        final StringBuilder message = new StringBuilder("HttpResponse: "
                + this.getHttpResponseCode()
                + ", JSONResponse: "
                + getJsonResponse());

        if (responseHeaders != null && responseHeaders.length > 0) {
            message.append(", Headers: [");
            for(Header header : responseHeaders) {
                message.append(header.getName()).append(" : ").append(header.getValue());
                message.append(",");
            }
            message.replace(message.length() - 1, message.length(), "]");
        }

        return message.toString();
    }
}

