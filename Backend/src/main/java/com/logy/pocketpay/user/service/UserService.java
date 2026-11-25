package com.logy.pocketpay.user.service;

import com.logy.pocketpay.user.exception.EmailAlreadyRegisteredException;
import com.logy.pocketpay.user.exception.PhoneNumberAlreadyRegisteredException;
import com.logy.pocketpay.user.mapper.UserMapper;
import com.logy.pocketpay.user.dto.RegisterUserRequest;
import com.logy.pocketpay.user.dto.UserResponse;
import com.logy.pocketpay.user.entity.User;
import com.logy.pocketpay.user.entity.UserRole;
import com.logy.pocketpay.user.entity.UserStatus;
import com.logy.pocketpay.user.repository.UserRepository;
import com.logy.pocketpay.wallet.entity.Wallet;
import com.logy.pocketpay.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerUser(RegisterUserRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phoneNumber = request.phoneNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(
                    "An account with this email address already exists."
            );
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyRegisteredException(
                    "An account with this phone number already exists."
            );
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        walletRepository.save(wallet);

        return userMapper.toResponse(savedUser);
    }
}
