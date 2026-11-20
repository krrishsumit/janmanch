package com.janmanch.auth;

import com.janmanch.auth.AuthDtos.AuthResponse;
import com.janmanch.auth.AuthDtos.LoginRequest;
import com.janmanch.auth.AuthDtos.SignupRequest;
import com.janmanch.channel.Channel;
import com.janmanch.channel.ChannelRepository;
import com.janmanch.common.ApiException;
import com.janmanch.user.User;
import com.janmanch.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, ChannelRepository channelRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Creates the user and their channel together, then logs them in. */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        Channel channel = new Channel();
        channel.setUser(user);
        channel.setName(user.getName());
        channelRepository.save(channel);

        return new AuthResponse(jwtService.createToken(user.getId()), user.getId(), user.getName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same error for unknown email and wrong password, so emails cannot be probed.
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new AuthResponse(jwtService.createToken(user.getId()), user.getId(), user.getName());
    }
}
