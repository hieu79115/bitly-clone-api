package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.CreateUrlRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateUrlRequestDto;
import com.thinh.shortener.domain.dto.response.UrlResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UrlService {
    UrlResponseDto createShortUrl(CreateUrlRequestDto request, String email);

    String getOriginalUrl(String shortCode);

    Page<UrlResponseDto> getUserUrls(String email, Long tagId, Pageable pageable);

    UrlResponseDto updateUrl(Long id, UpdateUrlRequestDto request, String email);

    void deleteUrl(Long id, String email);
}
