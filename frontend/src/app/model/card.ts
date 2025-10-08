export class Card {
	suit: 'HEARTS' | 'CLUBS' | 'DIAMONDS' | 'SPADES';
	value: 'TWO' | 'THREE' | 'FOUR' | 'FIVE' | 'SIX' | 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE';

	constructor(suit: 'HEARTS' | 'CLUBS' | 'DIAMONDS' | 'SPADES', value: 'TWO' | 'THREE' | 'FOUR' | 'FIVE' | 'SIX' | 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE') {
		this.suit = suit;
		this.value = value;
	}
}