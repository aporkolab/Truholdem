import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardSelectorComponent } from './card-selector.component';
import { RANKS, SUITS, SelectedCard, SUIT_SYMBOLS } from '../models/analysis.models';

describe('CardSelectorComponent', () => {
  let component: CardSelectorComponent;
  let fixture: ComponentFixture<CardSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardSelectorComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(CardSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should display empty slots by default', () => {
      const slots = fixture.nativeElement.querySelectorAll('.empty-slot');
      expect(slots.length).toBe(2); 
    });

    it('should show toggle button', () => {
      const toggleBtn = fixture.nativeElement.querySelector('.toggle-picker-btn');
      expect(toggleBtn).toBeTruthy();
    });
  });

  describe('Card Display', () => {
    it('should display selected cards', () => {
      const cards: SelectedCard[] = [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ];
      
      fixture.componentRef.setInput('selectedCards', cards);
      fixture.detectChanges();
      
      const selectedCards = fixture.nativeElement.querySelectorAll('.selected-card');
      expect(selectedCards.length).toBe(2);
    });

    it('should show correct suit symbols', () => {
      const cards: SelectedCard[] = [
        { rank: 'A', suit: 'SPADES', display: 'A♠' }
      ];
      
      fixture.componentRef.setInput('selectedCards', cards);
      fixture.detectChanges();
      
      const suitEl = fixture.nativeElement.querySelector('.card-suit');
      expect(suitEl.textContent).toBe('♠');
    });

    it('should apply correct colors to suits', () => {
      const cards: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' }
      ];
      
      fixture.componentRef.setInput('selectedCards', cards);
      fixture.detectChanges();
      
      const cardEl = fixture.nativeElement.querySelector('.selected-card');
      expect(cardEl.style.color).toBe('rgb(229, 57, 53)'); 
    });
  });

  describe('Card Picker', () => {
    beforeEach(() => {
      
      const toggleBtn = fixture.nativeElement.querySelector('.toggle-picker-btn');
      toggleBtn.click();
      fixture.detectChanges();
    });

    it('should show picker when toggled', () => {
      const picker = fixture.nativeElement.querySelector('.card-picker');
      expect(picker).toBeTruthy();
    });

    it('should display 52 card buttons', () => {
      const cardBtns = fixture.nativeElement.querySelectorAll('.card-btn');
      expect(cardBtns.length).toBe(52);
    });

    it('should have 13 rank rows', () => {
      const rankRows = fixture.nativeElement.querySelectorAll('.rank-row');
      expect(rankRows.length).toBe(13);
    });

    it('should have 4 suit headers', () => {
      const suitHeaders = fixture.nativeElement.querySelectorAll('.suit-header');
      expect(suitHeaders.length).toBe(4);
    });
  });

  describe('Card Selection', () => {
    beforeEach(() => {
      
      const toggleBtn = fixture.nativeElement.querySelector('.toggle-picker-btn');
      toggleBtn.click();
      fixture.detectChanges();
    });

    it('should emit cardsChange when card is selected', () => {
      const spy = jest.spyOn(component.cardsChange, 'emit');
      
      const cardBtn = fixture.nativeElement.querySelector('.card-btn:not(:disabled)');
      cardBtn.click();
      
      expect(spy).toHaveBeenCalled();
    });

    it('should not allow more than maxCards selections', () => {
      fixture.componentRef.setInput('maxCards', 2);
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ]);
      fixture.detectChanges();

      const cardBtns = fixture.nativeElement.querySelectorAll('.card-btn');
      const enabledBtns = Array.from<Element>(cardBtns).filter(
        (btn) => {
          const htmlBtn = btn as HTMLButtonElement;
          return !htmlBtn.disabled && !htmlBtn.classList.contains('selected');
        }
      );



      expect(enabledBtns.length).toBe(0);
    });
  });

  describe('Card Removal', () => {
    it('should emit cardsChange when card is removed', () => {
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' }
      ]);
      fixture.detectChanges();
      
      const spy = jest.spyOn(component.cardsChange, 'emit');
      
      const removeBtn = fixture.nativeElement.querySelector('.remove-btn');
      removeBtn.click();
      
      expect(spy).toHaveBeenCalledWith([]);
    });

    it('should remove correct card when clicked', () => {
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ]);
      fixture.detectChanges();
      
      const spy = jest.spyOn(component.cardsChange, 'emit');
      
      
      const removeBtns = fixture.nativeElement.querySelectorAll('.remove-btn');
      removeBtns[0].click();
      
      const emittedCards = spy.mock.calls[0][0] as SelectedCard[];
      expect(emittedCards.length).toBe(1);
      expect(emittedCards[0].rank).toBe('K');
    });
  });

  describe('Dead Cards', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('deadCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'A', suit: 'HEARTS', display: 'A♥' }
      ]);
      
      
      const toggleBtn = fixture.nativeElement.querySelector('.toggle-picker-btn');
      toggleBtn.click();
      fixture.detectChanges();
    });

    it('should disable dead cards in picker', () => {
      const disabledBtns = fixture.nativeElement.querySelectorAll('.card-btn.disabled, .card-btn:disabled');
      expect(disabledBtns.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Clear All', () => {
    it('should show clear button when cards are selected', () => {
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' }
      ]);
      fixture.detectChanges();
      
      const clearBtn = fixture.nativeElement.querySelector('.clear-btn');
      expect(clearBtn).toBeTruthy();
    });

    it('should emit empty array when clear is clicked', () => {
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ]);
      fixture.detectChanges();
      
      const spy = jest.spyOn(component.cardsChange, 'emit');
      
      const clearBtn = fixture.nativeElement.querySelector('.clear-btn');
      clearBtn.click();
      
      expect(spy).toHaveBeenCalledWith([]);
    });
  });

  describe('Accessibility', () => {
    it('should have label input', () => {
      fixture.componentRef.setInput('label', 'Test Cards');
      fixture.detectChanges();
      
      const label = fixture.nativeElement.querySelector('.section-label');
      expect(label?.textContent).toBe('Test Cards');
    });

    it('should have aria-label on card selector', () => {
      fixture.componentRef.setInput('label', 'Hero Hand');
      fixture.detectChanges();
      
      const selector = fixture.nativeElement.querySelector('.card-selector');
      expect(selector?.getAttribute('aria-label')).toBe('Hero Hand');
    });

    it('should have keyboard accessible cards', () => {
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' }
      ]);
      fixture.detectChanges();
      
      const card = fixture.nativeElement.querySelector('.selected-card');
      expect(card?.getAttribute('tabindex')).toBe('0');
    });
  });

  describe('Different maxCards Settings', () => {
    it('should show 5 empty slots for board', () => {
      fixture.componentRef.setInput('maxCards', 5);
      fixture.componentRef.setInput('label', 'Board');
      fixture.detectChanges();
      
      const slots = fixture.nativeElement.querySelectorAll('.empty-slot');
      expect(slots.length).toBe(5);
    });

    it('should allow up to maxCards selections', () => {
      fixture.componentRef.setInput('maxCards', 5);
      fixture.componentRef.setInput('selectedCards', [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' },
        { rank: 'Q', suit: 'CLUBS', display: 'Q♣' }
      ]);
      fixture.detectChanges();
      
      const selectedCards = fixture.nativeElement.querySelectorAll('.selected-card');
      const emptySlots = fixture.nativeElement.querySelectorAll('.empty-slot');
      
      expect(selectedCards.length).toBe(3);
      expect(emptySlots.length).toBe(2);
    });
  });
});

describe('Card Constants', () => {
  it('should have 13 ranks', () => {
    expect(RANKS.length).toBe(13);
  });

  it('should have 4 suits', () => {
    expect(SUITS.length).toBe(4);
  });

  it('should have correct suit symbols', () => {
    expect(SUIT_SYMBOLS.HEARTS).toBe('♥');
    expect(SUIT_SYMBOLS.DIAMONDS).toBe('♦');
    expect(SUIT_SYMBOLS.CLUBS).toBe('♣');
    expect(SUIT_SYMBOLS.SPADES).toBe('♠');
  });
});
