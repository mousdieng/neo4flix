import { Component, signal, OnInit, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Auth, User } from './services/auth';
import { WatchlistService } from './services/watchlist';
import { LogoAnimation } from './components/logo-animation/logo-animation';
import { DialogComponent } from './components/dialog/dialog.component';
import { ToastComponent } from './components/toast/toast.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, FormsModule, LogoAnimation, DialogComponent, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('Neo4flix');
  public searchQuery = signal('');
  public showUserMenu = signal(false);
  public showMobileMenu = signal(false);
  public showLogoAnimation = signal(!sessionStorage.getItem('logoAnimationShown'));

  public isAuthenticated = computed(() => this.authService.isAuthenticated());
  public currentUser = computed(() => this.authService.getCurrentUser());
  public watchlistCount = computed(() => this.watchlistService.watchlistCount());
  public isAdmin = computed(() => {
    const user = this.authService.getCurrentUser();
    console.log("hhhhhh", user)
    return user?.role === 'ADMIN';
  });

  constructor(
    private authService: Auth,
    private watchlistService: WatchlistService,
    private router: Router
  ) {}

  ngOnInit(): void {
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
