import { Component, Input, Output, EventEmitter, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RatingComponent } from '../rating/rating';
import { RatingService, Rating, RatingRequest } from '../../services/rating';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-review',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RatingComponent],
  templateUrl: './review.html',
  styleUrl: './review.css'
})
export class ReviewComponent implements OnInit {
  @Input() movieId!: string;
  @Input() existingRating?: Rating;
  @Input() readonly: boolean = false;

  @Output() reviewSubmitted = new EventEmitter<Rating>();
  @Output() reviewDeleted = new EventEmitter<void>();

  public reviewForm: FormGroup;
  public isSubmitting = signal(false);
  public error = signal<string | null>(null);
  public isEditing = signal(false);

  constructor(
    private fb: FormBuilder,
    private ratingService: RatingService,
    private authService: Auth
  ) {
    this.reviewForm = this.fb.group({
      rating: [0, [Validators.required, Validators.min(0.5), Validators.max(10)]],
      review: ['', [Validators.maxLength(1000)]]
    });
  }

  ngOnInit(): void {
    if (this.existingRating) {
      this.reviewForm.patchValue({
        rating: this.existingRating.rating,
        review: this.existingRating.review || ''
      });
    }
  }

  public get canEdit(): boolean {
    const currentUser = this.authService.getCurrentUser();
    return !this.readonly && currentUser !== null;
  }

  public get hasExistingRating(): boolean {
    return !!this.existingRating;
  }

  public onRatingChange(rating: number): void {
    this.reviewForm.patchValue({ rating });
  }

  public onSubmit(): void {
    if (this.reviewForm.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.error.set(null);

    const ratingRequest: RatingRequest = {
      movieId: this.movieId,
      rating: this.reviewForm.value.rating,
      review: this.reviewForm.value.review?.trim() || undefined
    };

    const request = this.existingRating
      ? this.ratingService.updateRating(this.existingRating.id, ratingRequest)
      : this.ratingService.submitRating(ratingRequest);

    request.subscribe({
      next: (response: Rating) => {
        this.isSubmitting.set(false);
        // Backend returns Rating directly, not wrapped in {success, data}
        this.reviewSubmitted.emit(response);
        this.isEditing.set(false);
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.error.set(error?.error?.message || 'An error occurred while submitting your review');
      }
    });
  }

  public onDelete(): void {
    if (!this.existingRating || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.error.set(null);

    this.ratingService.deleteRating(this.existingRating.id).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        // Check if response indicates success (if it returns an object with success field)
        // Otherwise, successful completion means it worked (204 No Content)
        if (!response || response.success !== false) {
          this.reviewDeleted.emit();
        } else {
          this.error.set(response.message || 'Failed to delete review');
        }
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.error.set(error?.error?.message || 'An error occurred while deleting your review');
      }
    });
  }

  public startEditing(): void {
    this.isEditing.set(true);
  }

  public cancelEditing(): void {
    this.isEditing.set(false);
    if (this.existingRating) {
      this.reviewForm.patchValue({
        rating: this.existingRating.rating,
        review: this.existingRating.review || ''
      });
    }
  }

  public formatDate(dateString: string | Date | undefined): string {
    if (!dateString) return 'Unknown date';
    const date = typeof dateString === 'string' ? new Date(dateString) : dateString;
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  public getCharacterCount(): number {
    return this.reviewForm.value.review?.length || 0;
  }

  public isCharacterLimitNear(): boolean {
    return this.getCharacterCount() > 800;
  }
}