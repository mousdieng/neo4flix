import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  profilePictureUrl?: string;
  preferences?: any;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
  twoFactorCode?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  requiresTwoFactor?: boolean;
  twoFactorQrCode?: string;
}

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.auth}`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  private tokenKey = 'neo4flix_token';
  private refreshTokenKey = 'neo4flix_refresh_token';
  private userKey = 'neo4flix_user';

  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated = signal(false);

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage(): void {
    const token = localStorage.getItem(this.tokenKey);
    const userJson = localStorage.getItem(this.userKey);

    if (token && this.isTokenValid(token) && userJson) {
      try {
        const userData = JSON.parse(userJson);
        this.currentUserSubject.next(userData);
        this.isAuthenticated.set(true);
      } catch {
        // If parsing fails, fall back to extracting from token
        const userData = this.extractUserFromToken(token);
        if (userData) {
          this.currentUserSubject.next(userData);
          this.isAuthenticated.set(true);
        }
      }
    }
  }

  initializeUserProfile(): void {
    // Fetch fresh user data from API to get updated profile picture URL
    // This should be called after the app has fully initialized to avoid circular dependencies
    if (this.isAuthenticated()) {
      this.fetchCurrentUserProfile().subscribe({
        next: () => {
          // User data updated successfully
        },
        error: () => {
          // Keep using cached data if API call fails
        }
      });
    }
  }

  private isTokenValid(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp > Date.now() / 1000;
    } catch {
      return false;
    }
  }

  private extractUserFromToken(token: string): User | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        id: payload.sub,
        username: payload.username,
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        role: payload.role,
        profilePictureUrl: payload.profilePictureUrl
      };
    } catch {
      return null;
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    console.log(credentials)
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          if (!response.requiresTwoFactor && response.accessToken) {
            this.setAuthData(response.accessToken, response.refreshToken, response.user);
          }
        }),
        catchError(error => {
          console.error('Login error:', error);
          return throwError(() => error);
        })
      );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, userData)
      .pipe(
        tap(response => {
          if (response.accessToken) {
            this.setAuthData(response.accessToken, response.refreshToken, response.user);
          }
        }),
        catchError(error => {
          console.error('Registration error:', error);
          return throwError(() => error);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSubject.next(null);
    this.isAuthenticated.set(false);
  }

  private setAuthData(token: string, refreshToken: string, user: User): void {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.refreshTokenKey, refreshToken);
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
    this.isAuthenticated.set(true);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  fetchCurrentUserProfile(): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/users/me`)
      .pipe(
        tap(user => {
          // Update the stored user data with fresh profile picture URL
          const currentToken = this.getToken();
          const currentRefreshToken = localStorage.getItem(this.refreshTokenKey);
          if (currentToken && currentRefreshToken) {
            this.setAuthData(currentToken, currentRefreshToken, user);
          }
        }),
        catchError(error => {
          console.error('Error fetching current user profile:', error);
          return throwError(() => error);
        })
      );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem(this.refreshTokenKey);
    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('No refresh token'));
    }

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          if (response.accessToken) {
            this.setAuthData(response.accessToken, response.refreshToken, response.user);
          }
        }),
        catchError(error => {
          this.logout();
          return throwError(() => error);
        })
      );
  }
}
