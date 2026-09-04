package com.feedstartup.service.impl;

import com.feedstartup.dto.LoginDto;
import com.feedstartup.dto.UserRegistrationDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.StoredFile;
import com.feedstartup.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    // Same defensive approach as EpmGalleryServiceImpl - the stored filename is always a fresh
    // UUID, and the extension comes from the sniffed content type, never the client's filename.
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final UserRepository userRepository;

    @Value("${feedworld.storage.profile-images-dir}")
    private String profileImagesDir;

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

    @Override
    public User updateProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase());
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP or GIF images are allowed");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Path dir = Paths.get(profileImagesDir);
        Path target = dir.resolve(UUID.randomUUID() + extension);

        try {
            Files.createDirectories(dir);
            file.transferTo(target);
        } catch (IOException e) {
            deleteQuietly(target);
            throw new RuntimeException("Failed to store the profile image: " + e.getMessage(), e);
        }

        String previousPath = user.getProfileImagePath();
        user.setProfileImagePath(target.toString());
        User saved = userRepository.save(user);
        if (previousPath != null) {
            deleteQuietly(Paths.get(previousPath));
        }
        return saved;
    }

    @Override
    public void removeProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String previousPath = user.getProfileImagePath();
        if (previousPath == null) {
            return;
        }
        user.setProfileImagePath(null);
        userRepository.save(user);
        deleteQuietly(Paths.get(previousPath));
    }

    @Override
    public StoredFile loadProfileImageFile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String path = user.getProfileImagePath();
        if (path == null) {
            throw new ResourceNotFoundException("No profile image set for user " + userId);
        }
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Profile image file is missing on the server for user " + userId);
        }
        Resource resource = new FileSystemResource(filePath);
        String contentType = contentTypeFor(filePath.toString());
        return new StoredFile(resource, contentType, "profile-" + userId + extensionOf(filePath.toString()));
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot) : "";
    }

    private String contentTypeFor(String path) {
        String ext = extensionOf(path).toLowerCase();
        return switch (ext) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            default -> "image/jpeg";
        };
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for.
        }
    }
}
