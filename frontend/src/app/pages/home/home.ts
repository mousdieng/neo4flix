import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MovieService, Movie } from '../../services/movie';
import { RecommendationService, RecommendedMovie } from '../../services/recommendation';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {
  public featuredMovies = signal<Movie[]>([]);
  public personalizedRecommendations = signal<RecommendedMovie[]>([]);
  public trendingMovies = signal<Movie[]>([]);
  public topRatedMovies = signal<Movie[]>([]);
  public isLoading = signal(false);
  public isAuthenticated = signal(false);

  constructor(
    private movieService: MovieService,
    private recommendationService: RecommendationService,
    private authService: Auth,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated.set(this.authService.isAuthenticated());
    this.loadHomeContent();
  }

  private loadHomeContent(): void {
    this.isLoading.set(true);

    // Load different content based on authentication status
    if (this.isAuthenticated()) {
      this.loadPersonalizedContent();
    } else {
      this.loadGuestContent();
    }
  }

  private loadPersonalizedContent(): void {
    // Randomize which page to load from for variety on each visit
    const randomPage = Math.floor(Math.random() * 3); // Random page 0-2

    // For now, use popular movies instead of personalized (until recommendation endpoints are implemented)
    this.movieService.getPopularMovies(randomPage, 8).subscribe({
      next: (response) => {
        if (response.success) {
          this.featuredMovies.set(response.data.movies);
        }
      },
      error: (error) => console.error('Error loading featured movies:', error)
    });

    // Load recent movies as "trending"
    this.movieService.getRecentMovies(randomPage, 6).subscribe({
      next: (response) => {
        if (response.success) {
          this.trendingMovies.set(response.data.movies);
        }
      },
      error: (error) => console.error('Error loading recent movies:', error)
    });

    // Load top-rated movies
    this.movieService.getTopRatedMovies(randomPage, 6).subscribe({
      next: (response) => {
        if (response.success) {
          this.topRatedMovies.set(response.data.movies);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading top-rated movies:', error);
        this.isLoading.set(false);
      }
    });
  }

  private loadGuestContent(): void {
    // Randomize which page to load from for variety on each visit
    const randomPage = Math.floor(Math.random() * 3); // Random page 0-2

    // Load popular movies for guest users
    this.movieService.getPopularMovies(randomPage, 8).subscribe({
      next: (response) => {
        if (response.success) {
          this.featuredMovies.set(response.data.movies);
        }
      },
      error: (error) => console.error('Error loading featured movies:', error)
    });

    // Load recent movies
    this.movieService.getRecentMovies(randomPage, 6).subscribe({
      next: (response) => {
        if (response.success) {
          this.trendingMovies.set(response.data.movies);
        }
      },
      error: (error) => console.error('Error loading recent movies:', error)
    });

    // Load top-rated movies
    this.movieService.getTopRatedMovies(randomPage, 6).subscribe({
      next: (response) => {
        if (response.success) {
          this.topRatedMovies.set(response.data.movies);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading top-rated movies:', error);
        this.isLoading.set(false);
      }
    });
  }

  public onMovieClick(movie: Movie): void {
    // Track user interaction
    if (this.isAuthenticated()) {
      this.recommendationService.trackUserInteraction({
        movieId: movie.id,
        action: 'view',
        timestamp: new Date().toISOString()
      }).subscribe();
    }

    this.router.navigate(['/movie', movie.id]);
  }

  public onGenreClick(genre: string): void {
    this.router.navigate(['/movies'], { queryParams: { genre } });
  }

  public navigateToMovies(): void {
    this.router.navigate(['/movies']);
  }

  public navigateToLogin(): void {
    this.router.navigate(['/login']);
  }

  public navigateToRegister(): void {
    this.router.navigate(['/register']);
  }

  public formatRecommendationReason(reason: string): string {
    return this.recommendationService.formatRecommendationReason(reason);
  }

  public getConfidenceColorClass(confidence: number): string {
    return this.recommendationService.getConfidenceColorClass(confidence);
  }
}
