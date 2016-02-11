package com.capgemini.camel.rest.client.circuitbreaker;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixRequestCache;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;

/**
 * Netflix Hystrix Circuit Breaker wrapper for all Cached REST WebService calls
 * <p/>
 * https://github.com/Netflix/Hystrix
 *
 * @author Abbas Attarwala
 * @author Nick Walter
 * @see HystrixCommand
 * @see com.netflix.hystrix.HystrixExecutable
 */
public class CommandCachedRestResourceCall extends CommandRestResourceCall {

    private final String requestCacheKey;

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
    public CommandCachedRestResourceCall(final String groupKeyName,
                                         final String commandName,
                                         final String requestCacheKey,
                                         final HttpRequestBase httpRequest,
                                         final HttpClient httpClient,
                                         final HttpContext httpContext) {

        super(groupKeyName, commandName, httpRequest, httpClient, httpContext);

        this.requestCacheKey = requestCacheKey;
    }

    @Override
    protected String getCacheKey() {
        return this.requestCacheKey;
    }

    /**
     * Allow the cache to be flushed for this object.
     *
     * @param commandName The hystrix command name
     * @param requestCacheKey key to flush from cache
     */
    public static void flushCache(String commandName, String requestCacheKey) {
        HystrixRequestCache.getInstance(HystrixCommandKey.Factory.asKey(commandName),
                HystrixConcurrencyStrategyDefault.getInstance()).clear(requestCacheKey);
    }
}
