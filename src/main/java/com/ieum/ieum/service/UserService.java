package com.ieum.ieum.service;

import com.ieum.ieum.domain.User;
import com.ieum.ieum.dto.UserResponse;
import com.ieum.ieum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}