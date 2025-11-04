import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RecommendationService, SharedRecommendation } from '../../services/recommendation';

@Component({
  selector: 'app-shared-recommendations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shared-recommendations.component.html',
  styleUrls: ['./shared-recommendations.component.css']
})
export class SharedRecommendationsComponent implements OnInit {
  sharedRecommendations = signal<SharedRecommendation[]>([]);
  unviewedCount = signal<number>(0);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  constructor(
    private recommendationService: RecommendationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSharedRecommendations();
    this.loadUnviewedCount();
  }

  loadSharedRecommendations(): void {
    this.loading.set(true);
    this.error.set(null);

    this.recommendationService.getSharedRecommendations().subscribe({
      next: (shared) => {
        console.log('Received shared recommendations:', shared);
        console.log('Number of shared recommendations:', shared?.length);
        this.sharedRecommendations.set(shared);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading shared recommendations:', err);
        console.error('Error details:', err.error);
        console.error('Error status:', err.status);
        this.error.set('Failed to load shared recommendations');
        this.loading.set(false);
      }
    });
  }

  loadUnviewedCount(): void {
    this.recommendationService.getUnviewedSharedCount().subscribe({
      next: (data) => {
        this.unviewedCount.set(data.count);
      },
      error: (err) => {
        console.error('Error loading unviewed count:', err);
      }
    });
  }

  markAsViewed(shared: SharedRecommendation): void {
    if (shared.viewed) return;

    this.recommendationService.markSharedRecommendationAsViewed(shared.id).subscribe({
      next: () => {
        // Update the local state
        const updated = this.sharedRecommendations().map(s =>
          s.id === shared.id ? { ...s, viewed: true, viewedAt: new Date().toISOString() } : s
        );
        this.sharedRecommendations.set(updated);
        this.loadUnviewedCount();
      },
      error: (err) => {
        console.error('Error marking as viewed:', err);
      }
    });
  }

  viewMovie(shared: SharedRecommendation): void {
    this.markAsViewed(shared);
    this.router.navigate(['/movies', shared.movie.id]);
  }

  getTimeAgo(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    const weeks = Math.floor(days / 7);
    if (weeks < 4) return `${weeks}w ago`;
    const months = Math.floor(days / 30);
    if (months < 12) return `${months}mo ago`;
    const years = Math.floor(days / 365);
    return `${years}y ago`;
  }
}
