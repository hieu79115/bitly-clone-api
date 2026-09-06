package com.thinh.shortener.domain.mapper;

import com.thinh.shortener.domain.dto.response.UrlResponseDto;
import com.thinh.shortener.domain.entity.Url;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TagMapper.class}, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UrlMapper {

    @Mapping(target = "shortUrl", expression = "java(domain + url.getShortCode())")
    UrlResponseDto toDto(Url url, String domain);
}
