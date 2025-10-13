import {
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
  analysisOverlayAnimation,
  handReplayAnimations
} from './hand-replay.animations';

describe('Hand Replay Animations', () => {

  describe('Animation Triggers', () => {
    it('should export cardFlipAnimation', () => {
      expect(cardFlipAnimation).toBeDefined();
      expect(cardFlipAnimation.name).toBe('cardFlip');
    });

    it('should export cardRevealAnimation', () => {
      expect(cardRevealAnimation).toBeDefined();
      expect(cardRevealAnimation.name).toBe('cardReveal');
    });

    it('should export cardDealAnimation', () => {
      expect(cardDealAnimation).toBeDefined();
      expect(cardDealAnimation.name).toBe('cardDeal');
    });

    it('should export potChangeAnimation', () => {
      expect(potChangeAnimation).toBeDefined();
      expect(potChangeAnimation.name).toBe('potChange');
    });

    it('should export actionPopAnimation', () => {
      expect(actionPopAnimation).toBeDefined();
      expect(actionPopAnimation.name).toBe('actionPop');
    });

    it('should export playerHighlightAnimation', () => {
      expect(playerHighlightAnimation).toBeDefined();
      expect(playerHighlightAnimation.name).toBe('playerHighlight');
    });

    it('should export chipsAnimation', () => {
      expect(chipsAnimation).toBeDefined();
      expect(chipsAnimation.name).toBe('chipsMove');
    });

    it('should export slideInAnimation', () => {
      expect(slideInAnimation).toBeDefined();
      expect(slideInAnimation.name).toBe('slideIn');
    });

    it('should export fadeAnimation', () => {
      expect(fadeAnimation).toBeDefined();
      expect(fadeAnimation.name).toBe('fade');
    });

    it('should export winnerAnimation', () => {
      expect(winnerAnimation).toBeDefined();
      expect(winnerAnimation.name).toBe('winner');
    });

    it('should export equityFillAnimation', () => {
      expect(equityFillAnimation).toBeDefined();
      expect(equityFillAnimation.name).toBe('equityFill');
    });

    it('should export pulseAnimation', () => {
      expect(pulseAnimation).toBeDefined();
      expect(pulseAnimation.name).toBe('pulse');
    });

    it('should export timelineItemAnimation', () => {
      expect(timelineItemAnimation).toBeDefined();
      expect(timelineItemAnimation.name).toBe('timelineItem');
    });

    it('should export analysisOverlayAnimation', () => {
      expect(analysisOverlayAnimation).toBeDefined();
      expect(analysisOverlayAnimation.name).toBe('analysisOverlay');
    });
  });

  describe('Animation Collection', () => {
    it('should export handReplayAnimations array', () => {
      expect(handReplayAnimations).toBeDefined();
      expect(Array.isArray(handReplayAnimations)).toBe(true);
    });

    it('should contain all animations', () => {
      expect(handReplayAnimations.length).toBe(14);
    });

    it('should include cardFlipAnimation', () => {
      expect(handReplayAnimations).toContain(cardFlipAnimation);
    });

    it('should include cardRevealAnimation', () => {
      expect(handReplayAnimations).toContain(cardRevealAnimation);
    });

    it('should include potChangeAnimation', () => {
      expect(handReplayAnimations).toContain(potChangeAnimation);
    });

    it('should include playerHighlightAnimation', () => {
      expect(handReplayAnimations).toContain(playerHighlightAnimation);
    });

    it('should include winnerAnimation', () => {
      expect(handReplayAnimations).toContain(winnerAnimation);
    });

    it('should include analysisOverlayAnimation', () => {
      expect(handReplayAnimations).toContain(analysisOverlayAnimation);
    });
  });

  describe('CardFlip Animation States', () => {
    it('should have faceDown state', () => {
      const states = cardFlipAnimation.definitions;
      const faceDownState = states.find((def: unknown) => (def as { name: string }).name === 'faceDown');
      expect(faceDownState).toBeDefined();
    });

    it('should have faceUp state', () => {
      const states = cardFlipAnimation.definitions;
      const faceUpState = states.find((def: unknown) => (def as { name: string }).name === 'faceUp');
      expect(faceUpState).toBeDefined();
    });
  });

  describe('PlayerHighlight Animation States', () => {
    it('should have inactive state', () => {
      const states = playerHighlightAnimation.definitions;
      const inactiveState = states.find((def: unknown) => (def as { name: string }).name === 'inactive');
      expect(inactiveState).toBeDefined();
    });

    it('should have active state', () => {
      const states = playerHighlightAnimation.definitions;
      const activeState = states.find((def: unknown) => (def as { name: string }).name === 'active');
      expect(activeState).toBeDefined();
    });

    it('should have folded state', () => {
      const states = playerHighlightAnimation.definitions;
      const foldedState = states.find((def: unknown) => (def as { name: string }).name === 'folded');
      expect(foldedState).toBeDefined();
    });
  });

  describe('Animation Definitions Structure', () => {
    it('cardRevealAnimation should have enter and leave transitions', () => {
      const transitions = cardRevealAnimation.definitions.filter(
        (def: unknown) => (def as { type: number }).type === 1
      );
      expect(transitions.length).toBeGreaterThanOrEqual(2);
    });

    it('fadeAnimation should have enter and leave transitions', () => {
      const transitions = fadeAnimation.definitions.filter(
        (def: unknown) => (def as { type: number }).type === 1
      );
      expect(transitions.length).toBeGreaterThanOrEqual(2);
    });

    it('actionPopAnimation should have enter transition', () => {
      const transitions = actionPopAnimation.definitions.filter(
        (def: unknown) => (def as { type: number }).type === 1
      );
      expect(transitions.length).toBeGreaterThanOrEqual(1);
    });
  });
});

describe('Animation Integration', () => {
  it('should be importable in components', () => {
    const animationsArray = [
      cardRevealAnimation,
      potChangeAnimation,
      actionPopAnimation,
      playerHighlightAnimation,
      winnerAnimation,
      analysisOverlayAnimation,
      timelineItemAnimation
    ];

    animationsArray.forEach(animation => {
      expect(animation).toBeDefined();
      expect(typeof animation.name).toBe('string');
    });
  });

  it('should have unique animation names', () => {
    const names = handReplayAnimations.map(a => a.name);
    const uniqueNames = new Set(names);
    expect(uniqueNames.size).toBe(names.length);
  });
});
