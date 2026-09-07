package com.thinh.shortener.domain.mapper;

import com.thinh.shortener.domain.dto.response.UserProfileResponseDto;
import com.thinh.shortener.domain.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "totalUrls", ignore = true)
    @Mapping(target = "totalClicks", ignore = true)
    UserProfileResponseDto toDto(UserProfile profile);
}
