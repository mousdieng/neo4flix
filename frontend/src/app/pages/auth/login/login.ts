import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth, LoginRequest } from '../../../services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  public loginForm: FormGroup;
  public showPassword = false;
  public isLoading = signal(false);
  public error = signal<string | null>(null);
  public requiresTwoFactor = signal(false);
  public twoFactorCode = signal('');
  private storedCredentials: LoginRequest | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: Auth,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', Validators.required],
      password: ['', Validators.required],
      rememberMe: [false]
    });
  }

  public togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  public onSubmit(): void {
    if (this.loginForm.valid || this.requiresTwoFactor()) {
      this.isLoading.set(true);
      this.error.set(null);

      let credentials: LoginRequest;

      if (this.requiresTwoFactor()) {
        // Second step: submit with 2FA code
        if (!this.storedCredentials) {
          this.error.set('Session expired. Please try again.');
          this.isLoading.set(false);
          this.requiresTwoFactor.set(false);
          return;
        }
        credentials = {
          ...this.storedCredentials,
          twoFactorCode: this.twoFactorCode()
        };
      } else {
        // First step: submit username/password
        credentials = {
          usernameOrEmail: this.loginForm.value.usernameOrEmail,
          password: this.loginForm.value.password
        };
      }

      this.authService.login(credentials).subscribe({
        next: (response) => {
          this.isLoading.set(false);
          if (response.requiresTwoFactor) {
            // 2FA required - show code input
            this.storedCredentials = credentials;
            this.requiresTwoFactor.set(true);
            this.twoFactorCode.set('');
          } else if (response.accessToken) {
            // Login successful
            this.requiresTwoFactor.set(false);
            this.storedCredentials = null;
            this.router.navigate(['/home']);
          } else {
            this.error.set('Login failed');
          }
        },
        error: (error) => {
          this.isLoading.set(false);
          this.error.set(error?.error?.message || error?.message || 'Invalid username or password');
        }
      });
    }
  }

  public cancelTwoFactor(): void {
    this.requiresTwoFactor.set(false);
    this.twoFactorCode.set('');
    this.storedCredentials = null;
    this.error.set(null);
  }
}
