import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error.html',
  styleUrl: './error.css'
})
export class Error implements OnInit {
  public readonly Date = Date;
  public errorCode: string = '500';
  public errorMessage: string = 'Something went wrong';
  public errorDetails: string = 'An unexpected error occurred. Please try again later.';

  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get error details from route state if available
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras?.state || window.history.state;

    if (state) {
      this.errorCode = state['errorCode'] || this.errorCode;
      this.errorMessage = state['errorMessage'] || this.errorMessage;
      this.errorDetails = state['errorDetails'] || this.errorDetails;
    }

    // Also check query params
    this.route.queryParams.subscribe(params => {
      if (params['code']) this.errorCode = params['code'];
      if (params['message']) this.errorMessage = params['message'];
      if (params['details']) this.errorDetails = params['details'];
    });
  }

  public goBack(): void {
    window.history.back();
  }

  public goHome(): void {
    this.router.navigate(['/home']);
  }

  public reload(): void {
    window.location.reload();
  }

  public reportError(): void {
    // In a real app, this would send error details to a logging service
    console.error('Error Report:', {
      code: this.errorCode,
      message: this.errorMessage,
      details: this.errorDetails,
      timestamp: new Date().toISOString(),
      url: window.location.href
    });
    alert('Error report sent. Thank you for helping us improve!');
  }
}
