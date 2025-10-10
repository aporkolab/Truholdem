import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService, RegisterRequest } from '../services/auth.service';
import { ErrorHandlerService } from '../services/error-handler.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="auth-container" data-cy="register-page">
      <div class="auth-card" data-cy="register-card">
        <div class="auth-header" data-cy="register-header">
          <h1 class="auth-title" data-cy="register-title">&#9824; TruHoldem</h1>
          <p class="auth-subtitle" data-cy="register-subtitle">Create your account</p>
        </div>

        <form [formGroup]="registerForm" (ngSubmit)="onRegister()" class="auth-form" data-cy="register-form">
          <div class="form-row" data-cy="name-row">
            <div class="form-group" data-cy="first-name-group">
              <label for="firstName" class="form-label">First Name <span class="required">*</span></label>
              <input
                type="text"
                id="firstName"
                formControlName="firstName"
                class="form-input"
                placeholder="John"
                [class.error]="isFieldInvalid('firstName')"
                data-cy="first-name-input"
                autocomplete="given-name">
              @if (isFieldInvalid('firstName')) {
                <div class="error-message" data-cy="first-name-error" role="alert">First name is required</div>
              }
            </div>

            <div class="form-group" data-cy="last-name-group">
              <label for="lastName" class="form-label">Last Name <span class="required">*</span></label>
              <input
                type="text"
                id="lastName"
                formControlName="lastName"
                class="form-input"
                placeholder="Doe"
                [class.error]="isFieldInvalid('lastName')"
                data-cy="last-name-input"
                autocomplete="family-name">
              @if (isFieldInvalid('lastName')) {
                <div class="error-message" data-cy="last-name-error" role="alert">Last name is required</div>
              }
            </div>
          </div>

          <div class="form-group" data-cy="username-group">
            <label for="username" class="form-label">Username <span class="required">*</span></label>
            <input
              type="text"
              id="username"
              formControlName="username"
              class="form-input"
              placeholder="pokerpro123"
              [class.error]="isFieldInvalid('username')"
              data-cy="username-input"
              autocomplete="username">
            @if (isFieldInvalid('username')) {
              <div class="error-message" data-cy="username-error" role="alert">
                @if (registerForm.get('username')?.errors?.['required']) {
                  Username is required
                } @else if (registerForm.get('username')?.errors?.['minlength']) {
                  Username must be at least 3 characters
                } @else if (registerForm.get('username')?.errors?.['pattern']) {
                  Username can only contain letters, numbers, and underscores
                }
              </div>
            }
          </div>

          <div class="form-group" data-cy="email-group">
            <label for="email" class="form-label">Email Address <span class="required">*</span></label>
            <input
              type="email"
              id="email"
              formControlName="email"
              class="form-input"
              placeholder="john&#64;example.com"
              [class.error]="isFieldInvalid('email')"
              data-cy="email-input"
              autocomplete="email">
            @if (isFieldInvalid('email')) {
              <div class="error-message" data-cy="email-error" role="alert">
                @if (registerForm.get('email')?.errors?.['required']) {
                  Email is required
                } @else if (registerForm.get('email')?.errors?.['email'] || registerForm.get('email')?.errors?.['pattern']) {
                  Please enter a valid email address
                }
              </div>
            }
          </div>

          <div class="form-group" data-cy="password-group">
            <label for="password" class="form-label">Password <span class="required">*</span></label>
            <div class="input-wrapper">
              <input
                [type]="showPassword ? 'text' : 'password'"
                id="password"
                formControlName="password"
                class="form-input with-toggle"
                placeholder="Min. 8 characters"
                [class.error]="isFieldInvalid('password')"
                data-cy="password-input"
                autocomplete="new-password">
              <button
                type="button"
                class="password-toggle-btn"
                (click)="togglePassword('password')"
                data-cy="toggle-password-btn"
                [attr.aria-label]="showPassword ? 'Hide password' : 'Show password'"
                tabindex="-1">
                <span class="toggle-icon">{{ showPassword ? '&#128584;' : '&#128065;' }}</span>
              </button>
            </div>
            @if (isFieldInvalid('password')) {
              <div class="error-message" data-cy="password-error" role="alert">
                @if (registerForm.get('password')?.errors?.['required']) {
                  Password is required
                } @else if (registerForm.get('password')?.errors?.['minlength']) {
                  Password must be at least 8 characters
                }
              </div>
            }
            <div class="password-strength" *ngIf="registerForm.get('password')?.value">
              <div class="strength-bar" [class]="getPasswordStrength()"></div>
              <span class="strength-text">{{ getPasswordStrengthText() }}</span>
            </div>
          </div>

          <div class="form-group" data-cy="confirm-password-group">
            <label for="confirmPassword" class="form-label">Confirm Password <span class="required">*</span></label>
            <div class="input-wrapper">
              <input
                [type]="showConfirmPassword ? 'text' : 'password'"
                id="confirmPassword"
                formControlName="confirmPassword"
                class="form-input with-toggle"
                placeholder="Re-enter your password"
                [class.error]="isFieldInvalid('confirmPassword')"
                data-cy="confirm-password-input"
                autocomplete="new-password">
              <button
                type="button"
                class="password-toggle-btn"
                (click)="togglePassword('confirm')"
                data-cy="toggle-confirm-password-btn"
                [attr.aria-label]="showConfirmPassword ? 'Hide password' : 'Show password'"
                tabindex="-1">
                <span class="toggle-icon">{{ showConfirmPassword ? '&#128584;' : '&#128065;' }}</span>
              </button>
            </div>
            @if (isFieldInvalid('confirmPassword')) {
              <div class="error-message" data-cy="confirm-password-error" role="alert">
                @if (registerForm.get('confirmPassword')?.errors?.['required']) {
                  Please confirm your password
                } @else if (registerForm.get('confirmPassword')?.errors?.['passwordMismatch']) {
                  Passwords do not match
                }
              </div>
            }
            @if (registerForm.get('confirmPassword')?.value && !registerForm.get('confirmPassword')?.errors) {
              <div class="success-message">&#10004; Passwords match</div>
            }
          </div>

          <div class="form-group checkbox-group" data-cy="form-options">
            <label class="checkbox-label" data-cy="terms-label">
              <input type="checkbox" formControlName="agreeToTerms" data-cy="terms-checkbox">
              <span class="checkmark"></span>
              <span class="checkbox-text">I agree to the <a href="#" class="link">Terms of Service</a> and <a href="#" class="link">Privacy Policy</a></span>
            </label>
            @if (registerForm.get('agreeToTerms')?.touched && registerForm.get('agreeToTerms')?.errors) {
              <div class="error-message" role="alert">You must agree to the terms to continue</div>
            }
          </div>

          <button
            type="submit"
            class="auth-button primary"
            [disabled]="registerForm.invalid || isLoading"
            data-cy="register-submit-btn">
            @if (isLoading) {
              <span class="loading-spinner" data-cy="register-loading"></span> Creating Account...
            } @else {
              Create Account
            }
          </button>
        </form>

        <div class="auth-footer" data-cy="register-footer">
          <p class="auth-link-text">
            Already have an account?
            <a routerLink="/auth/login" class="auth-link-button" data-cy="switch-to-login-btn">Sign in</a>
          </p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./auth.component.scss']
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly errorHandler = inject(ErrorHandlerService);
  private readonly router = inject(Router);

  registerForm: FormGroup;
  isLoading = false;
  showPassword = false;
  showConfirmPassword = false;

  // RFC 5322 compliant email regex
  private readonly emailPattern = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;

  constructor() {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(1)]],
      lastName: ['', [Validators.required, Validators.minLength(1)]],
      username: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
      email: ['', [Validators.required, Validators.pattern(this.emailPattern)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      agreeToTerms: [false, [Validators.requiredTrue]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    if (password && confirmPassword && password !== confirmPassword) {
      group.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }

    // Clear the error if passwords match
    const confirmControl = group.get('confirmPassword');
    if (confirmControl?.errors?.['passwordMismatch']) {
      delete confirmControl.errors['passwordMismatch'];
      if (Object.keys(confirmControl.errors).length === 0) {
        confirmControl.setErrors(null);
      }
    }
    return null;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!field && field.invalid && field.touched;
  }

  togglePassword(field: 'password' | 'confirm'): void {
    if (field === 'password') {
      this.showPassword = !this.showPassword;
    } else {
      this.showConfirmPassword = !this.showConfirmPassword;
    }
  }

  getPasswordStrength(): string {
    const password = this.registerForm.get('password')?.value || '';
    if (password.length < 8) return 'weak';

    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++;
    if (/\d/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;

    if (strength <= 1) return 'weak';
    if (strength === 2) return 'medium';
    return 'strong';
  }

  getPasswordStrengthText(): string {
    const strength = this.getPasswordStrength();
    switch (strength) {
      case 'weak': return 'Weak';
      case 'medium': return 'Medium';
      case 'strong': return 'Strong';
      default: return '';
    }
  }

  onRegister(): void {
    if (this.registerForm.invalid) {
      this.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const formValue = this.registerForm.value;

    const registerRequest: RegisterRequest = {
      username: formValue.username,
      email: formValue.email,
      password: formValue.password,
      firstName: formValue.firstName,
      lastName: formValue.lastName
    };

    this.authService.register(registerRequest).subscribe({
      next: () => {
        this.isLoading = false;
        this.errorHandler.addSuccess('Registration successful!', 'You can now log in.');
        this.router.navigate(['/auth/login']);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorHandler.handleHttpError(error);
      }
    });
  }

  private markAllAsTouched(): void {
    Object.keys(this.registerForm.controls).forEach(key => {
      this.registerForm.get(key)?.markAsTouched();
    });
  }
}
