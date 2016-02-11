package com.capgemini.camel.rest.client;

import java.net.URI;
import java.util.Map;

import com.capgemini.camel.exception.rest.*;
import com.capgemini.camel.rest.client.circuitbreaker.CommandCachedRestResourceCall;
import com.capgemini.camel.rest.client.model.RestClientResponse;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.capgemini.camel.exception.rest.ErrorScenario.*;

/**
 * A configurable and fully-functional REST client built on top of the Apache
 * {@link HttpClient} with calls wrapped in Netflix's Hystrix.
 *
 * @author Abbas Attarwala
 * @author Nick Walter
 */
public class CachingRestClient extends RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachingRestClient.class);

    /**
     * Creates a Cached REST Client specific to a REST API
     *
     * @param groupKeyName  The Hystrix groupKey name
     * @param endPoint      The REST API endpoint
     */
    public CachingRestClient(String groupKeyName, String endPoint) {
        this(groupKeyName, endPoint, false);
    }

    /**
     * Creates a REST Client specific to a REST API
     *
     * @param groupKeyName  The Hystrix groupKey name
     * @param endPoint      The REST API endpoint
     * @param prependGroupKeyNameToCommandKey      Set to TRUE to append the Group Key to the command key when building the Hystrix command,
     *                      FALSE otherwise
     */
    public CachingRestClient(String groupKeyName, String endPoint, boolean prependGroupKeyNameToCommandKey) {
        super(groupKeyName, endPoint, prependGroupKeyNameToCommandKey);
    }

    /**
     * A GET resource call to a specified resourcePath with the specified queryParams
     *
     * @param resourcePath  The resource path. Must start with "/".
     * @param commandName   The Hystrix command name
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String get(  String resourcePath,
                        String commandName,
                        String cacheKey,
                        Map<String, String> queryParams) throws ResourceStateConflictException,
                                                                RestProtocolException,
                                                                JsonReadException,
                                                                RestClientSideException,
                                                                RestServerSideException,
                                                                RestConnectionException,
                                                                RestEndpointException,
                                                                InstantiationException {


        // call the 'with headers' method
        return this.get(resourcePath, commandName, cacheKey, queryParams, null);
    }

    /**
     * A GET resource call to a specified resourcePath with the specified queryParams
     *
     * @param resourcePath  The resource path. Must start with "/".
     * @param commandName   The Hystrix command name
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @param headers       Key-Value pair {@link Map<String, String>} of header values (can be null).
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String get(  String resourcePath,
                        String commandName,
                        String cacheKey,
                        Map<String, String> queryParams,
                        Map<String, String> headers) throws ResourceStateConflictException,
                                                            RestProtocolException,
                                                            JsonReadException,
                                                            RestClientSideException,
                                                            RestServerSideException,
                                                            RestConnectionException,
                                                            RestEndpointException,
                                                            InstantiationException {

        LOGGER.debug("Creating an HTTP GET request for {} resource", commandName);
        URI endpointUri = createEndpointUri(resourcePath, queryParams);
        HttpRequestBase getRequest = new HttpGet(endpointUri);
        setHeaders(getRequest, headers);
        setHttpRequestConfig(getRequest, commandName);
        logHttpRequestDetails(getRequest);
        String json = callResource(getRequest, commandName, cacheKey);

        return json;
    }

    /**
     * A POST resource call to a specified resourcePath with the specified queryParams and a requestBody
     *
     * @param resourcePath  The resource path. Must start with "/".
     * @param commandName   The Hystrix command name
     * @param requestBody   The requestBody that needs to be sent to the REST resource. This can be XML or JSON or even a simple string.
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @param contentType   The HTTP content type for the request body, if null then we will assume 'text/plain'
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String post( String resourcePath,
                        String commandName,
                        String requestBody,
                        String requestCacheKey,
                        Map<String, String> queryParams,
                        ContentType contentType) throws ResourceStateConflictException,
                                                        RestProtocolException,
                                                        JsonReadException,
                                                        RestClientSideException,
                                                        RestServerSideException,
                                                        RestConnectionException,
                                                        RestEndpointException,
                                                        InstantiationException {

        // call the 'with headers' method
        return this.post(resourcePath, commandName, requestBody, requestCacheKey, queryParams, contentType, null);
    }

    /**
     * A POST resource call to a specified resourcePath with the specified queryParams and a requestBody
     *
     * @param resourcePath  The resource path. Must start with "/".
     * @param commandName   The Hystrix command name
     * @param requestBody   The requestBody that needs to be sent to the REST resource. This can be XML or JSON or even a simple string.
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @param contentType   The HTTP content type for the request body, if null then we will assume 'text/plain'
     * @param headers       Key-Value pair {@link Map<String, String>} of header variables (can be null).
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String post( String resourcePath,
                        String commandName,
                        String requestBody,
                        String requestCacheKey,
                        Map<String, String> queryParams,
                        ContentType contentType,
                        Map<String, String> headers) throws ResourceStateConflictException,
                                                            RestProtocolException,
                                                            JsonReadException,
                                                            RestClientSideException,
                                                            RestServerSideException,
                                                            RestConnectionException,
                                                            RestEndpointException,
                                                            InstantiationException {

        LOGGER.debug("Creating an HTTP POST request for {} resource", commandName);
        URI endpointUri = createEndpointUri(resourcePath, queryParams);
        HttpEntityEnclosingRequestBase postRequest = new HttpPost(endpointUri);
        setHeaders(postRequest, headers);
        setPayload(requestBody, postRequest, commandName, contentType);
        setHttpRequestConfig(postRequest, commandName);
        logHttpRequestDetails(postRequest);
        return callResource(postRequest, commandName, requestCacheKey);
    }


    /**
     * A PUT resource call to a specified resourcePath with the specified queryParams and a requestBody
     *
     * @param resourcePath  The resource path. Must start with "/"
     * @param commandName   The Hystrix command name
     * @param requestBody   The requestBody that needs to be sent to the REST resource. This can be XML or JSON or even a simple string.
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @param contentType   The HTTP content type for the request body, if null then we will assume 'text/plain'
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String put(  String resourcePath,
                        String commandName,
                        String requestBody,
                        String requestCacheKey,
                        Map<String, String> queryParams,
                        ContentType contentType) throws ResourceStateConflictException,
                                                        RestProtocolException,
                                                        JsonReadException,
                                                        RestClientSideException,
                                                        RestServerSideException,
                                                        RestConnectionException,
                                                        RestEndpointException,
                                                        InstantiationException {

        // call the 'with headers' method
        return this.put(resourcePath,commandName,requestBody, requestCacheKey, queryParams, contentType, null);
    }

    /**
     * A PUT resource call to a specified resourcePath with the specified queryParams and a requestBody
     *
     * @param resourcePath  The resource path. Must start with "/"
     * @param commandName   The Hystrix command name
     * @param requestBody   The requestBody that needs to be sent to the REST resource. This can be XML or JSON or even a simple string.
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @param contentType   The HTTP content type for the request body, if null then we will assume 'text/plain'
     * @param headers       Key-Value pair {@link Map<String, String>} of header values (can be null).
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String put(  String resourcePath,
                        String commandName,
                        String requestBody,
                        String requestCacheKey,
                        Map<String, String> queryParams,
                        ContentType contentType,
                        Map<String, String> headers) throws ResourceStateConflictException,
                                                            RestProtocolException,
                                                            JsonReadException,
                                                            RestClientSideException,
                                                            RestServerSideException,
                                                            RestConnectionException,
                                                            RestEndpointException,
                                                            InstantiationException {

        LOGGER.debug("Creating an HTTP PUT request for {} resource", commandName);
        URI endpointUri = createEndpointUri(resourcePath, queryParams);
        HttpEntityEnclosingRequestBase putRequest = new HttpPut(endpointUri);
        setHeaders(putRequest, headers);
        setPayload(requestBody, putRequest, commandName, contentType);
        setHttpRequestConfig(putRequest, commandName);
        logHttpRequestDetails(putRequest);
        String json = callResource(putRequest, commandName, requestCacheKey);

        return json;
    }

    /**
     * A DELETE resource call to a specified resourcePath with the specified queryParams
     *
     * @param resourcePath  The resource path. Must start with "/"
     * @param commandName   The Hystrix command name
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String delete(   String resourcePath,
                            String commandName,
                            String requestCacheKey,
                            Map<String, String> queryParams) throws ResourceStateConflictException,
                                                                    RestProtocolException,
                                                                    JsonReadException,
                                                                    RestClientSideException,
                                                                    RestServerSideException,
                                                                    RestConnectionException,
                                                                    RestEndpointException,
                                                                    InstantiationException {

        // call the 'with headers' method
        return this.delete(resourcePath,commandName,requestCacheKey,queryParams, null);
    }

    /**
     * A DELETE resource call to a specified resourcePath with the specified queryParams
     *
     * @param resourcePath  The resource path. Must start with "/"
     * @param commandName   The Hystrix command name
     * @param queryParams   Key-Value pair {@link Map<String, String>} of query parameters.
     * @param headers       Key-Value pair {@link Map<String, String>} of header values (can be null).
     * @return              The JSON string
     * @throws RestProtocolException
     * @throws JsonReadException
     * @throws RestClientSideException
     * @throws RestServerSideException
     * @throws RestConnectionException
     * @throws RestEndpointException
     * @throws InstantiationException
     */
    public String delete(   String resourcePath,
                            String commandName,
                            String requestCacheKey,
                            Map<String, String> queryParams,
                            Map<String, String> headers) throws ResourceStateConflictException,
                                                                RestProtocolException,
                                                                JsonReadException,
                                                                RestClientSideException,
                                                                RestServerSideException,
                                                                RestConnectionException,
                                                                RestEndpointException,
                                                                InstantiationException {

        LOGGER.debug("Creating an HTTP DELETE request for {} resource", commandName);
        URI endpointUri = createEndpointUri(resourcePath, queryParams);
        HttpRequestBase deleteRequest = new HttpDelete(endpointUri);
        setHeaders(deleteRequest, headers);
        setHttpRequestConfig(deleteRequest, commandName);
        logHttpRequestDetails(deleteRequest);
        String json = callResource(deleteRequest, commandName, requestCacheKey);

        return json;
    }

    /**
     * This method triggers the resource call via the Hystrix command,
     * extracts the JSON,
     * handles and wraps exceptions if any,
     * releases resources related to the HTTP call,
     * and returns the JSON String.
     */
    private String callResource(HttpRequestBase httpRequest,
                                String commandName,
                                String requestCacheKey) throws ResourceStateConflictException,
                                                               JsonReadException,
                                                               RestClientSideException,
                                                               RestServerSideException,
                                                               RestConnectionException,
                                                               RestEndpointException,
                                                               InstantiationException {

        RestClientResponse restClientResponse = null;
        commandName = prependGroupKeyNameToCommandNameIfRequired(commandName);

        final CommandCachedRestResourceCall restResourceCall
            = new CommandCachedRestResourceCall(groupKeyName, commandName, requestCacheKey, httpRequest, getHttpClient(), HttpClientContext.create());

        try {
            restClientResponse = restResourceCall.execute();

            LOGGER.debug("Call To CachedRestResource: {}, Was Response Returned From Cache: {}", commandName, restResourceCall.isResponseFromCache());

            processResponseFailures(restClientResponse.getHttpResponseCode(), restClientResponse.getJsonResponse(), commandName);

        } catch (HystrixRuntimeException hre) {
            CommandCachedRestResourceCall.flushCache(commandName, requestCacheKey);
            httpRequest.abort();

            LOGGER.debug("HTTP Request to {} resource has been aborted.");
            switch(hre.getFailureType()) {
                case TIMEOUT:
                    LOGGER.error(CB_TIMED_OUT.getLogMessage(commandName), hre);
                    throw new RestConnectionException(CB_TIMED_OUT);
                case SHORTCIRCUIT:
                    LOGGER.error(CB_SHORT_CIRCUITED.getLogMessage(commandName), hre);
                    throw new RestEndpointException(CB_SHORT_CIRCUITED, hre);
                case REJECTED_THREAD_EXECUTION:
                    LOGGER.error(CB_REJECTED_THREAD_EXECUTION.getLogMessage(commandName), hre);
                    throw new RestEndpointException(CB_REJECTED_THREAD_EXECUTION, hre);
                case REJECTED_SEMAPHORE_FALLBACK:
                    LOGGER.error(CB_REJECTED_SEMAPHORE_FALLBACK.getLogMessage(commandName), hre);
                    throw new RestEndpointException(CB_REJECTED_SEMAPHORE_FALLBACK, hre);
                case REJECTED_SEMAPHORE_EXECUTION:
                    LOGGER.error(CB_REJECTED_SEMAPHORE_EXECUTION.getLogMessage(commandName), hre);
                    throw new RestServerSideException(CB_REJECTED_SEMAPHORE_EXECUTION, hre);
                case COMMAND_EXCEPTION:
                    LOGGER.error(CB_BAD_REQUEST.getLogMessage(commandName), hre);
                    throw new RestEndpointException(CB_BAD_REQUEST);
                default:
                    LOGGER.error(CB_UNKNOWN_ERROR.getLogMessage(commandName), hre);
                    throw new RestServerSideException(CB_UNKNOWN_ERROR, hre);
            }
        } catch (HystrixBadRequestException hbre) {
            LOGGER.error(CB_BAD_REQUEST.getLogMessage(commandName), hbre);
            throw new RestClientSideException(CB_BAD_REQUEST);

        } catch (Exception e) {
            // for all other exceptions flush this key from the cache.
            CommandCachedRestResourceCall.flushCache(commandName, requestCacheKey);
            throw new RestServerSideException(CB_UNKNOWN_ERROR, e);

        } finally {
            if (restClientResponse != null) {
                LOGGER.debug("Releasing the connections associated with {} resource", commandName);
                httpRequest.releaseConnection();
            }
        }

        LOGGER.debug("JSON recieved from {} resource is - {}", commandName, restClientResponse);
        return restClientResponse.getJsonResponse();
    }
}
