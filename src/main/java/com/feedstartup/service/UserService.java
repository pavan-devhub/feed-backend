package com.feedstartup.service;

import com.feedstartup.dto.LoginDto;
import com.feedstartup.dto.UserRegistrationDto;
import com.feedstartup.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User registerNewUser(UserRegistrationDto registrationDto);
    User loginUser(LoginDto loginDto);
    User updateProfileImage(Long userId, MultipartFile file);
    void removeProfileImage(Long userId);
    StoredFile loadProfileImageFile(Long userId);
}
