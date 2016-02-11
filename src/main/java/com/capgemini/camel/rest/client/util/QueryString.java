package com.capgemini.camel.rest.client.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * This class builds up a queryString from a {@link Map<String, String>} of key-value pairs.
 *
 * Warning: This class is not suitable for use as a spring-bean.
 *
 * @author Abbas Attarwala
 */
public class QueryString {

    private StringBuilder queryString;
    private String encoding = "UTF-8";

    private final String QUERY_SEPARATOR = "&";
    private final String QUERY_STRING_FORMAT = "%s=%s";
    private final String QUERY_STRING_SEPARATOR = "?";

    /**
     * Constructor with key-value {@link Map<String, String>} as arg.
     *
     * @param queryParams
     * @throws UnsupportedEncodingException
     * @throws InstantiationException
     */
    public QueryString(Map<String, String> queryParams) throws UnsupportedEncodingException,
                                                               InstantiationException {
        checkNullOrEmptyQueryParams(queryParams);
        buildQueryString(queryParams);
    }

    /**
     * Constructor with key-value {@link java.applet.Map<String, String>} and the URL encoding as arg.
     *
     * @param queryParams
     * @param encoding The URL encoding
     * @throws UnsupportedEncodingException
     * @throws InstantiationException
     */
    public QueryString(Map<String, String> queryParams, String encoding) throws UnsupportedEncodingException, 
                                                                                InstantiationException {
        this.encoding = encoding;
        checkNullOrEmptyQueryParams(queryParams);
        buildQueryString(queryParams);
    }    
    
    /**
     * This method will build the queryString. 
     * Ex:- a=1&b=2&c=3
     */
    private void buildQueryString(Map<String, String> queryParams) throws UnsupportedEncodingException {
        queryString = new StringBuilder();
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append(QUERY_SEPARATOR);
            }
            
            queryString.append(String.format(QUERY_STRING_FORMAT, 
                                            urlEncode(param.getKey()), 
                                            urlEncode(param.getValue())));
        }
    }
    
    /**
     * URL-Encodes the provided argument.
     */
    private String urlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, encoding);
    }
    
    /**
     * The overridden toString implementation.
     * 
     * @return The queryString with the '?' prefix.
     * Ex: ?a=1&b=2&c=3
     */
    @Override
    public String toString() {
        return QUERY_STRING_SEPARATOR + queryString.toString();
    }
    
    /**
     * Checks if any query parameters have been specified,
     * Throws an exception if not.
     */
    private void checkNullOrEmptyQueryParams(Map<String, String> queryParams) throws InstantiationException {
        if (queryParams == null || queryParams.isEmpty()) {
            throw new InstantiationException("Cannot create a queryString if no query params are provided.");
        }
    }
}
