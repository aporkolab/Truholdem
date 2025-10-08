import { Card } from './card';

describe('Card', () => {
  describe('Construction', () => {
    it('should create an instance with valid suit and value', () => {
      const card = new Card('HEARTS', 'ACE');
      expect(card).toBeTruthy();
      expect(card.suit).toBe('HEARTS');
      expect(card.value).toBe('ACE');
    });

    it('should create cards for all suits', () => {
      const hearts = new Card('HEARTS', 'KING');
      const diamonds = new Card('DIAMONDS', 'QUEEN');
      const clubs = new Card('CLUBS', 'JACK');
      const spades = new Card('SPADES', 'TEN');

      expect(hearts.suit).toBe('HEARTS');
      expect(diamonds.suit).toBe('DIAMONDS');
      expect(clubs.suit).toBe('CLUBS');
      expect(spades.suit).toBe('SPADES');
    });

    it('should create cards for all face values', () => {
      const ace = new Card('SPADES', 'ACE');
      const king = new Card('SPADES', 'KING');
      const queen = new Card('SPADES', 'QUEEN');
      const jack = new Card('SPADES', 'JACK');

      expect(ace.value).toBe('ACE');
      expect(king.value).toBe('KING');
      expect(queen.value).toBe('QUEEN');
      expect(jack.value).toBe('JACK');
    });

    it('should create cards for all number values', () => {
      const two = new Card('HEARTS', 'TWO');
      const five = new Card('HEARTS', 'FIVE');
      const ten = new Card('HEARTS', 'TEN');

      expect(two.value).toBe('TWO');
      expect(five.value).toBe('FIVE');
      expect(ten.value).toBe('TEN');
    });
  });

  describe('Properties', () => {
    it('should have correct suit property', () => {
      const card = new Card('DIAMONDS', 'SEVEN');
      expect(card.suit).toBe('DIAMONDS');
    });

    it('should have correct value property', () => {
      const card = new Card('CLUBS', 'NINE');
      expect(card.value).toBe('NINE');
    });

    it('should allow creating multiple distinct cards', () => {
      const card1 = new Card('HEARTS', 'ACE');
      const card2 = new Card('SPADES', 'ACE');
      const card3 = new Card('HEARTS', 'KING');

      expect(card1.suit).not.toBe(card2.suit);
      expect(card1.value).not.toBe(card3.value);
      expect(card1.value).toBe(card2.value);
    });
  });
});
