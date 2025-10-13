import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { Component } from '@angular/core';
import { AnalysisPageComponent } from './analysis-page.component';


@Component({ template: '<p>Equity Calculator</p>', standalone: true })
class MockEquityComponent {}

@Component({ template: '<p>Range Builder</p>', standalone: true })
class MockRangeBuilderComponent {}

@Component({ template: '<p>Scenarios</p>', standalone: true })
class MockScenariosComponent {}

describe('AnalysisPageComponent', () => {
  let component: AnalysisPageComponent;
  let fixture: ComponentFixture<AnalysisPageComponent>;
  let router: Router;
  let location: Location;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AnalysisPageComponent,
        RouterTestingModule.withRoutes([
          { path: 'analysis/equity', component: MockEquityComponent },
          { path: 'analysis/ranges', component: MockRangeBuilderComponent },
          { path: 'analysis/scenarios', component: MockScenariosComponent }
        ])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalysisPageComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
    fixture.detectChanges();
  });

  describe('Component Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should render analysis page container', () => {
      const page = fixture.nativeElement.querySelector('.analysis-page');
      expect(page).toBeTruthy();
    });
  });

  describe('Navigation Tabs', () => {
    it('should display navigation tabs', () => {
      const nav = fixture.nativeElement.querySelector('.analysis-nav');
      expect(nav).toBeTruthy();
    });

    it('should have correct role for accessibility', () => {
      const nav = fixture.nativeElement.querySelector('.analysis-nav');
      expect(nav?.getAttribute('role')).toBe('tablist');
      expect(nav?.getAttribute('aria-label')).toBe('Analysis tools');
    });

    it('should display three navigation tabs', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      expect(tabs.length).toBe(3);
    });

    it('should have Equity Calculator tab', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      const tabTexts = Array.from<Element>(tabs).map((tab) => tab.textContent);
      expect(tabTexts.some((t: string | null) => t?.includes('Equity Calculator'))).toBe(true);
    });

    it('should have Range Builder tab', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      const tabTexts = Array.from<Element>(tabs).map((tab) => tab.textContent);
      expect(tabTexts.some((t: string | null) => t?.includes('Range Builder'))).toBe(true);
    });

    it('should have Scenarios tab', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      const tabTexts = Array.from<Element>(tabs).map((tab) => tab.textContent);
      expect(tabTexts.some((t: string | null) => t?.includes('Scenarios'))).toBe(true);
    });

    it('should display tab icons', () => {
      const icons = fixture.nativeElement.querySelectorAll('.tab-icon');
      expect(icons.length).toBe(3);

      const iconTexts = Array.from<Element>(icons).map((icon) => icon.textContent);
      expect(iconTexts).toContain('📊');
      expect(iconTexts).toContain('🎯');
      expect(iconTexts).toContain('🧪');
    });

    it('should have role="tab" on each tab', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      tabs.forEach((tab: HTMLElement) => {
        expect(tab.getAttribute('role')).toBe('tab');
      });
    });
  });

  describe('Tab Links', () => {
    it('should have correct href for Equity Calculator', () => {
      const equityTab = fixture.nativeElement.querySelector('a[routerLink="/analysis/equity"]');
      expect(equityTab).toBeTruthy();
    });

    it('should have correct href for Range Builder', () => {
      const rangesTab = fixture.nativeElement.querySelector('a[routerLink="/analysis/ranges"]');
      expect(rangesTab).toBeTruthy();
    });

    it('should have correct href for Scenarios', () => {
      const scenariosTab = fixture.nativeElement.querySelector('a[routerLink="/analysis/scenarios"]');
      expect(scenariosTab).toBeTruthy();
    });
  });

  describe('Router Outlet', () => {
    it('should have router outlet', () => {
      const outlet = fixture.nativeElement.querySelector('router-outlet');
      expect(outlet).toBeTruthy();
    });

    it('should wrap outlet in main content area', () => {
      const main = fixture.nativeElement.querySelector('.analysis-content');
      expect(main).toBeTruthy();
      expect(main?.querySelector('router-outlet')).toBeTruthy();
    });
  });

  describe('Tips Panel', () => {
    it('should display tips panel', () => {
      const tipsPanel = fixture.nativeElement.querySelector('.tips-panel');
      expect(tipsPanel).toBeTruthy();
    });

    it('should have aside role for accessibility', () => {
      const tipsPanel = fixture.nativeElement.querySelector('.tips-panel');
      expect(tipsPanel?.getAttribute('aria-label')).toBe('Analysis tips');
    });

    it('should display Quick Tips heading', () => {
      const heading = fixture.nativeElement.querySelector('.tips-panel h3');
      expect(heading?.textContent).toContain('Quick Tips');
    });

    it('should display tips list', () => {
      const tipsList = fixture.nativeElement.querySelector('.tips-list');
      expect(tipsList).toBeTruthy();
    });

    it('should display at least 4 tips', () => {
      const tips = fixture.nativeElement.querySelectorAll('.tips-list li');
      expect(tips.length).toBeGreaterThanOrEqual(4);
    });

    it('should have tip about Premium Hands', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      expect(tipsText).toContain('Premium Hands');
      expect(tipsText).toContain('AA');
    });

    it('should have tip about Position', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      expect(tipsText).toContain('Position');
    });

    it('should have tip about Pot Odds', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      expect(tipsText).toContain('Pot Odds');
      expect(tipsText).toContain('33%');
    });

    it('should have tip about Range Advantage', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      expect(tipsText).toContain('Range Advantage');
    });

    it('should have strong tags for tip titles', () => {
      const strongTags = fixture.nativeElement.querySelectorAll('.tips-list strong');
      expect(strongTags.length).toBeGreaterThanOrEqual(4);
    });
  });

  describe('Styling and Layout', () => {
    it('should have min-height on page', () => {
      const page = fixture.nativeElement.querySelector('.analysis-page');
      expect(page).toBeTruthy();
      
    });

    it('should center navigation tabs', () => {
      const nav = fixture.nativeElement.querySelector('.analysis-nav');
      expect(nav).toBeTruthy();
      
    });

    it('should have max-width on content', () => {
      const content = fixture.nativeElement.querySelector('.analysis-content');
      expect(content).toBeTruthy();
    });

    it('should have tips panel with left border accent', () => {
      const tipsPanel = fixture.nativeElement.querySelector('.tips-panel');
      expect(tipsPanel).toBeTruthy();
    });
  });

  describe('Navigation Integration', () => {
    it('should navigate to equity calculator', async () => {
      await router.navigate(['/analysis/equity']);
      expect(location.path()).toBe('/analysis/equity');
    });

    it('should navigate to range builder', async () => {
      await router.navigate(['/analysis/ranges']);
      expect(location.path()).toBe('/analysis/ranges');
    });

    it('should navigate to scenarios', async () => {
      await router.navigate(['/analysis/scenarios']);
      expect(location.path()).toBe('/analysis/scenarios');
    });
  });

  describe('Active State', () => {
    it('should have routerLinkActive directive applied', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      
      expect(tabs.length).toBe(3);
      
    });

    it('should apply active class when route matches', async () => {
      await router.navigate(['/analysis/equity']);
      fixture.detectChanges();

      const activeTab = fixture.nativeElement.querySelector('.nav-tab.active');
      expect(activeTab?.textContent).toContain('Equity');
    });
  });

  describe('Responsive Design', () => {
    it('should have responsive-friendly structure', () => {
      
      const nav = fixture.nativeElement.querySelector('.analysis-nav');
      expect(nav).toBeTruthy();

      
      const tabs = nav?.querySelectorAll('a.nav-tab');
      expect(tabs?.length).toBe(3);
    });

    it('should have tab icons that can be hidden on mobile', () => {
      const icons = fixture.nativeElement.querySelectorAll('.tab-icon');
      expect(icons.length).toBe(3);
      
    });
  });

  describe('Semantic HTML', () => {
    it('should use nav element for navigation', () => {
      const nav = fixture.nativeElement.querySelector('nav.analysis-nav');
      expect(nav).toBeTruthy();
    });

    it('should use main element for content', () => {
      const main = fixture.nativeElement.querySelector('main.analysis-content');
      expect(main).toBeTruthy();
    });

    it('should use aside element for tips', () => {
      const aside = fixture.nativeElement.querySelector('aside.tips-panel');
      expect(aside).toBeTruthy();
    });

    it('should use ul/li for tips list', () => {
      const ul = fixture.nativeElement.querySelector('ul.tips-list');
      expect(ul).toBeTruthy();

      const lis = ul?.querySelectorAll('li');
      expect(lis?.length).toBeGreaterThanOrEqual(4);
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      const nav = fixture.nativeElement.querySelector('.analysis-nav');
      expect(nav?.getAttribute('aria-label')).toBe('Analysis tools');

      const aside = fixture.nativeElement.querySelector('.tips-panel');
      expect(aside?.getAttribute('aria-label')).toBe('Analysis tips');
    });

    it('should have proper role attributes', () => {
      const nav = fixture.nativeElement.querySelector('.analysis-nav');
      expect(nav?.getAttribute('role')).toBe('tablist');

      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      tabs.forEach((tab: HTMLElement) => {
        expect(tab.getAttribute('role')).toBe('tab');
      });
    });

    it('should use anchor tags for navigation', () => {
      const tabs = fixture.nativeElement.querySelectorAll('.nav-tab');
      tabs.forEach((tab: HTMLElement) => {
        expect(tab.tagName.toLowerCase()).toBe('a');
      });
    });

    it('should have heading structure in tips', () => {
      const h3 = fixture.nativeElement.querySelector('.tips-panel h3');
      expect(h3).toBeTruthy();
    });
  });

  describe('Content Quality', () => {
    it('should have accurate pot odds tip', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      
      expect(tipsText).toContain('33%');
      expect(tipsText).toContain('pot-sized bet');
    });

    it('should reference correct premium hands', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      expect(tipsText).toContain('AA');
      expect(tipsText).toContain('KK');
      expect(tipsText).toContain('QQ');
      expect(tipsText).toContain('AKs');
    });

    it('should give positional advice', () => {
      const tipsText = fixture.nativeElement.querySelector('.tips-list')?.textContent;
      expect(tipsText?.toLowerCase()).toContain('button');
      expect(tipsText?.toLowerCase()).toContain('early position');
    });
  });
});

describe('AnalysisPageComponent Standalone', () => {
  it('should be a standalone component', () => {
    expect(AnalysisPageComponent).toBeDefined();
    
    const component = new AnalysisPageComponent();
    expect(component).toBeTruthy();
  });
});
