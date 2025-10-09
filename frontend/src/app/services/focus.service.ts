import { Injectable, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';


@Injectable({
  providedIn: 'root'
})
export class FocusService {

  private readonly document = inject(DOCUMENT);


  private readonly _isKeyboardNavigation = signal(false);
  readonly isKeyboardNavigation = this._isKeyboardNavigation.asReadonly();


  private focusStack: HTMLElement[] = [];
  private trapContainer: HTMLElement | null = null;
  private trapCleanup: (() => void) | null = null;


  private readonly FOCUSABLE_SELECTORS = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
    '[contenteditable="true"]'
  ].join(', ');

  constructor() {
    this.initKeyboardDetection();
  }


  private initKeyboardDetection(): void {

    this.document.addEventListener('keydown', (event: KeyboardEvent) => {
      if (event.key === 'Tab') {
        this._isKeyboardNavigation.set(true);
        this.document.body.classList.add('keyboard-navigation-active');
      }
    });


    this.document.addEventListener('mousedown', () => {
      this._isKeyboardNavigation.set(false);
      this.document.body.classList.remove('keyboard-navigation-active');
    });


    this.document.addEventListener('touchstart', () => {
      this._isKeyboardNavigation.set(false);
      this.document.body.classList.remove('keyboard-navigation-active');
    });
  }






  trapFocus(container: HTMLElement): void {
    if (!container) return;


    const currentFocus = this.document.activeElement as HTMLElement;
    if (currentFocus) {
      this.focusStack.push(currentFocus);
    }

    this.trapContainer = container;


    setTimeout(() => {
      this.focusFirst(container);
    }, 50);


    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return;

      const focusableElements = this.getFocusableElements(container);
      if (focusableElements.length === 0) return;

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      const activeElement = this.document.activeElement;

      if (event.shiftKey) {

        if (activeElement === firstElement || !container.contains(activeElement)) {
          event.preventDefault();
          lastElement.focus();
        }
      } else {

        if (activeElement === lastElement || !container.contains(activeElement)) {
          event.preventDefault();
          firstElement.focus();
        }
      }
    };

    this.document.addEventListener('keydown', handleKeydown);

    this.trapCleanup = () => {
      this.document.removeEventListener('keydown', handleKeydown);
    };
  }


  releaseTrap(): void {
    if (this.trapCleanup) {
      this.trapCleanup();
      this.trapCleanup = null;
    }

    this.trapContainer = null;


    const previousFocus = this.focusStack.pop();
    if (previousFocus && this.document.body.contains(previousFocus)) {
      setTimeout(() => {
        previousFocus.focus();
      }, 50);
    }
  }






  getFocusableElements(container: HTMLElement): HTMLElement[] {
    const elements = Array.from(
      container.querySelectorAll<HTMLElement>(this.FOCUSABLE_SELECTORS)
    );


    return elements.filter(el => {
      return !el.hasAttribute('hidden') &&
             el.getAttribute('aria-hidden') !== 'true' &&
             el.offsetParent !== null &&
             getComputedStyle(el).visibility !== 'hidden';
    });
  }


  focusFirst(container: HTMLElement): void {
    const elements = this.getFocusableElements(container);
    if (elements.length > 0) {
      elements[0].focus();
    }
  }


  focusLast(container: HTMLElement): void {
    const elements = this.getFocusableElements(container);
    if (elements.length > 0) {
      elements[elements.length - 1].focus();
    }
  }


  focusElement(element: HTMLElement | null): void {
    if (element && typeof element.focus === 'function') {
      element.focus();
    }
  }


  focusBySelector(selector: string): void {
    const element = this.document.querySelector<HTMLElement>(selector);
    this.focusElement(element);
  }


  isFocused(element: HTMLElement): boolean {
    return this.document.activeElement === element;
  }


  getActiveElement(): HTMLElement | null {
    return this.document.activeElement as HTMLElement;
  }


  blur(): void {
    const active = this.document.activeElement as HTMLElement;
    if (active && typeof active.blur === 'function') {
      active.blur();
    }
  }






  skipToMain(): void {
    const main = this.document.querySelector<HTMLElement>('main, [role="main"], #main-content');
    if (main) {
      main.setAttribute('tabindex', '-1');
      main.focus();

      setTimeout(() => main.removeAttribute('tabindex'), 100);
    }
  }


  skipTo(elementId: string): void {
    const element = this.document.getElementById(elementId);
    if (element) {
      element.setAttribute('tabindex', '-1');
      element.focus();
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      setTimeout(() => element.removeAttribute('tabindex'), 100);
    }
  }






  setupRovingTabindex(
    container: HTMLElement,
    selector: string,
    initialIndex = 0
  ): { cleanup: () => void } {
    const elements = Array.from(container.querySelectorAll<HTMLElement>(selector));
    if (elements.length === 0) return { cleanup: () => { /* no elements to cleanup */ } };

    let currentIndex = initialIndex;


    elements.forEach((el, index) => {
      el.setAttribute('tabindex', index === currentIndex ? '0' : '-1');
    });

    const handleKeydown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement;
      const targetIndex = elements.indexOf(target);
      if (targetIndex === -1) return;

      let newIndex = currentIndex;

      switch (event.key) {
        case 'ArrowRight':
        case 'ArrowDown':
          event.preventDefault();
          newIndex = (currentIndex + 1) % elements.length;
          break;
        case 'ArrowLeft':
        case 'ArrowUp':
          event.preventDefault();
          newIndex = (currentIndex - 1 + elements.length) % elements.length;
          break;
        case 'Home':
          event.preventDefault();
          newIndex = 0;
          break;
        case 'End':
          event.preventDefault();
          newIndex = elements.length - 1;
          break;
        default:
          return;
      }

      if (newIndex !== currentIndex) {
        elements[currentIndex].setAttribute('tabindex', '-1');
        elements[newIndex].setAttribute('tabindex', '0');
        elements[newIndex].focus();
        currentIndex = newIndex;
      }
    };

    container.addEventListener('keydown', handleKeydown);

    return {
      cleanup: () => {
        container.removeEventListener('keydown', handleKeydown);
      }
    };
  }


  announceFocusChange(elementLabel: string): void {

    console.debug(`Focus moved to: ${elementLabel}`);
  }
}
