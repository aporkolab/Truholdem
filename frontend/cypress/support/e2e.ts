




import './commands';


declare global {
  namespace Cypress {
    interface Chainable {
      
      startGame(players?: Array<{ name: string; chips: number; isBot: boolean }>): Chainable<void>;
      
      
      playerAction(action: 'fold' | 'check' | 'call' | 'raise', amount?: number): Chainable<void>;
      
      
      waitForPhase(phase: string): Chainable<void>;
      
      
      getCurrentPlayer(): Chainable<JQuery<HTMLElement>>;
      
      
      interceptApi(): Chainable<void>;
    }
  }
}


const app = window.top;
if (app && !app.document.head.querySelector('[data-hide-command-log-request]')) {
  const style = app.document.createElement('style');
  style.innerHTML =
    '.command-name-request, .command-name-xhr { display: none }';
  style.setAttribute('data-hide-command-log-request', '');
  app.document.head.appendChild(style);
}
