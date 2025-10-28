import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Friend {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  profilePictureUrl?: string;
  friendCount: number;
}

export interface FriendRequest {
  id: string;
  sender: Friend;
  receiver: Friend;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
  respondedAt?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class FriendService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/users`;

  /**
   * Get current user's friends list
   */
  getMyFriends(): Observable<Friend[]> {
    return this.http.get<Friend[]>(`${this.apiUrl}/me/friends`);
  }

  /**
   * Get a specific user's friends list
   */
  getUserFriends(userId: string): Observable<Friend[]> {
    return this.http.get<Friend[]>(`${this.apiUrl}/${userId}/friends`);
  }

  /**
   * Add a user as friend
   */
  addFriend(friendId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/me/friends/${friendId}`, {});
  }

  /**
   * Remove a friend
   */
  removeFriend(friendId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/me/friends/${friendId}`);
  }

  /**
   * Check if current user is friends with another user
   */
  checkFriendshipStatus(userId: string): Observable<{ areFriends: boolean }> {
    return this.http.get<{ areFriends: boolean }>(`${this.apiUrl}/me/friends/${userId}/status`);
  }

  /**
   * Get pending friend requests (incoming)
   */
  getPendingRequests(): Observable<FriendRequest[]> {
    return this.http.get<FriendRequest[]>(`${this.apiUrl}/me/friend-requests/pending`);
  }

  /**
   * Get sent friend requests (outgoing)
   */
  getSentRequests(): Observable<FriendRequest[]> {
    return this.http.get<FriendRequest[]>(`${this.apiUrl}/me/friend-requests/sent`);
  }

  /**
   * Accept a friend request
   */
  acceptRequest(requestId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/me/friend-requests/${requestId}/accept`, {});
  }

  /**
   * Reject a friend request
   */
  rejectRequest(requestId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/me/friend-requests/${requestId}/reject`, {});
  }

  /**
   * Cancel a sent friend request
   */
  cancelRequest(requestId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/me/friend-requests/${requestId}`);
  }

  /**
   * Get count of pending requests
   */
  getPendingRequestCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/me/friend-requests/pending/count`);
  }
}
