import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, switchMap } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MovieService, Movie } from '../../services/movie';
import { RatingService, Rating, RatingResponse } from '../../services/rating';
import { RecommendationService, RecommendedMovie } from '../../services/recommendation';
import { WatchlistService } from '../../services/watchlist';
import { Auth } from '../../services/auth';
import { RatingComponent } from '../../components/rating/rating';
import { ReviewComponent } from '../../components/review/review';
import { ShareMovieDialogComponent, ShareMovieDialogResult } from '../../components/share-movie-dialog/share-movie-dialog.component';
import { ToastService } from '../../components/toast/toast.service';

@Component({
  selector: 'app-movie-detail',
  imports: [CommonModule, RatingComponent, ReviewComponent],
  templateUrl: './movie-detail.html',
  styleUrl: './movie-detail.css'
})
export class MovieDetail implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  public movie = signal<Movie | null>(null);
  public similarMovies = signal<RecommendedMovie[]>([]);
  public userRating = signal<Rating | null>(null);
  public isLoading = signal(true);
  public isAuthenticated = signal(false);
  public showReviewForm = signal(false);
  public isInWatchlist = signal(false);
  public showShareMenu = signal(false);

  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private movieService: MovieService,
    private ratingService: RatingService,
    private recommendationService: RecommendationService,
    private watchlistService: WatchlistService,
    private authService: Auth,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated.set(this.authService.isAuthenticated());

    this.route.paramMap.pipe(
      takeUntil(this.destroy$),
      switchMap(params => {
        const movieId = params.get('id');
        if (!movieId) {
          this.isLoading.set(false);
          this.router.navigate(['/movies']);
          throw new Error('No movie ID provided');
        }
        return this.movieService.getMovieById(movieId);
      })
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.movie.set(response.data);
          this.loadMovieDetails(response.data.id);
        } else {
          // Response came back but success is false
          console.error('Movie fetch failed:', response);
          this.isLoading.set(false);
          this.router.navigate(['/movies']);
        }
      },
      error: (error) => {
        console.error('Error loading movie:', error);
        this.isLoading.set(false);
        this.router.navigate(['/movies']);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadMovieDetails(movieId: string): void {
    // Load similar movies (only for authenticated users)
    if (this.isAuthenticated()) {
      this.recommendationService.getSimilarMovies(movieId, 6).subscribe({
        next: (response) => {
          if (response.success) {
            this.similarMovies.set(response.data.recommendations);
          }
        },
        error: (error) => console.error('Error loading similar movies:', error)
      });
    }

    // Load user rating if authenticated
    if (this.isAuthenticated()) {
      this.ratingService.getUserMovieRating(movieId).subscribe({
        next: (rating: Rating) => {
          // Backend returns the rating directly
          this.userRating.set(rating);
        },
        error: (error: any) => {
          // 404 is expected if user hasn't rated the movie yet
          if (error.status !== 404) {
            console.error('Error loading user rating:', error);
          }
        }
      });

      // Check if movie is in watchlist
      this.isInWatchlist.set(this.watchlistService.isInWatchlist(movieId));
    }

    this.isLoading.set(false);
  }

  public onRatingSubmit(rating: number): void {
    const movie = this.movie();
    if (!movie || !this.isAuthenticated()) return;

    console.log('Submitting rating:', rating, 'for movie:', movie.id);

    this.ratingService.submitRating({
      movieId: movie.id,
      rating: rating
    }).subscribe({
      next: (response: Rating) => {
        console.log('Raw response from backend:', response);
        console.log('Response type:', typeof response);
        console.log('Response has id?', 'id' in response, response.id);
        console.log('Response has rating?', 'rating' in response, response.rating);

        // Set the user rating signal
        this.userRating.set(response);

        console.log('userRating signal after set:', this.userRating());
        console.log('Signal value check:', !!this.userRating());

        // Track the rating interaction
        this.recommendationService.trackUserInteraction({
          movieId: movie.id,
          action: 'rate',
          value: rating,
          timestamp: new Date().toISOString()
        }).subscribe();
      },
      error: (error: any) => {
        console.error('Error submitting rating:', error);
        console.error('Error status:', error.status);
        console.error('Error body:', error.error);
      }
    });
  }

  public onReviewSubmit(rating: Rating): void {
    this.userRating.set(rating);
    this.showReviewForm.set(false);

    // Track the review interaction
    const movie = this.movie();
    if (movie && this.isAuthenticated()) {
      this.recommendationService.trackUserInteraction({
        movieId: movie.id,
        action: 'rate',
        value: rating.rating,
        timestamp: new Date().toISOString()
      }).subscribe();
    }
  }

  public onSimilarMovieClick(movie: RecommendedMovie): void {
    if (this.isAuthenticated()) {
      this.recommendationService.trackUserInteraction({
        movieId: movie.id,
        action: 'view',
        timestamp: new Date().toISOString()
      }).subscribe();
    }
    this.router.navigate(['/movie', movie.id]);
  }

  public toggleReviewForm(): void {
    this.showReviewForm.set(!this.showReviewForm());
  }

  public navigateToLogin(): void {
    this.router.navigate(['/login'], {
      queryParams: { returnUrl: this.router.url }
    });
  }

  public formatGenres(genres: Array<{id: string | null; name: string; description: string | null}>): string {
    return genres.map(g => g.name).join(', ');
  }

  public formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`;
  }

  public getDirectors(): string {
    return this.movie()?.directors?.map(d => d.name).join(', ') || '';
  }

  public getActors(): string {
    return this.movie()?.actors?.map(a => a.name).join(', ') || '';
  }

  public toggleWatchlist(): void {
    const movie = this.movie();
    if (!movie || !this.isAuthenticated()) return;

    if (this.isInWatchlist()) {
      // Remove from watchlist
      this.watchlistService.removeFromWatchlist(movie.id).subscribe({
        next: () => {
          this.isInWatchlist.set(false);
          // Track interaction
          this.recommendationService.trackUserInteraction({
            movieId: movie.id,
            action: 'watchlist_remove',
            timestamp: new Date().toISOString()
          }).subscribe();
        },
        error: (error) => console.error('Error removing from watchlist:', error)
      });
    } else {
      // Add to watchlist
      this.watchlistService.addToWatchlist({ movieId: movie.id }).subscribe({
        next: () => {
          this.isInWatchlist.set(true);
          // Track interaction
          this.recommendationService.trackUserInteraction({
            movieId: movie.id,
            action: 'watchlist_add',
            timestamp: new Date().toISOString()
          }).subscribe();
        },
        error: (error) => console.error('Error adding to watchlist:', error)
      });
    }
  }

  public toggleShareMenu(): void {
    this.showShareMenu.set(!this.showShareMenu());
  }

  public shareWithFriends(): void {
    const movie = this.movie();
    if (!movie) return;

    const dialogRef = this.dialog.open(ShareMovieDialogComponent, {
      width: '600px',
      data: {
        movieId: movie.id,
        movieTitle: movie.title
      }
    });

    dialogRef.afterClosed().subscribe((result: ShareMovieDialogResult | undefined) => {
      if (result && result.selectedFriends.length > 0) {
        this.recommendationService.shareRecommendation({
          movieId: movie.id,
          friendIds: result.selectedFriends,
          message: result.message
        }).subscribe({
          next: (response) => {
            this.snackBar.open(
              `Shared with ${response.sharedCount} friend${response.sharedCount !== 1 ? 's' : ''}!`,
              'OK',
              { duration: 3000 }
            );
            this.showShareMenu.set(false);
          },
          error: (err) => {
            console.error('Error sharing recommendation:', err);
            this.snackBar.open(
              'Failed to share recommendation. Please try again.',
              'OK',
              { duration: 3000 }
            );
          }
        });
      }
    });
  }

  public shareMovie(platform: 'twitter' | 'facebook' | 'linkedin' | 'copy'): void {
    const movie = this.movie();
    if (!movie) return;

    const url = window.location.href;
    const text = `Check out ${movie.title} (${movie.releaseYear}) on Neo4flix!`;

    // Track share interaction
    this.recommendationService.trackUserInteraction({
      movieId: movie.id,
      action: 'share',
      timestamp: new Date().toISOString()
    }).subscribe();

    switch (platform) {
      case 'twitter':
        window.open(
          `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`,
          '_blank',
          'width=600,height=400'
        );
        break;
      case 'facebook':
        window.open(
          `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
          '_blank',
          'width=600,height=400'
        );
        break;
      case 'linkedin':
        window.open(
          `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`,
          '_blank',
          'width=600,height=400'
        );
        break;
      case 'copy':
        navigator.clipboard.writeText(url).then(() => {
          this.toastService.success('Link copied to clipboard!');
        }).catch(err => {
          console.error('Failed to copy link:', err);
          this.toastService.error('Failed to copy link');
        });
        break;
    }

    this.showShareMenu.set(false);
  }

  public addSimilarToWatchlist(similarMovie: RecommendedMovie, event: Event): void {
    event.stopPropagation();

    if (!this.isAuthenticated()) {
      this.navigateToLogin();
      return;
    }

    this.watchlistService.addToWatchlist({ movieId: similarMovie.id }).subscribe({
      next: () => {
        // Track interaction
        this.recommendationService.trackUserInteraction({
          movieId: similarMovie.id,
          action: 'watchlist_add',
          timestamp: new Date().toISOString()
        }).subscribe();
      },
      error: (error) => {
        console.error('Error adding to watchlist:', error);
      }
    });
  }

  public isSimilarInWatchlist(movieId: string): boolean {
    return this.watchlistService.isInWatchlist(movieId);
  }
}
