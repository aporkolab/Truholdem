package com.truholdem.mapper;

import com.truholdem.dto.UserProfileDto;
import com.truholdem.dto.UserRegistrationDto;
import com.truholdem.model.Role;
import com.truholdem.model.User;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringList")
    @Mapping(target = "totalGamesPlayed", expression = "java(0)")
    @Mapping(target = "totalWinnings", expression = "java(java.math.BigDecimal.ZERO)")
    UserProfileDto toProfileDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "oauthProvider", ignore = true)
    @Mapping(target = "oauthId", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UserRegistrationDto registrationDto);

    @Named("rolesToStringList")
    default List<String> rolesToStringList(Set<Role> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }
}
