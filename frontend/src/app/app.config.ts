import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import {
  provideRouter,
  withPreloading,
  PreloadAllModules,
  withComponentInputBinding,
  withViewTransitions,
  withInMemoryScrolling
} from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    
    provideZoneChangeDetection({ eventCoalescing: true }),

    
    provideRouter(
      routes,
      
      withPreloading(PreloadAllModules),
      
      withComponentInputBinding(),
      
      withViewTransitions({
        onViewTransitionCreated: ({ transition }) => {
          // Skip transition if document is hidden to avoid errors
          if (document.hidden) {
            transition.skipTransition();
          }
        }
      }),
      
      withInMemoryScrolling({
        scrollPositionRestoration: 'enabled',
        anchorScrolling: 'enabled'
      })
    ),

    
    provideHttpClient(
      withFetch(), 
      withInterceptors([authInterceptor])
    ),

    
    provideAnimationsAsync()
  ]
};
