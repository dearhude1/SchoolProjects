/**
 * Helper.java
 * @author 胡裕靖
 * March 23rd 2011
 * 
 * Helper类,为其他类提供一些静态的帮助函数
 * 例如:
 * 两张牌大小的比较;
 * 比较两手牌的大小;
 */
package guandan.helper;

import java.util.ArrayList;
import java.util.ListIterator;

import guandan.constants.PokerGrade;
import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;
import guandan.constants.PokerType;
import guandan.game.Poker;

public class Helper {

	/**
	 * 判断某张牌是否是当前牌局主牌
	 * 
	 * @param poker
	 *            :需要判断的牌
	 * @param grade
	 *            :当前牌局牌级
	 * @return:布尔值
	 */
	public static final boolean isMasterCard(Poker poker, int grade) {
		if (poker.pattern == PokerPattern.HEART && poker.points == grade)
			return true;
		else if (poker.pattern == PokerPattern.HEART
				&& poker.points == PokerPoints.ACE && grade == PokerGrade.ACE)
			return true;
		else
			return false;
	}

	/**
	 * 将一手扑克牌拆分成主牌和其他牌 也就是将主牌抽取出来
	 * 
	 * @param pokerList
	 * @return:元素个数为2的链表数组 注意第一个链表存放主牌 第二个存放非主牌
	 */
	public static ArrayList<Poker>[] splitPokers(ArrayList<Poker> pokerList,
			int grade) {
		ArrayList<Poker>[] returnLists = new ArrayList[2];
		returnLists[0] = new ArrayList<Poker>();
		returnLists[1] = new ArrayList<Poker>();

		for (int i = 0; i < pokerList.size(); i++) {
			Poker pk = pokerList.get(i);
			if (isMasterCard(pk, grade))
				returnLists[0].add(pk);
			else
				returnLists[1].add(pk);
		}

		return returnLists;
	}

	/**
	 * 不考虑逢人配,检验一手牌中其中每张牌是否花色都一样 存在大小鬼则返回false
	 */
	public static boolean isSamePattern(ArrayList<Poker> pokerList) {

		if (hasJoker(pokerList))
			return false;

		Poker firstPoker = pokerList.get(0);
		for (int i = 1; i < pokerList.size(); i++) {
			if (pokerList.get(i).pattern != firstPoker.pattern)
				return false;
		}
		return true;
	}

	public static boolean hasJoker(ArrayList<Poker> pokerList) {
		for (int i = 0; i < pokerList.size(); i++) {
			if (pokerList.get(i).pattern == PokerPattern.JOKER)
				return true;
		}
		return false;
	}

	/**
	 * 将牌级由数字转为字符串
	 * 
	 * @param grade
	 * @return
	 */
	public static String gradeToString(int grade) {
		if (grade >= PokerGrade.TWO && grade <= PokerGrade.TEN)
			return String.valueOf(grade);
		else if (grade == PokerGrade.ACE)
			return new String("A");
		else if (grade == PokerGrade.JACK)
			return new String("J");
		else if (grade == PokerGrade.QUEEN)
			return new String("Q");
		else if (grade == PokerGrade.KING)
			return new String("K");
		else
			return new String();
	}
}
