import { Component, OnInit, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService, LoginRequest } from '../services/auth.service';
import { ErrorHandlerService } from '../services/error-handler.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./auth.component.scss']
})
export class LoginComponent implements OnInit {
  @Output() switchMode = new EventEmitter<string>();

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private errorHandler = inject(ErrorHandlerService);
  private router = inject(Router);

  loginForm: FormGroup;
  isLoading = false;
  showPassword = false;

  // OAuth availability flags
  googleEnabled = false;
  githubEnabled = false;
  oauthLoaded = false;

  constructor() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/game']);
    }

    // Check OAuth availability
    this.checkOAuthProviders();
  }

  private checkOAuthProviders(): void {
    fetch(`${environment.apiUrl}/auth/oauth2/providers`)
      .then(response => response.json())
      .then((data: Record<string, boolean>) => {
        this.googleEnabled = data['google'] === true;
        this.githubEnabled = data['github'] === true;
        this.oauthLoaded = true;
      })
      .catch(() => {
        // Backend not available - hide OAuth buttons
        this.googleEnabled = false;
        this.githubEnabled = false;
        this.oauthLoaded = true;
      });
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.isLoading = true;
    const loginRequest: LoginRequest = {
      username: this.loginForm.value.username,
      password: this.loginForm.value.password
    };

    this.authService.login(loginRequest).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.errorHandler.addSuccess('Login successful!', `Welcome back, ${response.username}!`);
        this.router.navigate(['/game']);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorHandler.handleHttpError(error);
        
        
        this.loginForm.patchValue({ password: '' });
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  switchToRegister(): void {
    this.switchMode.emit('register');
  }

  private markFormGroupTouched(): void {
    Object.keys(this.loginForm.controls).forEach(key => {
      const control = this.loginForm.get(key);
      if (control) {
        control.markAsTouched();
      }
    });
  }

  loginWithGoogle(): void {
    if (this.googleEnabled) {
      window.location.href = environment.googleOAuthUrl;
    }
  }

  loginWithGitHub(): void {
    if (this.githubEnabled) {
      window.location.href = environment.githubOAuthUrl;
    }
  }

  get isAnyOAuthEnabled(): boolean {
    return this.googleEnabled || this.githubEnabled;
  }
}
