package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.request.CreateUrlRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateUrlRequestDto;
import com.thinh.shortener.domain.dto.response.UrlResponseDto;
import com.thinh.shortener.domain.entity.Tag;
import com.thinh.shortener.domain.entity.Url;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.mapper.UrlMapper;
import com.thinh.shortener.exception.AliasAlreadyExistsException;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.exception.UrlExpiredException;
import com.thinh.shortener.repository.TagRepository;
import com.thinh.shortener.repository.UrlRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.UrlService;
import com.thinh.shortener.util.Base62Encoder;
import com.thinh.shortener.util.RedisKeyConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final Base62Encoder base62Encoder;
    private final UrlMapper urlMapper;
    private final TagRepository tagRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Get the domain from the configuration file; defaults to localhost:8080/
    @Value("${app.domain:http://localhost:8080/}")
    private String domain;

    @Override
    @Transactional
    public UrlResponseDto createShortUrl(CreateUrlRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findByIdInAndUserId(request.getTagIds(), user.getId());
        }

        String shortCode = request.getCustomAlias();

        // CASE 1: User assigns a custom name (customAlias)
        if (StringUtils.hasText(shortCode)) {
            if (urlRepository.findByShortCode(shortCode).isPresent()) {
                throw new AliasAlreadyExistsException("This alias is already in use!");
            }

            Url url = Url.builder()
                    .originalUrl(request.getOriginalUrl())
                    .shortCode(shortCode)
                    .expiresAt(request.getExpiresAt())
                    .user(user)
                    .tags(tags)
                    .build();

            url = urlRepository.save(url);
            return urlMapper.toDto(url, domain);
        }

        // CASE 2: Auto-generated using the Base62 algorithm
        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode("tmp_" + UUID.randomUUID().toString().substring(0, 8))
                .expiresAt(request.getExpiresAt())
                .user(user)
                .tags(tags)
                .build();
        url = urlRepository.save(url);

        shortCode = base62Encoder.encode(url.getId());

        url.setShortCode(shortCode);
        urlRepository.save(url);

        return urlMapper.toDto(url, domain);
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        String cacheKey = RedisKeyConstants.URL_PREFIX + shortCode;

        String cachedOriginalUrl = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedOriginalUrl != null) {
            return cachedOriginalUrl;
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found or has been deleted"));

        if (url.getExpiresAt() != null) {
            long remainingSeconds = Duration.between(LocalDateTime.now(), url.getExpiresAt()).getSeconds();
            if (remainingSeconds <= 0) {
                throw new UrlExpiredException("This link has expired!");
            }
            redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), remainingSeconds, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), 24, TimeUnit.HOURS);
        }

        return url.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UrlResponseDto> getUserUrls(String email, Long tagId, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Url> urlPage;
        if (tagId != null) {
            urlPage = urlRepository.findByUserIdAndTagId(user.getId(), tagId, pageable);
        } else {
            urlPage = urlRepository.findByUserId(user.getId(), pageable);
        }

        return urlPage.map(url -> urlMapper.toDto(url, domain));
    }

    @Override
    @Transactional
    public UrlResponseDto updateUrl(Long id, UpdateUrlRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Url url = urlRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("URL not found or you do not have permission to update it"));

        if (request.getExpiresAt() != null) {
            url.setExpiresAt(request.getExpiresAt());
        }

        if (request.getTagIds() != null) {
            url.getTags().clear();
            if (!request.getTagIds().isEmpty()) {
                Set<Tag> tags = tagRepository.findByIdInAndUserId(request.getTagIds(), user.getId());
                url.getTags().addAll(tags);
            }
        }

        url = urlRepository.save(url);

        redisTemplate.delete(RedisKeyConstants.URL_PREFIX + url.getShortCode());

        return urlMapper.toDto(url, domain);
    }

    @Override
    @Transactional
    public void deleteUrl(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Url url = urlRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AccessDeniedException("You do not have permission to delete this URL"));

        urlRepository.delete(url);

        redisTemplate.delete(RedisKeyConstants.URL_PREFIX + url.getShortCode());
    }
}
