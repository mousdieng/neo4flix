import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  role: string;
  dateOfBirth?: string;
  profilePictureUrl?: string;
  bio?: string;
  enabled?: boolean;
  emailVerified?: boolean;
  twoFactorEnabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
  lastLoginAt?: string;
  // Statistics (included in UserResponse from backend)
  totalRatings?: number;
  watchlistSize?: number;
  friendCount?: number;
  averageRating?: number;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  profilePictureUrl?: string;
  bio?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.users}`;

  constructor(private http: HttpClient) {}

  getUserProfile(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${userId}`);
  }

  getCurrentUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/me`);
  }

  updateProfile(updates: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/me`, updates);
  }

  changePassword(request: ChangePasswordRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/me/change-password`, request, { responseType: 'text' });
  }

  getUserWithStatistics(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${userId}/stats`);
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }

  uploadProfilePicture(file: File): Observable<{success: string; message: string; profilePictureUrl: string}> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{success: string; message: string; profilePictureUrl: string}>(`${this.apiUrl}/me/upload-avatar`, formData);
  }

  searchUsers(query: string): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.apiUrl}/search?q=${encodeURIComponent(query)}`);
  }
}
