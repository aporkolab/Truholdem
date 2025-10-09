import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { ErrorHandlerService } from '../services/error-handler.service';


let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);


export const authInterceptor: HttpInterceptorFn = (
  request: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const errorHandler = inject(ErrorHandlerService);

  
  const authRequest = addAuthHeader(request, authService);

  return next(authRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      
      if (error.status === 401 && !authRequest.url.includes('/auth/login')) {
        return handle401Error(authRequest, next, authService);
      }

      
      handleHttpError(error, errorHandler);
      return throwError(() => error);
    })
  );
};

function addAuthHeader(request: HttpRequest<unknown>, authService: AuthService): HttpRequest<unknown> {
  const token = authService.getToken();

  if (token && !request.headers.has('Authorization')) {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return request;
}

function handle401Error(
  request: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService
): Observable<HttpEvent<unknown>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response: { accessToken: string }) => {
        isRefreshing = false;
        refreshTokenSubject.next(response.accessToken);

        
        const retryRequest = addAuthHeader(request, authService);
        return next(retryRequest);
      }),
      catchError((refreshError) => {
        isRefreshing = false;

        
        console.error('Token refresh failed, logging out user');
        authService.logout().subscribe({
          error: () => {
            
            
            console.log('Forcing logout due to refresh failure');
          }
        });

        return throwError(() => refreshError);
      })
    );
  }

  
  return refreshTokenSubject.pipe(
    filter(token => token !== null),
    take(1),
    switchMap(() => {
      const retryRequest = addAuthHeader(request, authService);
      return next(retryRequest);
    })
  );
}

function handleHttpError(error: HttpErrorResponse, errorHandler: ErrorHandlerService): void {
  
  const silentEndpoints = ['/auth/refresh', '/auth/validate'];
  const shouldShowError = !silentEndpoints.some(endpoint => error.url?.includes(endpoint));

  if (shouldShowError) {
    switch (error.status) {
      case 400:
        handleBadRequestError(error, errorHandler);
        break;
      case 401:
        errorHandler.handleAuthError('You are not authorized to perform this action');
        break;
      case 403:
        errorHandler.addError('Access Denied', 'You do not have permission to access this resource');
        break;
      case 404:
        errorHandler.addError('Not Found', 'The requested resource was not found');
        break;
      case 409:
        errorHandler.addError('Conflict', 'The request conflicts with current state of the resource');
        break;
      case 422:
        handleValidationError(error, errorHandler);
        break;
      case 500:
        errorHandler.addError('Server Error', 'An internal server error occurred. Please try again later.');
        break;
      case 503:
        errorHandler.addError('Service Unavailable', 'The service is temporarily unavailable. Please try again later.');
        break;
      default:
        if (error.status >= 400) {
          errorHandler.handleHttpError(error);
        }
        break;
    }
  }
}

function handleBadRequestError(error: HttpErrorResponse, errorHandler: ErrorHandlerService): void {
  if (error.error && typeof error.error === 'object') {
    
    if (error.error.errors) {
      errorHandler.handleValidationErrors(error.error.errors);
    } else if (error.error.message) {
      errorHandler.addError('Bad Request', error.error.message);
    } else {
      errorHandler.addError('Bad Request', 'The request was invalid');
    }
  } else {
    errorHandler.addError('Bad Request', 'The request was invalid');
  }
}

function handleValidationError(error: HttpErrorResponse, errorHandler: ErrorHandlerService): void {
  if (error.error && error.error.validationErrors) {
    errorHandler.handleValidationErrors(error.error.validationErrors);
  } else {
    errorHandler.addError('Validation Error', 'Please check your input and try again');
  }
}



import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpHandler } from '@angular/common/http';

@Injectable()
export class AuthInterceptorLegacy implements HttpInterceptor {
  private authService = inject(AuthService);
  private errorHandler = inject(ErrorHandlerService);

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const authRequest = addAuthHeader(request, this.authService);

    return next.handle(authRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !authRequest.url.includes('/auth/login')) {
          return handle401Error(authRequest, (req) => next.handle(req), this.authService);
        }
        handleHttpError(error, this.errorHandler);
        return throwError(() => error);
      })
    );
  }
}

