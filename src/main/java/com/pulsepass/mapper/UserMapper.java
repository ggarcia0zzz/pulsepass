package com.pulsepass.mapper;

import com.pulsepass.domain.User;
import com.pulsepass.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = UserProfileMapper.class
)
public interface UserMapper {

    // profile es nullable: MapStruct llama a UserProfileMapper.toDto solo si no es null
    UserDto toDto(User user);
}
