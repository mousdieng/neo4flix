import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Movie {
  id: string;
  title: string;
  plot: string;
  releaseYear: number;
  duration: number;
  language?: string;
  country?: string;
  budget?: number;
  boxOffice?: number;
  posterUrl?: string;
  trailerUrl?: string;
  backdropUrl?: string;
  source?: string;
  tmdbId?: number;
  createdAt?: string;
  averageRating?: number; // 0-10 scale
  totalRatings?: number;
  genres: Array<{id: string | null; name: string; description: string | null}>;
  directors: Array<{id: string; name: string; birthDate: string | null; biography: string | null; nationality: string | null}>;
  actors: Array<{id: string; name: string; birthDate: string | null; biography: string | null; nationality: string | null}>;
}

export interface SearchCriteria {
  // Text search
  query?: string;
  title?: string;

  // Genre filters
  genre?: string;

  // People filters
  director?: string;
  actor?: string;

  // Year filters
  year?: number;
  minYear?: number;
  maxYear?: number;

  // Rating filters
  minRating?: number;
  maxRating?: number;

  // Metadata filters
  language?: string;
  country?: string;
  minDuration?: number;
  maxDuration?: number;

  // Budget/Revenue filters
  minBudget?: number;
  maxBudget?: number;
  minBoxOffice?: number;
  maxBoxOffice?: number;

  // Pagination & Sorting
  sortBy?: 'title' | 'releaseYear' | 'averageRating' | 'duration';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export interface MovieResponse {
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

@Injectable({
  providedIn: 'root'
})
export class MovieService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.movies}`;
  private searchCriteriaSubject = new BehaviorSubject<SearchCriteria>({});

  public searchCriteria$ = this.searchCriteriaSubject.asObservable();

  constructor(private http: HttpClient) {}

  searchMovies(criteria: SearchCriteria): Observable<MovieResponse> {
    let params = new HttpParams();

    // Text search
    if (criteria.query) params = params.set('query', criteria.query);
    if (criteria.title) params = params.set('title', criteria.title);

    // Genre filters
    if (criteria.genre) params = params.set('genre', criteria.genre);

    // People filters
    if (criteria.director) params = params.set('director', criteria.director);
    if (criteria.actor) params = params.set('actor', criteria.actor);

    // Year filters
    if (criteria.year) params = params.set('year', criteria.year.toString());
    if (criteria.minYear) params = params.set('minYear', criteria.minYear.toString());
    if (criteria.maxYear) params = params.set('maxYear', criteria.maxYear.toString());

    // Rating filters
    if (criteria.minRating) params = params.set('minRating', criteria.minRating.toString());
    if (criteria.maxRating) params = params.set('maxRating', criteria.maxRating.toString());

    // Metadata filters
    if (criteria.language) params = params.set('language', criteria.language);
    if (criteria.country) params = params.set('country', criteria.country);
    if (criteria.minDuration) params = params.set('minDuration', criteria.minDuration.toString());
    if (criteria.maxDuration) params = params.set('maxDuration', criteria.maxDuration.toString());

    // Budget/Revenue filters
    if (criteria.minBudget) params = params.set('minBudget', criteria.minBudget.toString());
    if (criteria.maxBudget) params = params.set('maxBudget', criteria.maxBudget.toString());
    if (criteria.minBoxOffice) params = params.set('minBoxOffice', criteria.minBoxOffice.toString());
    if (criteria.maxBoxOffice) params = params.set('maxBoxOffice', criteria.maxBoxOffice.toString());

    // Pagination & Sorting
    if (criteria.sortBy) params = params.set('sortBy', criteria.sortBy);
    if (criteria.sortOrder) params = params.set('sortOrder', criteria.sortOrder);
    if (criteria.page !== undefined) params = params.set('page', criteria.page.toString());
    if (criteria.size) params = params.set('size', criteria.size.toString());

    this.searchCriteriaSubject.next(criteria);

    return this.http.get<MovieResponse>(`${this.apiUrl}/search`, { params });
  }

  getMovieById(id: string): Observable<{success: boolean; data: Movie}> {
    return this.http.get<{success: boolean; data: Movie}>(`${this.apiUrl}/${id}`);
  }

  getPopularMovies(page: number = 0, size: number = 20): Observable<MovieResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'popularity')
      .set('sortOrder', 'desc');

    return this.http.get<MovieResponse>(`${this.apiUrl}`, { params });
  }

  getRecentMovies(page: number = 0, size: number = 20): Observable<MovieResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'release_date')
      .set('sortOrder', 'desc');

    return this.http.get<MovieResponse>(`${this.apiUrl}`, { params });
  }

  getTopRatedMovies(page: number = 0, size: number = 20): Observable<MovieResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'rating')
      .set('sortOrder', 'desc');

    return this.http.get<MovieResponse>(`${this.apiUrl}`, { params });
  }

  getMoviesByGenre(genre: string, page: number = 0, size: number = 20): Observable<MovieResponse> {
    const params = new HttpParams()
      .set('genre', genre)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<MovieResponse>(`${this.apiUrl}`, { params });
  }

  getGenres(): Observable<{success: boolean; data: string[]}> {
    return this.http.get<{success: boolean; data: string[]}>(`${this.apiUrl}/genres`);
  }

  updateSearchCriteria(criteria: SearchCriteria): void {
    this.searchCriteriaSubject.next({ ...this.searchCriteriaSubject.value, ...criteria });
  }

  getCurrentSearchCriteria(): SearchCriteria {
    return this.searchCriteriaSubject.value;
  }

  uploadMoviePoster(movieId: string, file: File): Observable<{success: string; message: string; posterUrl: string}> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{success: string; message: string; posterUrl: string}>(`${this.apiUrl}/${movieId}/upload-poster`, formData);
  }

  uploadMovieTrailer(movieId: string, file: File): Observable<{success: string; message: string; trailerUrl: string}> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{success: string; message: string; trailerUrl: string}>(`${this.apiUrl}/${movieId}/upload-trailer`, formData);
  }
}
