import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, throwError } from 'rxjs';
import { Movie } from './movie';
import { environment } from '../../environments/environment';
import { AlertService } from './alert.service';

export interface WatchlistItem {
  id: string;
  userId: string;
  movieId: string;
  addedAt: string;
  priority: number; // 1=High, 2=Medium, 3=Low
  priorityLabel: string;
  notes: string | null;
  watched: boolean;
  watchedAt: string | null;
  movie?: Movie; // Movie details fetched from movie service
}

export interface WatchlistStats {
  totalMovies: number;
  watchedMovies: number;
  unwatchedMovies: number;
  highPriority: number;
  mediumPriority: number;
  lowPriority: number;
}

export interface WatchlistPageResponse {
  items: WatchlistItem[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  stats: WatchlistStats;
}

export interface WatchlistQueryParams {
  page?: number;
  pageSize?: number;
  priority?: number;
  watched?: boolean;
  sortBy?: 'addedAt' | 'priority' | 'title';
  sortDirection?: 'ASC' | 'DESC';
  genres?: string[];
  fromYear?: number;
  toYear?: number;
}

export interface AddToWatchlistRequest {
  movieId: string;
  priority?: number;
  notes?: string;
}

export interface UpdateWatchlistRequest {
  priority?: number;
  notes?: string;
  watched?: boolean;
}

export interface WatchlistCheckResponse {
  inWatchlist: boolean;
  watchlistId?: string;
  priority?: number;
  watched?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.watchlist}`;
  private watchlistSubject = new BehaviorSubject<WatchlistItem[]>([]);
  private statsSubject = new BehaviorSubject<WatchlistStats | null>(null);

  public watchlist$ = this.watchlistSubject.asObservable();
  public stats$ = this.statsSubject.asObservable();
  public watchlistCount = signal(0);

  private alertService = inject(AlertService);

  constructor(private http: HttpClient) {}

  /**
   * Get watchlist with pagination, filtering and sorting
   */
  getWatchlist(params?: WatchlistQueryParams): Observable<WatchlistPageResponse> {
    let httpParams = new HttpParams();

    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.pageSize !== undefined) httpParams = httpParams.set('pageSize', params.pageSize.toString());
      if (params.priority !== undefined) httpParams = httpParams.set('priority', params.priority.toString());
      if (params.watched !== undefined) httpParams = httpParams.set('watched', params.watched.toString());
      if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
      if (params.sortDirection) httpParams = httpParams.set('sortDirection', params.sortDirection);
      if (params.fromYear !== undefined) httpParams = httpParams.set('fromYear', params.fromYear.toString());
      if (params.toYear !== undefined) httpParams = httpParams.set('toYear', params.toYear.toString());
      if (params.genres && params.genres.length > 0) {
        params.genres.forEach(genre => {
          httpParams = httpParams.append('genres', genre);
        });
      }
    }

    return this.http.get<WatchlistPageResponse>(this.apiUrl, { params: httpParams })
      .pipe(
        tap(response => {
          this.watchlistSubject.next(response.items);
          this.statsSubject.next(response.stats);
          this.watchlistCount.set(response.totalItems);
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to load watchlist');
          return throwError(() => error);
        })
      );
  }

  /**
   * Get watchlist statistics
   */
  getWatchlistStats(): Observable<WatchlistStats> {
    return this.http.get<WatchlistStats>(`${this.apiUrl}/stats`)
      .pipe(
        tap(stats => {
          this.statsSubject.next(stats);
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to load watchlist stats');
          return throwError(() => error);
        })
      );
  }

  /**
   * Add movie to watchlist
   */
  addToWatchlist(request: AddToWatchlistRequest): Observable<WatchlistItem> {
    return this.http.post<WatchlistItem>(this.apiUrl, request)
      .pipe(
        tap(item => {
          const currentWatchlist = this.watchlistSubject.value;
          this.watchlistSubject.next([item, ...currentWatchlist]);
          this.watchlistCount.set(this.watchlistCount() + 1);
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to add to watchlist');
          return throwError(() => error);
        })
      );
  }

  /**
   * Remove movie from watchlist
   */
  removeFromWatchlist(movieId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/movies/${movieId}`)
      .pipe(
        tap(() => {
          const currentWatchlist = this.watchlistSubject.value;
          const updatedWatchlist = currentWatchlist.filter(item => item.movieId !== movieId);
          this.watchlistSubject.next(updatedWatchlist);
          this.watchlistCount.set(this.watchlistCount() - 1);
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to remove from watchlist');
          return throwError(() => error);
        })
      );
  }

  /**
   * Update watchlist entry
   */
  updateWatchlistEntry(movieId: string, request: UpdateWatchlistRequest): Observable<WatchlistItem> {
    return this.http.patch<WatchlistItem>(`${this.apiUrl}/movies/${movieId}`, request)
      .pipe(
        tap(updatedItem => {
          const currentWatchlist = this.watchlistSubject.value;
          const index = currentWatchlist.findIndex(item => item.movieId === movieId);
          if (index !== -1) {
            currentWatchlist[index] = updatedItem;
            this.watchlistSubject.next([...currentWatchlist]);
          }
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to update watchlist entry');
          return throwError(() => error);
        })
      );
  }

  /**
   * Mark movie as watched or unwatched
   */
  markAsWatched(movieId: string, watched: boolean): Observable<WatchlistItem> {
    return this.http.put<WatchlistItem>(`${this.apiUrl}/movies/${movieId}/watched`, null, {
      params: { watched: watched.toString() }
    })
      .pipe(
        tap(updatedItem => {
          const currentWatchlist = this.watchlistSubject.value;
          const index = currentWatchlist.findIndex(item => item.movieId === movieId);
          if (index !== -1) {
            currentWatchlist[index] = updatedItem;
            this.watchlistSubject.next([...currentWatchlist]);
          }
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to mark as watched');
          return throwError(() => error);
        })
      );
  }

  /**
   * Check if movie is in watchlist
   */
  checkInWatchlist(movieId: string): Observable<WatchlistCheckResponse> {
    return this.http.get<WatchlistCheckResponse>(`${this.apiUrl}/movies/${movieId}/check`)
      .pipe(
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to check watchlist');
          return throwError(() => error);
        })
      );
  }

  /**
   * Check if movie is in watchlist (synchronous, using cached data)
   */
  isInWatchlist(movieId: string): boolean {
    return this.watchlistSubject.value.some(item => item.movieId === movieId);
  }

  /**
   * Clear all watched movies from watchlist
   */
  clearWatchedMovies(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/watched`)
      .pipe(
        tap(() => {
          const currentWatchlist = this.watchlistSubject.value;
          const updatedWatchlist = currentWatchlist.filter(item => !item.watched);
          this.watchlistSubject.next(updatedWatchlist);
          this.watchlistCount.set(updatedWatchlist.length);
        }),
        catchError(error => {
          this.alertService.error(error.error?.message || error.message || 'Failed to clear watched movies');
          return throwError(() => error);
        })
      );
  }

  /**
   * Get unwatched movies
   */
  getUnwatchedMovies(page: number = 0, pageSize: number = 20): Observable<WatchlistPageResponse> {
    return this.getWatchlist({
      page,
      pageSize,
      watched: false,
      sortBy: 'priority',
      sortDirection: 'ASC'
    });
  }

  /**
   * Get high priority movies
   */
  getHighPriorityMovies(page: number = 0, pageSize: number = 20): Observable<WatchlistPageResponse> {
    return this.getWatchlist({
      page,
      pageSize,
      priority: 1,
      watched: false,
      sortBy: 'addedAt',
      sortDirection: 'DESC'
    });
  }

  /**
   * Clear watchlist cache
   */
  clearWatchlist(): void {
    this.watchlistSubject.next([]);
    this.statsSubject.next(null);
    this.watchlistCount.set(0);
  }

  /**
   * Get current watchlist (synchronous, using cached data)
   */
  getCurrentWatchlist(): WatchlistItem[] {
    return this.watchlistSubject.value;
  }

  /**
   * Get current stats (synchronous, using cached data)
   */
  getCurrentStats(): WatchlistStats | null {
    return this.statsSubject.value;
  }
}
