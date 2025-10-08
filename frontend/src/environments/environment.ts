export const environment = {
  production: false,
  apiUrl: '/api',
  wsUrl: '/ws',

  // OAuth2 URLs
  googleOAuthUrl: '/api/auth/oauth2/authorize/google',
  githubOAuthUrl: '/api/auth/oauth2/authorize/github',
  
  
  defaultStartingChips: 1000,
  defaultSmallBlind: 10,
  defaultBigBlind: 20,
  
  
  cardAnimationDuration: 500,
  actionTimeout: 30000, 
  botActionDelay: 800,
  
  
  enableWebSocket: true,
  enableSoundEffects: true,
  enableAnimations: true,
  enableHandHistory: true,
  enableStatistics: true,
  
  
  showDebugInfo: true,
  logApiCalls: true
};
