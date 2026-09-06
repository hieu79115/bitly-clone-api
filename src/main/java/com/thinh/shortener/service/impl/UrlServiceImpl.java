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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final Base62Encoder base62Encoder;
    private final UrlMapper urlMapper;
    private final TagRepository tagRepository;

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
        /*
         * Since the short_code column in the database enforces a NOT NULL constraint,
         * we must temporarily store a dummy string.
         * Only after the database assigns an ID do we convert that ID to Base62 and perform an update.
         */
        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode("tmp_" + UUID.randomUUID().toString().substring(0, 8))
                .expiresAt(request.getExpiresAt())
                .user(user)
                .tags(tags)
                .build();
        url = urlRepository.save(url); // 1st attempt: Save to get the ID

        // Get the ID and encode it using Base62
        shortCode = base62Encoder.encode(url.getId());

        url.setShortCode(shortCode);
        urlRepository.save(url); // 2nd attempt: Update the standard code in the database

        return urlMapper.toDto(url, domain);
    }

    @Override
    @Transactional
    public String getOriginalUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found or has been deleted"));


        if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
            throw new UrlExpiredException("This link has expired!");
        }

        url.setClickCount(url.getClickCount() + 1);
        urlRepository.save(url);

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
    }
}
