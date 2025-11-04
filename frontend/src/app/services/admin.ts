import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
  lastLoginAt?: string;
  totalRatings?: number;
  watchlistSize?: number;
}

export interface PagedUsers {
  content: UserProfile[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface GenreResponse {
  id: string;
  name: string;
}

export interface PersonResponse {
  id: string;
  name: string;
}

export interface Movie {
  id: string;
  title: string;
  plot: string;
  releaseYear: number;
  duration?: number;
  language?: string;
  country?: string;
  budget?: number;
  boxOffice?: number;
  posterUrl?: string;
  trailerUrl?: string;
  averageRating?: number;
  totalRatings?: number;
  genres?: GenreResponse[];
  directors?: PersonResponse[];
  actors?: PersonResponse[];
}

export interface PagedMovies {
  success: boolean;
  message: string;
  data: {
    movies: Movie[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

export interface CreateMovieRequest {
  title: string;
  plot: string;
  releaseYear: number;
  duration?: number;
  language?: string;
  country?: string;
  budget?: number;
  boxOffice?: number;
  posterUrl?: string;
  trailerUrl?: string;
}

export interface UpdateMovieRequest {
  title?: string;
  plot?: string;
  releaseYear?: number;
  duration?: number;
  language?: string;
  country?: string;
  budget?: number;
  boxOffice?: number;
  posterUrl?: string;
  trailerUrl?: string;
}

export interface SystemStats {
  totalMovies: number;
  totalGenres: number;
  totalActors: number;
  totalDirectors: number;
  averageRating: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private userApiUrl = `${environment.apiUrl}${environment.endpoints.users}`;
  private movieApiUrl = `${environment.apiUrl}/movies`;

  constructor(private http: HttpClient) {}

  // User Management
  getAllUsers(page: number = 0, size: number = 20, search?: string, sortBy: string = 'createdAt', sortDir: string = 'DESC'): Observable<PagedUsers> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<PagedUsers>(this.userApiUrl, { params });
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.userApiUrl}/${userId}`);
  }

  toggleUserStatus(userId: string, enabled: boolean): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.userApiUrl}/${userId}/status`, null, {
      params: { enabled: enabled.toString() }
    });
  }

  getUserStats(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.userApiUrl}/${userId}/stats`);
  }

  // Movie Management
  getAllMovies(page: number = 0, size: number = 20, sortBy: string = 'title', sortOrder: string = 'asc'): Observable<PagedMovies> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortOrder', sortOrder);

    return this.http.get<PagedMovies>(this.movieApiUrl, { params });
  }

  getMovie(movieId: string): Observable<{ success: boolean; data: Movie }> {
    return this.http.get<{ success: boolean; data: Movie }>(`${this.movieApiUrl}/${movieId}`);
  }

  createMovie(movie: CreateMovieRequest, posterFile?: File, trailerFile?: File): Observable<Movie> {
    // If files are provided, use FormData (multipart), otherwise use JSON
    if (posterFile || trailerFile) {
      const formData = new FormData();
      formData.append('movie', JSON.stringify(movie));

      if (posterFile) {
        formData.append('poster', posterFile);
      }

      if (trailerFile) {
        formData.append('trailer', trailerFile);
      }

      return this.http.post<Movie>(this.movieApiUrl, formData);
    } else {
      // No files, send as JSON
      return this.http.post<Movie>(this.movieApiUrl, movie);
    }
  }

  updateMovie(movieId: string, movie: UpdateMovieRequest, posterFile?: File, trailerFile?: File): Observable<Movie> {
    // If files are provided, use FormData (multipart), otherwise use JSON
    if (posterFile || trailerFile) {
      const formData = new FormData();
      formData.append('movie', JSON.stringify(movie));

      if (posterFile) {
        formData.append('poster', posterFile);
      }

      if (trailerFile) {
        formData.append('trailer', trailerFile);
      }

      return this.http.put<Movie>(`${this.movieApiUrl}/${movieId}`, formData);
    } else {
      // No files, send as JSON
      return this.http.put<Movie>(`${this.movieApiUrl}/${movieId}`, movie);
    }
  }

  deleteMovie(movieId: string): Observable<void> {
    return this.http.delete<void>(`${this.movieApiUrl}/${movieId}`);
  }

  getMovieStats(): Observable<SystemStats> {
    return this.http.get<SystemStats>(`${this.movieApiUrl}/stats`);
  }

  searchMovies(query: string): Observable<PagedMovies> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', '0')
      .set('size', '20');

    return this.http.get<PagedMovies>(`${this.movieApiUrl}/search`, { params });
  }
}
