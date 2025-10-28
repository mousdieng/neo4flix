# Friend System & Recommendation Sharing Feature

## Overview

This document describes the friend management and movie recommendation sharing feature implemented for Neo4flix. Users can now add friends and share movie recommendations directly within the application.

## Features Implemented

### 1. Friend Management System

Users can:
- ✅ Add other users as friends
- ✅ Remove friends
- ✅ View their friends list
- ✅ View another user's friends list
- ✅ Check friendship status with another user

### 2. Share Recommendations with Friends

Users can:
- ✅ Share a movie recommendation with one or multiple friends
- ✅ Add an optional message when sharing
- ✅ View recommendations shared with them by friends
- ✅ View recommendations they've shared with others
- ✅ Mark shared recommendations as viewed
- ✅ See count of unviewed shared recommendations

## Backend API

### User Service Endpoints

#### 1. Get My Friends
```http
GET /api/v1/users/me/friends
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "user123",
    "username": "john_doe",
    "firstName": "John",
    "lastName": "Doe",
    "profilePictureUrl": "https://...",
    "friendCount": 15
  }
]
```

#### 2. Get User's Friends
```http
GET /api/v1/users/{userId}/friends
Authorization: Bearer <token>
```

#### 3. Add Friend
```http
POST /api/v1/users/me/friends/{friendId}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "message": "Friend added successfully"
}
```

#### 4. Remove Friend
```http
DELETE /api/v1/users/me/friends/{friendId}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "message": "Friend removed successfully"
}
```

#### 5. Check Friendship Status
```http
GET /api/v1/users/me/friends/{userId}/status
Authorization: Bearer <token>
```

**Response:**
```json
{
  "areFriends": true
}
```

### Recommendation Service Endpoints

#### 1. Share Recommendation with Friends
```http
POST /api/v1/recommendations/share
Authorization: Bearer <token>
Content-Type: application/json

{
  "movieId": "movie123",
  "friendIds": ["friend1", "friend2", "friend3"],
  "message": "You should watch this movie, it's amazing!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Recommendation shared successfully",
  "sharedCount": 3
}
```

#### 2. Get Recommendations Shared With Me
```http
GET /api/v1/recommendations/shared/received
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "share123",
    "fromUserId": "user456",
    "fromUsername": "jane_doe",
    "toUserId": "user789",
    "movie": {
      "id": "movie123",
      "title": "The Shawshank Redemption",
      "releaseYear": 1994,
      "posterUrl": "https://..."
    },
    "message": "You should watch this movie, it's amazing!",
    "sharedAt": "2025-10-20T14:30:00",
    "viewed": false,
    "viewedAt": null
  }
]
```

#### 3. Get Recommendations I Shared
```http
GET /api/v1/recommendations/shared/sent
Authorization: Bearer <token>
```

#### 4. Mark Shared Recommendation as Viewed
```http
POST /api/v1/recommendations/shared/{sharedRecommendationId}/mark-viewed
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true
}
```

#### 5. Get Unviewed Shared Recommendations Count
```http
GET /api/v1/recommendations/shared/unviewed-count
Authorization: Bearer <token>
```

**Response:**
```json
{
  "count": 5
}
```

## Data Model

### User Model
```java
@Node("User")
public class User {
    @Id
    private String id;

    @Relationship(type = "FRIENDS_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<User> friends;

    // ... other fields
}
```

### SharedRecommendation Model
```java
@Node("SharedRecommendation")
public class SharedRecommendation {
    @Id
    private String id;

    @Property("from_user_id")
    private String fromUserId;

    @Property("to_user_id")
    private String toUserId;

    @Property("movie_id")
    private String movieId;

    @Property("message")
    private String message;

    @Property("shared_at")
    private LocalDateTime sharedAt;

    @Property("viewed")
    private boolean viewed;

    @Property("viewed_at")
    private LocalDateTime viewedAt;
}
```

## Neo4j Graph Structure

### Friend Relationship
```cypher
(User)-[:FRIENDS_WITH]->(User)
```

**Properties:**
- Bidirectional relationship (when User A adds User B as a friend, both get the relationship)

### Shared Recommendations
```cypher
(SharedRecommendation {
  from_user_id: "user123",
  to_user_id: "user456",
  movie_id: "movie789",
  message: "Check this out!",
  shared_at: datetime(),
  viewed: false
})
```

## Key Cypher Queries

### Find Shared Recommendations for a User
```cypher
MATCH (fromUser:User)-[:FRIENDS_WITH]->(toUser:User {id: $userId})
MATCH (sr:SharedRecommendation {to_user_id: $userId, from_user_id: fromUser.id})
MATCH (m:Movie {id: sr.movie_id})
RETURN sr.id AS id,
       sr.from_user_id AS fromUserId,
       fromUser.username AS fromUsername,
       sr.to_user_id AS toUserId,
       m AS movie,
       sr.message AS message,
       sr.shared_at AS sharedAt,
       sr.viewed AS viewed,
       sr.viewed_at AS viewedAt
ORDER BY sr.shared_at DESC
```

### Count Unviewed Shared Recommendations
```cypher
MATCH (sr:SharedRecommendation {to_user_id: $userId, viewed: false})
RETURN count(sr) AS count
```

## Frontend Integration Guide

### 1. Friend Management UI

**Add to User Profile Page:**
```typescript
// friend.service.ts
@Injectable()
export class FriendService {
  constructor(private http: HttpClient) {}

  getFriends(): Observable<User[]> {
    return this.http.get<User[]>('/api/v1/users/me/friends');
  }

  addFriend(friendId: string): Observable<any> {
    return this.http.post(`/api/v1/users/me/friends/${friendId}`, {});
  }

  removeFriend(friendId: string): Observable<any> {
    return this.http.delete(`/api/v1/users/me/friends/${friendId}`);
  }

  checkFriendship(userId: string): Observable<{areFriends: boolean}> {
    return this.http.get<{areFriends: boolean}>(`/api/v1/users/me/friends/${userId}/status`);
  }
}
```

### 2. Share Movie Recommendation

**Add to Movie Detail Page:**
```typescript
// movie-detail.component.ts
shareWithFriends(movieId: string) {
  // Show dialog to select friends
  const dialogRef = this.dialog.open(ShareMovieDialogComponent, {
    data: { movieId }
  });

  dialogRef.afterClosed().subscribe(result => {
    if (result) {
      this.recommendationService.shareRecommendation({
        movieId: movieId,
        friendIds: result.selectedFriends,
        message: result.message
      }).subscribe(() => {
        this.snackBar.open('Recommendation shared!', 'OK', { duration: 3000 });
      });
    }
  });
}
```

### 3. Display Shared Recommendations

**Add to Recommendations Page:**
```typescript
// recommendations.component.ts
export class RecommendationsComponent implements OnInit {
  sharedRecommendations: SharedRecommendation[] = [];
  unviewedCount = 0;

  ngOnInit() {
    this.loadSharedRecommendations();
    this.loadUnviewedCount();
  }

  loadSharedRecommendations() {
    this.recommendationService.getSharedRecommendations().subscribe(
      shared => this.sharedRecommendations = shared
    );
  }

  loadUnviewedCount() {
    this.recommendationService.getUnviewedCount().subscribe(
      data => this.unviewedCount = data.count
    );
  }

  markAsViewed(sharedRecId: string) {
    this.recommendationService.markAsViewed(sharedRecId).subscribe(() => {
      this.loadSharedRecommendations();
      this.loadUnviewedCount();
    });
  }
}
```

**Template Example:**
```html
<!-- shared-recommendations.component.html -->
<div class="shared-section">
  <h2>Movies Shared By Friends
    <span class="badge" *ngIf="unviewedCount > 0">{{unviewedCount}} new</span>
  </h2>

  <div class="shared-list">
    <div *ngFor="let shared of sharedRecommendations" class="shared-item"
         [class.unviewed]="!shared.viewed">
      <div class="movie-poster">
        <img [src]="shared.movie.posterUrl" [alt]="shared.movie.title">
      </div>
      <div class="shared-info">
        <h3>{{shared.movie.title}} ({{shared.movie.releaseYear}})</h3>
        <p class="shared-by">Shared by <strong>{{shared.fromUsername}}</strong></p>
        <p class="message" *ngIf="shared.message">{{shared.message}}</p>
        <p class="shared-at">{{shared.sharedAt | date:'short'}}</p>
        <button (click)="markAsViewed(shared.id)" *ngIf="!shared.viewed">
          Mark as viewed
        </button>
      </div>
    </div>
  </div>
</div>
```

## Usage Flow

### Scenario 1: Adding a Friend

1. User searches for another user
2. Clicks "Add Friend" button
3. Frontend calls `POST /api/v1/users/me/friends/{friendId}`
4. Backend creates bidirectional `FRIENDS_WITH` relationship
5. Both users can now see each other in friends list

### Scenario 2: Sharing a Movie Recommendation

1. User views a movie detail page
2. Clicks "Share with Friends" button
3. Dialog shows list of friends (checkbox selection)
4. User selects friends and optionally adds a message
5. Frontend calls `POST /api/v1/recommendations/share` with:
   - movieId
   - friendIds (array)
   - message (optional)
6. Backend creates `SharedRecommendation` nodes for each friend
7. Friends see the shared recommendation in their "Shared by Friends" section

### Scenario 3: Viewing Shared Recommendations

1. User opens recommendations page
2. Frontend calls `GET /api/v1/recommendations/shared/received`
3. Shows list of movies shared by friends
4. Highlights unviewed recommendations
5. User clicks on a shared movie
6. Frontend calls `POST /api/v1/recommendations/shared/{id}/mark-viewed`
7. Recommendation marked as viewed with timestamp

## Features

### Duplicate Prevention
- Backend checks if a movie has already been shared with a specific friend
- Prevents duplicate shares of the same movie to the same friend

### Friendship Validation
- Shared recommendations queries verify `FRIENDS_WITH` relationship exists
- Only friends can see each other's shared recommendations

### Viewing Tracking
- `viewed` boolean flag tracks if user has seen the shared recommendation
- `viewedAt` timestamp records when it was viewed
- Unviewed count badge shows in UI

## Files Modified/Created

### Backend Files Created:
1. `SharedRecommendation.java` - Model for shared recommendations
2. `ShareRecommendationRequest.java` - DTO for sharing request
3. `SharedRecommendationResponse.java` - DTO for sharing response
4. `SharedRecommendationRepository.java` - Neo4j repository with queries

### Backend Files Modified:
1. `UserController.java` - Added friend management endpoints
2. `UserService.java` - Added friend management methods
3. `RecommendationController.java` - Added sharing endpoints
4. `RecommendationService.java` - Added sharing method signatures
5. `RecommendationServiceImpl.java` - Implemented sharing methods

### Frontend Implementation Needed:
1. `friend.service.ts` - Service for friend API calls
2. `share-movie-dialog.component.ts` - Dialog for selecting friends
3. `shared-recommendations.component.ts` - Display shared movies
4. Update `movie-detail.component.ts` - Add share button
5. Update `recommendations.component.ts` - Add shared section

## Testing

### Manual Testing Steps

#### 1. Test Friend Management
```bash
# Login as user1
POST /api/v1/auth/login
{
  "usernameOrEmail": "user1",
  "password": "password"
}

# Get friends list (should be empty)
GET /api/v1/users/me/friends

# Add user2 as friend
POST /api/v1/users/me/friends/user2-id

# Verify friendship
GET /api/v1/users/me/friends
# Should return user2

# Check friendship status
GET /api/v1/users/me/friends/user2-id/status
# Returns: {"areFriends": true}
```

#### 2. Test Movie Sharing
```bash
# Share movie with friends
POST /api/v1/recommendations/share
{
  "movieId": "movie123",
  "friendIds": ["user2-id", "user3-id"],
  "message": "Great movie, you'll love it!"
}

# Response: {"success": true, "sharedCount": 2}

# Login as user2
POST /api/v1/auth/login

# Get shared recommendations
GET /api/v1/recommendations/shared/received
# Shows movie123 shared by user1

# Get unviewed count
GET /api/v1/recommendations/shared/unviewed-count
# Returns: {"count": 1}

# Mark as viewed
POST /api/v1/recommendations/shared/{sharedRecId}/mark-viewed
```

## Security Considerations

1. **Authentication Required**: All endpoints require valid JWT token
2. **Authorization**: Users can only:
   - Add/remove their own friends
   - Share with users who are their friends
   - View recommendations shared by their friends only
3. **Input Validation**:
   - Friend IDs validated before adding
   - Movie IDs validated before sharing
   - Cannot add yourself as a friend
4. **Duplicate Prevention**: Backend prevents duplicate shares

## Performance Considerations

1. **Indexing**: Create indexes on:
   ```cypher
   CREATE INDEX ON :SharedRecommendation(to_user_id);
   CREATE INDEX ON :SharedRecommendation(from_user_id);
   CREATE INDEX ON :SharedRecommendation(viewed);
   ```

2. **Query Optimization**:
   - Shared recommendations query uses friendship relationship for filtering
   - Limits results to avoid large payloads
   - Sorted by `sharedAt` DESC for recent-first display

3. **Caching**: Consider caching:
   - Friends list (invalidate on add/remove)
   - Unviewed count (invalidate on mark viewed)

## Future Enhancements

1. **Notifications**:
   - Real-time notifications when friend shares a movie
   - Email notifications for shared recommendations
   - WebSocket push for instant updates

2. **Privacy Settings**:
   - Allow users to set privacy level for friends list
   - Option to disable sharing feature
   - Block specific users

3. **Advanced Features**:
   - Friend groups (Family, Colleagues, etc.)
   - Share with multiple groups at once
   - Trending among friends feature
   - Most shared movies among friends

4. **Social Features**:
   - Comments on shared recommendations
   - Like/react to shared recommendations
   - Share discussions/threads

## Conclusion

The friend system and recommendation sharing feature enables social interaction within Neo4flix, allowing users to:
- Build a network of friends with similar movie tastes
- Easily share great movie discoveries
- Get personalized recommendations from trusted friends
- Track which recommendations they've viewed

This feature enhances user engagement and creates a more social movie discovery experience.
