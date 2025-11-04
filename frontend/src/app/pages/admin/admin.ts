import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../services/auth';
import { AdminService, UserProfile, PagedUsers, Movie, PagedMovies, CreateMovieRequest, UpdateMovieRequest, SystemStats } from '../../services/admin';
import { MovieService } from '../../services/movie';
import { DialogService } from '../../components/dialog/dialog.service';
import { ToastService } from '../../components/toast/toast.service';

@Component({
  selector: 'app-admin',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class Admin implements OnInit {
  public currentUser = computed(() => this.authService.getCurrentUser());
  public stats = signal<SystemStats>({
    totalMovies: 0,
    totalGenres: 0,
    totalActors: 0,
    totalDirectors: 0,
    averageRating: 0
  });

  // UI State
  public activeTab = signal<'dashboard' | 'users' | 'movies'>('dashboard');
  public loading = signal(false);
  public error = signal<string | null>(null);

  // User Management
  public users = signal<UserProfile[]>([]);
  public usersPage = signal(0);
  public usersTotalPages = signal(0);
  public userSearchQuery = signal('');

  // Movie Management
  public movies = signal<Movie[]>([]);
  public moviesPage = signal(0);
  public moviesTotalPages = signal(0);
  public movieSearchQuery = signal('');
  public showMovieForm = signal(false);
  public editingMovie = signal<Movie | null>(null);

  // Movie Form
  public movieForm = signal<CreateMovieRequest>({
    title: '',
    plot: '',
    releaseYear: new Date().getFullYear(),
    duration: 0,
    language: '',
    country: '',
    budget: 0,
    boxOffice: 0,
    posterUrl: '',
    trailerUrl: ''
  });

  // File upload state
  public selectedPosterFile: File | null = null;
  public selectedTrailerFile: File | null = null;
  public uploadingPoster = signal(false);
  public uploadingTrailer = signal(false);
  public posterPreviewUrl = signal<string | null>(null);
  public trailerPreviewUrl = signal<string | null>(null);

  constructor(
    private authService: Auth,
    private adminService: AdminService,
    private movieService: MovieService,
    private dialogService: DialogService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading.set(true);
    this.adminService.getMovieStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading stats:', err);
        this.error.set('Failed to load statistics');
        this.loading.set(false);
      }
    });
  }

  switchTab(tab: 'dashboard' | 'users' | 'movies'): void {
    this.activeTab.set(tab);
    if (tab === 'users' && this.users().length === 0) {
      this.loadUsers();
    } else if (tab === 'movies' && this.movies().length === 0) {
      this.loadMovies();
    }
  }

  // User Management Methods
  loadUsers(page: number = 0): void {
    this.loading.set(true);
    this.adminService.getAllUsers(page, 20, this.userSearchQuery() || undefined).subscribe({
      next: (response: PagedUsers) => {
        this.users.set(response.content);
        this.usersPage.set(response.number);
        this.usersTotalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.error.set('Failed to load users');
        this.loading.set(false);
      }
    });
  }

  searchUsers(): void {
    this.loadUsers(0);
  }

  async deleteUser(userId: string, username: string): Promise<void> {
    const result = await this.dialogService.confirm(
      `Are you sure you want to delete user "${username}"? This action cannot be undone.`,
      'Delete User'
    );

    if (!result.confirmed) {
      return;
    }

    this.adminService.deleteUser(userId).subscribe({
      next: () => {
        this.toastService.success(`User "${username}" deleted successfully`);
        this.loadUsers(this.usersPage());
      },
      error: (err) => {
        console.error('Error deleting user:', err);
        this.toastService.error('Failed to delete user');
      }
    });
  }

  toggleUserStatus(user: UserProfile): void {
    this.adminService.toggleUserStatus(user.id, !user.enabled).subscribe({
      next: (updatedUser) => {
        const users = this.users();
        const index = users.findIndex(u => u.id === user.id);
        if (index !== -1) {
          users[index] = updatedUser;
          this.users.set([...users]);
        }
        const status = updatedUser.enabled ? 'enabled' : 'disabled';
        this.toastService.success(`User ${updatedUser.username} ${status} successfully`);
      },
      error: (err) => {
        console.error('Error toggling user status:', err);
        this.toastService.error('Failed to update user status');
      }
    });
  }

  // Movie Management Methods
  loadMovies(page: number = 0): void {
    this.loading.set(true);
    this.adminService.getAllMovies(page, 20).subscribe({
      next: (response: PagedMovies) => {
        this.movies.set(response.data.movies);
        this.moviesPage.set(response.data.currentPage);
        this.moviesTotalPages.set(response.data.totalPages);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading movies:', err);
        this.error.set('Failed to load movies');
        this.loading.set(false);
      }
    });
  }

  searchMovies(): void {
    if (this.movieSearchQuery()) {
      this.adminService.searchMovies(this.movieSearchQuery()).subscribe({
        next: (response: PagedMovies) => {
          this.movies.set(response.data.movies);
          this.moviesPage.set(response.data.currentPage);
          this.moviesTotalPages.set(response.data.totalPages);
        },
        error: (err) => {
          console.error('Error searching movies:', err);
          this.toastService.error('Failed to search movies');
        }
      });
    } else {
      this.loadMovies(0);
    }
  }

  openMovieForm(movie?: Movie): void {
    if (movie) {
      this.editingMovie.set(movie);
      this.movieForm.set({
        title: movie.title,
        plot: movie.plot,
        releaseYear: movie.releaseYear,
        duration: movie.duration,
        language: movie.language,
        country: movie.country,
        budget: movie.budget,
        boxOffice: movie.boxOffice,
        posterUrl: movie.posterUrl,
        trailerUrl: movie.trailerUrl
      });
    } else {
      this.editingMovie.set(null);
      this.movieForm.set({
        title: '',
        plot: '',
        releaseYear: new Date().getFullYear(),
        duration: 0,
        language: '',
        country: '',
        budget: 0,
        boxOffice: 0,
        posterUrl: '',
        trailerUrl: ''
      });
    }
    this.showMovieForm.set(true);
  }

  closeMovieForm(): void {
    this.showMovieForm.set(false);
    this.editingMovie.set(null);
    this.selectedPosterFile = null;
    this.selectedTrailerFile = null;
    this.posterPreviewUrl.set(null);
    this.trailerPreviewUrl.set(null);
  }

  async saveMovie(): Promise<void> {
    const movie = this.movieForm();
    const editing = this.editingMovie();

    // Warn if uploading file will overwrite existing URL
    if (this.selectedPosterFile && movie.posterUrl) {
      const result = await this.dialogService.confirm(
        'You have a poster URL and a file selected. The uploaded file will replace the URL. Continue?',
        'Replace Poster URL'
      );

      if (!result.confirmed) {
        return;
      }
    }

    if (editing) {
      // Update existing movie with files
      this.adminService.updateMovie(editing.id, movie, this.selectedPosterFile || undefined, this.selectedTrailerFile || undefined).subscribe({
        next: (updatedMovie) => {
          this.toastService.success(`Movie "${updatedMovie.title}" updated successfully`);
          this.closeMovieForm();
          this.loadMovies(this.moviesPage());
        },
        error: (err) => {
          console.error('Error updating movie:', err);
          this.toastService.error('Failed to update movie');
        }
      });
    } else {
      // Create new movie with files
      this.adminService.createMovie(movie, this.selectedPosterFile || undefined, this.selectedTrailerFile || undefined).subscribe({
        next: (createdMovie) => {
          this.toastService.success(`Movie "${createdMovie.title}" created successfully`);
          this.closeMovieForm();
          this.loadMovies(this.moviesPage());
        },
        error: (err) => {
          console.error('Error creating movie:', err);
          this.toastService.error('Failed to create movie');
        }
      });
    }
  }

  async deleteMovie(movieId: string, title: string): Promise<void> {
    const result = await this.dialogService.confirm(
      `Are you sure you want to delete movie "${title}"? This action cannot be undone.`,
      'Delete Movie'
    );

    if (!result.confirmed) {
      return;
    }

    this.adminService.deleteMovie(movieId).subscribe({
      next: () => {
        this.toastService.success(`Movie "${title}" deleted successfully`);
        this.loadMovies(this.moviesPage());
      },
      error: (err) => {
        console.error('Error deleting movie:', err);
        this.toastService.error('Failed to delete movie');
      }
    });
  }

  nextPage(type: 'users' | 'movies'): void {
    if (type === 'users' && this.usersPage() < this.usersTotalPages() - 1) {
      this.loadUsers(this.usersPage() + 1);
    } else if (type === 'movies' && this.moviesPage() < this.moviesTotalPages() - 1) {
      this.loadMovies(this.moviesPage() + 1);
    }
  }

  previousPage(type: 'users' | 'movies'): void {
    if (type === 'users' && this.usersPage() > 0) {
      this.loadUsers(this.usersPage() - 1);
    } else if (type === 'movies' && this.moviesPage() > 0) {
      this.loadMovies(this.moviesPage() - 1);
    }
  }

  onPosterFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.toastService.error('Please select an image file for the poster');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.toastService.error('Poster file size must be less than 5MB');
        return;
      }

      this.selectedPosterFile = file;

      // Create preview URL
      const reader = new FileReader();
      reader.onload = (e) => {
        this.posterPreviewUrl.set(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  onTrailerFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Validate file type
      if (!file.type.startsWith('video/')) {
        this.toastService.error('Please select a video file for the trailer');
        return;
      }

      // Validate file size (max 50MB)
      if (file.size > 50 * 1024 * 1024) {
        this.toastService.error('Trailer file size must be less than 50MB');
        return;
      }

      this.selectedTrailerFile = file;

      // Create preview URL
      const url = URL.createObjectURL(file);
      this.trailerPreviewUrl.set(url);
    }
  }
}
