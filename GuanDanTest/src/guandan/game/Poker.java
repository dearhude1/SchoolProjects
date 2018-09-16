package guandan.game;

import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;

public class Poker {

	public int pattern = PokerPattern.INVALID_PATTERN;
	public int points = PokerPoints.INVALID_POINTS;

	public Poker(int pttrn, int pnts) {

		/**
		 * 对超出范围的处理
		 */
		if (pttrn >= PokerPattern.JOKER && pttrn <= PokerPattern.SPADE)
			pattern = pttrn;

		if (pattern == PokerPattern.JOKER) {
			if (pnts >= PokerPoints.LITTLE_JOKER
					&& pnts <= PokerPoints.OLD_JOKER)
				points = pnts;
		} else {
			if (pnts >= PokerPoints.ACE && pnts <= PokerPoints.KING)
				points = pnts;
		}

	}

	public boolean isInvalid() {
		if (pattern == PokerPattern.INVALID_PATTERN
				|| points == PokerPoints.INVALID_POINTS)
			return true;
		else
			return false;
	}

	public String toString() {
		String returnString = "";
		switch (pattern) {
		case PokerPattern.CLUB:
			returnString += "梅花";
			break;
		case PokerPattern.SQUARE:
			returnString += "方块";
			break;
		case PokerPattern.HEART:
			returnString += "红桃";
			break;
		case PokerPattern.SPADE:
			returnString += "黑桃";
			break;
		/*
		 * case PokerPattern.JOKER: returnString += "鬼"; break;
		 */
		default:
			break;
		}
		if (pattern == PokerPattern.JOKER) {
			if (points == PokerPoints.OLD_JOKER)
				returnString += "大";
			else
				returnString += "小";
		} else if (points >= 2 && points <= 10)
			returnString += String.valueOf(points);
		else if (points == PokerPoints.ACE)
			returnString += "A";
		else if (points == PokerPoints.JACK)
			returnString += "J";
		else if (points == PokerPoints.QUEEN)
			returnString += "Q";
		else if (points == PokerPoints.KING)
			returnString += "K";

		return returnString;
	}

}
