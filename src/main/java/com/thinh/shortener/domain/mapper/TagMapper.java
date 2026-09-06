package com.thinh.shortener.domain.mapper;

import com.thinh.shortener.domain.dto.response.TagResponseDto;
import com.thinh.shortener.domain.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponseDto toDto(Tag tag);
}
