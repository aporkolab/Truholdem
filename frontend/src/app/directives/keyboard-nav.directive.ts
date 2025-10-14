import {
  Directive,
  HostListener,
  Input,
  Output,
  EventEmitter,
  inject,
  ElementRef,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScreenReaderService } from '../services/screen-reader.service';


export interface KeyboardActionEvent {
  action: 'FOLD' | 'CHECK' | 'CALL' | 'RAISE' | 'ALL_IN' | 'HELP' | 'ESCAPE';
  key: string;
  timestamp: number;
}


@Directive({
  selector: '[appKeyboardNav]',
  standalone: true
})
export class KeyboardNavDirective implements OnInit {
  
  private readonly screenReader = inject(ScreenReaderService);
  private readonly elementRef = inject(ElementRef);
  
  
  private lastKeyTime = 0;
  private readonly DEBOUNCE_MS = 200;

  
  @Input() canAct = false;
  @Input() canCheck = false;
  @Input() canCall = false;
  @Input() callAmount = 0;
  @Input() minRaiseAmount = 0;
  @Input() isRaiseDialogOpen = false;
  @Input() disabled = false;

  
  @Output() keyAction = new EventEmitter<KeyboardActionEvent>();
  @Output() helpRequested = new EventEmitter<void>();
  @Output() escapePressed = new EventEmitter<void>();

  ngOnInit(): void {
    
    const el = this.elementRef.nativeElement;
    if (!el.hasAttribute('tabindex')) {
      el.setAttribute('tabindex', '0');
    }
  }

  @HostListener('keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    
    if (this.disabled) return;

    
    if (this.isInInputElement(event)) return;

    
    const now = Date.now();
    if (now - this.lastKeyTime < this.DEBOUNCE_MS) return;
    this.lastKeyTime = now;

    const key = event.key.toLowerCase();

    
    if (event.key === 'Escape') {
      event.preventDefault();
      this.emitAction('ESCAPE', event.key);
      this.escapePressed.emit();
      return;
    }

    
    if (key === 'h' || key === '?') {
      event.preventDefault();
      this.handleHelp();
      return;
    }

    
    if (!this.canAct) return;

    switch (key) {
      case 'f':
        event.preventDefault();
        this.handleFold();
        break;
      case 'c':
        event.preventDefault();
        this.handleCheckOrCall();
        break;
      case 'r':
        event.preventDefault();
        this.handleRaise();
        break;
      case 'a':
        event.preventDefault();
        this.handleAllIn();
        break;
    }
  }

  private isInInputElement(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement;
    const tagName = target.tagName.toLowerCase();
    return (
      tagName === 'input' ||
      tagName === 'textarea' ||
      tagName === 'select' ||
      target.isContentEditable
    );
  }

  private handleFold(): void {
    this.screenReader.announcePolite('Folding hand.');
    this.emitAction('FOLD', 'f');
  }

  private handleCheckOrCall(): void {
    if (this.canCheck) {
      this.screenReader.announcePolite('Checking.');
      this.emitAction('CHECK', 'c');
    } else if (this.canCall) {
      this.screenReader.announcePolite(`Calling ${this.callAmount} chips.`);
      this.emitAction('CALL', 'c');
    }
  }

  private handleRaise(): void {
    this.screenReader.announcePolite('Opening raise dialog.');
    this.emitAction('RAISE', 'r');
  }

  private handleAllIn(): void {
    this.screenReader.announcePolite('Going all-in!');
    this.emitAction('ALL_IN', 'a');
  }

  private handleHelp(): void {
    this.screenReader.announceAvailableActions(
      this.canCheck,
      this.canCall,
      this.callAmount,
      this.minRaiseAmount
    );
    this.helpRequested.emit();
  }

  private emitAction(action: KeyboardActionEvent['action'], key: string): void {
    this.keyAction.emit({
      action,
      key,
      timestamp: Date.now()
    });
  }
}


@Component({
  selector: 'app-keyboard-hint',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="keyboard-hint" aria-hidden="true">{{ key }}</span>
  `,
  styles: [`
    .keyboard-hint {
      display: none;
      position: absolute;
      top: -8px;
      right: -8px;
      width: 20px;
      height: 20px;
      background: var(--hint-bg, #333);
      color: var(--hint-color, #fff);
      border-radius: 4px;
      font-size: 11px;
      font-weight: bold;
      line-height: 20px;
      text-align: center;
      font-family: monospace;
      box-shadow: 0 1px 3px rgba(0,0,0,0.3);
      pointer-events: none;
    }

    :host-context(.keyboard-navigation-active) .keyboard-hint {
      display: block;
    }
  `]
})
export class KeyboardHintComponent {
  @Input() key = '';
}


@Component({
  selector: 'app-skip-link',
  standalone: true,
  template: `
    <a 
      href="#{{ targetId }}" 
      class="skip-link"
      (click)="handleClick($event)"
    >
      {{ label }}
    </a>
  `,
  styles: [`
    .skip-link {
      position: absolute;
      top: -100%;
      left: 16px;
      z-index: 10000;
      padding: 12px 24px;
      background: #005fcc;
      color: #fff;
      text-decoration: none;
      font-weight: bold;
      border-radius: 0 0 4px 4px;
      transition: top 0.2s ease;
    }

    .skip-link:focus {
      top: 0;
      outline: 3px solid #ffcc00;
      outline-offset: 2px;
    }
  `]
})
export class SkipLinkComponent {
  @Input() targetId = 'main-content';
  @Input() label = 'Skip to main content';

  handleClick(event: Event): void {
    event.preventDefault();
    const target = document.getElementById(this.targetId);
    if (target) {
      target.setAttribute('tabindex', '-1');
      target.focus();
      target.scrollIntoView({ behavior: 'smooth' });
      setTimeout(() => target.removeAttribute('tabindex'), 100);
    }
  }
}
