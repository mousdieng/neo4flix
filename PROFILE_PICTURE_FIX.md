# Profile Picture URL Fix

## Problem

The application was storing **full MinIO presigned URLs** in the database instead of object keys:

### ❌ Before (WRONG):
```
Database: "http://localhost:9000/neo4flix-users/avatars/abc123.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20251030%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20251030T215459Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=fefd31bf985805bd48b419fa4afa4f5e7e94e9b4f5091ce67e42069902d988f2"
```

### Issues:
1. **URLs expire in 7 days** - Profile pictures break after 7 days
2. **Database bloat** - Each URL is 300+ characters
3. **Wrong architecture** - Presigned URLs should be temporary, not permanent
4. **Security concern** - Exposing signature parameters unnecessarily

## Solution

Store **only the object key** in the database and generate presigned URLs on-demand:

### ✅ After (CORRECT):
```
Database: "avatars/abc123.png"
          ↓
    User requests profile
          ↓
Generate fresh presigned URL (15 min expiry)
          ↓
Return: "http://localhost:9000/neo4flix-users/avatars/abc123.png?X-Amz-Algorithm=..."
```

## Changes Made

### 1. FileStorageService.java

**Changed `uploadFile()` to return object key instead of presigned URL:**
```java
// BEFORE:
public String uploadFile(MultipartFile file, String folder) {
    // ... upload logic ...
    return getFileUrl(filename);  // ❌ Returns presigned URL
}

// AFTER:
public String uploadFile(MultipartFile file, String folder) {
    // ... upload logic ...
    return filename;  // ✅ Returns object key
}
```

**Updated `getFileUrl()` to generate SHORT-LIVED presigned URLs:**
```java
// BEFORE: 7 days expiry
.expiry(7, TimeUnit.DAYS)

// AFTER: 15 minutes expiry
.expiry(15, TimeUnit.MINUTES)
```

**Added support for legacy URLs:**
```java
// If it's already a full URL (legacy data), extract the object key first
if (objectKey.startsWith("http")) {
    objectKey = extractFilenameFromUrl(objectKey);
}
```

### 2. UserService.java

**Added FileStorageService injection:**
```java
private final FileStorageService fileStorageService;

@Autowired
public UserService(..., FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
}
```

**Updated `convertToUserResponse()` to generate presigned URLs on-the-fly:**
```java
// Generate fresh presigned URL from object key stored in database
String objectKey = user.getProfilePictureUrl();
if (objectKey != null && !objectKey.isEmpty()) {
    String presignedUrl = fileStorageService.getFileUrl(objectKey);
    response.setProfilePictureUrl(presignedUrl);
}
```

**Updated `getFriends()` to generate presigned URLs for friends:**
```java
public List<FriendResponse> getFriends(String userId) {
    List<FriendResponse> friends = friendRequestRepository.findAllFriends(userId);

    // Generate fresh presigned URLs for each friend's profile picture
    friends.forEach(friend -> {
        String objectKey = friend.getProfilePictureUrl();
        if (objectKey != null && !objectKey.isEmpty()) {
            String presignedUrl = fileStorageService.getFileUrl(objectKey);
            friend.setProfilePictureUrl(presignedUrl);
        }
    });

    return friends;
}
```

### 3. UserController.java

**Updated upload endpoint to save object key but return presigned URL:**
```java
// Upload file - returns object key (e.g., "avatars/abc123.png")
String objectKey = fileStorageService.uploadFile(file, "avatars");

// Update user with object key (NOT presigned URL)
userService.updateUserProfilePictureUrl(currentUser.getId(), objectKey);

// Generate a fresh presigned URL to return to the client
String presignedUrl = fileStorageService.getFileUrl(objectKey);

return ResponseEntity.ok(Map.of(
    "profilePictureUrl", presignedUrl  // Client gets presigned URL
));
```

## Benefits

### 1. **No More Broken Images**
- URLs are always fresh (15 min expiry)
- Generated on-demand when user requests profile

### 2. **Smaller Database**
```
Before: 300+ characters per URL
After:  30 characters per object key
= 90% reduction in storage
```

### 3. **Better Security**
- Presigned URLs expire quickly (15 min)
- Signature parameters not stored permanently
- Each request gets new signature

### 4. **Backward Compatible**
- Handles legacy URLs gracefully
- `getFileUrl()` extracts object key from old URLs
- No data loss during migration

## Migration

### For Existing Data:

Run the cleanup script to convert existing presigned URLs to object keys:

```bash
# Interactive mode (with confirmation)
./scripts/cleanup_profile_urls.sh

# Or run directly via Cypher
docker exec neo4flix-neo4j cypher-shell -u neo4j -p password "
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
WITH u, split(split(u.profilePictureUrl, '?')[0], '/')[4..] AS pathParts
WITH u, reduce(s = '', part IN pathParts |
    s + CASE WHEN s = '' THEN part ELSE '/' + part END) AS objectKey
SET u.profilePictureUrl = objectKey
RETURN count(u) AS usersUpdated
"
```

### Verify:
```bash
# Should return 0 users with http URLs
docker exec neo4flix-neo4j cypher-shell -u neo4j -p password "
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
RETURN count(u) AS remainingPresignedUrls
"
```

## Testing

### 1. Upload a new profile picture:
```bash
curl -X POST http://localhost:9084/api/v1/users/me/upload-avatar \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@profile.jpg"
```

**Expected response:**
```json
{
  "success": "true",
  "message": "Profile picture uploaded successfully",
  "profilePictureUrl": "http://localhost:9000/neo4flix-users/avatars/abc123.png?X-Amz-Expires=900..."
}
```

**In database:**
```
User.profilePictureUrl = "avatars/abc123.png"  ✅
```

### 2. Get user profile:
```bash
curl http://localhost:9084/api/v1/users/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:**
- Response contains fresh presigned URL (valid for 15 min)
- URL different on each request (new signature)

### 3. Get friends list:
```bash
curl http://localhost:9084/api/v1/users/me/friends \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:**
- Each friend has fresh presigned URL for their profile picture

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Storage** | Full presigned URL (300+ chars) | Object key (30 chars) |
| **Expiry** | 7 days | 15 minutes |
| **Database** | Bloated with signatures | Clean object keys |
| **Generation** | Once (on upload) | On-demand (every request) |
| **Security** | Signatures stored permanently | Signatures generated fresh |

## Important Notes

1. **NEVER** store presigned URLs in the database
2. Object keys are permanent, presigned URLs are temporary
3. Generate presigned URLs only when needed (API responses)
4. Short expiry times (15 min) force fresh URL generation
5. Backward compatible with legacy data via `extractFilenameFromUrl()`

## Files Modified

- `FileStorageService.java` - Upload logic and URL generation
- `UserService.java` - Response mapping with presigned URLs
- `UserController.java` - Upload endpoint
- `cleanup_profile_urls.sh` - Migration script
- `cleanup_profile_picture_urls.cypher` - Cypher migration queries

## Monitoring

Check for any remaining presigned URLs in database:
```bash
docker exec neo4flix-neo4j cypher-shell -u neo4j -p password "
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
RETURN u.username, u.profilePictureUrl
"
```

Should return **0 results** ✅
