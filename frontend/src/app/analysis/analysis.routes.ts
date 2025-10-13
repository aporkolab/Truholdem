import { Routes } from '@angular/router';

export const ANALYSIS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => 
      import('./analysis-page.component').then(m => m.AnalysisPageComponent),
    children: [
      {
        path: '',
        redirectTo: 'equity',
        pathMatch: 'full'
      },
      {
        path: 'equity',
        loadComponent: () => 
          import('./equity-calculator/equity-calculator.component').then(m => m.EquityCalculatorComponent),
        title: 'Equity Calculator - TruHoldem'
      },
      {
        path: 'ranges',
        loadComponent: () => 
          import('./range-builder/range-builder.component').then(m => m.RangeBuilderComponent),
        title: 'Range Builder - TruHoldem'
      },
      {
        path: 'scenarios',
        loadComponent: () => 
          import('./scenarios/scenarios.component').then(m => m.ScenariosComponent),
        title: 'Practice Scenarios - TruHoldem'
      }
    ]
  }
];
