// ========================================================================
// Cleanup Script: Convert Presigned URLs to Object Keys
// ========================================================================
// This script fixes profile picture URLs in the database by converting
// full MinIO presigned URLs to just the object keys.
//
// BEFORE: http://localhost:9000/neo4flix-users/avatars/abc123.png?X-Amz-Algorithm=...
// AFTER:  avatars/abc123.png
//
// Run this script using:
// docker exec -it neo4flix-neo4j cypher-shell -u neo4j -p password -f /path/to/this/script.cypher
//
// Or copy and paste into Neo4j Browser
// ========================================================================

// Step 1: Check how many users have presigned URLs
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
RETURN count(u) AS usersWithPresignedUrls;

// Step 2: Preview the transformation (no changes made)
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
WITH u, u.profilePictureUrl AS oldUrl
WITH u, oldUrl,
     // Extract object key from URL
     // URL format: http://host:port/bucket-name/folder/file.png?query
     // Split by '?' to remove query params
     split(split(oldUrl, '?')[0], '/')[4..] AS pathParts
WITH u, oldUrl,
     // Join path parts with '/' to get object key
     reduce(s = '', part IN pathParts | s + CASE WHEN s = '' THEN part ELSE '/' + part END) AS newKey
RETURN u.username, oldUrl, newKey
LIMIT 10;

// Step 3: Perform the actual cleanup
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
WITH u, u.profilePictureUrl AS oldUrl
WITH u, oldUrl,
     // Extract object key from URL by splitting and removing bucket name
     split(split(oldUrl, '?')[0], '/')[4..] AS pathParts
WITH u, oldUrl,
     // Join path parts to get object key (e.g., "avatars/abc123.png")
     reduce(s = '', part IN pathParts | s + CASE WHEN s = '' THEN part ELSE '/' + part END) AS objectKey
SET u.profilePictureUrl = objectKey
RETURN count(u) AS usersUpdated;

// Step 4: Verify cleanup
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
RETURN count(u) AS remainingPresignedUrls;

// Step 5: Show sample of cleaned data
MATCH (u:User)
WHERE u.profilePictureUrl IS NOT NULL
RETURN u.username, u.profilePictureUrl
LIMIT 10;
