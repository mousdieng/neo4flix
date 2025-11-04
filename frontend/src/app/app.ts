import { Component, signal, OnInit, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Auth, User } from './services/auth';
import { WatchlistService } from './services/watchlist';
import { LogoAnimation } from './components/logo-animation/logo-animation';
import { DialogComponent } from './components/dialog/dialog.component';
import { ToastComponent } from './components/toast/toast.component';
import { AlertComponent } from './components/alert/alert.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, FormsModule, LogoAnimation, DialogComponent, ToastComponent, AlertComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private authService = inject(Auth);
  private watchlistService = inject(WatchlistService);
  private router = inject(Router);

  protected readonly title = signal('Neo4flix');
  public searchQuery = signal('');
  public showUserMenu = signal(false);
  public showMobileMenu = signal(false);
  public showLogoAnimation = signal(!sessionStorage.getItem('logoAnimationShown'));

  public isAuthenticated = computed(() => this.authService.isAuthenticated());
  public currentUser = toSignal(this.authService.currentUser$, { initialValue: null });
  public watchlistCount = computed(() => this.watchlistService.watchlistCount());
  public isAdmin = computed(() => {
    const user = this.currentUser();
    console.log("hhhhhh", user)
    return user?.role === 'ADMIN';
  });

  constructor() {}

  ngOnInit(): void {
    // Initialize user profile to fetch fresh profile picture URL
    this.authService.initializeUserProfile();

    // TODO: Load watchlist count if user is authenticated (once watchlist service is implemented)
    // if (this.isAuthenticated()) {
    //   this.watchlistService.getWatchlist().subscribe();
    // }

    // Close user menu when clicking outside
    document.addEventListener('click', (event) => {
      const target = event.target as HTMLElement;
      if (!target.closest('.user-menu')) {
        this.showUserMenu.set(false);
      }
    });
  }

  public toggleMobileMenu(): void {
    this.showMobileMenu.set(!this.showMobileMenu());
  }

  public onSearch(event: any): void {
    const query = event.target.value.trim();
    if (query) {
      this.router.navigate(['/movies'], { queryParams: { query } });
      this.searchQuery.set('');
    }
  }

  public toggleUserMenu(): void {
    this.showUserMenu.set(!this.showUserMenu());
  }

  public logout(): void {
    this.authService.logout();
    this.watchlistService.clearWatchlist();
    this.showUserMenu.set(false);
    this.router.navigate(['/home']);
  }

  public onAnimationComplete(): void {
    this.showLogoAnimation.set(false);
    sessionStorage.setItem('logoAnimationShown', 'true');
  }
}
