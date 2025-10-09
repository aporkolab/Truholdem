import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface AppError {
  id: string;
  type: 'error' | 'warning' | 'info' | 'success';
  message: string;
  details?: string;
  timestamp: Date;
  dismissible: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ErrorHandlerService {
  private errorsSubject = new BehaviorSubject<AppError[]>([]);
  public errors$ = this.errorsSubject.asObservable();

  private maxErrors = 10; // Maximum number of errors to keep in memory

  constructor() {
    // Initialize error handler service
  }

  // Add error methods
  addError(message: string, details?: string): void {
    this.addNotification('error', message, details, true);
  }

  addWarning(message: string, details?: string): void {
    this.addNotification('warning', message, details, true);
  }

  addInfo(message: string, details?: string): void {
    this.addNotification('info', message, details, true);
  }

  addSuccess(message: string, details?: string): void {
    this.addNotification('success', message, details, true);
  }

  // Handle HTTP errors
  handleHttpError(error: { error?: { message?: string; details?: string } | string; message?: string; status?: number }): void {
    let message = 'An unexpected error occurred';
    let details = '';

    if (error.error) {
      if (typeof error.error === 'string') {
        message = error.error;
      } else if (error.error.message) {
        message = error.error.message;
        details = error.error.details || '';
      }
    } else if (error.message) {
      message = error.message;
    }

    // Add status code info if available
    if (error.status) {
      details = `HTTP ${error.status}: ${details}`.trim();
    }

    this.addError(message, details);
  }

  // Handle validation errors
  handleValidationErrors(errors: Record<string, string>): void {
    Object.entries(errors).forEach(([field, message]) => {
      this.addWarning(`${field}: ${message}`);
    });
  }

  // Handle WebSocket errors
  handleWebSocketError(error: string): void {
    this.addError('Real-time connection error', error);
  }

  // Handle authentication errors
  handleAuthError(message: string): void {
    this.addError('Authentication Error', message);
  }

  // Handle game errors
  handleGameError(message: string, details?: string): void {
    this.addError('Game Error', details ? `${message}: ${details}` : message);
  }

  // Dismiss specific error
  dismissError(errorId: string): void {
    const currentErrors = this.errorsSubject.value;
    const updatedErrors = currentErrors.filter(error => error.id !== errorId);
    this.errorsSubject.next(updatedErrors);
  }

  // Clear all errors
  clearAllErrors(): void {
    this.errorsSubject.next([]);
  }

  // Clear errors of specific type
  clearErrorsByType(type: AppError['type']): void {
    const currentErrors = this.errorsSubject.value;
    const updatedErrors = currentErrors.filter(error => error.type !== type);
    this.errorsSubject.next(updatedErrors);
  }

  // Auto-dismiss success and info messages after delay
  private autoDismiss(errorId: string, type: AppError['type'], delay: number): void {
    if (type === 'success' || type === 'info') {
      setTimeout(() => {
        this.dismissError(errorId);
      }, delay);
    }
  }

  private addNotification(
    type: AppError['type'], 
    message: string, 
    details?: string, 
    dismissible = true
  ): void {
    const error: AppError = {
      id: this.generateId(),
      type,
      message,
      details,
      timestamp: new Date(),
      dismissible
    };

    const currentErrors = this.errorsSubject.value;
    const updatedErrors = [error, ...currentErrors].slice(0, this.maxErrors);
    this.errorsSubject.next(updatedErrors);

    // Auto-dismiss certain types of messages
    this.autoDismiss(error.id, type, type === 'success' ? 3000 : 5000);

    // Log to console for debugging
    this.logToConsole(error);
  }

  private generateId(): string {
    return Math.random().toString(36).substr(2, 9) + Date.now().toString(36);
  }

  private logToConsole(error: AppError): void {
    const logMessage = `[${error.type.toUpperCase()}] ${error.message}`;
    const logDetails = error.details ? `\nDetails: ${error.details}` : '';

    switch (error.type) {
      case 'error':
        console.error(logMessage + logDetails);
        break;
      case 'warning':
        console.warn(logMessage + logDetails);
        break;
      case 'info':
        console.info(logMessage + logDetails);
        break;
      case 'success':
        console.log(logMessage + logDetails);
        break;
      default:
        console.log(logMessage + logDetails);
    }
  }

  // Get current errors
  getCurrentErrors(): AppError[] {
    return this.errorsSubject.value;
  }

  // Check if there are any errors
  hasErrors(): boolean {
    return this.errorsSubject.value.length > 0;
  }

  // Get errors by type
  getErrorsByType(type: AppError['type']): AppError[] {
    return this.errorsSubject.value.filter(error => error.type === type);
  }

  // Check if there are errors of specific type
  hasErrorsOfType(type: AppError['type']): boolean {
    return this.getErrorsByType(type).length > 0;
  }
}
