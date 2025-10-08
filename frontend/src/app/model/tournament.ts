import { Player } from './player';


export type TournamentStatus = 
  | 'REGISTERING'  
  | 'STARTING'     
  | 'RUNNING'      
  | 'PAUSED'       
  | 'FINAL_TABLE'  
  | 'FINISHED';    


export interface BlindLevel {
  level: number;
  smallBlind: number;
  bigBlind: number;
  ante: number;
  durationMinutes: number;
}


export interface TournamentTable {
  id: string;
  tableNumber: number;
  players: TournamentPlayer[];
  currentHandNumber: number;
  dealerPosition: number;
  isActive: boolean;
}


export interface TournamentPlayer extends Player {
  rank?: number;
  finishPosition?: number;
  prizeMoney?: number;
  tablesPlayed: number;
  handsWon: number;
  biggestPot: number;
  knockouts: number;
  isEliminated: boolean;
  eliminatedBy?: string;
  eliminatedAtLevel?: number;
}


export interface PrizeStructure {
  position: number;
  percentage: number;
  amount?: number;
}


export interface TournamentConfig {
  name: string;
  buyIn: number;
  startingChips: number;
  maxPlayers: number;
  minPlayers: number;
  blindLevels: BlindLevel[];
  levelDurationMinutes: number;
  breakAfterLevels: number;
  breakDurationMinutes: number;
  prizeStructure: PrizeStructure[];
  lateRegistrationLevels: number;
  rebuyAllowed: boolean;
  rebuyLevels: number;
  addOnAllowed: boolean;
}


export interface Tournament {
  id: string;
  name: string;
  status: TournamentStatus;
  config: TournamentConfig;
  
  
  currentLevel: number;
  currentBlinds: BlindLevel;
  levelStartTime: number;        
  levelEndTime: number;          
  nextBreakAtLevel: number;
  
  
  registeredPlayers: TournamentPlayer[];
  remainingPlayers: number;
  totalPlayers: number;
  
  
  tables: TournamentTable[];
  
  
  averageStack: number;
  largestStack: number;
  smallestStack: number;
  totalChipsInPlay: number;
  handsPlayed: number;
  
  
  prizePool: number;
  
  
  startTime?: number;
  endTime?: number;
  createdAt: number;
  updatedAt: number;
}


export interface TournamentListItem {
  id: string;
  name: string;
  status: TournamentStatus;
  buyIn: number;
  startingChips: number;
  currentLevel: number;
  registeredCount: number;
  maxPlayers: number;
  prizePool: number;
  startTime?: number;
  smallBlind: number;
  bigBlind: number;
}


export interface TournamentRegistrationRequest {
  tournamentId: string;
  playerName: string;
  buyIn?: number;
}


export interface TournamentUpdate {
  type: 'LEVEL_CHANGE' | 'PLAYER_ELIMINATED' | 'TABLE_BREAK' | 'BREAK_START' | 'BREAK_END' | 'TOURNAMENT_END';
  tournamentId: string;
  data: Partial<Tournament>;
  message: string;
  timestamp: number;
}


export const DEFAULT_BLIND_LEVELS: BlindLevel[] = [
  { level: 1, smallBlind: 25, bigBlind: 50, ante: 0, durationMinutes: 15 },
  { level: 2, smallBlind: 50, bigBlind: 100, ante: 0, durationMinutes: 15 },
  { level: 3, smallBlind: 75, bigBlind: 150, ante: 0, durationMinutes: 15 },
  { level: 4, smallBlind: 100, bigBlind: 200, ante: 25, durationMinutes: 15 },
  { level: 5, smallBlind: 150, bigBlind: 300, ante: 25, durationMinutes: 15 },
  { level: 6, smallBlind: 200, bigBlind: 400, ante: 50, durationMinutes: 15 },
  { level: 7, smallBlind: 300, bigBlind: 600, ante: 75, durationMinutes: 15 },
  { level: 8, smallBlind: 400, bigBlind: 800, ante: 100, durationMinutes: 15 },
  { level: 9, smallBlind: 500, bigBlind: 1000, ante: 100, durationMinutes: 15 },
  { level: 10, smallBlind: 600, bigBlind: 1200, ante: 150, durationMinutes: 15 },
  { level: 11, smallBlind: 800, bigBlind: 1600, ante: 200, durationMinutes: 15 },
  { level: 12, smallBlind: 1000, bigBlind: 2000, ante: 250, durationMinutes: 15 },
];


export const DEFAULT_PRIZE_STRUCTURE: PrizeStructure[] = [
  { position: 1, percentage: 50 },
  { position: 2, percentage: 30 },
  { position: 3, percentage: 20 },
];


export function calculateTimeRemaining(levelEndTime: number): number {
  const now = Date.now();
  return Math.max(0, levelEndTime - now);
}


export function formatTimeRemaining(milliseconds: number): string {
  const totalSeconds = Math.floor(milliseconds / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}


export function getNextBlindLevel(
  currentLevel: number, 
  blindLevels: BlindLevel[]
): BlindLevel | null {
  const nextIndex = blindLevels.findIndex(bl => bl.level === currentLevel + 1);
  return nextIndex >= 0 ? blindLevels[nextIndex] : null;
}
