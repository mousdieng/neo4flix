import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RecommendationService, RecommendedMovie } from '../../services/recommendation';
import { WatchlistService } from '../../services/watchlist';
import { Auth } from '../../services/auth';
import { forkJoin } from 'rxjs';
import {MovieService} from '../../services/movie';
import { SharedRecommendationsComponent } from '../../components/shared-recommendations/shared-recommendations.component';

@Component({
  selector: 'app-recommendations',
  imports: [CommonModule, SharedRecommendationsComponent],
  templateUrl: './recommendations.html',
  styleUrl: './recommendations.css'
})
export class Recommendations implements OnInit {
  public personalizedRecommendations = signal<RecommendedMovie[]>([]);
  public trendingRecommendations = signal<RecommendedMovie[]>([]);
  public genreBasedRecommendations = signal<RecommendedMovie[]>([]);
  public isLoading = signal(true);
  public isAuthenticated = signal(false);
  public selectedGenres = signal<string[]>([]);
  public availableGenres = signal<string[]>([]); // ['Action', 'Comedy', 'Drama', 'Thriller', 'Sci-Fi', 'Romance', 'Horror', 'Adventure'];
  public selectedYearRange = signal<{ fromYear: number | null; toYear: number | null }>({ fromYear: null, toYear: null });
  public availableYears: number[] = [];
  public showYearFilter = signal(false);

  public toastMessage = signal<string | null>(null);
  public toastType = signal<'success' | 'error' | 'info'>('success');

  constructor(
    private recommendationService: RecommendationService,
    private movieService: MovieService,
    private watchlistService: WatchlistService,
    private authService: Auth,
    private router: Router
  ) {
    const currentYear = new Date().getFullYear();
    for (let year = currentYear; year >= 1950; year--) {
      this.availableYears.push(year);
    }
  }

  ngOnInit(): void {
    this.isAuthenticated.set(this.authService.isAuthenticated());
    this.loadGenres()

    if (this.isAuthenticated()) {
      this.loadPersonalizedRecommendations();
    } else {
      this.loadGuestRecommendations();
    }
  }

  private loadGenres(): void {
    this.movieService.getGenres().subscribe({
      next: (response) => {
        if (response.success) {
          console.log(response.data)
          this.availableGenres.set(response.data);
        }
      },
      error: (error) => console.error('Error loading genres:', error)
    });
  }

  private loadPersonalizedRecommendations(): void {
    this.isLoading.set(true);

    this.recommendationService.getPersonalizedRecommendations(12).subscribe({
      next: (response) => {
        console.log('Personalized recommendations:', response);
        if (response.success && response.data?.recommendations) {
          this.enrichRecommendations(response.data.recommendations, this.personalizedRecommendations);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading personalized recommendations:', error);
        this.loadTrendingRecommendations();
      }
    });

    this.recommendationService.getWatchHistoryRecommendations(6).subscribe({
      next: (response) => {
        if (response.success) {
          // Handle watch history recommendations separately if needed
        }
      },
      error: (error) => console.error('Error loading history recommendations:', error)
    });
  }

  private loadGuestRecommendations(): void {
    this.loadTrendingRecommendations();
  }

  private loadTrendingRecommendations(): void {
    this.recommendationService.getTrendingRecommendations(12).subscribe({
      next: (response) => {
        console.log("trending", response)
        if (response.success && response.data?.recommendations) {
          this.enrichRecommendations(response.data.recommendations, this.trendingRecommendations);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading trending recommendations:', error);
        this.isLoading.set(false);
      }
    });
  }

  private loadGenreBasedRecommendations(): void {
    const yearRange = this.selectedYearRange();
    this.recommendationService.getGenreBasedRecommendations(
      this.selectedGenres(),
      12,
      yearRange.fromYear,
      yearRange.toYear
    ).subscribe({
      next: (response) => {
        if (response.success && response.data?.recommendations) {
          response.data.recommendations.forEach(v => {
            console.log(v.genres)
          })
          this.enrichRecommendations(response.data.recommendations, this.genreBasedRecommendations);
        }
      },
      error: (error) => console.error('Error loading genre recommendations:', error)
    });
  }

  private enrichRecommendations(
    rawRecommendations: any[],
    targetSignal: ReturnType<typeof signal<RecommendedMovie[]>>
  ): void {
    const movieIds = rawRecommendations.map(rec => rec.movieId).filter(Boolean);

    if (movieIds.length === 0) {
      targetSignal.set([]);
      return;
    }

    // Fetch all movies in parallel
    const movieRequests = movieIds.map(id =>
      this.movieService.getMovieById(id)
    );

    forkJoin(movieRequests).subscribe({
      next: (responses) => {
        const enrichedRecommendations: RecommendedMovie[] = rawRecommendations
          .map((rawRec, index) => {
            const movieData = responses[index]?.data;
            if (!movieData) return null;

            return {
              ...movieData,
              confidence: (rawRec.confidence || rawRec.score || 0) * 100,
              reason: rawRec.reason || '',
              similarity: rawRec.score || 0
            } as RecommendedMovie;
          })
          .filter((rec): rec is RecommendedMovie => rec !== null);

        targetSignal.set(enrichedRecommendations);
      },
      error: (error) => {
        console.error('Error fetching movie details:', error);
        targetSignal.set([]);
      }
    });
  }

  public onGenreSelect(genre: string): void {
    const current = this.selectedGenres();
    if (current.includes(genre)) {
      this.selectedGenres.set(current.filter(g => g !== genre));
    } else {
      this.selectedGenres.set([...current, genre]);
    }

    if (this.selectedGenres().length > 0) {
      this.loadGenreBasedRecommendations();
    } else {
      this.genreBasedRecommendations.set([]);
    }
  }

  public onMovieClick(movie: RecommendedMovie): void {
    if (this.isAuthenticated()) {
      this.recommendationService.trackUserInteraction({
        movieId: movie.id,
        action: 'view',
        timestamp: new Date().toISOString()
      }).subscribe();
    }

    this.router.navigate(['/movie', movie.id]);
  }

  public navigateToLogin(): void {
    this.router.navigate(['/login'], {
      queryParams: { returnUrl: '/recommendations' }
    });
  }

  public formatRecommendationReason(reason: string): string {
    return this.recommendationService.formatRecommendationReason(reason);
  }

  public getConfidenceColorClass(confidence: number): string {
    return this.recommendationService.getConfidenceColorClass(confidence);
  }

  public refreshRecommendations(): void {
    if (this.isAuthenticated()) {
      this.isLoading.set(true);
      this.recommendationService.refreshRecommendations().subscribe({
        next: () => {
          this.loadPersonalizedRecommendations();
        },
        error: (error) => {
          console.error('Error refreshing recommendations:', error);
          this.isLoading.set(false);
        }
      });
    }
  }

  public toggleYearFilter(): void {
    this.showYearFilter.set(!this.showYearFilter());
  }

  public onYearRangeChange(type: 'from' | 'to', year: number | null): void {
    const currentRange = this.selectedYearRange();

    if (type === 'from') {
      this.selectedYearRange.set({ ...currentRange, fromYear: year });
    } else {
      this.selectedYearRange.set({ ...currentRange, toYear: year });
    }

    if (this.selectedGenres().length > 0 || currentRange.fromYear !== null || currentRange.toYear !== null) {
      this.loadFilteredRecommendations();
    }
  }

  public clearYearFilter(): void {
    this.selectedYearRange.set({ fromYear: null, toYear: null });

    if (this.selectedGenres().length > 0) {
      this.loadGenreBasedRecommendations();
    } else {
      this.genreBasedRecommendations.set([]);
    }
  }

  private loadFilteredRecommendations(): void {
    if (this.selectedGenres().length > 0) {
      this.loadGenreBasedRecommendations();
    }
  }

  public hasActiveFilters(): boolean {
    const yearRange = this.selectedYearRange();
    return this.selectedGenres().length > 0 || yearRange.fromYear !== null || yearRange.toYear !== null;
  }

  public clearAllFilters(): void {
    this.selectedGenres.set([]);
    this.selectedYearRange.set({ fromYear: null, toYear: null });
    this.genreBasedRecommendations.set([]);
  }

  public addToWatchlist(movie: RecommendedMovie, event: Event): void {
    event.stopPropagation();

    if (!this.isAuthenticated()) {
      this.navigateToLogin();
      return;
    }

    this.watchlistService.addToWatchlist({ movieId: movie.id }).subscribe({
      next: () => {
        this.showToast('Added to watchlist', 'success');
        // Track interaction
        this.recommendationService.trackUserInteraction({
          movieId: movie.id,
          action: 'watchlist_add',
          timestamp: new Date().toISOString()
        }).subscribe();
      },
      error: (error) => {
        console.error('Error adding to watchlist:', error);
        if (error.status === 409) {
          this.showToast('Movie already in watchlist', 'info');
        } else {
          this.showToast('Failed to add to watchlist', 'error');
        }
      }
    });
  }

  public isInWatchlist(movieId: string): boolean {
    return this.watchlistService.isInWatchlist(movieId);
  }

  private showToast(message: string, type: 'success' | 'error' | 'info' = 'success'): void {
    this.toastMessage.set(message);
    this.toastType.set(type);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 3000);
  }

  public formatGenres(genres: Array<{id: string | null; name: string; description: string | null}>): string {
    return genres.slice(0, 3).map(g => g.name).join(', ');
  }
}
















// import { Component, OnInit, signal } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { Router } from '@angular/router';
// import { RecommendationService, RecommendedMovie } from '../../services/recommendation';
// import { Auth } from '../../services/auth';
// import {Movie} from '../../services/movie';
//
// @Component({
//   selector: 'app-recommendations',
//   imports: [CommonModule],
//   templateUrl: './recommendations.html',
//   styleUrl: './recommendations.css'
// })
// export class Recommendations implements OnInit {
//   public personalizedRecommendations = signal<Movie[]>([]);
//   public trendingRecommendations = signal<Movie[]>([]);
//   public genreBasedRecommendations = signal<Movie[]>([]);
//   public isLoading = signal(true);
//   public isAuthenticated = signal(false);
//   public selectedGenres = signal<string[]>([]);
//   public availableGenres = ['Action', 'Comedy', 'Drama', 'Thriller', 'Sci-Fi', 'Romance', 'Horror', 'Adventure'];
//   public selectedYearRange = signal<{ fromYear: number | null; toYear: number | null }>({ fromYear: null, toYear: null });
//   public availableYears: number[] = [];
//   public showYearFilter = signal(false);
//
//   constructor(
//     private recommendationService: RecommendationService,
//     private authService: Auth,
//     private router: Router
//   ) {
//     // Generate available years (from 1950 to current year)
//     const currentYear = new Date().getFullYear();
//     for (let year = currentYear; year >= 1950; year--) {
//       this.availableYears.push(year);
//     }
//   }
//
//   ngOnInit(): void {
//     this.isAuthenticated.set(this.authService.isAuthenticated());
//
//     if (this.isAuthenticated()) {
//       this.loadPersonalizedRecommendations();
//     } else {
//       this.loadGuestRecommendations();
//     }
//   }
//
//   private loadPersonalizedRecommendations(): void {
//     this.isLoading.set(true);
//
//     // Load personalized recommendations
//     this.recommendationService.getPersonalizedRecommendations(12).subscribe({
//       next: (response) => {
//         console.log(response)
//         if (response.success && response.data?.recommendations) {
//           // this.personalizedRecommendations.set(response.data.recommendations);
//           this.fetchMovieDetails(response.data.recommendations, this.personalizedRecommendations);
//
//         }
//         this.isLoading.set(false);
//       },
//       error: (error) => {
//         console.error('Error loading personalized recommendations:', error);
//         // Fallback to trending if personalized fails
//         this.loadTrendingRecommendations();
//       }
//     });
//
//     // Load watch history based recommendations
//     this.recommendationService.getWatchHistoryRecommendations(6).subscribe({
//       next: (response ) => {
//         if (response.success && response.data?.recommendations) {
//           // You could add these to a separate section
//         }
//       },
//       error: (error) => console.error('Error loading history recommendations:', error)
//     });
//   }
//
//   private loadGuestRecommendations(): void {
//     this.loadTrendingRecommendations();
//   }
//
//   private loadTrendingRecommendations(): void {
//     this.recommendationService.getTrendingRecommendations(12).subscribe({
//       next: (response) => {
//         if (response.success && response.data?.recommendations) {
//           // this.trendingRecommendations.set(response.data.recommendations);
//           this.fetchMovieDetails(response.data.recommendations, this.trendingRecommendations);
//         }
//         this.isLoading.set(false);
//       },
//       error: (error) => {
//         console.error('Error loading trending recommendations:', error);
//         this.isLoading.set(false);
//       }
//     });
//   }
//
//   public onGenreSelect(genre: string): void {
//     const current = this.selectedGenres();
//     if (current.includes(genre)) {
//       this.selectedGenres.set(current.filter(g => g !== genre));
//     } else {
//       this.selectedGenres.set([...current, genre]);
//     }
//
//     // Load genre-based recommendations if any genre is selected
//     if (this.selectedGenres().length > 0) {
//       this.loadGenreBasedRecommendations();
//     } else {
//       this.genreBasedRecommendations.set([]);
//     }
//   }
//
//   private loadGenreBasedRecommendations(): void {
//     const yearRange = this.selectedYearRange();
//     this.recommendationService.getGenreBasedRecommendations(
//       this.selectedGenres(),
//       12,
//       yearRange.fromYear,
//       yearRange.toYear
//     ).subscribe({
//       next: (response) => {
//         if (response.success && response.data?.recommendations) {
//           // this.genreBasedRecommendations.set(response.data.recommendations);
//           this.fetchMovieDetails(response.data.recommendations, this.genreBasedRecommendations);
//         }
//       },
//       error: (error) => console.error('Error loading genre recommendations:', error)
//     });
//   }
//
//   private fetchMovieDetails(recommendations: RecommendedMovie[], targetSignal: ReturnType<typeof signal<Movie[]>>): void {
//     const movieIds = recommendations.map(rec => rec.movieId).filter(Boolean);
//
//     if (movieIds.length === 0) {
//       targetSignal.set([]);
//       return;
//     }
//
//     // Fetch all movies in parallel
//     const movieRequests = movieIds.map(id =>
//       this.movieService.getMovieById(id)
//     );
//
//     forkJoin(movieRequests).subscribe({
//       next: (responses) => {
//         const movies = responses
//           .map(response => response.data)
//           .filter(movie => movie !== null && movie !== undefined);
//         targetSignal.set(movies);
//       },
//       error: (error) => {
//         console.error('Error fetching movie details:', error);
//         targetSignal.set([]);
//       }
//     });
//   }
//
//   public onMovieClick(movie: RecommendedMovie): void {
//     // Track user interaction
//     if (this.isAuthenticated()) {
//       this.recommendationService.trackUserInteraction({
//         movieId: movie.id,
//         action: 'view',
//         timestamp: new Date().toISOString()
//       }).subscribe();
//     }
//
//     this.router.navigate(['/movie', movie.id]);
//   }
//
//   public navigateToLogin(): void {
//     this.router.navigate(['/login'], {
//       queryParams: { returnUrl: '/recommendations' }
//     });
//   }
//
//   public formatRecommendationReason(reason: string): string {
//     return this.recommendationService.formatRecommendationReason(reason);
//   }
//
//   public getConfidenceColorClass(confidence: number): string {
//     return this.recommendationService.getConfidenceColorClass(confidence);
//   }
//
//   public refreshRecommendations(): void {
//     if (this.isAuthenticated()) {
//       this.isLoading.set(true);
//       this.recommendationService.refreshRecommendations().subscribe({
//         next: () => {
//           this.loadPersonalizedRecommendations();
//         },
//         error: (error) => {
//           console.error('Error refreshing recommendations:', error);
//           this.isLoading.set(false);
//         }
//       });
//     }
//   }
//
//   public toggleYearFilter(): void {
//     this.showYearFilter.set(!this.showYearFilter());
//   }
//
//   public onYearRangeChange(type: 'from' | 'to', year: number | null): void {
//     const currentRange = this.selectedYearRange();
//
//     if (type === 'from') {
//       this.selectedYearRange.set({ ...currentRange, fromYear: year });
//     } else {
//       this.selectedYearRange.set({ ...currentRange, toYear: year });
//     }
//
//     // Reload recommendations with year filter if any filters are active
//     if (this.selectedGenres().length > 0 || currentRange.fromYear !== null || currentRange.toYear !== null) {
//       this.loadFilteredRecommendations();
//     }
//   }
//
//   public clearYearFilter(): void {
//     this.selectedYearRange.set({ fromYear: null, toYear: null });
//
//     // Reload recommendations
//     if (this.selectedGenres().length > 0) {
//       this.loadGenreBasedRecommendations();
//     } else {
//       this.genreBasedRecommendations.set([]);
//     }
//   }
//
//   private loadFilteredRecommendations(): void {
//     const yearRange = this.selectedYearRange();
//
//     // For now, we'll use genre-based recommendations with year filtering
//     // The backend supports fromYear and toYear parameters
//     if (this.selectedGenres().length > 0) {
//       this.loadGenreBasedRecommendations();
//     }
//   }
//
//   public hasActiveFilters(): boolean {
//     const yearRange = this.selectedYearRange();
//     return this.selectedGenres().length > 0 || yearRange.fromYear !== null || yearRange.toYear !== null;
//   }
//
//   public clearAllFilters(): void {
//     this.selectedGenres.set([]);
//     this.selectedYearRange.set({ fromYear: null, toYear: null });
//     this.genreBasedRecommendations.set([]);
//   }
// }
