package com.neo4flix.userservice.controller;

import com.neo4flix.userservice.dto.ChangePasswordRequest;
import com.neo4flix.userservice.dto.FriendRequestResponse;
import com.neo4flix.userservice.dto.UpdateUserRequest;
import com.neo4flix.userservice.dto.UserResponse;
import com.neo4flix.userservice.model.User;
import com.neo4flix.userservice.service.FileStorageService;
import com.neo4flix.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for user management operations
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User profile and account management operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Autowired
    public UserController(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Get current user profile", description = "Retrieves the current authenticated user's profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        UserResponse userWithStats = userService.getUserWithStatistics(currentUser.getId());
        return ResponseEntity.ok(userWithStats);
    }

    @Operation(summary = "Update current user profile", description = "Updates the current authenticated user's profile information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        UserResponse updatedUser = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Change current user password", description = "Changes the current authenticated user's password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid password data"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PostMapping("/me/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @Operation(summary = "Enable 2FA", description = "Enables two-factor authentication for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "2FA enabled successfully, QR code returned"),
        @ApiResponse(responseCode = "400", description = "2FA already enabled"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PostMapping("/me/enable-2fa")
    public ResponseEntity<String> enableTwoFactor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        String qrCodeUri = userService.enableTwoFactor(currentUser.getId());
        return ResponseEntity.ok(qrCodeUri);
    }

    @Operation(summary = "Disable 2FA", description = "Disables two-factor authentication for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "2FA disabled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid 2FA code or 2FA not enabled"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PostMapping("/me/disable-2fa")
    public ResponseEntity<String> disableTwoFactor(@RequestParam String twoFactorCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.disableTwoFactor(currentUser.getId(), twoFactorCode);
        return ResponseEntity.ok("2FA disabled successfully");
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a user's public profile by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@Parameter(description = "User ID") @PathVariable String userId) {
        return userService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user by username", description = "Retrieves a user's public profile by username")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@Parameter(description = "Username") @PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search users", description = "Search users by username or name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@Parameter(description = "Search term") @RequestParam String q) {
        List<UserResponse> users = userService.searchUsers(q);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get all users (Admin only)", description = "Retrieves all users with pagination - Admin access required")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserResponse> users = userService.getAllUsers(search, pageable);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Update user (Admin only)", description = "Updates any user's information - Admin access required")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Toggle user status (Admin only)", description = "Enable or disable a user account - Admin access required")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Enable or disable user") @RequestParam boolean enabled) {
        UserResponse updatedUser = userService.toggleUserStatus(userId, enabled);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Delete user (Admin only)", description = "Deletes a user account - Admin access required")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@Parameter(description = "User ID") @PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if user exists", description = "Checks if a user exists by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User existence checked")
    })
    @GetMapping("/{userId}/exists")
    public ResponseEntity<Boolean> userExists(@Parameter(description = "User ID") @PathVariable String userId) {
        boolean exists = userService.userExists(userId);
        return ResponseEntity.ok(exists);
    }

    @Operation(summary = "Get user with statistics", description = "Retrieves a user's profile with detailed statistics")
    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserResponse> getUserWithStatistics(@Parameter(description = "User ID") @PathVariable String userId) {
        UserResponse userWithStats = userService.getUserWithStatistics(userId);
        return ResponseEntity.ok(userWithStats);
    }

    @Operation(summary = "Upload profile picture", description = "Uploads a profile picture for the current authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PostMapping("/me/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @Parameter(description = "Profile picture file") @RequestParam("file") MultipartFile file) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        // Upload file
        String profilePictureUrl = fileStorageService.uploadFile(file, "avatars");

        // Update user with profile picture URL
        userService.updateUserProfilePictureUrl(currentUser.getId(), profilePictureUrl);

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Profile picture uploaded successfully",
            "profilePictureUrl", profilePictureUrl
        ));
    }

    // ==================== FRIEND MANAGEMENT ENDPOINTS ====================

    @Operation(summary = "Get current user's friends", description = "Retrieves the list of friends for the current authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friends list retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/me/friends")
    public ResponseEntity<List<UserResponse>> getMyFriends() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        List<UserResponse> friends = userService.getFriends(currentUser.getId());
        return ResponseEntity.ok(friends);
    }

    @Operation(summary = "Get user's friends", description = "Retrieves the list of friends for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friends list retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<UserResponse>> getUserFriends(@Parameter(description = "User ID") @PathVariable String userId) {
        List<UserResponse> friends = userService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    @Operation(summary = "Send friend request", description = "Sends a friend request to another user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend request sent successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot send request to yourself or already friends"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/me/friends/{friendId}")
    public ResponseEntity<Map<String, String>> addFriend(@Parameter(description = "Friend's user ID") @PathVariable String friendId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getId().equals(friendId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot send friend request to yourself"));
        }

        userService.sendFriendRequest(currentUser.getId(), friendId);
        return ResponseEntity.ok(Map.of("message", "Friend request sent successfully"));
    }

    @Operation(summary = "Remove friend", description = "Removes a user from friends list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend removed successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "Friend not found")
    })
    @DeleteMapping("/me/friends/{friendId}")
    public ResponseEntity<Map<String, String>> removeFriend(@Parameter(description = "Friend's user ID") @PathVariable String friendId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.removeFriend(currentUser.getId(), friendId);
        return ResponseEntity.ok(Map.of("message", "Friend removed successfully"));
    }

    @Operation(summary = "Check friendship status", description = "Checks if two users are friends")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friendship status checked")
    })
    @GetMapping("/me/friends/{userId}/status")
    public ResponseEntity<Map<String, Boolean>> checkFriendshipStatus(@Parameter(description = "User ID to check") @PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        boolean areFriends = userService.areFriends(currentUser.getId(), userId);
        return ResponseEntity.ok(Map.of("areFriends", areFriends));
    }

    // ==================== FRIEND REQUEST ENDPOINTS ====================

    @Operation(summary = "Get pending friend requests", description = "Get all pending friend requests for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend requests retrieved successfully")
    })
    @GetMapping("/me/friend-requests/pending")
    public ResponseEntity<List<FriendRequestResponse>> getPendingFriendRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        List<FriendRequestResponse> requests = userService.getPendingFriendRequests(currentUser.getId());
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Get sent friend requests", description = "Get all friend requests sent by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sent requests retrieved successfully")
    })
    @GetMapping("/me/friend-requests/sent")
    public ResponseEntity<List<FriendRequestResponse>> getSentFriendRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        List<FriendRequestResponse> requests = userService.getSentFriendRequests(currentUser.getId());
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Accept friend request", description = "Accept a pending friend request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend request accepted"),
        @ApiResponse(responseCode = "404", description = "Friend request not found")
    })
    @PostMapping("/me/friend-requests/{requestId}/accept")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(@Parameter(description = "Friend request ID") @PathVariable String requestId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.acceptFriendRequest(requestId, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
    }

    @Operation(summary = "Reject friend request", description = "Reject a pending friend request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend request rejected"),
        @ApiResponse(responseCode = "404", description = "Friend request not found")
    })
    @PostMapping("/me/friend-requests/{requestId}/reject")
    public ResponseEntity<Map<String, String>> rejectFriendRequest(@Parameter(description = "Friend request ID") @PathVariable String requestId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.rejectFriendRequest(requestId, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
    }

    @Operation(summary = "Cancel friend request", description = "Cancel a sent friend request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend request cancelled"),
        @ApiResponse(responseCode = "404", description = "Friend request not found")
    })
    @DeleteMapping("/me/friend-requests/{requestId}")
    public ResponseEntity<Map<String, String>> cancelFriendRequest(@Parameter(description = "Friend request ID") @PathVariable String requestId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.cancelFriendRequest(requestId, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Friend request cancelled"));
    }

    @Operation(summary = "Get pending request count", description = "Get count of pending friend requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    @GetMapping("/me/friend-requests/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingRequestCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        long count = userService.getPendingRequestCount(currentUser.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}