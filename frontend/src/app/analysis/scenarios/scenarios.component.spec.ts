import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ScenariosComponent } from './scenarios.component';
import { PRESET_RANGES } from '../models/analysis.models';

describe('ScenariosComponent', () => {
  let component: ScenariosComponent;
  let fixture: ComponentFixture<ScenariosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScenariosComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ScenariosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should render page header', () => {
      const header = fixture.nativeElement.querySelector('.page-header h2');
      expect(header?.textContent).toContain('Practice Scenarios');
    });

    it('should display subtitle', () => {
      const subtitle = fixture.nativeElement.querySelector('.subtitle');
      expect(subtitle?.textContent).toContain('decision-making');
    });
  });

  describe('Scenario Selection View', () => {
    it('should display difficulty filters', () => {
      const filters = fixture.nativeElement.querySelectorAll('.difficulty-filters button');
      expect(filters.length).toBe(4); 
    });

    it('should have "All" filter active by default', () => {
      const allBtn = fixture.nativeElement.querySelector('.difficulty-filters button.active');
      expect(allBtn?.textContent).toContain('All');
    });

    it('should display scenario cards', () => {
      const cards = fixture.nativeElement.querySelectorAll('.scenario-card');
      expect(cards.length).toBeGreaterThan(0);
    });

    it('should display scenario name and description', () => {
      const card = fixture.nativeElement.querySelector('.scenario-card');
      const name = card?.querySelector('.scenario-name');
      const desc = card?.querySelector('.scenario-desc');

      expect(name?.textContent).toBeTruthy();
      expect(desc?.textContent).toBeTruthy();
    });

    it('should display difficulty badges', () => {
      const badges = fixture.nativeElement.querySelectorAll('.difficulty-badge');
      expect(badges.length).toBeGreaterThan(0);
    });

    it('should display pot size and position meta', () => {
      const meta = fixture.nativeElement.querySelector('.scenario-meta');
      expect(meta?.textContent).toContain('Pot');
      expect(meta?.textContent).toContain('Position');
    });
  });

  describe('Difficulty Filtering', () => {
    it('should filter scenarios by easy difficulty', () => {
      const easyBtn = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.difficulty-filters button')
      ).find((btn) => btn.textContent?.includes('Easy')) as HTMLButtonElement;

      easyBtn?.click();
      fixture.detectChanges();

      const visibleCards = fixture.nativeElement.querySelectorAll('.scenario-card');
      visibleCards.forEach((card: HTMLElement) => {
        const badge = card.querySelector('.difficulty-badge');
        expect(badge?.textContent?.toLowerCase()).toContain('easy');
      });
    });

    it('should update active filter button', () => {
      const mediumBtn = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.difficulty-filters button')
      ).find((btn) => btn.textContent?.includes('Medium')) as HTMLButtonElement;

      mediumBtn?.click();
      fixture.detectChanges();

      expect(mediumBtn?.classList.contains('active')).toBe(true);
    });
  });

  describe('Active Scenario View', () => {
    beforeEach(() => {
      
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();
    });

    it('should show back button when scenario is active', () => {
      const backBtn = fixture.nativeElement.querySelector('.back-btn');
      expect(backBtn).toBeTruthy();
      expect(backBtn?.textContent).toContain('Back');
    });

    it('should display scenario content', () => {
      const content = fixture.nativeElement.querySelector('.scenario-content');
      expect(content).toBeTruthy();
    });

    it('should display game info (position, pot, to call, pot odds)', () => {
      const infoItems = fixture.nativeElement.querySelectorAll('.game-info .info-item');
      expect(infoItems.length).toBe(4);

      const labels = Array.from<Element>(infoItems).map((item) =>
        item.querySelector('.label')?.textContent
      );
      expect(labels).toContain('Position');
      expect(labels).toContain('Pot');
      expect(labels).toContain('To Call');
      expect(labels).toContain('Pot Odds');
    });

    it('should display hero hand', () => {
      const handDisplay = fixture.nativeElement.querySelector('.hand-display');
      expect(handDisplay).toBeTruthy();

      const cards = handDisplay?.querySelectorAll('.card');
      expect(cards?.length).toBe(2);
    });

    it('should display villain range matrix', () => {
      const rangeDisplay = fixture.nativeElement.querySelector('.range-display');
      expect(rangeDisplay).toBeTruthy();
      expect(rangeDisplay?.textContent).toContain('Villain');
    });

    it('should display decision buttons', () => {
      const buttons = fixture.nativeElement.querySelectorAll('.decision-btn');
      expect(buttons.length).toBe(3);

      const buttonTexts = Array.from<Element>(buttons).map((btn) => btn.textContent?.trim());
      expect(buttonTexts).toContain('Fold');
      expect(buttonTexts).toContain('Call');
      expect(buttonTexts).toContain('Raise');
    });
  });

  describe('Answer Submission', () => {
    beforeEach(() => {
      
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();
    });

    it('should hide decision buttons after answer submission', () => {
      const foldBtn = fixture.nativeElement.querySelector('.decision-btn.fold');
      foldBtn?.click();
      fixture.detectChanges();

      const decisionSection = fixture.nativeElement.querySelector('.decision-section');
      expect(decisionSection).toBeFalsy();
    });

    it('should show answer section after submission', () => {
      const callBtn = fixture.nativeElement.querySelector('.decision-btn.call');
      callBtn?.click();
      fixture.detectChanges();

      const answerSection = fixture.nativeElement.querySelector('.answer-section');
      expect(answerSection).toBeTruthy();
    });

    it('should display explanation after answer', () => {
      const raiseBtn = fixture.nativeElement.querySelector('.decision-btn.raise');
      raiseBtn?.click();
      fixture.detectChanges();

      const explanation = fixture.nativeElement.querySelector('.explanation');
      expect(explanation?.textContent).toBeTruthy();
    });

    it('should show next scenario button', () => {
      const callBtn = fixture.nativeElement.querySelector('.decision-btn.call');
      callBtn?.click();
      fixture.detectChanges();

      const nextBtn = fixture.nativeElement.querySelector('.next-btn');
      expect(nextBtn).toBeTruthy();
      expect(nextBtn?.textContent).toContain('Next');
    });

    it('should apply correct class for correct answer', () => {
      
      const raiseBtn = fixture.nativeElement.querySelector('.decision-btn.raise');
      raiseBtn?.click();
      fixture.detectChanges();

      const answerSection = fixture.nativeElement.querySelector('.answer-section');
      expect(answerSection?.classList.contains('correct')).toBe(true);
    });

    it('should apply incorrect class for wrong answer', () => {
      
      const foldBtn = fixture.nativeElement.querySelector('.decision-btn.fold');
      foldBtn?.click();
      fixture.detectChanges();

      const answerSection = fixture.nativeElement.querySelector('.answer-section');
      expect(answerSection?.classList.contains('incorrect')).toBe(true);
    });
  });

  describe('Navigation', () => {
    it('should return to scenario selection on back button click', () => {
      
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();

      
      const backBtn = fixture.nativeElement.querySelector('.back-btn');
      backBtn?.click();
      fixture.detectChanges();

      
      const grid = fixture.nativeElement.querySelector('.scenario-grid');
      expect(grid).toBeTruthy();
    });

    it('should advance to next scenario on next button click', () => {
      
      const cards = fixture.nativeElement.querySelectorAll('.scenario-card');
      const firstScenarioName = cards[0]?.querySelector('.scenario-name')?.textContent;
      cards[0]?.click();
      fixture.detectChanges();

      
      const raiseBtn = fixture.nativeElement.querySelector('.decision-btn.raise');
      raiseBtn?.click();
      fixture.detectChanges();

      
      const nextBtn = fixture.nativeElement.querySelector('.next-btn');
      nextBtn?.click();
      fixture.detectChanges();

      
      const currentName = fixture.nativeElement.querySelector('.scenario-info h3')?.textContent;
      expect(currentName).not.toBe(firstScenarioName);
    });
  });

  describe('Statistics Tracking', () => {
    it('should display stats bar', () => {
      const statsBar = fixture.nativeElement.querySelector('.stats-bar');
      expect(statsBar).toBeTruthy();
      expect(statsBar?.textContent).toContain('Completed');
      expect(statsBar?.textContent).toContain('Correct');
      expect(statsBar?.textContent).toContain('Accuracy');
    });

    it('should update completed count after answer', () => {
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();

      const callBtn = fixture.nativeElement.querySelector('.decision-btn.call');
      callBtn?.click();
      fixture.detectChanges();

      const statsBar = fixture.nativeElement.querySelector('.stats-bar');
      expect(statsBar?.textContent).toContain('Completed: 1');
    });

    it('should update correct count for correct answer', () => {
      
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();

      const raiseBtn = fixture.nativeElement.querySelector('.decision-btn.raise');
      raiseBtn?.click();
      fixture.detectChanges();

      const statsBar = fixture.nativeElement.querySelector('.stats-bar');
      expect(statsBar?.textContent).toContain('Correct: 1');
    });

    it('should calculate accuracy correctly', () => {
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();

      
      const raiseBtn = fixture.nativeElement.querySelector('.decision-btn.raise');
      raiseBtn?.click();
      fixture.detectChanges();

      const statsBar = fixture.nativeElement.querySelector('.stats-bar');
      expect(statsBar?.textContent).toContain('100%');
    });
  });

  describe('Card Display', () => {
    beforeEach(() => {
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();
    });

    it('should apply correct color to heart cards', () => {
      const cards = fixture.nativeElement.querySelectorAll('.hand-display .card');

      const cardWithHeart = Array.from<Element>(cards).find((c) =>
        c.textContent?.includes('♥')
      ) as HTMLElement;

      if (cardWithHeart) {
        expect(cardWithHeart.style.color).toContain('rgb');
      }
    });

    it('should display suit symbols correctly', () => {
      const cardTexts = fixture.nativeElement.querySelector('.hand-display')?.textContent;
      
      expect(cardTexts).toMatch(/[♠♥♦♣]/);
    });
  });

  describe('Board Display', () => {
    it('should display community cards when present', () => {

      const cards = fixture.nativeElement.querySelectorAll('.scenario-card');
      const mediumCard = Array.from<Element>(cards).find((c) =>
        c.querySelector('.difficulty-badge')?.textContent?.includes('medium')
      ) as HTMLElement;

      mediumCard?.click();
      fixture.detectChanges();

      const boardDisplay = fixture.nativeElement.querySelector('.board-display');
      expect(boardDisplay).toBeTruthy();
    });
  });

  describe('Pot Odds Calculation', () => {
    it('should calculate pot odds correctly', () => {
      
      
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();

      const potOddsValue = fixture.nativeElement.querySelector(
        '.info-item:last-child .value'
      )?.textContent;
      expect(potOddsValue).toContain('40%');
    });
  });

  describe('Scenarios Data', () => {
    it('should have at least 3 scenarios', () => {
      const cards = fixture.nativeElement.querySelectorAll('.scenario-card');
      expect(cards.length).toBeGreaterThanOrEqual(3);
    });

    it('should have scenarios of different difficulties', () => {
      const badges = fixture.nativeElement.querySelectorAll('.difficulty-badge');
      const difficulties = new Set(
        Array.from<Element>(badges).map((b) => b.textContent?.trim().toLowerCase())
      );
      expect(difficulties.size).toBeGreaterThan(1);
    });

    it('should use PRESET_RANGES for villain ranges', () => {
      expect(PRESET_RANGES['premium']).toBeDefined();
      expect(PRESET_RANGES['broadway']).toBeDefined();
      expect(PRESET_RANGES['buttonOpen']).toBeDefined();
    });
  });

  describe('Accessibility', () => {
    it('should have accessible navigation', () => {
      const header = fixture.nativeElement.querySelector('.page-header');
      expect(header).toBeTruthy();
    });

    it('should have clickable scenario cards', () => {
      const cards = fixture.nativeElement.querySelectorAll('.scenario-card');
      cards.forEach((card: HTMLElement) => {
        expect(card.tagName.toLowerCase()).toBe('button');
      });
    });

    it('should have proper button semantics for decisions', () => {
      const card = fixture.nativeElement.querySelector('.scenario-card');
      card?.click();
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.decision-btn');
      buttons.forEach((btn: HTMLElement) => {
        expect(btn.tagName.toLowerCase()).toBe('button');
      });
    });
  });
});
