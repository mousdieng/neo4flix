import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-rating',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rating.html',
  styleUrls: ['./rating.css']
})
export class RatingComponent {
  @Input() rating: number = 0; // 0-10 scale (displayed as 5 stars, each star = 2 points)
  @Input() readonly: boolean = false;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showValue: boolean = true;
  @Input() allowHalf: boolean = true; // Enable half-stars by default (0.5 increments)

  @Output() ratingChange = new EventEmitter<number>(); // Emits 0-10 scale values

  public hoverRating = signal(0);
  public isHovering = signal(false);

  // 🟡 Get star size based on prop
  public get starSize(): string {
    switch (this.size) {
      case 'sm': return 'w-4 h-4';
      case 'lg': return 'w-8 h-8';
      default: return 'w-6 h-6';
    }
  }

  // ⭐ Get rating currently displayed in stars (0-5 scale for display)
  // Converts internal 0-10 scale to 0-5 stars for display
  public get displayRating(): number {
    const rating10Scale = this.isHovering() ? this.hoverRating() : this.rating;
    return rating10Scale / 2; // Convert 10-point to 5-star display
  }

  public get starsArray(): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  // ⭐ Determine the star fill type
  public getStarType(starIndex: number): 'full' | 'half' | 'empty' {
    if (this.displayRating >= starIndex) {
      return 'full';
    } else if (this.allowHalf && this.displayRating >= starIndex - 0.5) {
      return 'half';
    } else {
      return 'empty';
    }
  }

  // 🖱️ When clicking a star
  public onStarClick(starIndex: number): void {
    if (this.readonly) return;

    // Get star value (0-5 scale)
    const starValue = this.allowHalf
      ? this.hoverRating() / 2 || starIndex
      : starIndex;

    // Convert to 10-point scale before emitting
    const rating10Scale = starValue * 2;

    this.rating = rating10Scale;
    this.ratingChange.emit(rating10Scale); // Emit 0-10 scale value
  }

  // 🖱️ When hovering a star
  public onStarHover(starIndex: number): void {
    if (this.readonly) return;

    this.isHovering.set(true);
    // Store hover value in 10-point scale
    const starValue = this.allowHalf ? starIndex - 0.5 : starIndex;
    this.hoverRating.set(starValue * 2); // Convert to 10-point scale
  }

  public onMouseLeave(): void {
    if (this.readonly) return;

    this.isHovering.set(false);
    this.hoverRating.set(0);
  }

  // 🔢 Rating text (e.g., "8.5/10")
  public getRatingText(): string {
    if (this.rating === null || this.rating === undefined || this.rating === 0) {
      return 'N/A';
    }
    return `${this.rating.toFixed(1)}/10`; // Display on 10-point scale
  }

  // 🎨 Rating color (based on 10-point scale)
  public getRatingColor(): string {
    if (this.rating >= 8) return 'text-green-500';   // 8-10: Great
    if (this.rating >= 6) return 'text-yellow-500';  // 6-7.9: Good
    if (this.rating >= 4) return 'text-orange-500';  // 4-5.9: Average
    return 'text-red-500';                            // 0-3.9: Poor
  }
}
