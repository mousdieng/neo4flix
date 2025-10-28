import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { MovieService, Movie, SearchCriteria, MovieResponse } from '../../services/movie';
import { RatingComponent } from '../../components/rating/rating';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-movies',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RatingComponent],
  templateUrl: './movies.html',
  styleUrl: './movies.css'
})
export class Movies implements OnInit {
  public movies = signal<Movie[]>([]);
  public isLoading = signal(false);
  public searchForm: FormGroup;
  public genres = signal<string[]>([]);
  public currentPage = signal(0);
  public totalPages = signal(0);
  public hasNext = signal(false);
  public hasPrevious = signal(false);
  public showAdvancedFilters = signal(false);

  private searchSubject = new Subject<SearchCriteria>();
  public readonly sortOptions = [
    { value: 'releaseYear', label: 'Release Year' },
    { value: 'title', label: 'Title' },
    { value: 'averageRating', label: 'Rating' },
    { value: 'duration', label: 'Duration' }
  ];

  constructor(
    private movieService: MovieService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.searchForm = this.fb.group({
      // Text search
      query: [''],
      title: [''],

      // Genre filter
      genre: [''],

      // People filters
      director: [''],
      actor: [''],

      // Year filters
      year: [''],
      minYear: [''],
      maxYear: [''],

      // Rating filters
      minRating: [''],
      maxRating: [''],

      // Metadata filters
      language: [''],
      country: [''],
      minDuration: [''],
      maxDuration: [''],

      // Budget/Revenue filters
      minBudget: [''],
      maxBudget: [''],
      minBoxOffice: [''],
      maxBoxOffice: [''],

      // Sorting
      sortBy: ['releaseYear'],
      sortOrder: ['desc']
    });
  }

  ngOnInit(): void {
    this.loadGenres();
    this.loadPopularMovies();
    this.setupSearch();
  }

  private setupSearch(): void {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(criteria => {
          this.isLoading.set(true);
          return this.movieService.searchMovies(criteria);
        })
      )
      .subscribe({
        next: (response: MovieResponse) => {
          console.log(response)
          this.handleMovieResponse(response);
        },
        error: (error) => {
          console.error('Search error:', error);
          this.isLoading.set(false);
        }
      });

    this.searchForm.valueChanges.subscribe(() => {
      this.onSearch();
    });
  }

  private loadGenres(): void {
    this.movieService.getGenres().subscribe({
      next: (response) => {
        if (response.success) {
          this.genres.set(response.data);
        }
      },
      error: (error) => console.error('Error loading genres:', error)
    });
  }

  private loadPopularMovies(): void {
    this.isLoading.set(true);
    this.movieService.getPopularMovies().subscribe({
      next: (response: MovieResponse) => {
        console.log(response)
        this.handleMovieResponse(response);
      },
      error: (error) => {
        console.error('Error loading popular movies:', error);
        this.isLoading.set(false);
      }
    });
  }

  private handleMovieResponse(response: MovieResponse): void {
    if (response.success) {
      this.movies.set(response.data.movies);
      this.currentPage.set(response.data.currentPage);
      this.totalPages.set(response.data.totalPages);
      this.hasNext.set(response.data.hasNext);
      this.hasPrevious.set(response.data.hasPrevious);
    }
    this.isLoading.set(false);
  }

  public onSearch(): void {
    const criteria: SearchCriteria = {
      ...this.searchForm.value,
      page: 0
    };
    this.searchSubject.next(criteria);
  }

  public onMovieClick(movie: Movie): void {
    this.router.navigate(['/movie', movie.id]);
  }

  public onGenreFilter(genre: string): void {
    this.searchForm.patchValue({ genre, query: '' });
    this.onSearch();
  }

  public loadPage(page: number): void {
    const criteria: SearchCriteria = {
      ...this.searchForm.value,
      page
    };
    this.searchSubject.next(criteria);
  }

  public nextPage(): void {
    if (this.hasNext()) {
      this.loadPage(this.currentPage() + 1);
    }
  }

  public previousPage(): void {
    if (this.hasPrevious()) {
      this.loadPage(this.currentPage() - 1);
    }
  }

  public clearSearch(): void {
    this.searchForm.reset({
      query: '',
      title: '',
      genre: '',
      director: '',
      actor: '',
      year: '',
      minYear: '',
      maxYear: '',
      minRating: '',
      maxRating: '',
      language: '',
      country: '',
      minDuration: '',
      maxDuration: '',
      minBudget: '',
      maxBudget: '',
      minBoxOffice: '',
      maxBoxOffice: '',
      sortBy: 'releaseYear',
      sortOrder: 'desc'
    });
    this.loadPopularMovies();
  }

  public toggleAdvancedFilters(): void {
    this.showAdvancedFilters.update(value => !value);
  }

  public getMovieRating(rating: number): string {
    return (rating / 2).toFixed(1);
  }

  public getStarRating(rating: number): number[] {
    const stars = Math.round(rating / 2);
    return Array(5).fill(0).map((_, i) => i < stars ? 1 : 0);
  }

  public formatDuration(minutes: number): string {
    if (!minutes) return '';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours === 0) return `${mins}m`;
    if (mins === 0) return `${hours}h`;
    return `${hours}h ${mins}m`;
  }
}
