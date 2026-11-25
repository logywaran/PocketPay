package com.logy.pocketpay.user.mapper;

import com.logy.pocketpay.user.dto.RegisterUserRequest;
import com.logy.pocketpay.user.dto.UserResponse;
import com.logy.pocketpay.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Locale;

@Mapper(
        componentModel = "spring",
        imports = Locale.class
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fullName", expression = "java(request.fullName().trim())")
    @Mapping(target = "email", expression = "java(request.email().trim().toLowerCase(Locale.ROOT))")
    @Mapping(target = "phoneNumber", expression = "java(request.phoneNumber().trim())")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterUserRequest request);

    UserResponse toResponse(User user);
}
