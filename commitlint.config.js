


const config = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    
    'type-enum': [
      2,
      'always',
      [
        'feat',     
        'fix',      
        'docs',     
        'style',    
        'refactor', 
        'perf',     
        'test',     
        'build',    
        'ci',       
        'chore',    
        'revert',   
        'wip',      
      ],
    ],
    
    'scope-enum': [
      1, 
      'always',
      [
        'backend',
        'frontend',
        'api',
        'ui',
        'auth',
        'game',
        'tournament',
        'websocket',
        'docker',
        'ci',
        'docs',
        'deps',
        'config',
        'test',
        'db',
        'security',
      ],
    ],
    
    'subject-case': [2, 'always', 'lower-case'],
    'subject-empty': [2, 'never'],
    'subject-full-stop': [2, 'never', '.'],
    'subject-max-length': [2, 'always', 72],
    
    'body-max-line-length': [2, 'always', 100],
    
    'header-max-length': [2, 'always', 100],
  },
  helpUrl: 'https://www.conventionalcommits.org/',
};

module.exports = config;
