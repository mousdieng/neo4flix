import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Movies } from './pages/movies/movies';
import { MovieDetail } from './pages/movie-detail/movie-detail';
import { Recommendations } from './pages/recommendations/recommendations';
import { Watchlist } from './pages/watchlist/watchlist';
import { Friends } from './pages/friends/friends';
import { Profile } from './pages/profile/profile';
import { Admin } from './pages/admin/admin';
import { Login } from './pages/auth/login/login';
import { Register } from './pages/auth/register/register';
import { NotFound } from './pages/not-found/not-found';
import { Error } from './pages/error/error';
import { authGuard, guestGuard, adminGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: Home },
  { path: 'movies', component: Movies },
  { path: 'movie/:id', component: MovieDetail },
  { path: 'recommendations', component: Recommendations, canActivate: [authGuard] },
  { path: 'watchlist', component: Watchlist, canActivate: [authGuard] },
  { path: 'friends', component: Friends, canActivate: [authGuard] },
  { path: 'profile', component: Profile, canActivate: [authGuard] },
  { path: 'admin', component: Admin, canActivate: [adminGuard] },
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: Register, canActivate: [guestGuard] },
  { path: 'error', component: Error },
  { path: '404', component: NotFound },
  { path: '**', component: NotFound }
];
