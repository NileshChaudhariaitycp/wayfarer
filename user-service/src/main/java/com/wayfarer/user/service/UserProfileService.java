package com.wayfarer.user.service;

import com.wayfarer.user.dto.CreateProfileRequest;
import com.wayfarer.user.dto.ProfileResponse;
import com.wayfarer.user.entity.UserProfile;
import com.wayfarer.user.exception.ProfileNotFoundException;
import com.wayfarer.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void createProfile(CreateProfileRequest request) {
        UserProfile profile = new UserProfile();
        profile.setId(request.userId());
        profile.setUsername(request.username());
        profile.setEmail(request.email());
        profile.setFullName(request.fullName());
        profile.setRole(request.role());
        userProfileRepository.save(profile);
        log.info("Created profile for userId={} username={}", request.userId(), request.username());
    }

    public ProfileResponse getById(Long id) {
        return userProfileRepository.findById(id)
                .map(ProfileResponse::from)
                .orElseThrow(() -> new ProfileNotFoundException(id));
    }
}
