import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Movie } from './movie';
import { environment } from '../../environments/environment';

export interface RecommendationRequest {
  userId?: string;
  movieId?: string;
  genres?: string[];
  limit?: number;
  algorithm?: 'collaborative' | 'content_based' | 'hybrid';
}

export interface RecommendationResponse {
  success: boolean;
  message: string;
  data: {
    recommendations: RecommendedMovie[];
    algorithm: string;
    confidence: number;
    totalResults: number;
  };
}

export interface RecommendedMovie extends Movie {
  confidence: number;
  reason: string;
  similarity: number;
}

export interface RecommandationResponse {
  movieId: string;
  score: number;
  algorithm: string;
  reason: string;
  recommendedAt: Date;
  clicked: boolean;
  watched: boolean;
}

export interface UserPreferences {
  favoriteGenres: string[];
  preferredRatingRange: [number, number];
  favoriteActors: string[];
  favoriteDirectors: string[];
  watchHistory: string[]; // movie IDs
}

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.recommendations}`;

  constructor(private http: HttpClient) {}

  // Get personalized recommendations for authenticated user
  getPersonalizedRecommendations(limit: number = 10): Observable<RecommendationResponse> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('algorithm', 'hybrid');

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/personalized`, { params });
  }

  // Get recommendations based on a specific movie (similar movies)
  getSimilarMovies(movieId: string, limit: number = 10): Observable<RecommendationResponse> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('algorithm', 'content_based');

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/similar/${movieId}`, { params });
  }

  // Get recommendations based on genre preferences
  getGenreBasedRecommendations(
    genres: string[],
    limit: number = 10,
    fromYear?: number | null,
    toYear?: number | null
  ): Observable<RecommendationResponse> {
    let params = new HttpParams()
      .set('limit', limit.toString())
      .set('algorithm', 'hybrid');

    genres.forEach(genre => {
      params = params.append('genres', genre);
    });

    // Add year filters if provided
    if (fromYear !== null && fromYear !== undefined) {
      params = params.set('fromYear', fromYear.toString());
    }
    if (toYear !== null && toYear !== undefined) {
      params = params.set('toYear', toYear.toString());
    }

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/by-genre`, { params });
  }

  // Get trending recommendations
  getTrendingRecommendations(limit: number = 10): Observable<RecommendationResponse> {
    const params = new HttpParams()
      .set('limit', limit.toString());

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/trending`, { params });
  }

  // Get recommendations for new users (popular movies)
  getNewUserRecommendations(limit: number = 10): Observable<RecommendationResponse> {
    const params = new HttpParams()
      .set('limit', limit.toString());

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/new-user`, { params });
  }

  // Update user preferences
  updateUserPreferences(preferences: UserPreferences): Observable<{success: boolean; message: string}> {
    return this.http.put<{success: boolean; message: string}>(`${this.apiUrl}/preferences`, preferences);
  }

  // Get user preferences
  getUserPreferences(): Observable<{success: boolean; data: UserPreferences}> {
    return this.http.get<{success: boolean; data: UserPreferences}>(`${this.apiUrl}/preferences`);
  }

  // Track user interaction (for improving recommendations)
  trackUserInteraction(interaction: {
    movieId: string;
    action: 'view' | 'rate' | 'like' | 'share' | 'watchlist_add' | 'watchlist_remove';
    value?: number; // for rating action
    timestamp?: string;
  }): Observable<{success: boolean}> {
    return this.http.post<{success: boolean}>(`${this.apiUrl}/interactions`, interaction);
  }

  // Get recommendations based on user's watch history
  getWatchHistoryRecommendations(limit: number = 10): Observable<RecommendationResponse> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('algorithm', 'collaborative');

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/history-based`, { params });
  }

  // Get recommendations for a specific user (admin/analytics use)
  getUserRecommendations(userId: string, limit: number = 10): Observable<RecommendationResponse> {
    const params = new HttpParams()
      .set('limit', limit.toString());

    return this.http.get<RecommendationResponse>(`${this.apiUrl}/user/${userId}`, { params });
  }

  // Refresh user's recommendation cache
  refreshRecommendations(): Observable<{success: boolean; message: string}> {
    return this.http.post<{success: boolean; message: string}>(`${this.apiUrl}/refresh`, {});
  }

  // Helper method to format recommendation reason
  formatRecommendationReason(reason: string): string {
    const reasonMap: { [key: string]: string } = {
      'similar_genre': 'Because you like similar genres',
      'similar_rating': 'Because you rated similar movies highly',
      'popular_choice': 'Popular among users like you',
      'director_match': 'From a director you enjoy',
      'actor_match': 'Featuring actors you like',
      'trending': 'Currently trending',
      'new_release': 'New release you might enjoy'
    };

    return reasonMap[reason] || reason;
  }

  // Helper method to get confidence color class
  getConfidenceColorClass(confidence: number): string {
    if (confidence >= 0.8) return 'text-green-500';
    if (confidence >= 0.6) return 'text-yellow-500';
    if (confidence >= 0.4) return 'text-orange-500';
    return 'text-red-500';
  }

  // ==================== SHARING WITH FRIENDS ====================

  /**
   * Share a movie recommendation with friends
   */
  shareRecommendation(request: ShareRecommendationRequest): Observable<ShareRecommendationResponse> {
    return this.http.post<ShareRecommendationResponse>(
      `${this.apiUrl}/share`,
      request
    );
  }

  /**
   * Get recommendations shared with me by friends
   */
  getSharedRecommendations(): Observable<SharedRecommendation[]> {
    return this.http.get<SharedRecommendation[]>(`${this.apiUrl}/shared/received`);
  }

  /**
   * Get recommendations I've shared with friends
   */
  getMySharedRecommendations(): Observable<SharedRecommendation[]> {
    return this.http.get<SharedRecommendation[]>(`${this.apiUrl}/shared/sent`);
  }

  /**
   * Mark a shared recommendation as viewed
   */
  markSharedRecommendationAsViewed(sharedRecommendationId: string): Observable<{ success: boolean }> {
    return this.http.post<{ success: boolean }>(
      `${this.apiUrl}/shared/${sharedRecommendationId}/mark-viewed`,
      {}
    );
  }

  /**
   * Get count of unviewed shared recommendations
   */
  getUnviewedSharedCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/shared/unviewed-count`);
  }
}

// ==================== SHARING INTERFACES ====================

export interface ShareRecommendationRequest {
  movieId: string;
  friendIds: string[];
  message?: string;
}

export interface ShareRecommendationResponse {
  success: boolean;
  message: string;
  sharedCount: number;
}

export interface SharedRecommendation {
  id: string;
  fromUserId: string;
  fromUsername: string;
  toUserId: string;
  movie: Movie;
  message?: string;
  sharedAt: string;
  viewed: boolean;
  viewedAt?: string;
}
