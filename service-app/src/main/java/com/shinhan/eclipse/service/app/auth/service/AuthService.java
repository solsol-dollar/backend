package com.shinhan.eclipse.service.app.auth.service;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.auth.TokenIssuer;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.user.User;
import com.shinhan.eclipse.service.app.auth.dto.SimpleLoginRequest;
import com.shinhan.eclipse.service.app.auth.dto.SimpleLoginResponse;
import com.shinhan.eclipse.service.app.auth.internal.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public SimpleLoginResponse login(SimpleLoginRequest request) {
        User matched = userRepository.findAll().stream()
                .filter(u -> passwordEncoder.matches(request.simplePassword(), u.getSimplePassword()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하는 사용자가 없습니다."));

        String token = tokenIssuer.issue(
                new AuthUser(matched.getId(), matched.getName(), "USER"));

        return new SimpleLoginResponse(token, "Bearer", tokenIssuer.getExpirationMs() / 1000);
    }
}