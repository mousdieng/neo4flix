import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Rating {
  id: string;
  userId: string;
  username?: string;
  movieId: string;
  movieTitle?: string;
  rating: number; // 0.5-10.0 scale (0.5 increments, matching IMDB scale)
  review?: string;
  ratedAt: string | Date;
  lastModified?: string | Date;
  // Legacy fields for backward compatibility
  createdAt?: string;
  updatedAt?: string;
  user?: {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
  };
}

export interface RatingRequest {
  movieId: string;
  rating: number;
  review?: string;
}

export interface RatingResponse {
  success: boolean;
  message: string;
  data: Rating;
}

export interface RatingsResponse {
  success: boolean;
  message: string;
  data: {
    ratings: Rating[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    averageRating: number;
    userRating?: Rating;
  };
}

export interface RatingStats {
  averageRating: number;
  totalRatings: number;
  ratingDistribution: {
    [key: number]: number; // rating (1-10) -> count
  };
}

@Injectable({
  providedIn: 'root'
})
export class RatingService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.ratings}`;

  constructor(private http: HttpClient) {
    console.log(this.apiUrl)
  }

  // Submit or update user's rating and review
  submitRating(ratingRequest: RatingRequest): Observable<Rating> {
    console.log(this.apiUrl)
    return this.http.post<Rating>(`${this.apiUrl}`, ratingRequest);
  }

  // Update existing rating
  updateRating(ratingId: string, ratingRequest: RatingRequest): Observable<Rating> {
    return this.http.put<Rating>(`${this.apiUrl}/${ratingId}`, ratingRequest);
  }

  // Delete user's rating
  deleteRating(ratingId: string): Observable<{success: boolean; message: string}> {
    return this.http.delete<{success: boolean; message: string}>(`${this.apiUrl}/${ratingId}`);
  }

  // Get all ratings for a movie
  getMovieRatings(movieId: string, page: number = 0, size: number = 10): Observable<RatingsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<RatingsResponse>(`${this.apiUrl}/movie/${movieId}`, { params });
  }

  // Get user's rating for a specific movie
  getUserMovieRating(movieId: string): Observable<Rating> {
    return this.http.get<Rating>(`${this.apiUrl}/movie/${movieId}/user`);
  }

  // Get all ratings by a user
  getUserRatings(userId: string, page: number = 0, size: number = 10): Observable<RatingsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<RatingsResponse>(`${this.apiUrl}/user/${userId}`, { params });
  }

  // Get rating statistics for a movie
  getMovieRatingStats(movieId: string): Observable<{success: boolean; data: RatingStats}> {
    return this.http.get<{success: boolean; data: RatingStats}>(`${this.apiUrl}/movie/${movieId}/stats`);
  }

  // Get recent reviews (with review text)
  getRecentReviews(page: number = 0, size: number = 10): Observable<RatingsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('hasReview', 'true');

    return this.http.get<RatingsResponse>(`${this.apiUrl}/recent`, { params });
  }

  // Get top-rated movies
  getTopRatedMovies(page: number = 0, size: number = 10): Observable<{
    success: boolean;
    data: {
      movies: Array<{
        movieId: string;
        title: string;
        averageRating: number;
        totalRatings: number;
        posterUrl?: string;
      }>;
      totalElements: number;
      totalPages: number;
      currentPage: number;
    };
  }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(`${this.apiUrl}/top-movies`, { params });
  }

  // Helper method to convert 10-point rating to 5-star display (each star = 2 points)
  convertToStars(rating: number): number {
    return rating / 2; // 10 points = 5 stars
  }

  // Helper method to convert 5-star input to 10-point scale (each star = 2 points)
  convertToTenPoint(stars: number): number {
    return stars * 2; // 5 stars = 10 points
  }

  // Get star array for display (filled/empty stars based on 10-point rating)
  getStarArray(rating: number): boolean[] {
    const stars = this.convertToStars(rating); // Convert 10-point to 5-star display
    return Array(5).fill(false).map((_, i) => i < Math.round(stars));
  }
}
