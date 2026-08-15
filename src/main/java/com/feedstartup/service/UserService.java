package com.feedstartup.service;

import com.feedstartup.dto.LoginDto;
import com.feedstartup.dto.UserRegistrationDto;
import com.feedstartup.model.User;

public interface UserService {
    User registerNewUser(UserRegistrationDto registrationDto);
    User loginUser(LoginDto loginDto);
}
