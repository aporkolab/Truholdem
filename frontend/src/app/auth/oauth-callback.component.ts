import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ErrorHandlerService } from '../services/error-handler.service';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="oauth-callback-container">
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>{{ message }}</p>
      </div>
    </div>
  `,
  styles: [`
    .oauth-callback-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    }

    .loading-spinner {
      text-align: center;
      color: #fff;
    }

    .spinner {
      width: 50px;
      height: 50px;
      border: 4px solid rgba(255, 255, 255, 0.3);
      border-top-color: #4ade80;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 20px;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    p {
      font-size: 1.2rem;
      margin-top: 16px;
    }
  `]
})
export class OAuthCallbackComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private errorHandler = inject(ErrorHandlerService);

  message = 'Completing authentication...';

  ngOnInit(): void {
    this.handleCallback();
  }

  private handleCallback(): void {
    const params = this.route.snapshot.queryParams;

    // Check for error
    if (params['error']) {
      this.message = 'Authentication failed';
      this.errorHandler.addError('OAuth Error', params['error_description'] || 'Authentication failed');
      setTimeout(() => this.router.navigate(['/auth/login']), 2000);
      return;
    }

    // Get tokens from URL
    const token = params['token'];
    const refreshToken = params['refreshToken'];

    if (token && refreshToken) {
      // Store tokens and complete login
      this.authService.handleOAuthCallback(token, refreshToken).subscribe({
        next: () => {
          this.message = 'Login successful!';
          this.errorHandler.addSuccess('Login successful!', 'Welcome!');
          this.router.navigate(['/game']);
        },
        error: (error) => {
          this.message = 'Authentication failed';
          this.errorHandler.handleHttpError(error);
          setTimeout(() => this.router.navigate(['/auth/login']), 2000);
        }
      });
    } else {
      this.message = 'Invalid callback';
      this.errorHandler.addError('OAuth Error', 'Invalid callback parameters');
      setTimeout(() => this.router.navigate(['/auth/login']), 2000);
    }
  }
}
