import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PlayerService } from '../services/player.service';


export const gameGuard: CanActivateFn = () => {
  const playerService = inject(PlayerService);
  const router = inject(Router);

  
  const hasPlayers = playerService.getPlayers().length >= 2;

  if (hasPlayers) {
    return true;
  }

  
  return router.createUrlTree(['/lobby']);
};


export const replayGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const handId = route.paramMap.get('handId');

  if (handId && handId.length > 0) {
    return true;
  }

  
  return router.createUrlTree(['/history']);
};
