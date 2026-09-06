package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.request.TagRequestDto;
import com.thinh.shortener.domain.dto.response.TagResponseDto;
import com.thinh.shortener.domain.entity.Tag;
import com.thinh.shortener.domain.entity.Url;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.mapper.TagMapper;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.exception.TagAlreadyExistsException;
import com.thinh.shortener.repository.TagRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public TagResponseDto createTag(TagRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (tagRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new TagAlreadyExistsException("Tag name '" + request.getName() + "' already exists!");
        }

        Tag tag = Tag.builder()
                .name(request.getName())
                .user(user)
                .build();

        tag = tagRepository.save(tag);

        return tagMapper.toDto(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDto> getUserTags(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return tagRepository.findByUserId(user.getId())
                .stream()
                .map(tagMapper::toDto)
                .toList();

    }

    @Override
    @Transactional
    public TagResponseDto updateTag(Long id, TagRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tag tag = tagRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

        if (tagRepository.existsByUserIdAndNameAndIdNot(user.getId(), request.getName(), id)) {
            throw new TagAlreadyExistsException("Tag name '" + request.getName() + "' already exists!");
        }

        tag.setName(request.getName());
        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Tag tag = tagRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

        for (Url url : tag.getUrls()) {
            url.getTags().remove(tag);
        }
        tagRepository.delete(tag);
    }
}
