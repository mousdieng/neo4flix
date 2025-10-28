import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
}

export interface PaginatedResponse<T = any> {
  success: boolean;
  message: string;
  data: {
    items: T[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Generic GET request
  get<T>(endpoint: string, params?: HttpParams): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.get<T>(url, { params });
  }

  // Generic POST request
  post<T>(endpoint: string, body: any, headers?: HttpHeaders): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.post<T>(url, body, { headers });
  }

  // Generic PUT request
  put<T>(endpoint: string, body: any, headers?: HttpHeaders): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.put<T>(url, body, { headers });
  }

  // Generic DELETE request
  delete<T>(endpoint: string): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.delete<T>(url);
  }

  // Helper method to create HTTP params from object
  createParams(params: { [key: string]: any }): HttpParams {
    let httpParams = new HttpParams();

    Object.keys(params).forEach(key => {
      const value = params[key];
      if (value !== null && value !== undefined) {
        if (Array.isArray(value)) {
          value.forEach(v => {
            httpParams = httpParams.append(key, v.toString());
          });
        } else {
          httpParams = httpParams.set(key, value.toString());
        }
      }
    });

    return httpParams;
  }

  // Helper method to build pagination params
  buildPaginationParams(page: number = 0, size: number = 20, sortBy?: string, sortOrder?: 'asc' | 'desc'): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (sortBy) {
      params = params.set('sortBy', sortBy);
    }

    if (sortOrder) {
      params = params.set('sortOrder', sortOrder);
    }

    return params;
  }

  // Auth endpoints
  auth = {
    login: (credentials: any) => this.post<any>('/auth/login', credentials),
    register: (userData: any) => this.post<any>('/auth/register', userData),
    refresh: (refreshToken: string) => this.post<any>('/auth/refresh', { refreshToken }),
    logout: () => this.post<any>('/auth/logout', {}),
    profile: () => this.get<any>('/auth/profile')
  };

  // Movie endpoints
  movies = {
    search: (params: HttpParams) => this.get<any>('/movies/search', params),
    getById: (id: string) => this.get<any>(`/movies/${id}`),
    getAll: (params: HttpParams) => this.get<any>('/movies', params),
    getGenres: () => this.get<any>('/movies/genres'),
    getPopular: (params: HttpParams) => this.get<any>('/movies/popular', params),
    getRecent: (params: HttpParams) => this.get<any>('/movies/recent', params),
    getTopRated: (params: HttpParams) => this.get<any>('/movies/top-rated', params)
  };

  // Rating endpoints
  ratings = {
    submit: (rating: any) => this.post<any>('/ratings', rating),
    update: (id: string, rating: any) => this.put<any>(`/ratings/${id}`, rating),
    delete: (id: string) => this.delete<any>(`/ratings/${id}`),
    getByMovie: (movieId: string, params: HttpParams) => this.get<any>(`/ratings/movie/${movieId}`, params),
    getUserRating: (movieId: string) => this.get<any>(`/ratings/movie/${movieId}/user`),
    getUserRatings: (userId: string, params: HttpParams) => this.get<any>(`/ratings/user/${userId}`, params),
    getStats: (movieId: string) => this.get<any>(`/ratings/movie/${movieId}/stats`),
    getRecent: (params: HttpParams) => this.get<any>('/ratings/recent', params),
    getTopMovies: (params: HttpParams) => this.get<any>('/ratings/top-movies', params)
  };

  // Recommendation endpoints
  recommendations = {
    getPersonalized: (params: HttpParams) => this.get<any>('/recommendations/personalized', params),
    getSimilar: (movieId: string, params: HttpParams) => this.get<any>(`/recommendations/similar/${movieId}`, params),
    getByGenre: (params: HttpParams) => this.get<any>('/recommendations/by-genre', params),
    getTrending: (params: HttpParams) => this.get<any>('/recommendations/trending', params),
    getNewUser: (params: HttpParams) => this.get<any>('/recommendations/new-user', params),
    updatePreferences: (preferences: any) => this.put<any>('/recommendations/preferences', preferences),
    getPreferences: () => this.get<any>('/recommendations/preferences'),
    trackInteraction: (interaction: any) => this.post<any>('/recommendations/interactions', interaction),
    getHistoryBased: (params: HttpParams) => this.get<any>('/recommendations/history-based', params),
    getUserRecommendations: (userId: string, params: HttpParams) => this.get<any>(`/recommendations/user/${userId}`, params),
    refresh: () => this.post<any>('/recommendations/refresh', {})
  };

  // Watchlist endpoints
  watchlist = {
    get: () => this.get<any>('/watchlist'),
    add: (movieId: string) => this.post<any>('/watchlist/add', { movieId }),
    remove: (movieId: string) => this.delete<any>(`/watchlist/remove/${movieId}`)
  };

  // User endpoints
  users = {
    getProfile: () => this.get<any>('/users/profile'),
    updateProfile: (profile: any) => this.put<any>('/users/profile', profile),
    getById: (id: string) => this.get<any>(`/users/${id}`),
    getPreferences: () => this.get<any>('/users/preferences'),
    updatePreferences: (preferences: any) => this.put<any>('/users/preferences', preferences)
  };
}