import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService, UserProfile } from '../../services/user';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {
  public profile = signal<UserProfile | null>(null);
  public isLoading = signal(false);
  public isEditMode = signal(false);
  public activeTab = signal<'info' | 'ratings' | 'security'>('info');
  public errorMessage = signal<string | null>(null);
  public successMessage = signal<string | null>(null);

  public profileForm!: FormGroup;
  public passwordForm!: FormGroup;
  public selectedFile: File | null = null;
  public uploadingAvatar = signal(false);
  public twoFactorQrCode = signal<string | null>(null);
  public twoFactorSecret = signal<string | null>(null);
  public showDisable2FAPrompt = signal(false);
  public twoFactorCode = signal('');

  constructor(
    private userService: UserService,
    private authService: Auth,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.loadProfile();
  }

  private initializeForms(): void {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  private passwordMatchValidator(g: FormGroup) {
    const newPass = g.get('newPassword')?.value;
    const confirmPass = g.get('confirmPassword')?.value;
    return newPass === confirmPass ? null : { mismatch: true };
  }

  private loadProfile(): void {
    this.isLoading.set(true);
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileForm.patchValue({
          firstName: profile.firstName,
          lastName: profile.lastName,
          email: profile.email
        });
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        this.errorMessage.set('Failed to load profile');
        this.isLoading.set(false);
      }
    });
  }

  public toggleEditMode(): void {
    if (this.isEditMode()) {
      // Cancel edit - reset form
      const profile = this.profile();
      if (profile) {
        this.profileForm.patchValue({
          firstName: profile.firstName,
          lastName: profile.lastName,
          email: profile.email
        });
      }
    }
    this.isEditMode.update(v => !v);
    this.clearMessages();
  }

  public saveProfile(): void {
    if (this.profileForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    this.userService.updateProfile(this.profileForm.value).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.isEditMode.set(false);
        this.successMessage.set('Profile updated successfully');
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error updating profile:', error);
        this.errorMessage.set(error.error?.message || 'Failed to update profile');
        this.isLoading.set(false);
      }
    });
  }

  public changePassword(): void {
    if (this.passwordForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    const { currentPassword, newPassword } = this.passwordForm.value;

    this.userService.changePassword({ currentPassword, newPassword }).subscribe({
      next: (message) => {
        this.successMessage.set(message || 'Password changed successfully');
        this.passwordForm.reset();
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error changing password:', error);
        this.errorMessage.set(error.error || 'Failed to change password');
        this.isLoading.set(false);
      }
    });
  }

  public setActiveTab(tab: 'info' | 'ratings' | 'security'): void {
    this.activeTab.set(tab);
    this.clearMessages();
  }

  public logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  public getInitials(): string {
    const profile = this.profile();
    if (!profile) return '';
    return `${profile.firstName.charAt(0)}${profile.lastName.charAt(0)}`.toUpperCase();
  }

  public getFullName(): string {
    const profile = this.profile();
    if (!profile) return '';
    return `${profile.firstName} ${profile.lastName}`;
  }

  public getJoinDate(): string {
    const profile = this.profile();
    if (!profile || !profile.createdAt) return 'Unknown';
    return new Date(profile.createdAt).toLocaleDateString('en-US', {
      month: 'long',
      year: 'numeric'
    });
  }

  public onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.errorMessage.set('Please select an image file');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage.set('File size must be less than 5MB');
        return;
      }

      this.selectedFile = file;
      this.uploadAvatar();
    }
  }

  public uploadAvatar(): void {
    if (!this.selectedFile) {
      return;
    }

    this.uploadingAvatar.set(true);
    this.clearMessages();

    this.userService.uploadProfilePicture(this.selectedFile).subscribe({
      next: (response) => {
        const currentProfile = this.profile();
        if (currentProfile) {
          currentProfile.profilePictureUrl = response.profilePictureUrl;
          this.profile.set({...currentProfile});
        }
        this.successMessage.set('Profile picture uploaded successfully');
        this.selectedFile = null;
        this.uploadingAvatar.set(false);
      },
      error: (error) => {
        console.error('Error uploading avatar:', error);
        this.errorMessage.set(error.error?.error || 'Failed to upload profile picture');
        this.uploadingAvatar.set(false);
      }
    });
  }

  public enable2FA(): void {
    this.isLoading.set(true);
    this.clearMessages();

    this.userService.enable2FA().subscribe({
      next: (response) => {
        this.twoFactorQrCode.set(response.qrCode);
        this.twoFactorSecret.set(response.secret);

        const currentProfile = this.profile();
        if (currentProfile) {
          currentProfile.twoFactorEnabled = true;
          this.profile.set({...currentProfile});
        }
        this.successMessage.set('Two-factor authentication enabled! Scan the QR code or manually enter the secret key.');
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error enabling 2FA:', error);
        this.errorMessage.set(error.error || 'Failed to enable two-factor authentication');
        this.isLoading.set(false);
      }
    });
  }

  public disable2FA(): void {
    const code = this.twoFactorCode();
    if (!code || code.length !== 6) {
      this.errorMessage.set('Please enter a valid 6-digit code');
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    this.userService.disable2FA(code).subscribe({
      next: (message) => {
        this.twoFactorQrCode.set(null);
        this.twoFactorSecret.set(null);
        this.showDisable2FAPrompt.set(false);
        this.twoFactorCode.set('');
        const currentProfile = this.profile();
        if (currentProfile) {
          currentProfile.twoFactorEnabled = false;
          this.profile.set({...currentProfile});
        }
        this.successMessage.set(message || 'Two-factor authentication disabled successfully');
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error disabling 2FA:', error);
        this.errorMessage.set(error.error || 'Failed to disable two-factor authentication. Check your code.');
        this.isLoading.set(false);
      }
    });
  }

  public toggleDisable2FAPrompt(): void {
    this.showDisable2FAPrompt.update(v => !v);
    this.twoFactorCode.set('');
    this.clearMessages();
  }

  public copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.successMessage.set('Copied to clipboard!');
      setTimeout(() => this.clearMessages(), 2000);
    }).catch(err => {
      console.error('Failed to copy:', err);
    });
  }

  public clearQrCode(): void {
    this.twoFactorQrCode.set(null);
    this.twoFactorSecret.set(null);
  }
}
