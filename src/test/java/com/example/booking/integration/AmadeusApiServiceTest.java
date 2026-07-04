package com.example.booking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmadeusApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AmadeusApiService service;

    @BeforeEach
    void setUp() {
        service = new AmadeusApiService(restTemplate, redisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "tokenUrl", "https://example.test/oauth/token");
        ReflectionTestUtils.setField(service, "clientId", "client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "client-secret");
    }

    @Test
    void getAccessTokenFallsBackWhenRedisReadAndWriteFail() {
        JsonNode tokenResponse = new ObjectMapper().createObjectNode()
                .put("access_token", "sandbox-token")
                .put("expires_in", 1800);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("amadeus:access_token"))
                .thenThrow(new DataAccessResourceFailureException("Unable to connect to Redis"));
        when(restTemplate.postForEntity(eq("https://example.test/oauth/token"), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("Unable to connect to Redis"))
                .when(valueOperations)
                .set(eq("amadeus:access_token"), eq("sandbox-token"), any(Duration.class));

        String token = service.getAccessToken();

        assertThat(token).isEqualTo("sandbox-token");
        verify(restTemplate).postForEntity(eq("https://example.test/oauth/token"), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAccessTokenReusesInMemoryTokenAfterFirstFetch() {
        JsonNode tokenResponse = new ObjectMapper().createObjectNode()
                .put("access_token", "sandbox-token")
                .put("expires_in", 1800);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("amadeus:access_token")).thenReturn(null);
        when(restTemplate.postForEntity(eq("https://example.test/oauth/token"), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        String firstToken = service.getAccessToken();
        String secondToken = service.getAccessToken();

        assertThat(firstToken).isEqualTo("sandbox-token");
        assertThat(secondToken).isEqualTo("sandbox-token");
        verify(restTemplate).postForEntity(eq("https://example.test/oauth/token"), any(HttpEntity.class), eq(JsonNode.class));
                verify(valueOperations, times(1)).get("amadeus:access_token");
    }
}