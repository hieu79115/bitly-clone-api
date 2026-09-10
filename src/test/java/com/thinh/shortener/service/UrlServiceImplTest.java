package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.CreateUrlRequestDto;
import com.thinh.shortener.domain.dto.response.UrlResponseDto;
import com.thinh.shortener.domain.entity.Url;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.mapper.UrlMapper;
import com.thinh.shortener.exception.AliasAlreadyExistsException;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.exception.UrlExpiredException;
import com.thinh.shortener.repository.TagRepository;
import com.thinh.shortener.repository.UrlRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.impl.UrlServiceImpl;
import com.thinh.shortener.util.Base62Encoder;
import com.thinh.shortener.util.RedisKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - UrlServiceImpl Business Logic")
public class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @Mock
    private UrlMapper urlMapper;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private UrlServiceImpl urlService;

    private User mockUser;
    private Url mockUrl;


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "domain", "https://short.ly/");

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mockUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .build();

        mockUrl = Url.builder()
                .id(100L)
                .shortCode("myCustomAlias")
                .originalUrl("https://example.com/long-url")
                .user(mockUser)
                .build();
    }

    // ==========================================
    // 1. CREATE SHORT URL TEST
    // ==========================================

    @Test
    @DisplayName("Successfully create a link with a custom alias when the alias is not yet in use")
    void createShortUrl_WithCustomAlias_Success() {
        // GIVEN
        CreateUrlRequestDto request = CreateUrlRequestDto.builder()
                .originalUrl("https://example.com/long-url")
                .customAlias("myCustomAlias")
                .build();

        UrlResponseDto expectedResponse = UrlResponseDto.builder()
                .shortCode("myCustomAlias")
                .originalUrl("https://example.com/long-url")
                .shortUrl("https://short.ly/myCustomAlias")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(urlRepository.findByShortCode("myCustomAlias")).thenReturn(Optional.empty());
        when(urlRepository.save(any(Url.class))).thenReturn(mockUrl);
        when(urlMapper.toDto(any(Url.class), anyString())).thenReturn(expectedResponse);

        // WHEN
        UrlResponseDto actualResponse = urlService.createShortUrl(request, "user@test.com");

        // THEN
        assertNotNull(actualResponse);
        assertEquals("myCustomAlias", actualResponse.getShortCode());
        assertEquals("https://example.com/long-url", actualResponse.getOriginalUrl());
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    @DisplayName("Creating a link with a duplicate custom alias must throw an AliasAlreadyExistsException")
    void createShortUrl_WhenAliasAlreadyExists_ShouldThrowException() {
        // GIVEN
        CreateUrlRequestDto request = CreateUrlRequestDto.builder()
                .originalUrl("https://example.com/long-url")
                .customAlias("myCustomAlias")
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(urlRepository.findByShortCode("myCustomAlias")).thenReturn(Optional.of(mockUrl));
        // WHEN & THEN
        assertThrows(AliasAlreadyExistsException.class, () -> {
            urlService.createShortUrl(request, "user@test.com");
        });

        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    @DisplayName("Successfully generated a link automatically using the Base62 algorithm")
    void createShortUrl_AutoGenerateBase62_Success() {
        // GIVEN
        CreateUrlRequestDto request = CreateUrlRequestDto.builder()
                .originalUrl("https://example.com/long-url")
                .customAlias(null)
                .build();
        Url savedUrl = Url.builder()
                .id(125L)
                .originalUrl("https://example.com/long-url")
                .user(mockUser)
                .build();
        UrlResponseDto expectedResponse = UrlResponseDto.builder()
                .shortCode("cb")
                .originalUrl("https://example.com/long-url")
                .shortUrl("https://short.ly/cb")
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);
        when(base62Encoder.encode(125L)).thenReturn("cb");
        when(urlMapper.toDto(any(Url.class), anyString())).thenReturn(expectedResponse);
        // WHEN
        UrlResponseDto actualResponse = urlService.createShortUrl(request, "user@test.com");
        // THEN
        assertNotNull(actualResponse);
        assertEquals("cb", actualResponse.getShortCode());
        verify(base62Encoder, times(1)).encode(125L);
        verify(urlRepository, times(2)).save(any(Url.class));
    }

    // ========================================================
    // 2. TEST GET ORIGINAL LINK (getOriginalUrl & Caching)
    // ========================================================

    @Test
    @DisplayName("Cache Hit: Return immediately from Redis RAM without querying the database")
    void getOriginalUrl_WhenCacheHit_ShouldReturnFromRedisDirectly() {
        // GIVEN
        String shortCode = "myCustomAlias";
        String cachedOriginalUrl = "https://example.com/cached-url";

        when(valueOperations.get(RedisKeyConstants.URL_PREFIX + shortCode)).thenReturn(cachedOriginalUrl);
        // WHEN
        String actualUrl = urlService.getOriginalUrl(shortCode);
        // THEN
        assertEquals(cachedOriginalUrl, actualUrl);

        verify(urlRepository, never()).findByShortCode(anyString());
    }

    @Test
    @DisplayName("Cache Miss: Look up in the database, store in Redis, and return the result")
    void getOriginalUrl_WhenCacheMiss_ShouldQueryDbAndSaveToRedis() {
        // GIVEN
        String shortCode = "myCustomAlias";

        when(valueOperations.get(RedisKeyConstants.URL_PREFIX + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mockUrl));
        // WHEN
        String actualUrl = urlService.getOriginalUrl(shortCode);
        // THEN
        assertEquals("https://example.com/long-url", actualUrl);
        verify(urlRepository, times(1)).findByShortCode(shortCode);

        verify(valueOperations, times(1)).set(eq(RedisKeyConstants.URL_PREFIX + shortCode), eq("https://example.com/long-url"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("Throws UrlExpiredException when the link has expired")
    void getOriginalUrl_WhenUrlIsExpired_ShouldThrowUrlExpiredException() {
        // GIVEN
        String shortCode = "expiredLink";
        Url expiredUrl = Url.builder()
                .shortCode(shortCode)
                .originalUrl("https://example.com/expired")
                .expiresAt(LocalDateTime.now().minusHours(2))
                .build();
        when(valueOperations.get(RedisKeyConstants.URL_PREFIX + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(expiredUrl));
        // WHEN & THEN
        assertThrows(UrlExpiredException.class, () -> {
            urlService.getOriginalUrl(shortCode);
        });
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Throw a ResourceNotFoundException when the link is not found in either Redis or the database")
    void getOriginalUrl_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // GIVEN
        String shortCode = "nonExistent";
        when(valueOperations.get(RedisKeyConstants.URL_PREFIX + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());
        // WHEN & THEN
        assertThrows(ResourceNotFoundException.class, () -> {
            urlService.getOriginalUrl(shortCode);
        });
    }

    // ==================================================
    // 3. TEST DELETE LINK (deleteUrl & Evict Cache)
    // ==================================================

    @Test
    @DisplayName("Successfully deleted the link and cleared the corresponding cache in Redis")
    void deleteUrl_Success_ShouldDeleteFromDbAndEvictCache() {
        // GIVEN
        Long urlId = 100L;
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(urlRepository.findByIdAndUserId(urlId, 1L)).thenReturn(Optional.of(mockUrl));
        // WHEN
        urlService.deleteUrl(urlId, "user@test.com");
        // THEN
        verify(urlRepository, times(1)).delete(mockUrl);

        verify(redisTemplate, times(1)).delete(RedisKeyConstants.URL_PREFIX + "myCustomAlias");
    }
}
