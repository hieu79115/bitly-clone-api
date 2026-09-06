package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.TagRequestDto;
import com.thinh.shortener.domain.dto.response.TagResponseDto;

import java.util.List;

public interface TagService {
    TagResponseDto createTag(TagRequestDto request, String email);

    List<TagResponseDto> getUserTags(String email);

    TagResponseDto updateTag(Long id, TagRequestDto request, String email);

    void deleteTag(Long id, String email);
}
