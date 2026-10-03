package com.feedstartup.service;

import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Which account an EPM registration or volunteer sign-up belongs to - stored as its user_id, a
 * foreign key to users. The register / volunteer forms work signed out too, so a sign-up sent
 * without a login still goes to the account with the same email or mobile number, the same way
 * "Status of Activities" has always matched them.
 */
@Component
public class EpmSignUpOwner {

    private final UserRepository userRepository;

    public EpmSignUpOwner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** {@code loggedInUserId} if there is one, else the account with this email, else with this mobile number, else null. */
    public Long resolve(Long loggedInUserId, String email, String mobileNumber) {
        if (loggedInUserId != null) return loggedInUserId;
        if (email != null && !email.isBlank()) {
            Long byEmail = userRepository.findByEmail(email.trim()).map(User::getId).orElse(null);
            if (byEmail != null) return byEmail;
        }
        if (mobileNumber != null && !mobileNumber.isBlank()) {
            return userRepository.findByPhone(mobileNumber.trim()).map(User::getId).orElse(null);
        }
        return null;
    }
}
