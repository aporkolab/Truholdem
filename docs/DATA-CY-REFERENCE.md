# TruHoldem Data-CY Attribute Reference

## Overview
All Angular components have been updated with comprehensive `data-cy` attributes for reliable Cypress E2E testing. This document provides a complete reference of all selectors.

---

## Home Page (`/`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=home-page` | Container | Main home page container |
| `data-cy=logo` | Logo wrapper | Logo container |
| `data-cy=app-title` | h1 | Application title |
| `data-cy=tagline` | p | Tagline text |
| `data-cy=cta-section` | Section | Call-to-action section |
| `data-cy=welcome-message` | div | Welcome back message (logged in) |
| `data-cy=action-buttons` | div | Action buttons container (logged in) |
| `data-cy=new-game-btn` | button | Play Now button |
| `data-cy=leaderboard-btn` | button | Leaderboard button |
| `data-cy=history-btn` | button | Hand History button |
| `data-cy=auth-buttons` | div | Auth buttons (logged out) |
| `data-cy=login-btn` | button | Login button |
| `data-cy=register-btn` | button | Register button |
| `data-cy=guest-play-btn` | button | Play as guest button |
| `data-cy=features-section` | Section | Features section |
| `data-cy=feature-ai` | div | AI feature card |
| `data-cy=feature-stats` | div | Stats feature card |
| `data-cy=feature-compete` | div | Compete feature card |
| `data-cy=feature-replay` | div | Replay feature card |

---

## Lobby Page (`/lobby`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=lobby-page` | Container | Main lobby container |
| `data-cy=lobby-title` | h1 | Lobby title |
| `data-cy=players-section` | Section | Players configuration |
| `data-cy=player-list` | div | Player list container |
| `data-cy=human-player` | div | Human player entry |
| `data-cy=bot-player-{n}` | div | Bot player entry |
| `data-cy=player-name-input-{n}` | input | Player name input |
| `data-cy=player-chips-input-{n}` | input | Player chips input |
| `data-cy=remove-player-{n}` | button | Remove player button |
| `data-cy=add-bot-btn` | button | Add bot button |
| `data-cy=settings-section` | Section | Game settings |
| `data-cy=small-blind-input` | input | Small blind input |
| `data-cy=big-blind-display` | span | Big blind display |
| `data-cy=difficulty-select` | select | Bot difficulty |
| `data-cy=action-section` | div | Action buttons |
| `data-cy=start-game-btn` | button | Start game button |
| `data-cy=error-message` | div | Error message |
| `data-cy=back-btn` | button | Back to home |

---

## Game Table (`/game`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=poker-table` | Container | Main game table |
| `data-cy=loading-overlay` | div | Loading overlay |
| `data-cy=error-notification` | div | Error notification |
| `data-cy=game-info-bar` | div | Game info bar |
| `data-cy=game-phase` | div | Phase indicator |
| `data-cy=phase-value` | span | Current phase text |
| `data-cy=pot-display` | div | Pot display |
| `data-cy=pot-value` | span | Pot amount |
| `data-cy=hand-number` | div | Hand number |
| `data-cy=current-bet` | div | Current bet |
| `data-cy=player-seat` | div | Player seat |
| `data-cy=human-seat` | div | Human player seat |
| `data-cy=bot-seat-{n}` | div | Bot player seat |
| `data-cy=dealer-button` | div | Dealer button |
| `data-cy=player-cards` | div | Player cards container |
| `data-cy=player-card` | div | Individual player card |
| `data-cy=player-info` | div | Player info |
| `data-cy=player-name` | div | Player name |
| `data-cy=turn-indicator` | span | Turn indicator |
| `data-cy=player-chips` | div | Player chips |
| `data-cy=player-bet` | div | Player bet |
| `data-cy=player-status` | div | Player status |
| `data-cy=community-cards` | div | Community cards section |
| `data-cy=community-cards-container` | div | Cards container |
| `data-cy=community-card` | div | Individual community card |
| `data-cy=card-placeholder` | div | Card placeholder |
| `data-cy=actions-section` | div | Actions section |
| `data-cy=action-buttons` | div | Action buttons |
| `data-cy=fold-btn` | button | Fold button |
| `data-cy=check-btn` | button | Check button |
| `data-cy=call-btn` | button | Call button |
| `data-cy=all-in-btn` | button | All-in button |
| `data-cy=raise-input-component` | component | Raise input component |
| `data-cy=waiting-message` | div | Waiting message |
| `data-cy=folded-message` | div | Folded message |
| `data-cy=showdown-actions` | div | Showdown actions |
| `data-cy=show-results-btn` | button | Show results button |

### Result Modal
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=modal-overlay` | div | Modal overlay |
| `data-cy=result-modal` | div | Result modal |
| `data-cy=modal-title` | h5 | Modal title |
| `data-cy=modal-close-btn` | button | Close button |
| `data-cy=modal-body` | div | Modal body |
| `data-cy=winner-display` | p | Winner display |
| `data-cy=winning-hand` | p | Winning hand |
| `data-cy=final-pot` | div | Final pot |
| `data-cy=modal-footer` | div | Modal footer |
| `data-cy=next-hand-btn` | button | Next hand button |
| `data-cy=new-game-modal-btn` | button | New game button |
| `data-cy=back-to-lobby-btn` | button | Back to lobby |

---

## Raise Modal
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=raise-container` | div | Raise container |
| `data-cy=raise-btn` | button | Open raise modal |
| `data-cy=raise-overlay` | div | Raise overlay |
| `data-cy=raise-modal` | div | Raise modal |
| `data-cy=raise-title` | h4 | Raise title |
| `data-cy=raise-close-btn` | button | Close button |
| `data-cy=raise-info` | div | Raise info |
| `data-cy=min-raise-value` | span | Min raise |
| `data-cy=max-raise-value` | span | Max raise |
| `data-cy=pot-info` | span | Pot info |
| `data-cy=quick-bets` | div | Quick bet buttons |
| `data-cy=btn-min-raise` | button | Min raise |
| `data-cy=btn-half-pot` | button | Half pot |
| `data-cy=btn-full-pot` | button | Full pot |
| `data-cy=btn-quick-allin` | button | Quick all-in |
| `data-cy=amount-input-section` | div | Amount input section |
| `data-cy=raise-input` | input | Raise amount input |
| `data-cy=raise-slider` | input | Raise slider |
| `data-cy=raise-error` | div | Error message |
| `data-cy=raise-actions` | div | Action buttons |
| `data-cy=confirm-raise-btn` | button | Confirm raise |
| `data-cy=cancel-raise-btn` | button | Cancel |

---

## Leaderboard (`/leaderboard`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=leaderboard-page` | Container | Main container |
| `data-cy=leaderboard-header` | div | Header section |
| `data-cy=leaderboard-title` | h2 | Title |
| `data-cy=category-tabs` | div | Category tabs |
| `data-cy=tab-{category}` | button | Category tab |
| `data-cy=leaderboard-content` | div | Content area |
| `data-cy=leaderboard-table` | table | Data table |
| `data-cy=leaderboard-body` | tbody | Table body |
| `data-cy=leaderboard-row` | tr | Table row |
| `data-cy=leaderboard-row-{n}` | tr | Specific row |
| `data-cy=player-rank` | td | Player rank |
| `data-cy=rank-badge` | span | Rank badge |
| `data-cy=player-cell` | td | Player cell |
| `data-cy=player-name` | span | Player name |
| `data-cy=player-title` | span | Player title |
| `data-cy=stat-cell` | td | Stat cell |
| `data-cy=loading-state` | div | Loading state |
| `data-cy=empty-state` | div | Empty state |
| `data-cy=player-search-section` | div | Search section |
| `data-cy=player-search-input` | input | Search input |
| `data-cy=player-search-btn` | button | Search button |
| `data-cy=search-results` | div | Search results |
| `data-cy=search-result-item` | div | Result item |
| `data-cy=player-detail-modal` | div | Detail modal |

---

## Hand History (`/history`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=history-page` | Container | Main container |
| `data-cy=history-header` | div | Header section |
| `data-cy=history-title` | h1 | Title |
| `data-cy=loading-state` | div | Loading state |
| `data-cy=error-state` | div | Error state |
| `data-cy=empty-state` | div | Empty state |
| `data-cy=stats-summary` | div | Stats summary |
| `data-cy=total-hands-value` | span | Total hands |
| `data-cy=total-winnings-value` | span | Total winnings |
| `data-cy=win-rate-value` | span | Win rate |
| `data-cy=history-list` | div | History list |
| `data-cy=hand-card` | a | Hand card |
| `data-cy=hand-card-{n}` | a | Specific card |
| `data-cy=hand-info` | div | Hand info |
| `data-cy=hand-number` | span | Hand number |
| `data-cy=hand-result` | span | Result |
| `data-cy=hand-details` | div | Details |
| `data-cy=hand-type` | span | Hand type |
| `data-cy=hand-pot` | span | Pot size |
| `data-cy=hand-date` | span | Date |
| `data-cy=replay-hint` | span | Replay hint |
| `data-cy=back-to-home-btn` | a | Back button |

---

## Hand Replay (`/replay/:id`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=hand-replay-page` | Container | Main container |
| `data-cy=replay-header` | div | Header |
| `data-cy=replay-title` | h3 | Title |
| `data-cy=replay-content` | div | Content |
| `data-cy=players-section` | div | Players section |
| `data-cy=replay-player-card` | div | Player card |
| `data-cy=replay-player-{n}` | div | Specific player |
| `data-cy=board-section` | div | Board section |
| `data-cy=replay-phase` | div | Phase |
| `data-cy=replay-community-cards` | div | Community cards |
| `data-cy=replay-pot-display` | div | Pot display |
| `data-cy=current-action-display` | div | Current action |
| `data-cy=winner-display` | div | Winner display |
| `data-cy=action-timeline` | div | Timeline |
| `data-cy=timeline-scroll` | div | Timeline scroll |
| `data-cy=timeline-item` | div | Timeline item |
| `data-cy=replay-controls` | div | Controls |
| `data-cy=progress-bar` | div | Progress bar |
| `data-cy=control-buttons` | div | Control buttons |
| `data-cy=reset-btn` | button | Reset |
| `data-cy=prev-btn` | button | Previous |
| `data-cy=play-pause-btn` | button | Play/Pause |
| `data-cy=next-btn` | button | Next |
| `data-cy=end-btn` | button | End |
| `data-cy=speed-select` | select | Speed select |
| `data-cy=show-cards-checkbox` | input | Show cards |

---

## Settings (`/settings`)
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=settings-page` | Container | Main container |
| `data-cy=settings-title` | h2 | Title |
| `data-cy=sound-settings-section` | section | Sound settings |
| `data-cy=sound-enabled-toggle` | input | Sound toggle |
| `data-cy=volume-slider` | input | Volume slider |
| `data-cy=display-settings-section` | section | Display settings |
| `data-cy=card-animation-toggle` | input | Card animation |
| `data-cy=chip-animation-toggle` | input | Chip animation |
| `data-cy=table-color-options` | div | Table colors |
| `data-cy=card-back-options` | div | Card backs |
| `data-cy=gameplay-settings-section` | section | Gameplay |
| `data-cy=auto-muck-toggle` | input | Auto-muck |
| `data-cy=pot-odds-toggle` | input | Pot odds |
| `data-cy=confirm-actions-toggle` | input | Confirm actions |
| `data-cy=timing-settings-section` | section | Timing |
| `data-cy=time-limit-select` | select | Time limit |
| `data-cy=auto-fold-toggle` | input | Auto-fold |
| `data-cy=reset-defaults-btn` | button | Reset defaults |
| `data-cy=test-sound-btn` | button | Test sound |

---

## Authentication (`/auth/login`, `/auth/register`)

### Login
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=login-page` | Container | Main container |
| `data-cy=login-card` | div | Login card |
| `data-cy=login-form` | form | Login form |
| `data-cy=username-input` | input | Username input |
| `data-cy=password-input` | input | Password input |
| `data-cy=toggle-password-btn` | button | Toggle password |
| `data-cy=remember-me-checkbox` | input | Remember me |
| `data-cy=login-submit-btn` | button | Submit |
| `data-cy=switch-to-register-btn` | button | Switch to register |
| `data-cy=google-login-btn` | button | Google login |
| `data-cy=github-login-btn` | button | GitHub login |

### Register
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=register-page` | Container | Main container |
| `data-cy=register-card` | div | Register card |
| `data-cy=register-form` | form | Register form |
| `data-cy=first-name-input` | input | First name |
| `data-cy=last-name-input` | input | Last name |
| `data-cy=username-input` | input | Username |
| `data-cy=email-input` | input | Email |
| `data-cy=password-input` | input | Password |
| `data-cy=confirm-password-input` | input | Confirm password |
| `data-cy=terms-checkbox` | input | Terms checkbox |
| `data-cy=register-submit-btn` | button | Submit |
| `data-cy=switch-to-login-btn` | a | Switch to login |

---

## 404 Page
| Selector | Element | Description |
|----------|---------|-------------|
| `data-cy=not-found-page` | Container | Main container |
| `data-cy=error-code` | h1 | 404 code |
| `data-cy=error-title` | h2 | Error title |
| `data-cy=error-message` | p | Error message |
| `data-cy=go-home-btn` | a | Go home button |
| `data-cy=start-game-btn` | a | Start game button |

---

## Usage Examples

```typescript
// Click start game button
cy.get('[data-cy=start-game-btn]').click();

// Check pot value
cy.get('[data-cy=pot-value]').should('contain', '$100');

// Verify phase
cy.get('[data-cy=phase-value]').should('contain', 'Flop');

// Perform fold action
cy.get('[data-cy=fold-btn]').click();

// Open raise modal
cy.get('[data-cy=raise-btn]').click();
cy.get('[data-cy=raise-modal]').should('be.visible');
cy.get('[data-cy=raise-input]').clear().type('200');
cy.get('[data-cy=confirm-raise-btn]').click();

// Check community cards
cy.get('[data-cy=community-card]').should('have.length', 3);
```

---

## Best Practices

1. **Always prefer data-cy selectors** over class or ID selectors
2. **Use specific selectors** like `data-cy=fold-btn` instead of generic ones
3. **Use indexed selectors** for dynamic elements: `data-cy=player-card-{n}`
4. **Combine with assertions** for robust tests
5. **Wait for elements** before interacting: `cy.get('[data-cy=action-buttons]', { timeout: 10000 })`

