package com.interviewpilot.user.service.impl;

import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.user.dto.ProfileUpdateRequestDto;
import com.interviewpilot.user.dto.UserResponseDto;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import com.interviewpilot.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDto findById(Long userId) {
        return toDto(findUser(userId));
    }

    @Override
    public UserResponseDto updateProfile(Long userId, ProfileUpdateRequestDto request) {
        User user = findUser(userId);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        return toDto(userRepository.save(user));
    }

    @Override
    public List<UserResponseDto> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(this::toDto).toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
