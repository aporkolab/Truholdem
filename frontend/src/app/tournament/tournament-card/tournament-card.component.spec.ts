import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TournamentCardComponent } from './tournament-card.component';
import { TournamentListItem } from '../../model/tournament';

describe('TournamentCardComponent', () => {
  let component: TournamentCardComponent;
  let fixture: ComponentFixture<TournamentCardComponent>;

  
  const createMockTournamentListItem = (overrides: Partial<TournamentListItem> = {}): TournamentListItem => ({
    id: overrides.id ?? 'tournament-1',
    name: overrides.name ?? 'Test Tournament',
    status: overrides.status ?? 'REGISTERING',
    buyIn: overrides.buyIn ?? 100,
    startingChips: overrides.startingChips ?? 10000,
    currentLevel: overrides.currentLevel ?? 1,
    registeredCount: overrides.registeredCount ?? 5,
    maxPlayers: overrides.maxPlayers ?? 9,
    prizePool: overrides.prizePool ?? 500,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TournamentCardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TournamentCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('tournament', createMockTournamentListItem());
    fixture.detectChanges();
  });

  
  
  

  describe('Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should display tournament name', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ name: 'My Test Tournament' }));
      fixture.detectChanges();

      const nameEl = fixture.nativeElement.querySelector('.tournament-name');
      expect(nameEl.textContent).toContain('My Test Tournament');
    });
  });

  
  
  

  describe('Status Display', () => {
    it('should show REGISTERING status correctly', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.detectChanges();

      expect(component.statusText()).toBe('Open');
      expect(component.statusIcon()).toBe('📝');
    });

    it('should show RUNNING status correctly', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'RUNNING' }));
      fixture.detectChanges();

      expect(component.statusText()).toBe('Running');
      expect(component.statusIcon()).toBe('▶️');
    });

    it('should show FINAL_TABLE status correctly', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'FINAL_TABLE' }));
      fixture.detectChanges();

      expect(component.statusText()).toBe('Final Table');
      expect(component.statusIcon()).toBe('🔥');
    });

    it('should show FINISHED status correctly', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'FINISHED' }));
      fixture.detectChanges();

      expect(component.statusText()).toBe('Finished');
      expect(component.statusIcon()).toBe('🏁');
    });

    it('should show PAUSED status correctly', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'PAUSED' }));
      fixture.detectChanges();

      expect(component.statusText()).toBe('Break');
      expect(component.statusIcon()).toBe('⏸️');
    });
  });

  
  
  

  describe('Info Display', () => {
    it('should display buy-in amount', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ buyIn: 500 }));
      fixture.detectChanges();

      const buyInEl = fixture.nativeElement.querySelector('.buy-in');
      expect(buyInEl.textContent).toContain('500');
    });

    it('should display player count', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({
        registeredCount: 7,
        maxPlayers: 9
      }));
      fixture.detectChanges();

      const element = fixture.nativeElement;
      expect(element.textContent).toContain('7/9');
    });

    it('should display prize pool', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ prizePool: 1000 }));
      fixture.detectChanges();

      const prizeEl = fixture.nativeElement.querySelector('.prize');
      expect(prizeEl.textContent).toContain('1,000');
    });

    it('should display blinds for running tournament', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({
        status: 'RUNNING',
        smallBlind: 100,
        bigBlind: 200,
        currentLevel: 5
      }));
      fixture.detectChanges();

      expect(component.isRunning()).toBe(true);
      
      const blindsInfo = fixture.nativeElement.querySelector('.blinds-info');
      expect(blindsInfo).toBeTruthy();
      expect(blindsInfo.textContent).toContain('100');
      expect(blindsInfo.textContent).toContain('200');
    });

    it('should not display blinds info for registering tournament', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.detectChanges();

      expect(component.isRunning()).toBe(false);
      
      const blindsInfo = fixture.nativeElement.querySelector('.blinds-info');
      expect(blindsInfo).toBeFalsy();
    });
  });

  
  
  

  describe('Fill Percentage', () => {
    it('should calculate fill percentage correctly', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ 
        registeredCount: 5, 
        maxPlayers: 10 
      }));
      fixture.detectChanges();

      expect(component.fillPercentage()).toBe(50);
    });

    it('should handle empty tournament', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ 
        registeredCount: 0, 
        maxPlayers: 10 
      }));
      fixture.detectChanges();

      expect(component.fillPercentage()).toBe(0);
    });

    it('should handle full tournament', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ 
        registeredCount: 9, 
        maxPlayers: 9 
      }));
      fixture.detectChanges();

      expect(component.fillPercentage()).toBe(100);
    });

    it('should show fill bar for registering tournaments', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.detectChanges();

      const fillBar = fixture.nativeElement.querySelector('.fill-bar');
      expect(fillBar).toBeTruthy();
    });
  });

  
  
  

  describe('Registration Status', () => {
    it('should allow registration when open and not full', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ 
        status: 'REGISTERING',
        registeredCount: 5,
        maxPlayers: 9
      }));
      fixture.detectChanges();

      expect(component.canRegister()).toBe(true);
    });

    it('should not allow registration when full', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ 
        status: 'REGISTERING',
        registeredCount: 9,
        maxPlayers: 9
      }));
      fixture.detectChanges();

      expect(component.canRegister()).toBe(false);
    });

    it('should not allow registration when running', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ 
        status: 'RUNNING',
        registeredCount: 5,
        maxPlayers: 9
      }));
      fixture.detectChanges();

      expect(component.canRegister()).toBe(false);
    });
  });

  
  
  

  describe('Event Emission', () => {
    it('should emit register event', () => {
      const tournament = createMockTournamentListItem({ id: 'test-emit' });
      fixture.componentRef.setInput('tournament', tournament);
      fixture.componentRef.setInput('showJoin', true);
      fixture.detectChanges();

      spyOn(component.register, 'emit');

      const registerBtn = fixture.nativeElement.querySelector('[data-cy="register-btn"]');
      registerBtn.click();

      expect(component.register.emit).toHaveBeenCalledWith(tournament);
    });

    it('should emit viewDetails event', () => {
      const tournament = createMockTournamentListItem({ id: 'test-details' });
      fixture.componentRef.setInput('tournament', tournament);
      fixture.detectChanges();

      spyOn(component.viewDetails, 'emit');

      const detailsBtn = fixture.nativeElement.querySelector('[data-cy="details-btn"]');
      detailsBtn.click();

      expect(component.viewDetails.emit).toHaveBeenCalledWith(tournament);
    });

    it('should stop propagation on register click', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.componentRef.setInput('showJoin', true);
      fixture.detectChanges();

      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');

      component.onRegister(event);

      expect(event.stopPropagation).toHaveBeenCalled();
    });

    it('should stop propagation on view details click', () => {
      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');

      component.onViewDetails(event);

      expect(event.stopPropagation).toHaveBeenCalled();
    });
  });

  
  
  

  describe('Conditional Rendering', () => {
    it('should show register button when showJoin is true and canRegister', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.componentRef.setInput('showJoin', true);
      fixture.detectChanges();

      const registerBtn = fixture.nativeElement.querySelector('[data-cy="register-btn"]');
      expect(registerBtn).toBeTruthy();
    });

    it('should hide register button when showJoin is false', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.componentRef.setInput('showJoin', false);
      fixture.detectChanges();

      const registerBtn = fixture.nativeElement.querySelector('[data-cy="register-btn"]');
      expect(registerBtn).toBeFalsy();
    });

    it('should always show details button', () => {
      const detailsBtn = fixture.nativeElement.querySelector('[data-cy="details-btn"]');
      expect(detailsBtn).toBeTruthy();
    });
  });

  
  
  

  describe('CSS Classes', () => {
    it('should apply registering class', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'REGISTERING' }));
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.tournament-card');
      expect(card.classList.contains('registering')).toBe(true);
    });

    it('should apply running class', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'RUNNING' }));
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.tournament-card');
      expect(card.classList.contains('running')).toBe(true);
    });

    it('should apply finished class', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'FINISHED' }));
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.tournament-card');
      expect(card.classList.contains('finished')).toBe(true);
    });

    it('should return correct statusClass for final table', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ status: 'FINAL_TABLE' }));
      fixture.detectChanges();

      expect(component.statusClass()).toBe('final-table');
    });
  });

  
  
  

  describe('Accessibility', () => {
    it('should have article role', () => {
      const card = fixture.nativeElement.querySelector('.tournament-card');
      expect(card.getAttribute('role')).toBe('article');
    });

    it('should have aria-label', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ name: 'Aria Test' }));
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.tournament-card');
      expect(card.getAttribute('aria-label')).toContain('Aria Test');
    });

    it('should have data-cy attribute with tournament id', () => {
      fixture.componentRef.setInput('tournament', createMockTournamentListItem({ id: 'test-id-123' }));
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('[data-cy="tournament-card-test-id-123"]');
      expect(card).toBeTruthy();
    });
  });
});
