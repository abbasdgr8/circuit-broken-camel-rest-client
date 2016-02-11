package com.capgemini.camel.rest.client.circuitbreaker;

import java.io.IOException;

import com.capgemini.camel.exception.rest.JsonReadException;
import com.capgemini.camel.rest.client.model.RestClientResponse;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.capgemini.camel.exception.rest.ErrorScenario.JSON_READ_FAILED;
import static com.capgemini.camel.exception.rest.ErrorScenario.NULL_HTTP_RESPONSE;

/**
 * Netflix Hystrix Circuit Breaker wrapper for all REST WebService calls
 * <p/>
 * https://github.com/Netflix/Hystrix
 *
 * @see com.netflix.hystrix.HystrixCommand
 * @see com.netflix.hystrix.HystrixExecutable
 *
 *  @author Abbas Attarwala
 *  @author Andrew Harmel-Law
 */
public class CommandRestResourceCall extends HystrixCommand<RestClientResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRestResourceCall.class);

    private final HttpClient httpClient;
    private final String commandName;
    private final HttpRequestBase httpRequest;
    private final HttpContext httpContext;

    /**
     * Constructor for REST WebService HystrixCommand class, passing in all the
     * parameters to execute the call.
     *
     * @param commandName  String the web service call name used to create the Hystrix Command Key
     * @param groupKeyName The group key under which this web service call falls
     * @param httpRequest  Request method to be executed
     * @param httpClient   HttpClient that executes the request
     * @param httpContext  Http request execution context
     */
    public CommandRestResourceCall(final String groupKeyName,
                                   final String commandName,
                                   final HttpRequestBase httpRequest,
                                   final HttpClient httpClient,
                                   final HttpContext httpContext) {

        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKeyName))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName)));

        checkNullArguments(groupKeyName, commandName, httpRequest, httpClient, httpContext);

        this.commandName = commandName;
        this.httpRequest = httpRequest;
        this.httpClient = httpClient;
        this.httpContext = httpContext;
    }


    /**
     * Execute the REST web service call
     *
     * @return the JSON response
     * @throws IOException if any error happens.
     */
    @Override
    protected RestClientResponse run() throws Exception {
        LOGGER.debug("Executing {} Circuit Breaker Command", commandName);
        final HttpResponse httpResponse = httpClient.execute(httpRequest, httpContext);

        return processHttpResponse(httpResponse);
    }

    /**
     *  This method processes the response and extracts the JSON string from the HTTP response code.
     *  @param httpResponse The HttpResponse returned
     */
    protected RestClientResponse processHttpResponse(HttpResponse httpResponse) throws Exception {

        final String json;

        if (httpResponse == null) {
            LOGGER.error(NULL_HTTP_RESPONSE.getLogMessage(commandName));
            throw new JsonReadException(NULL_HTTP_RESPONSE);
        }

        final int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
        if (httpStatusCode == HttpStatus.SC_NO_CONTENT && httpResponse.getEntity() == null) {
            LOGGER.debug("Http No Content response from the {} resource with null payload.", commandName);
            return null;
        }

        LOGGER.debug("HTTP status code returned by the REST resource is --> {}", httpStatusCode);
        HttpEntity entity = httpResponse.getEntity();

        try {
            json = EntityUtils.toString(entity, "UTF-8");
            LOGGER.debug(httpResponse.getStatusLine().toString());
        } catch (ParseException | IllegalArgumentException | IOException ex) {
            LOGGER.error(JSON_READ_FAILED.getLogMessage(commandName), ex);
            throw new JsonReadException(JSON_READ_FAILED, ex);
        }

        return new RestClientResponse(json, httpStatusCode, httpResponse.getAllHeaders());
    }

    /**
     * This method checks for null arguments
     */
    private void checkNullArguments(String groupKeyName,
                                    String commandName,
                                    HttpRequestBase httpRequest,
                                    HttpClient httpClient,
                                    HttpContext httpContext) {

        if (groupKeyName == null) {
            LOGGER.error("groupKeyName is null");
            throw new HystrixBadRequestException("groupKeyName is null");
        }
        if (commandName == null) {
            LOGGER.error("commandName is null");
            throw new HystrixBadRequestException("groupKeyName is null");
        }
        if (httpClient == null) {
            LOGGER.error("httpClient is null");
            throw new HystrixBadRequestException("httpClient is null");
        }
        if (httpRequest == null) {
            LOGGER.error("httpRequest is null");
            throw new HystrixBadRequestException("httpRequest is null");
        }
        if (httpContext == null) {
            LOGGER.error("httpContext is null");
            throw new HystrixBadRequestException("httpContext is null");
        }
    }
}
