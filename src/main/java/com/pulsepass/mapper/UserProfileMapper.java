package com.pulsepass.mapper;

import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.UserProfileDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfileMapper {

    UserProfileDto toDto(UserProfile profile);
}
