package com.neo4flix.userservice.service;

import com.neo4flix.userservice.dto.*;
import com.neo4flix.userservice.exception.UserAlreadyExistsException;
import com.neo4flix.userservice.exception.UserNotFoundException;
import com.neo4flix.userservice.exception.InvalidPasswordException;
import com.neo4flix.userservice.model.FriendRequest;
import com.neo4flix.userservice.model.FriendRequest.FriendRequestStatus;
import com.neo4flix.userservice.model.User;
import com.neo4flix.userservice.model.UserRole;
import com.neo4flix.userservice.repository.FriendRequestRepository;
import com.neo4flix.userservice.repository.UserRepository;
import com.neo4flix.userservice.security.JwtService;
import com.neo4flix.userservice.security.TwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for user operations
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserService(
            UserRepository userRepository,
            FriendRequestRepository friendRequestRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TwoFactorService twoFactorService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.twoFactorService = twoFactorService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user
     */
    public AuthenticationResponse registerUser(UserRegistrationRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(UserRole.USER);
        user.setEnabled(true);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtService.generateTokenWithUserInfo(savedUser, savedUser.getId(), savedUser.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Create response
        AuthenticationResponse response = new AuthenticationResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtService.getExpirationTime());
        response.setUser(convertToUserResponse(savedUser));

        return response;
    }

    /**
     * Authenticate user login
     */
    public AuthenticationResponse authenticateUser(UserLoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            // Check if 2FA is enabled
            if (user.isTwoFactorEnabled()) {
                if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
                    AuthenticationResponse response = new AuthenticationResponse();
                    response.setRequiresTwoFactor(true);
                    response.setUser(convertToUserResponse(user));
                    return response;
                }

                // Verify 2FA code
                if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                    throw new InvalidPasswordException("Invalid 2FA code");
                }
            }

            // Update last login time
            user.updateLastLogin();
            userRepository.save(user);

            // Generate JWT tokens
            String accessToken = jwtService.generateTokenWithUserInfo(user, user.getId(), user.getRole().name());
            String refreshToken = jwtService.generateRefreshToken(user);

            // Create response
            AuthenticationResponse response = new AuthenticationResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setExpiresIn(jwtService.getExpirationTime());
            response.setUser(convertToUserResponse(user));

            return response;

        } catch (AuthenticationException e) {
            throw new InvalidPasswordException("Invalid credentials");
        }
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(String userId) {
        return userRepository.findById(userId)
                .map(this::convertToUserResponse);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToUserResponse);
    }

    /**
     * Update user information
     */
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Check username uniqueness if changed
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Reset email verification
        }

        // Update other fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user.updateTimestamp();
        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    /**
     * Change user password
     */
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Verify password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("New password and confirmation do not match");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.updateTimestamp();
        userRepository.save(user);
    }

    /**
     * Enable 2FA for user
     */
    public String enableTwoFactor(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("2FA is already enabled for this user");
        }

        // Generate secret and QR code
        String secret = twoFactorService.generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        user.updateTimestamp();
        userRepository.save(user);

        return twoFactorService.generateQrCodeImageUri(secret, user.getUsername());
    }

    /**
     * Disable 2FA for user
     */
    public void disableTwoFactor(String userId, String twoFactorCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("2FA is not enabled for this user");
        }

        // Verify 2FA code before disabling
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), twoFactorCode)) {
            throw new InvalidPasswordException("Invalid 2FA code");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.updateTimestamp();
        userRepository.save(user);
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String searchTerm, Pageable pageable) {
        Page<User> users = userRepository.findUsersWithSearch(searchTerm, pageable);
        return users.map(this::convertToUserResponse);
    }

    /**
     * Search users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String searchTerm, String currentUserId) {
        List<User> users = userRepository.searchByUsername(searchTerm);
        users.addAll(userRepository.searchByFullName(searchTerm));
        return users.stream()
                .distinct()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete user
     */
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        userRepository.delete(user);
    }

    /**
     * Enable/disable user account
     */
    public UserResponse toggleUserStatus(String userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.setEnabled(enabled);
        user.updateTimestamp();
        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserResponse getUserWithStatistics(String userId) {
        // Get user
        User user = userRepository.findUserForStatistics(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Convert to response
        UserResponse response = convertToUserResponse(user);

        // Get statistics separately
        Long totalRatings = userRepository.countUserRatings(userId);
        Double averageRating = userRepository.getAverageRating(userId);
        Long watchlistSize = userRepository.countWatchlistItems(userId);
        Long friendCount = userRepository.countFriends(userId);

        // Set statistics
        response.setTotalRatings(totalRatings != null ? totalRatings : 0L);
        response.setWatchlistSize(watchlistSize != null ? watchlistSize : 0L);
        response.setFriendCount(friendCount != null ? friendCount : 0L);
        response.setAverageRating(averageRating != null ? averageRating : 0.0);

        return response;
    }

    /**
     * Check if user exists
     */
    @Transactional(readOnly = true)
    public boolean userExists(String userId) {
        return userRepository.existsById(userId);
    }

    /**
     * Check if username is available
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Check if email is available
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Update user profile picture URL
     */
    public void updateUserProfilePictureUrl(String userId, String profilePictureUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setProfilePictureUrl(profilePictureUrl);
        user.updateTimestamp();
        userRepository.save(user);
    }

    // ==================== FRIEND MANAGEMENT ====================

    /**
     * Get user's friends list
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(String userId) {
        return friendRequestRepository.findAllFriends(userId);
    }


    /**
     * Add a friend (now sends a friend request instead)
     * @deprecated Use sendFriendRequest instead
     */
    @Deprecated
    public void addFriend(String userId, String friendId) {
        // This now just sends a friend request
        sendFriendRequest(userId, friendId);
    }

    /**
     * Remove a friend
     */
    public void removeFriend(String userId, String friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new UserNotFoundException("Friend not found: " + friendId));

        // Remove bidirectional friendship
        user.getFriends().remove(friend);
        friend.getFriends().remove(user);

        userRepository.save(user);
        userRepository.save(friend);
    }

    /**
     * Check if two users are friends
     */
    @Transactional(readOnly = true)
    public boolean areFriends(String userId, String friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return user.getFriends().stream()
                .anyMatch(friend -> friend.getId().equals(friendId));
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setFullName(user.getFullName());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setBio(user.getBio());
        response.setEnabled(user.isEnabled());
        response.setEmailVerified(user.isEmailVerified());
        response.setTwoFactorEnabled(user.isTwoFactorEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setRole(user.getRole());
        return response;
    }

    // ==================== FRIEND REQUEST METHODS ====================

    /**
     * Send a friend request
     */
    public FriendRequestResponse sendFriendRequest(String senderId, String receiverId) {
        // Check if users exist
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender not found: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver not found: " + receiverId));

        // Check if they're already friends
        if (areFriends(senderId, receiverId)) {
            throw new IllegalStateException("Users are already friends");
        }

        Map<String, String> statusMessages = Map.of(
                "PENDING", "A pending friend request already exists between these users",
                "ACCEPTED", "You are already friends with this user",
                "REJECTED", "A friend request was rejected previously between these users"
        );

        for (var entry : statusMessages.entrySet()) {
            String status = entry.getKey();
            String message = entry.getValue();

            if (friendRequestRepository.existsStatusRequestBetweenUsers(senderId, receiverId, status)) {
                throw new IllegalStateException(message);
            }
        }

        // Create and save friend request
        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .build();

        request.onCreate();
        FriendRequest savedRequest = friendRequestRepository.save(request);
        return savedRequest.convertToFriendRequestResponse();
    }

    /**
     * Accept a friend request
     */
    public void acceptFriendRequest(String requestId, String userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Verify the user is the receiver
        if (!request.getReceiver().getId().equals(userId)) {
            throw new IllegalStateException("Only the receiver can accept this request");
        }

        // Verify status is PENDING
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("This request has already been responded to");
        }

        // Update request status
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        // Add bidirectional friendship
        User sender = request.getSender();
        User receiver = request.getReceiver();
        sender.getFriends().add(receiver);
        receiver.getFriends().add(sender);
        userRepository.save(sender);
        userRepository.save(receiver);
    }

    /**
     * Reject a friend request
     */
    public void rejectFriendRequest(String requestId, String userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Verify the user is the receiver
        if (!request.getReceiver().getId().equals(userId)) {
            throw new IllegalStateException("Only the receiver can reject this request");
        }

        // Verify status is PENDING
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("This request has already been responded to");
        }

        // Update request status
        request.setStatus(FriendRequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);
    }

    /**
     * Cancel a sent friend request
     */
    public void cancelFriendRequest(String requestId, String userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Verify the user is the sender
        if (!request.getSender().getId().equals(userId)) {
            throw new IllegalStateException("Only the sender can cancel this request");
        }

        // Verify status is PENDING
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel a request that has been responded to");
        }

        // Delete the request
        friendRequestRepository.delete(request);
    }

    /**
     * Get pending friend requests for a user
     */
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getPendingFriendRequests(String userId) {
        var a = friendRequestRepository.findPendingRequestsDataForUser(userId);
        System.out.println("0000000000000000000000000000000000000000000000000000000000000");
        System.out.println(a);
        System.out.println("0000000000000000000000000000000000000000000000000000000000000");
        return a;
    }

    /**
     * Get friend requests sent by a user
     */
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getSentFriendRequests(String userId) {
        var a = friendRequestRepository.findRequestsDataSentByUser(userId);
        System.out.println("fdfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        System.out.println(a);
        System.out.println("fdfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        return a;
    }

    /**
     * Get count of pending friend requests
     */
    @Transactional(readOnly = true)
    public long getPendingRequestCount(String userId) {
        return friendRequestRepository.countPendingRequestsForUser(userId);
    }
}