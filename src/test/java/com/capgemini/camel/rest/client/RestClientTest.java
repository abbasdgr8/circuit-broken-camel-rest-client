package com.capgemini.camel.rest.client;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * Test for the RestClient
 *
 * @author Simon Irving
 */
@RunWith(MockitoJUnitRunner.class)
public class RestClientTest {

    @Mock
    private HttpClient httpClient;

    @InjectMocks
    RestClient restClientInitiatedWithOldConstructor = new RestClient("GroupKeyName", "Endpoint");

    @InjectMocks
    RestClient restClientPrependGroupKeyNameToCommandName = new RestClient("GroupKeyName", "Endpoint", true);

    @InjectMocks
    RestClient restClientDoNotPrependGroupKeyNameToCommandName = new RestClient("GroupKeyName", "Endpoint", false);

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOldConstructor() {
        assertEquals("CommandName", restClientInitiatedWithOldConstructor.prependGroupKeyNameToCommandNameIfRequired("CommandName"));
    }

    @Test
    public void testDoNotPrependGroupKeyNameToCommandName() {
        assertEquals("CommandName", restClientDoNotPrependGroupKeyNameToCommandName.prependGroupKeyNameToCommandNameIfRequired("CommandName"));
    }

    @Test
    public void testPrependGroupKeyNameToCommandName() {
        assertEquals("GroupKeyName.CommandName", restClientPrependGroupKeyNameToCommandName.prependGroupKeyNameToCommandNameIfRequired("CommandName"));
    }

}
