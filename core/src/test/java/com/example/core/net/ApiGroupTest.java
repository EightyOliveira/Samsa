package com.example.core.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiGroupTest {
    @Test
    void shouldRegisterAndFindGetHandler() {
        ApiGroup apiGroup = new ApiGroup();

        apiGroup.registerGetHandler("/test", () -> "test");
        apiGroup.registerGetHandler("/echo", (String s) -> s);

        assertTrue(apiGroup.getHandler("GET", "/test").isPresent());
        assertTrue(apiGroup.getHandler("GET", "/echo").isPresent());
        assertFalse(apiGroup.getHandler("GET", "/notfound").isPresent());
    }

    @Test
    void shouldRegisterAndFindPostHandler() {
        ApiGroup apiGroup = new ApiGroup();
        apiGroup.registerPostHandler("/users", (String user) -> "created");

        assertTrue(apiGroup.getHandler("POST", "/users").isPresent());
        assertFalse(apiGroup.getHandler("POST", "/notfound").isPresent());
    }

    @Test
    void shouldReturnEmptyForUnsupportedMethod() {
        ApiGroup apiGroup = new ApiGroup();
        apiGroup.registerGetHandler("/test", () -> "test");

        assertFalse(apiGroup.getHandler("PUT", "/test").isPresent());
        assertFalse(apiGroup.getHandler("DELETE", "/test").isPresent());
    }
}
