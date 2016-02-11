package com.capgemini.camel.rest.client.constants;

/**
 * REST Client request configuration defaults are specified here. The units are specified in milliseconds.
 * These will be overridden if properties more specific to the REST resource have been provided.
 *
 * @author Abbas Attarwala
 */
public class RestRequestConfigurationDefaults {
    public static final int CONNECTION_TIMEOUT = 2000;
    public static final int SOCKET_TIMEOUT = 2000;
    public static final int CONNECTION_REQUEST_TIMEOUT = 2000;
    public static final boolean STALE_CONNECTION_CHECK = true;
    
    /**
     * Private constructor
     */
    private RestRequestConfigurationDefaults() {
        throw new RuntimeException("This is a non-instantiable class");
    }
}
