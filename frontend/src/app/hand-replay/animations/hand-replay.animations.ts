import {
  trigger,
  state,
  style,
  animate,
  transition,
  keyframes,
  group
} from '@angular/animations';


export const cardFlipAnimation = trigger('cardFlip', [
  state('faceDown', style({
    transform: 'rotateY(180deg)'
  })),
  state('faceUp', style({
    transform: 'rotateY(0deg)'
  })),
  transition('faceDown => faceUp', [
    animate('400ms ease-out')
  ]),
  transition('faceUp => faceDown', [
    animate('300ms ease-in')
  ])
]);


export const cardRevealAnimation = trigger('cardReveal', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'translateY(-30px) scale(0.8)'
    }),
    animate('350ms cubic-bezier(0.35, 0, 0.25, 1)', style({
      opacity: 1,
      transform: 'translateY(0) scale(1)'
    }))
  ]),
  transition(':leave', [
    animate('200ms ease-in', style({
      opacity: 0,
      transform: 'scale(0.8)'
    }))
  ])
]);


export const cardDealAnimation = trigger('cardDeal', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(-50px) rotateX(90deg)' }),
    animate('400ms cubic-bezier(0.35, 0, 0.25, 1)',
      style({ opacity: 1, transform: 'translateY(0) rotateX(0)' })
    )
  ])
]);


export const potChangeAnimation = trigger('potChange', [
  transition('* => *', [
    animate('300ms ease-out', keyframes([
      style({ transform: 'scale(1)', color: '*', offset: 0 }),
      style({ transform: 'scale(1.15)', color: '#4ade80', offset: 0.5 }),
      style({ transform: 'scale(1)', color: '*', offset: 1 })
    ]))
  ])
]);


export const actionPopAnimation = trigger('actionPop', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'scale(0.3) translateY(10px)'
    }),
    animate('250ms cubic-bezier(0.68, -0.55, 0.265, 1.55)', style({
      opacity: 1,
      transform: 'scale(1) translateY(0)'
    }))
  ]),
  transition(':leave', [
    animate('150ms ease-in', style({
      opacity: 0,
      transform: 'scale(0.8)'
    }))
  ])
]);


export const playerHighlightAnimation = trigger('playerHighlight', [
  state('inactive', style({
    boxShadow: 'none',
    transform: 'scale(1)'
  })),
  state('active', style({
    boxShadow: '0 0 20px 5px rgba(74, 222, 128, 0.5)',
    transform: 'scale(1.02)'
  })),
  state('folded', style({
    opacity: 0.5,
    filter: 'grayscale(50%)'
  })),
  transition('* => active', animate('200ms ease-out')),
  transition('active => *', animate('300ms ease-in')),
  transition('* => folded', animate('400ms ease-out'))
]);


export const chipsAnimation = trigger('chipsMove', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'translateY(20px)'
    }),
    animate('300ms ease-out', style({
      opacity: 1,
      transform: 'translateY(0)'
    }))
  ]),
  transition('* => *', [
    animate('200ms ease-out')
  ])
]);


export const slideInAnimation = trigger('slideIn', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'translateX(100%)'
    }),
    animate('300ms cubic-bezier(0.25, 0.8, 0.25, 1)', style({
      opacity: 1,
      transform: 'translateX(0)'
    }))
  ]),
  transition(':leave', [
    animate('200ms ease-in', style({
      opacity: 0,
      transform: 'translateX(100%)'
    }))
  ])
]);


export const fadeAnimation = trigger('fade', [
  transition(':enter', [
    style({ opacity: 0 }),
    animate('200ms ease-out', style({ opacity: 1 }))
  ]),
  transition(':leave', [
    animate('150ms ease-in', style({ opacity: 0 }))
  ])
]);


export const winnerAnimation = trigger('winner', [
  transition(':enter', [
    animate('600ms ease-out', keyframes([
      style({ opacity: 0, transform: 'scale(0.5)', offset: 0 }),
      style({ opacity: 1, transform: 'scale(1.1)', offset: 0.7 }),
      style({ opacity: 1, transform: 'scale(1)', offset: 1 })
    ]))
  ])
]);


export const equityFillAnimation = trigger('equityFill', [
  transition('* => *', [
    animate('500ms cubic-bezier(0.4, 0, 0.2, 1)')
  ])
]);


export const pulseAnimation = trigger('pulse', [
  transition('* => pulse', [
    animate('300ms ease-in-out', keyframes([
      style({ transform: 'scale(1)', offset: 0 }),
      style({ transform: 'scale(1.05)', offset: 0.5 }),
      style({ transform: 'scale(1)', offset: 1 })
    ]))
  ])
]);


export const timelineItemAnimation = trigger('timelineItem', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'translateX(-20px)',
      height: 0
    }),
    animate('250ms ease-out', style({
      opacity: 1,
      transform: 'translateX(0)',
      height: '*'
    }))
  ])
]);


export const analysisOverlayAnimation = trigger('analysisOverlay', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'translateY(-10px)'
    }),
    group([
      animate('200ms ease-out', style({ opacity: 1 })),
      animate('300ms cubic-bezier(0.25, 0.8, 0.25, 1)',
        style({ transform: 'translateY(0)' })
      )
    ])
  ]),
  transition(':leave', [
    animate('150ms ease-in', style({
      opacity: 0,
      transform: 'translateY(-10px)'
    }))
  ])
]);


export const handReplayAnimations = [
  cardFlipAnimation,
  cardRevealAnimation,
  cardDealAnimation,
  potChangeAnimation,
  actionPopAnimation,
  playerHighlightAnimation,
  chipsAnimation,
  slideInAnimation,
  fadeAnimation,
  winnerAnimation,
  equityFillAnimation,
  pulseAnimation,
  timelineItemAnimation,
  analysisOverlayAnimation
];
