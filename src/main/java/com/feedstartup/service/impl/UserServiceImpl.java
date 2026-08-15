package com.feedstartup.service.impl;

import com.feedstartup.dto.LoginDto;
import com.feedstartup.dto.UserRegistrationDto;
import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerNewUser(UserRegistrationDto registrationDto) {
        // Validate password match
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email or phone already exists
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.existsByPhone(registrationDto.getPhone())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        User user = new User();
        user.setFirstName(registrationDto.getFirstName());
        user.setMiddleName(registrationDto.getMiddleName());
        user.setLastName(registrationDto.getLastName());
        user.setGender(registrationDto.getGender());
        user.setPhone(registrationDto.getPhone());
        user.setDob(LocalDate.parse(registrationDto.getDob()));
        user.setEmail(registrationDto.getEmail());
        user.setEducation(registrationDto.getEducation());
        
        // Hash the password
        String hashed = BCrypt.hashpw(registrationDto.getPassword(), BCrypt.gensalt(12));
        user.setPassword(hashed);
        
        user.setState(registrationDto.getState());
        user.setDistrict(registrationDto.getDistrict());
        user.setCity(registrationDto.getCity());
        user.setUserType(registrationDto.getUserType());

        return userRepository.save(user);
    }

    @Override
    public User loginUser(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!BCrypt.checkpw(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return user;
    }
}
