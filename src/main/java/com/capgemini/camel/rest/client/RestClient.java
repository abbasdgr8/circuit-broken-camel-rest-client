package com.capgemini.camel.rest.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import com.capgemini.camel.rest.client.model.RestClientResponse;
import com.capgemini.camel.rest.client.util.QueryString;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capgemini.camel.rest.client.circuitbreaker.CommandRestResourceCall;
import com.capgemini.camel.rest.client.constants.RestRequestConfigurationDefaults;
import com.capgemini.camel.exception.rest.*;

import static com.capgemini.camel.exception.rest.ErrorScenario.*;

/**
 * A configurable and fully-functional REST client built on top of the
 * {@link org.apache.http.client.HttpClient} with calls wrapped in Hystrix.
 *
 * @author Abbas Attarwala
 */
public class RestClient {

    public static final String HTTP_PROXY_ENABLED = "http.proxy.enabled";
    public static final String HTTP_PROXY_HOST = "http.proxy.host";
    public static final String HTTP_PROXY_PORT = "http.proxy.port";

    private HttpClient httpClient;

    protected final String groupKeyName;
    protected final String endPoint;
    protected boolean prependGroupKeyNameToCommandKey;
    protected String groupKeyPropertyPrefix;

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);

    /**
     * Creates a REST Client specific to a REST API
     *
     * @param groupKeyName  The Hystrix groupKey name
     * @param endPoint      The REST API endpoint
     */
    public RestClient(String groupKeyName, String endPoint) {
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
    public RestClient(String groupKeyName, String endPoint, boolean prependGroupKeyNameToCommandKey) {
        this.groupKeyName = groupKeyName;
        this.endPoint = endPoint;
        this.prependGroupKeyNameToCommandKey = prependGroupKeyNameToCommandKey;
        this.groupKeyPropertyPrefix = "http.request." + this.groupKeyName;
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
                        Map<String, String> queryParams) throws ResourceStateConflictException, 
                                                                RestProtocolException,
                                                                JsonReadException,
                                                                RestClientSideException,
                                                                RestServerSideException,
                                                                RestConnectionException,
                                                                RestEndpointException,
                                                                InstantiationException {


        // call the 'with headers' method
        return this.get(resourcePath, commandName, queryParams, null);
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
        String json = callResource(getRequest, commandName);

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
                        Map<String, String> queryParams,
                        ContentType contentType) throws ResourceStateConflictException, RestProtocolException,
                                                                JsonReadException,
                                                                RestClientSideException,
                                                                RestServerSideException,
                                                                RestConnectionException,
                                                                RestEndpointException,
                                                                InstantiationException {

        // call the 'with headers' method
        return this.post(resourcePath, commandName, requestBody, queryParams, contentType, null);
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
        return callResource(postRequest, commandName);
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
                        Map<String, String> queryParams,
                        ContentType contentType) throws ResourceStateConflictException, RestProtocolException,
                                                                JsonReadException,
                                                                RestClientSideException,
                                                                RestServerSideException,
                                                                RestConnectionException,
                                                                RestEndpointException,
                                                                InstantiationException {

        // call the 'with headers' method
        return this.put(resourcePath,commandName,requestBody,queryParams, contentType, null);
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
        String json = callResource(putRequest, commandName);

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
                            Map<String, String> queryParams) throws ResourceStateConflictException, RestProtocolException,
                                                                    JsonReadException,
                                                                    RestClientSideException,
                                                                    RestServerSideException,
                                                                    RestConnectionException,
                                                                    RestEndpointException,
                                                                    InstantiationException {

        // call the 'with headers' method
        return this.delete(resourcePath,commandName,queryParams, null);
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
        String json = callResource(deleteRequest, commandName);

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
                                String commandName) throws ResourceStateConflictException, JsonReadException,
                                                           RestClientSideException,
                                                           RestServerSideException,
                                                           RestConnectionException,
                                                           RestEndpointException,
                                                           InstantiationException {

        RestClientResponse restClientResponse = null;
        commandName = prependGroupKeyNameToCommandNameIfRequired(commandName);

        final CommandRestResourceCall restResourceCall
            = new CommandRestResourceCall(groupKeyName, commandName, httpRequest, getHttpClient(), HttpClientContext.create());

        try {
            restClientResponse = restResourceCall.execute();

            processResponseFailures(restClientResponse.getHttpResponseCode(), restClientResponse.getJsonResponse(), commandName);

        } catch (HystrixRuntimeException hre) {
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
        } finally {
            if (restClientResponse != null) {
                LOGGER.debug("Releasing the connections associated with {} resource", commandName);
                httpRequest.releaseConnection();
            }
        }

        LOGGER.debug("JSON recieved from {} resource is - {}", commandName, restClientResponse);
        return restClientResponse.getJsonResponse();
    }

    protected String prependGroupKeyNameToCommandNameIfRequired(String commandName) {

        if (prependGroupKeyNameToCommandKey) {
            StringBuilder stringBuilder = new StringBuilder(commandName);
            stringBuilder.insert(0, groupKeyName);
            stringBuilder.insert(groupKeyName.length(), ".");
            commandName = stringBuilder.toString();
        }

        return commandName;

    }

    /*
    This method sets the request payload to the REST request
    */
    protected HttpEntityEnclosingRequestBase setPayload(  String requestBody,
                                                        HttpEntityEnclosingRequestBase request,
                                                        String commandName,
                                                        ContentType contentType) throws RestProtocolException,
                                                                                   InstantiationException {

        if (requestBody == null) {
            LOGGER.error(NULL_REQUEST_PAYLOAD.getLogMessage(request.getMethod(), commandName));
            throw new RestProtocolException(NULL_REQUEST_PAYLOAD);
        }
        if (requestBody.equals("")) {
            LOGGER.warn(EMPTY_REQUEST_PAYLOAD.getLogMessage(request.getMethod(), commandName));
        }
        if (contentType == null) {
            contentType = ContentType.TEXT_PLAIN;
        }

        HttpEntity entity = EntityBuilder.create()
                                .setText(requestBody)
                                .setContentType(contentType)
                            .build();
        request.setEntity(entity);
        LOGGER.debug("Request Body --> {}", requestBody);

        return request;
    }

    /*
    This method extracts the JSON string from the HTTP response and
    checks the HTTP status code to check for valid response types
    */
    protected String extractJson(HttpResponse httpResponse, String commandName) throws ResourceStateConflictException, JsonReadException,
                                                                                     RestClientSideException,
                                                                                     RestServerSideException,
                                                                                     InstantiationException{

        String json = null;

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
            logHttpHeaders(httpResponse.getAllHeaders(), "Response");
        } catch (ParseException | IllegalArgumentException | IOException ex) {
            LOGGER.error(JSON_READ_FAILED.getLogMessage(commandName), ex);
            throw new JsonReadException(JSON_READ_FAILED, ex);
        }

        processResponseFailures(httpStatusCode, json, commandName);
        return json;
    }

    /**
     * Checks if the httpResponse from the REST Web Service is a success or a failure
     */
    protected void processResponseFailures( int httpStatusCode,
                                            String json,
                                            String commandName) throws ResourceStateConflictException,RestClientSideException,
                                                                       RestServerSideException,
                                                                       InstantiationException {

        if (httpStatusCode == 400) {
            LOGGER.error(CB_BAD_REQUEST.getLogMessage(commandName, httpStatusCode, json));
            throw new RestClientSideException(json);
        }else if(httpStatusCode == 409){
            LOGGER.error(CONFLICT_HTTP_RESPONSE.getLogMessage(commandName, httpStatusCode, json));
            throw new ResourceStateConflictException(CONFLICT_HTTP_RESPONSE,json);
        }else if (httpStatusCode >= 401 && httpStatusCode < 500) {
            LOGGER.error(RESPONSE_FAILURE.getLogMessage(commandName, httpStatusCode, json));
            throw new RestClientSideException(RESPONSE_FAILURE, json);
        } else if (httpStatusCode >= 500 && httpStatusCode < 600) {
            LOGGER.error(RESPONSE_FAILURE.getLogMessage(commandName, httpStatusCode, json));
            throw new RestServerSideException(RESPONSE_FAILURE, json);
        }
    }


    /**
     * Sets the HTTP Request configuration parameters.
     *
     * First priority is given to the resource specific configuration parameters
     * which are identified by the commandName.
     *
     * Second priority is given to the service/API specific configuration parameters
     * which are identified by the groupKeyName.
     *
     * Third and last fallback are constants specified in the
     * {@link com.capgemini.camel.rest.client.constants.RestRequestConfigurationDefaults} class
     */
    protected void setHttpRequestConfig(HttpRequestBase httpRequest, String commandName) {
        DynamicPropertyFactory propertyFactory = DynamicPropertyFactory.getInstance();

        final String commandNamePropertyPrefix = "http.request." + commandName;

        int connectionTimeout = propertyFactory.getIntProperty(commandNamePropertyPrefix + ".connectionTimeout",
                                    propertyFactory.getIntProperty(groupKeyPropertyPrefix + ".connectionTimeout",
                                        RestRequestConfigurationDefaults.CONNECTION_TIMEOUT).getValue()).getValue();
        int connectionRequestTimeout = propertyFactory.getIntProperty(commandNamePropertyPrefix + ".connectionRequestTimeout",
                                            propertyFactory.getIntProperty(groupKeyPropertyPrefix + ".connectionRequestTimeout",
                                                RestRequestConfigurationDefaults.CONNECTION_REQUEST_TIMEOUT).getValue()).getValue();
        int socketTimeout = propertyFactory.getIntProperty(commandNamePropertyPrefix + ".socketTimeout",
                                    propertyFactory.getIntProperty(groupKeyPropertyPrefix + ".socketTimeout",
                                        RestRequestConfigurationDefaults.SOCKET_TIMEOUT).getValue()).getValue();
        boolean staleConnectionCheck = propertyFactory.getBooleanProperty(commandNamePropertyPrefix + ".staleConnectionCheck",
                                            propertyFactory.getBooleanProperty(groupKeyPropertyPrefix + ".staleConnectionCheck",
                                                RestRequestConfigurationDefaults.STALE_CONNECTION_CHECK).getValue()).getValue();

        // proxies can only be set at group key level

        boolean httpProxyEnabled = propertyFactory.getBooleanProperty(groupKeyPropertyPrefix + ".proxy.enabled",
                propertyFactory.getBooleanProperty(HTTP_PROXY_ENABLED, false).getValue()).getValue();
        String httpProxyHost = propertyFactory.getStringProperty(groupKeyPropertyPrefix + ".proxy.host",
                propertyFactory.getStringProperty(HTTP_PROXY_HOST, null).getValue()).getValue();
        int httpProxyPort = propertyFactory.getIntProperty(groupKeyPropertyPrefix + ".proxy.port",
                propertyFactory.getIntProperty(HTTP_PROXY_PORT, 0).getValue()).getValue();

        String cookieSpec = propertyFactory.getStringProperty(groupKeyPropertyPrefix + ".cookieSpec", CookieSpecs.IGNORE_COOKIES)
                .getValue();

        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setSocketTimeout(socketTimeout)
                .setStaleConnectionCheckEnabled(staleConnectionCheck)
                .setCookieSpec(cookieSpec);

        if (httpProxyEnabled && httpProxyHost != null && httpProxyPort != 0) {
            HttpHost proxy = new HttpHost(httpProxyHost, httpProxyPort);
            configBuilder.setProxy(proxy);
            LOGGER.debug("Using HTTP proxy Host: {} , Port: {}", httpProxyHost, httpProxyPort);
        }
        httpRequest.setConfig(configBuilder.build());
    }

    /**
     * Logs the HTTP Request Details
     */
    protected void logHttpRequestDetails(HttpRequestBase httpRequest) {
        LOGGER.debug("HTTP request configuration --> connectTimeout: " + httpRequest.getConfig().getConnectTimeout()
                                                + ", connectionRequestTimeout: " + httpRequest.getConfig().getConnectionRequestTimeout()
                                                + ", socketTimeout: " + httpRequest.getConfig().getSocketTimeout()
                                                + ", staleConnectionCheck: " + httpRequest.getConfig().isStaleConnectionCheckEnabled()
                                                + ", cookieSpec: " + httpRequest.getConfig().getCookieSpec());
        LOGGER.debug(httpRequest.getRequestLine().toString());
        logHttpHeaders(httpRequest.getAllHeaders(), "Request");
    }

    /**
     * Logs the HTTP headers
     */
    protected void logHttpHeaders(Header[] httpHeaders, String type) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(type);
        logBuilder.append(" Headers --> \n");
        for (Header httpHeader : httpHeaders) {
            if (httpHeader.getName().contains("Authorization")) {
                continue;
            }
            logBuilder.append(httpHeader.getName());
            logBuilder.append(": ");
            logBuilder.append(httpHeader.getValue());
            logBuilder.append('\n');
        }
        LOGGER.debug("{}", logBuilder.toString());
    }

    /**
     * This method builds up the request URL with the endpoint, resourcePath and the queryString.
     * And the it returns the URI object of the requestURL.
     * @see com.capgemini.camel.rest.client.util.QueryString
     */
    protected URI createEndpointUri(String resourcePath, Map<String, String> queryParams) throws RestClientSideException,
                                                                                               InstantiationException {
        String fullUri = endPoint + resourcePath;
        URI uri = null;

        try {
            if (queryParams != null && !queryParams.isEmpty()) {
                QueryString queryString = new QueryString(queryParams);
                fullUri = endPoint + resourcePath + queryString;
            }
            uri = URI.create(fullUri);
        } catch (IllegalArgumentException | UnsupportedEncodingException | InstantiationException ex) {
            LOGGER.error(URI_CREATION_FAILED.getLogMessage(fullUri, ex.getMessage()));
            throw new RestClientSideException(URI_CREATION_FAILED);
        }

        return uri;
    }

    /**
     * Set the headers on the request
     * @param requestBase the request
     * @param headers the headers map (can be null)
     */
    protected void setHeaders(HttpRequestBase requestBase, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        } else {
            for (final String key : headers.keySet()) {
                requestBase.setHeader(key, headers.get(key));
            }
        }
    }

    /**
     * Public getter for use with Spring (or suchlike)
     * @return
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Public setter for use with Spring (or suchlike)
     * @param httpClient
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
