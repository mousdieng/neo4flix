import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  WatchlistService,
  WatchlistItem,
  WatchlistQueryParams,
  WatchlistStats
} from '../../services/watchlist';
import { RecommendationService } from '../../services/recommendation';
import { MovieService } from '../../services/movie';
import { DialogService } from '../../components/dialog/dialog.service';

@Component({
  selector: 'app-watchlist',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './watchlist.html',
  styleUrl: './watchlist.css'
})
export class Watchlist implements OnInit {
  // Expose Math to template
  public readonly Math = Math;

  // State
  public watchlistItems = signal<WatchlistItem[]>([]);
  public stats = signal<WatchlistStats | null>(null);
  public isLoading = signal(true);
  public isEmpty = signal(false);

  // Pagination
  public currentPage = signal(0);
  public pageSize = signal(20);
  public totalPages = signal(0);
  public totalItems = signal(0);
  public hasNext = signal(false);
  public hasPrevious = signal(false);

  // Filters & Sorting
  public searchQuery = signal('');
  public selectedPriority = signal<number | undefined>(undefined);
  public showWatched = signal<boolean | undefined>(undefined);
  public sortBy = signal<'addedAt' | 'priority' | 'title'>('addedAt');
  public sortDirection = signal<'ASC' | 'DESC'>('DESC');

  // UI State
  public showFilters = signal(false);
  public showConfirmDialog = signal(false);
  public itemToRemove = signal<WatchlistItem | null>(null);
  public toastMessage = signal<string | null>(null);
  public toastType = signal<'success' | 'error' | 'info'>('success');

  // Computed
  public filteredItems = computed(() => {
    const items = this.watchlistItems();
    const query = this.searchQuery().toLowerCase();

    if (!query) return items;

    return items.filter(item =>
      item.movie?.title.toLowerCase().includes(query) ||
      item.movieId.toLowerCase().includes(query)
    );
  });

  constructor(
    private watchlistService: WatchlistService,
    private recommendationService: RecommendationService,
    private movieService: MovieService,
    private router: Router,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {
    this.loadWatchlist();
  }

  private loadWatchlist(): void {
    this.isLoading.set(true);

    const params: WatchlistQueryParams = {
      page: this.currentPage(),
      pageSize: this.pageSize(),
      sortBy: this.sortBy(),
      sortDirection: this.sortDirection()
    };

    if (this.selectedPriority() !== undefined) {
      params.priority = this.selectedPriority();
    }

    if (this.showWatched() !== undefined) {
      params.watched = this.showWatched();
    }

    this.watchlistService.getWatchlist(params).subscribe({
      next: (response) => {
        // Update stats and pagination first
        this.stats.set(response.stats);
        this.totalItems.set(response.totalItems);
        this.totalPages.set(response.totalPages);
        this.hasNext.set(response.hasNext);
        this.hasPrevious.set(response.hasPrevious);
        this.isEmpty.set(response.items.length === 0);

        // If no items, we're done
        if (response.items.length === 0) {
          this.watchlistItems.set([]);
          this.isLoading.set(false);
          return;
        }

        // Fetch movie details for each watchlist item
        const movieRequests = response.items.map(item =>
          this.movieService.getMovieById(item.movieId).pipe(
            catchError(error => {
              console.error(`Error loading movie ${item.movieId}:`, error);
              return of({ success: false, data: null });
            })
          )
        );

        // Fetch all movies in parallel
        forkJoin(movieRequests).subscribe({
          next: (movieResponses) => {
            // Combine watchlist items with movie details
            const itemsWithMovies: WatchlistItem[] = response.items.map((item, index) => {
              const movieResponse = movieResponses[index];
              return {
                ...item,
                movie: movieResponse.success && movieResponse.data ? movieResponse.data : undefined
              };
            });

            this.watchlistItems.set(itemsWithMovies);
            this.isLoading.set(false);
          },
          error: (error) => {
            console.error('Error loading movie details:', error);
            // Still show watchlist items even if movie fetch fails
            this.watchlistItems.set(response.items);
            this.isLoading.set(false);
          }
        });
      },
      error: (error) => {
        console.error('Error loading watchlist:', error);
        this.showToast('Failed to load watchlist', 'error');
        this.isLoading.set(false);
        this.isEmpty.set(true);
      }
    });
  }

  // Pagination
  public nextPage(): void {
    if (this.hasNext()) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadWatchlist();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  public previousPage(): void {
    if (this.hasPrevious()) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadWatchlist();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  public goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadWatchlist();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  // Sorting
  public changeSortBy(sortBy: 'addedAt' | 'priority' | 'title'): void {
    if (this.sortBy() === sortBy) {
      // Toggle direction
      this.sortDirection.set(this.sortDirection() === 'ASC' ? 'DESC' : 'ASC');
    } else {
      this.sortBy.set(sortBy);
      this.sortDirection.set('DESC');
    }
    this.currentPage.set(0);
    this.loadWatchlist();
  }

  // Filtering
  public filterByPriority(priority: number | undefined): void {
    this.selectedPriority.set(priority);
    this.currentPage.set(0);
    this.loadWatchlist();
  }

  public filterByWatched(watched: boolean | undefined): void {
    this.showWatched.set(watched);
    this.currentPage.set(0);
    this.loadWatchlist();
  }

  public clearFilters(): void {
    this.selectedPriority.set(undefined);
    this.showWatched.set(undefined);
    this.searchQuery.set('');
    this.currentPage.set(0);
    this.loadWatchlist();
  }

  public toggleFilters(): void {
    this.showFilters.set(!this.showFilters());
  }

  // Search
  public onSearchChange(query: string): void {
    this.searchQuery.set(query);
  }

  // Priority Management
  public changePriority(item: WatchlistItem, priority: number, event: Event): void {
    event.stopPropagation();

    this.watchlistService.updateWatchlistEntry(item.movieId, { priority }).subscribe({
      next: () => {
        this.showToast(`Priority updated to ${this.getPriorityLabel(priority)}`, 'success');
        this.loadWatchlist();
      },
      error: (error) => {
        console.error('Error updating priority:', error);
        this.showToast('Failed to update priority', 'error');
      }
    });
  }

  // Watched Status
  public toggleWatched(item: WatchlistItem, event: Event): void {
    event.stopPropagation();

    const newStatus = !item.watched;
    this.watchlistService.markAsWatched(item.movieId, newStatus).subscribe({
      next: () => {
        this.showToast(newStatus ? 'Marked as watched' : 'Marked as unwatched', 'success');
        this.loadWatchlist();
      },
      error: (error) => {
        console.error('Error updating watched status:', error);
        this.showToast('Failed to update watched status', 'error');
      }
    });
  }

  // Remove
  public confirmRemove(item: WatchlistItem, event: Event): void {
    event.stopPropagation();
    this.itemToRemove.set(item);
    this.showConfirmDialog.set(true);
  }

  public cancelRemove(): void {
    this.showConfirmDialog.set(false);
    this.itemToRemove.set(null);
  }

  public removeFromWatchlist(): void {
    const item = this.itemToRemove();
    if (!item) return;

    this.watchlistService.removeFromWatchlist(item.movieId).subscribe({
      next: () => {
        this.showToast('Removed from watchlist', 'success');
        this.showConfirmDialog.set(false);
        this.itemToRemove.set(null);
        this.loadWatchlist();
      },
      error: (error) => {
        console.error('Error removing from watchlist:', error);
        this.showToast('Failed to remove from watchlist', 'error');
        this.showConfirmDialog.set(false);
        this.itemToRemove.set(null);
      }
    });
  }

  // Clear Watched
  public async clearWatchedMovies(): Promise<void> {
    const result = await this.dialogService.confirm(
      'Are you sure you want to remove all watched movies from your watchlist?',
      'Clear Watched Movies'
    );

    if (!result.confirmed) {
      return;
    }

    this.watchlistService.clearWatchedMovies().subscribe({
      next: () => {
        this.showToast('Cleared all watched movies', 'success');
        this.loadWatchlist();
      },
      error: (error) => {
        console.error('Error clearing watched movies:', error);
        this.showToast('Failed to clear watched movies', 'error');
      }
    });
  }

  // Navigation
  public onMovieClick(item: WatchlistItem): void {
    this.recommendationService.trackUserInteraction({
      movieId: item.movieId,
      action: 'view',
      timestamp: new Date().toISOString()
    }).subscribe();

    this.router.navigate(['/movie', item.movieId]);
  }

  public navigateToMovies(): void {
    this.router.navigate(['/movies']);
  }

  // Toast Notification
  private showToast(message: string, type: 'success' | 'error' | 'info' = 'success'): void {
    this.toastMessage.set(message);
    this.toastType.set(type);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 3000);
  }

  // Helpers
  public formatAddedDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return 'Added today';
    } else if (diffDays === 1) {
      return 'Added yesterday';
    } else if (diffDays < 7) {
      return `Added ${diffDays} days ago`;
    } else if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return `Added ${weeks} week${weeks > 1 ? 's' : ''} ago`;
    } else {
      return `Added on ${date.toLocaleDateString()}`;
    }
  }

  public getPriorityLabel(priority: number): string {
    switch (priority) {
      case 1: return 'High';
      case 2: return 'Medium';
      case 3: return 'Low';
      default: return 'Medium';
    }
  }

  public getPriorityColor(priority: number): string {
    switch (priority) {
      case 1: return 'text-red-500';
      case 2: return 'text-yellow-500';
      case 3: return 'text-green-500';
      default: return 'text-gray-500';
    }
  }

  public getPriorityBgColor(priority: number): string {
    switch (priority) {
      case 1: return 'bg-red-500/10 border-red-500/30';
      case 2: return 'bg-yellow-500/10 border-yellow-500/30';
      case 3: return 'bg-green-500/10 border-green-500/30';
      default: return 'bg-gray-500/10 border-gray-500/30';
    }
  }

  public getPageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: number[] = [];

    if (total <= 7) {
      for (let i = 0; i < total; i++) {
        pages.push(i);
      }
    } else {
      if (current < 4) {
        for (let i = 0; i < 5; i++) pages.push(i);
        pages.push(-1); // ellipsis
        pages.push(total - 1);
      } else if (current >= total - 4) {
        pages.push(0);
        pages.push(-1);
        for (let i = total - 5; i < total; i++) pages.push(i);
      } else {
        pages.push(0);
        pages.push(-1);
        for (let i = current - 1; i <= current + 1; i++) pages.push(i);
        pages.push(-1);
        pages.push(total - 1);
      }
    }

    return pages;
  }
}
