package com.example.devops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {

    @Test
    void testMessage() {
        assertEquals("DevOps Pipeline works from feature-demo!", App.message());
    }
}
