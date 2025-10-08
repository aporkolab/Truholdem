import { Card } from "./card";

export class Player {
	id: string;
	name: string;
	hand: Card[];
	chips: number;
	betAmount: number;
	totalBetInRound: number;
	folded: boolean;
	isBot: boolean;
	isAllIn: boolean;
	hasActed: boolean;
	seatPosition: number;

	constructor() {
		this.id = '';
		this.name = '';
		this.hand = [];
		this.chips = 0;
		this.betAmount = 0;
		this.totalBetInRound = 0;
		this.folded = false;
		this.isBot = false;
		this.isAllIn = false;
		this.hasActed = false;
		this.seatPosition = 0;
	}

	
	canAct(): boolean {
		return !this.folded && !this.isAllIn && this.chips > 0;
	}

	
	getDisplayName(): string {
		if (this.name.startsWith('Bot')) {
			return this.name.substring(3) || 'Bot';
		}
		return this.name || 'Anonymous';
	}

	
	isHuman(): boolean {
		return !this.isBot && !this.name.startsWith('Bot');
	}

	
	getStatusText(): string {
		if (this.folded) return 'Folded';
		if (this.isAllIn) return 'All-In';
		if (this.chips === 0) return 'Out';
		return '';
	}
}