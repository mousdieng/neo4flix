import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FriendService, Friend, FriendRequest } from '../../services/friend';
import { UserService, UserProfile } from '../../services/user';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DialogService } from '../../components/dialog/dialog.service';
import { ToastService } from '../../components/toast/toast.service';

@Component({
  selector: 'app-friends',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './friends.html',
  styleUrl: './friends.css'
})
export class Friends implements OnInit {
  public friends = signal<Friend[]>([]);
  public searchResults = signal<UserProfile[]>([]);
  public pendingRequests = signal<FriendRequest[]>([]);
  public sentRequests = signal<FriendRequest[]>([]);
  public isLoading = signal(true);
  public isSearching = signal(false);
  public searchQuery = signal('');
  public activeTab = signal<'friends' | 'requests' | 'search'>('friends');

  constructor(
    private friendService: FriendService,
    private userService: UserService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialogService: DialogService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadFriends();
    this.loadPendingRequests();
    this.loadSentRequests();
  }

  private loadFriends(): void {
    this.isLoading.set(true);
    this.friendService.getMyFriends().subscribe({
      next: (friends) => {
        this.friends.set(friends);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading friends:', error);
        this.isLoading.set(false);
      }
    });
  }

  public onSearch(): void {
    const query = this.searchQuery().trim();
    if (query.length < 2) {
      this.searchResults.set([]);
      return;
    }

    this.isSearching.set(true);
    this.userService.searchUsers(query).subscribe({
      next: (users) => {
        this.searchResults.set(users);
        this.isSearching.set(false);
      },
      error: (error) => {
        console.error('Error searching users:', error);
        this.isSearching.set(false);
      }
    });
  }

  public addFriend(userId: string): void {
    this.friendService.addFriend(userId).subscribe({
      next: (response) => {
        this.snackBar.open(response.message || 'Friend added successfully!', 'OK', { duration: 3000 });
        this.loadFriends();
        this.searchResults.update(results => results.filter(u => u.id !== userId));
      },
      error: (error) => {
        console.error('Error adding friend:', error);
        this.snackBar.open('Failed to add friend', 'OK', { duration: 3000 });
      }
    });
  }

  public async removeFriend(friendId: string): Promise<void> {
    const result = await this.dialogService.confirm(
      'Are you sure you want to remove this friend?',
      'Remove Friend'
    );

    if (!result.confirmed) {
      return;
    }

    this.friendService.removeFriend(friendId).subscribe({
      next: (response) => {
        this.toastService.success(response.message || 'Friend removed');
        this.friends.update(friends => friends.filter(f => f.id !== friendId));
      },
      error: (error) => {
        console.error('Error removing friend:', error);
        this.toastService.error('Failed to remove friend');
      }
    });
  }

  public switchTab(tab: 'friends' | 'requests' | 'search'): void {
    this.activeTab.set(tab);
    if (tab === 'search') {
      this.searchQuery.set('');
      this.searchResults.set([]);
    } else if (tab === 'requests') {
      this.loadPendingRequests();
      this.loadSentRequests();
    }
  }

  private loadPendingRequests(): void {
    this.friendService.getPendingRequests().subscribe({
      next: (requests) => {
        this.pendingRequests.set(requests);
      },
      error: (error) => {
        console.error('Error loading pending requests:', error);
      }
    });
  }

  private loadSentRequests(): void {
    this.friendService.getSentRequests().subscribe({
      next: (requests) => {
        this.sentRequests.set(requests);
      },
      error: (error) => {
        console.error('Error loading sent requests:', error);
      }
    });
  }

  public acceptRequest(requestId: string): void {
    this.friendService.acceptRequest(requestId).subscribe({
      next: (response) => {
        this.snackBar.open(response.message || 'Friend request accepted!', 'OK', { duration: 3000 });
        this.loadPendingRequests();
        this.loadFriends();
      },
      error: (error) => {
        console.error('Error accepting request:', error);
        this.snackBar.open('Failed to accept request', 'OK', { duration: 3000 });
      }
    });
  }

  public rejectRequest(requestId: string): void {
    this.friendService.rejectRequest(requestId).subscribe({
      next: (response) => {
        this.snackBar.open(response.message || 'Friend request rejected', 'OK', { duration: 3000 });
        this.loadPendingRequests();
      },
      error: (error) => {
        console.error('Error rejecting request:', error);
        this.snackBar.open('Failed to reject request', 'OK', { duration: 3000 });
      }
    });
  }

  public cancelRequest(requestId: string): void {
    this.friendService.cancelRequest(requestId).subscribe({
      next: (response) => {
        this.snackBar.open(response.message || 'Friend request cancelled', 'OK', { duration: 3000 });
        this.loadSentRequests();
      },
      error: (error) => {
        console.error('Error cancelling request:', error);
        this.snackBar.open('Failed to cancel request', 'OK', { duration: 3000 });
      }
    });
  }

  public getUserInitials(user: Friend | UserProfile): string {
    const firstName = user.firstName || '';
    const lastName = user.lastName || '';
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  public navigateToProfile(userId: string): void {
    this.router.navigate(['/profile', userId]);
  }
}
