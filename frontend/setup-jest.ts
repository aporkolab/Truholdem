import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import 'zone.js';
import 'zone.js/testing';


getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting()
);


function createJasmineSpy(name?: string): jest.Mock & {
  and: {
    returnValue: (val: unknown) => void;
    callFake: (fn: Function) => void;
  };
} {
  const spy = jest.fn() as jest.Mock & {
    and: {
      returnValue: (val: unknown) => void;
      callFake: (fn: Function) => void;
    };
  };

  spy.and = {
    returnValue: (val: unknown) => {
      spy.mockReturnValue(val);
    },
    callFake: (fn: Function) => {
      spy.mockImplementation(fn as any);
    },
  };

  return spy;
}


const globalAny = global as any;
globalAny.jasmine = {
  createSpyObj: <T = any>(
    baseName: string | string[],
    methodNames?: string[] | Record<string, unknown>,
    propertyNames?: Record<string, unknown>
  ): T => {
    if (Array.isArray(baseName)) {
      methodNames = baseName;
      baseName = 'mock';
    }

    const methods = Array.isArray(methodNames) ? methodNames : [];
    const properties =
      typeof methodNames === 'object' && !Array.isArray(methodNames)
        ? methodNames
        : propertyNames || {};

    const obj: Record<string, any> = {};

    for (const method of methods) {
      obj[method] = createJasmineSpy(method);
    }

    for (const [key, value] of Object.entries(properties)) {
      if (typeof value === 'object' && value !== null && 'subscribe' in value) {
        
        Object.defineProperty(obj, key, {
          get: () => value,
          configurable: true,
        });
      } else {
        Object.defineProperty(obj, key, {
          get: () => value,
          configurable: true,
        });
      }
    }

    return obj as T;
  },
};


Object.defineProperty(window, 'CSS', { value: null });
Object.defineProperty(document, 'doctype', {
  value: '<!DOCTYPE html>',
});
Object.defineProperty(window, 'getComputedStyle', {
  value: () => {
    return {
      display: 'none',
      appearance: ['-webkit-appearance'],
    };
  },
});


const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });
Object.defineProperty(window, 'sessionStorage', { value: localStorageMock });


Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});
