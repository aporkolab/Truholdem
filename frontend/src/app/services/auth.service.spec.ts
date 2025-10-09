import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService, User, AuthResponse, LoginRequest, RegisterRequest } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerMock: jest.Mocked<Router>;

  const mockAuthResponse: AuthResponse = {
    accessToken: 'test-access-token',
    refreshToken: 'test-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    username: 'testuser',
    email: 'test@example.com',
    roles: ['USER']
  };

  const mockUser: User = {
    id: 'user-123',
    username: 'testuser',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    roles: ['USER'],
    totalGamesPlayed: 10,
    totalWinnings: 500
  };

  beforeEach(() => {
    // Clear localStorage before setting up mocks
    localStorage.clear();

    routerMock = {
      navigate: jest.fn().mockReturnValue(Promise.resolve(true))
    } as unknown as jest.Mocked<Router>;

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should initialize with stored token and user', () => {
      // Clear and reset localStorage
      localStorage.clear();
      localStorage.setItem('access_token', 'stored-token');
      localStorage.setItem('user', JSON.stringify(mockUser));

      // Create a fresh TestBed and service instance
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: routerMock }
        ]
      });

      const newService = TestBed.inject(AuthService);

      expect(newService.isAuthenticated()).toBe(true);
      expect(newService.getCurrentUserValue()).toEqual(mockUser);
    });

    it('should not authenticate without stored token', () => {
      expect(service.isAuthenticated()).toBe(false);
      expect(service.getCurrentUserValue()).toBeNull();
    });
  });

  describe('login', () => {
    it('should login successfully and store tokens', fakeAsync(() => {
      const credentials: LoginRequest = { username: 'testuser', password: 'password123' };

      service.login(credentials).subscribe(response => {
        expect(response).toEqual(mockAuthResponse);
      });

      const loginReq = httpMock.expectOne('/api/auth/login');
      expect(loginReq.request.method).toBe('POST');
      loginReq.flush(mockAuthResponse);

      
      const profileReq = httpMock.expectOne('/api/v1/users/profile');
      profileReq.flush(mockUser);

      tick();

      expect(localStorage.getItem('access_token')).toBe('test-access-token');
      expect(localStorage.getItem('refresh_token')).toBe('test-refresh-token');
      expect(service.isAuthenticated()).toBe(true);

      discardPeriodicTasks();
    }));

    it('should handle login error', fakeAsync(() => {
      const credentials: LoginRequest = { username: 'wrong', password: 'wrong' };

      service.login(credentials).subscribe({
        error: (error) => {
          expect(error.status).toBe(401);
        }
      });

      const loginReq = httpMock.expectOne('/api/auth/login');
      loginReq.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      tick();
      discardPeriodicTasks();
    }));
  });

  describe('register', () => {
    it('should register a new user', fakeAsync(() => {
      const userData: RegisterRequest = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
        firstName: 'New',
        lastName: 'User'
      };

      service.register(userData).subscribe(response => {
        expect(response.message).toBe('Registration successful');
      });

      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(userData);
      req.flush({ message: 'Registration successful' });

      tick();
      discardPeriodicTasks();
    }));
  });

  describe('logout', () => {
    it('should clear storage and navigate to login', fakeAsync(() => {
      
      localStorage.setItem('access_token', 'token');
      localStorage.setItem('refresh_token', 'refresh');
      localStorage.setItem('user', JSON.stringify(mockUser));

      service.logout().subscribe();

      const req = httpMock.expectOne('/api/auth/logout');
      req.flush({ message: 'Logged out' });

      tick();

      expect(localStorage.getItem('access_token')).toBeNull();
      expect(localStorage.getItem('refresh_token')).toBeNull();
      expect(localStorage.getItem('user')).toBeNull();
      expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);

      discardPeriodicTasks();
    }));

    it('should still logout even if API fails', fakeAsync(() => {
      localStorage.setItem('access_token', 'token');

      service.logout().subscribe({
        error: () => {
          // Expected error - test continues after flush
        }
      });

      const req = httpMock.expectOne('/api/auth/logout');
      req.flush({}, { status: 500, statusText: 'Server Error' });

      tick();

      expect(localStorage.getItem('access_token')).toBeNull();
      expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);

      discardPeriodicTasks();
    }));
  });

  describe('token refresh', () => {
    it('should refresh token successfully', fakeAsync(() => {
      localStorage.setItem('refresh_token', 'old-refresh-token');

      service.refreshToken().subscribe(response => {
        expect(response.accessToken).toBe('new-access-token');
      });

      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'old-refresh-token' });

      req.flush({ ...mockAuthResponse, accessToken: 'new-access-token' });

      
      const profileReq = httpMock.expectOne('/api/v1/users/profile');
      profileReq.flush(mockUser);

      tick();
      discardPeriodicTasks();
    }));

    it('should fail refresh when no refresh token', fakeAsync(() => {
      localStorage.removeItem('refresh_token');

      service.refreshToken().subscribe({
        error: (error) => {
          expect(error.message).toBe('No refresh token available');
        }
      });

      tick();
      discardPeriodicTasks();
    }));
  });

  describe('password change', () => {
    it('should change password', fakeAsync(() => {
      service.changePassword('oldpass', 'newpass').subscribe(response => {
        expect(response.message).toBe('Password changed');
      });

      const req = httpMock.expectOne('/api/auth/change-password');
      expect(req.request.body).toEqual({
        currentPassword: 'oldpass',
        newPassword: 'newpass'
      });
      req.flush({ message: 'Password changed' });

      tick();
      discardPeriodicTasks();
    }));
  });

  describe('role management', () => {
    it('should check user roles correctly', fakeAsync(() => {
      
      service.login({ username: 'test', password: 'test' }).subscribe();

      const loginReq = httpMock.expectOne('/api/auth/login');
      loginReq.flush({ ...mockAuthResponse, roles: ['USER', 'ADMIN'] });

      const profileReq = httpMock.expectOne('/api/v1/users/profile');
      profileReq.flush({ ...mockUser, roles: ['USER', 'ADMIN'] });

      tick();

      expect(service.hasRole('USER')).toBe(true);
      expect(service.hasRole('ADMIN')).toBe(true);
      expect(service.hasRole('SUPERADMIN')).toBe(false);

      discardPeriodicTasks();
    }));

    it('should return false for roles when not authenticated', () => {
      expect(service.hasRole('USER')).toBe(false);
      expect(service.isAdmin()).toBe(false);
    });

    it('should correctly identify admin users', fakeAsync(() => {
      // Clear and reset localStorage
      localStorage.clear();
      localStorage.setItem('user', JSON.stringify({ ...mockUser, roles: ['USER', 'ADMIN'] }));
      localStorage.setItem('access_token', 'token');
      localStorage.setItem('refresh_token', 'refresh-token');

      // Create a fresh TestBed and service instance
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          AuthService,
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: routerMock }
        ]
      });

      const newService = TestBed.inject(AuthService);
      const newHttpMock = TestBed.inject(HttpTestingController);

      tick();

      // The timer fires immediately (starts at 0), so we need to handle the refresh token request
      const refreshReq = newHttpMock.match(`/api/auth/refresh`);
      if (refreshReq.length > 0) {
        refreshReq[0].flush({
          ...mockAuthResponse,
          roles: ['USER', 'ADMIN']
        });

        // Handle the profile request that follows
        const profileReq = newHttpMock.match('/api/v1/users/profile');
        if (profileReq.length > 0) {
          profileReq[0].flush({ ...mockUser, roles: ['USER', 'ADMIN'] });
        }
      }

      tick();

      expect(newService.isAdmin()).toBe(true);

      newHttpMock.verify();
      discardPeriodicTasks();
    }));
  });

  describe('getToken', () => {
    it('should return stored token', () => {
      localStorage.setItem('access_token', 'my-token');
      expect(service.getToken()).toBe('my-token');
    });

    it('should return null when no token', () => {
      expect(service.getToken()).toBeNull();
    });
  });
});
