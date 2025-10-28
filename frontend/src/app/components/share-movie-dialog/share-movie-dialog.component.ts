import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FriendService, Friend } from '../../services/friend';

export interface ShareMovieDialogData {
  movieId: string;
  movieTitle: string;
}

export interface ShareMovieDialogResult {
  selectedFriends: string[];
  message: string;
}

@Component({
  selector: 'app-share-movie-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './share-movie-dialog.component.html',
  styleUrls: ['./share-movie-dialog.component.css']
})
export class ShareMovieDialogComponent implements OnInit {
  friends = signal<Friend[]>([]);
  selectedFriends = signal<Set<string>>(new Set());
  message = signal<string>('');
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  constructor(
    public dialogRef: MatDialogRef<ShareMovieDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ShareMovieDialogData,
    private friendService: FriendService
  ) {}

  ngOnInit(): void {
    this.loadFriends();
  }

  loadFriends(): void {
    this.loading.set(true);
    this.error.set(null);

    this.friendService.getMyFriends().subscribe({
      next: (friends) => {
        this.friends.set(friends);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading friends:', err);
        this.error.set('Failed to load friends. Please try again.');
        this.loading.set(false);
      }
    });
  }

  toggleFriend(friendId: string): void {
    const selected = new Set(this.selectedFriends());
    if (selected.has(friendId)) {
      selected.delete(friendId);
    } else {
      selected.add(friendId);
    }
    this.selectedFriends.set(selected);
  }

  isFriendSelected(friendId: string): boolean {
    return this.selectedFriends().has(friendId);
  }

  selectAll(): void {
    const allFriendIds = new Set(this.friends().map(f => f.id));
    this.selectedFriends.set(allFriendIds);
  }

  deselectAll(): void {
    this.selectedFriends.set(new Set());
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onShare(): void {
    if (this.selectedFriends().size === 0) {
      this.error.set('Please select at least one friend');
      return;
    }

    const result: ShareMovieDialogResult = {
      selectedFriends: Array.from(this.selectedFriends()),
      message: this.message()
    };

    this.dialogRef.close(result);
  }

  get canShare(): boolean {
    return this.selectedFriends().size > 0;
  }
}
