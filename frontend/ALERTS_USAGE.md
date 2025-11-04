# Alert System Usage Guide

## Overview
The Neo4flix application now includes a comprehensive alert/notification system to provide user feedback for various actions throughout the application.

## Features
- 🎨 Four alert types: Success, Error, Warning, Info
- ⏱️ Auto-dismiss with customizable duration
- 🎭 Smooth animations (slide-in from right)
- 📱 Responsive design
- 🎬 Optional action buttons
- 💫 Modern gradient-based design
- 🔔 Multiple alerts can stack

## Basic Usage

### 1. Inject the Alert Service

```typescript
import { Component, inject } from '@angular/core';
import { AlertService } from './services/alert.service';

@Component({
  // ...
})
export class YourComponent {
  private alertService = inject(AlertService);

  // Or using constructor injection:
  // constructor(private alertService: AlertService) {}
}
```

### 2. Show Alerts

#### Success Alert
```typescript
// Basic success
this.alertService.success('Movie added to watchlist!');

// With custom title and duration
this.alertService.success('Successfully shared movie', {
  title: 'Shared!',
  duration: 3000  // 3 seconds
});
```

#### Error Alert
```typescript
// Basic error
this.alertService.error('Failed to load movies');

// With action button
this.alertService.error('Failed to connect to server', {
  title: 'Connection Error',
  duration: 10000,
  action: {
    label: 'Retry',
    callback: () => this.retryConnection()
  }
});
```

#### Warning Alert
```typescript
this.alertService.warning('Your session will expire soon', {
  duration: 8000
});
```

#### Info Alert
```typescript
this.alertService.info('New movies added to the database!', {
  title: 'Updates Available'
});
```

## Real-World Examples

### Example 1: Friend Request
```typescript
// In friends component
sendFriendRequest(friendId: string): void {
  this.friendService.addFriend(friendId).subscribe({
    next: () => {
      this.alertService.success('Friend request sent successfully!');
    },
    error: (err) => {
      this.alertService.error(err.error?.message || 'Failed to send friend request', {
        action: {
          label: 'Try Again',
          callback: () => this.sendFriendRequest(friendId)
        }
      });
    }
  });
}
```

### Example 2: Share Movie
```typescript
// In movie detail component
shareMovie(result: ShareMovieDialogResult): void {
  this.recommendationService.shareMovie(
    this.movieId,
    result.selectedFriends,
    result.message
  ).subscribe({
    next: () => {
      const count = result.selectedFriends.length;
      this.alertService.success(
        `Movie shared with ${count} friend${count !== 1 ? 's' : ''}!`,
        { title: 'Shared Successfully' }
      );
    },
    error: (err) => {
      this.alertService.error('Failed to share movie. Please try again.');
    }
  });
}
```

### Example 3: Add to Watchlist
```typescript
addToWatchlist(): void {
  this.watchlistService.addToWatchlist(this.movieId).subscribe({
    next: () => {
      this.alertService.success('Added to your watchlist');
    },
    error: (err) => {
      if (err.status === 409) {
        this.alertService.warning('Movie is already in your watchlist');
      } else {
        this.alertService.error('Failed to add to watchlist');
      }
    }
  });
}
```

### Example 4: Rate Movie
```typescript
rateMovie(rating: number): void {
  this.movieService.rateMovie(this.movieId, rating).subscribe({
    next: () => {
      this.alertService.success(`You rated this movie ${rating}/10`, {
        title: 'Rating Saved'
      });
    },
    error: () => {
      this.alertService.error('Failed to save your rating', {
        action: {
          label: 'Retry',
          callback: () => this.rateMovie(rating)
        }
      });
    }
  });
}
```

### Example 5: Authentication
```typescript
login(): void {
  this.authService.login(this.credentials).subscribe({
    next: (user) => {
      this.alertService.success(`Welcome back, ${user.firstName}!`);
      this.router.navigate(['/movies']);
    },
    error: (err) => {
      if (err.status === 401) {
        this.alertService.error('Invalid username or password');
      } else {
        this.alertService.error('Login failed. Please try again later.');
      }
    }
  });
}
```

### Example 6: Delete Confirmation
```typescript
deleteMovie(movieId: string): void {
  // Show warning first
  this.alertService.warning('Are you sure you want to delete this movie?', {
    duration: 0,  // Don't auto-dismiss
    action: {
      label: 'Confirm Delete',
      callback: () => {
        this.movieService.deleteMovie(movieId).subscribe({
          next: () => {
            this.alertService.success('Movie deleted successfully');
            this.loadMovies();
          },
          error: () => {
            this.alertService.error('Failed to delete movie');
          }
        });
      }
    }
  });
}
```

## Advanced Usage

### Manual Dismiss
```typescript
// Store the alert ID
const alertId = this.alertService.success('Processing...');

// Later, dismiss it manually
this.alertService.dismiss(alertId);
```

### Dismiss All Alerts
```typescript
this.alertService.dismissAll();
```

### Persistent Alert (No Auto-Dismiss)
```typescript
this.alertService.error('Critical error occurred', {
  duration: 0  // Won't auto-dismiss
});
```

## Best Practices

### 1. Use Appropriate Alert Types
- ✅ **Success**: Completed actions (saved, created, updated, deleted)
- ❌ **Error**: Failed operations, validation errors, server errors
- ⚠️ **Warning**: Non-critical issues, confirmations, session warnings
- ℹ️ **Info**: General information, tips, updates

### 2. Keep Messages Concise
```typescript
// ✅ Good
this.alertService.success('Movie added to watchlist');

// ❌ Too verbose
this.alertService.success('The movie has been successfully added to your personal watchlist and you can now view it later');
```

### 3. Provide Context
```typescript
// ✅ Good - tells what went wrong
this.alertService.error('Failed to connect to server', {
  action: {
    label: 'Retry',
    callback: () => this.retry()
  }
});

// ❌ Too vague
this.alertService.error('Something went wrong');
```

### 4. Use Action Buttons Wisely
```typescript
// ✅ Good - actionable error
this.alertService.error('Your changes could not be saved', {
  action: {
    label: 'Try Again',
    callback: () => this.saveChanges()
  }
});

// ❌ Action doesn't make sense
this.alertService.success('Profile updated', {
  action: { label: 'OK', callback: () => {} }  // Unnecessary
});
```

### 5. Adjust Duration Based on Content
```typescript
// Short message - default duration (5s)
this.alertService.success('Saved');

// Important error - longer duration
this.alertService.error('Payment processing failed', {
  duration: 10000  // 10 seconds
});

// Critical alert - no auto-dismiss
this.alertService.error('Connection lost', {
  duration: 0
});
```

## Styling & Customization

The alert system uses the app's design system:
- Gradient backgrounds for visual appeal
- Smooth slide-in/out animations
- Responsive design (stacks on mobile)
- Matches Neo4flix's purple gradient theme

### Alert Types & Colors
- **Success**: Green gradient (#10b981 → #059669)
- **Error**: Red gradient (#ef4444 → #dc2626)
- **Warning**: Orange gradient (#f59e0b → #d97706)
- **Info**: Blue gradient (#3b82f6 → #2563eb)

## Testing
```typescript
// You can test alerts easily
testAllAlerts(): void {
  this.alertService.success('This is a success message');

  setTimeout(() => {
    this.alertService.error('This is an error message');
  }, 500);

  setTimeout(() => {
    this.alertService.warning('This is a warning');
  }, 1000);

  setTimeout(() => {
    this.alertService.info('This is an info message');
  }, 1500);
}
```

## Integration Checklist

When adding alerts to a new feature:
- [ ] Import AlertService
- [ ] Show success alerts for completed actions
- [ ] Show error alerts for failures with retry actions when appropriate
- [ ] Show warnings for non-critical issues
- [ ] Show info alerts for helpful tips
- [ ] Test on mobile devices
- [ ] Verify alert duration is appropriate
- [ ] Ensure messages are clear and concise

## Support

For issues or questions about the alert system, please contact the development team.
