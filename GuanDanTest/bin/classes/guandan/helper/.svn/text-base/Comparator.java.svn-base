package guandan.helper;

import java.util.ArrayList;

import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;
import guandan.constants.PokerType;
import guandan.game.Poker;

public class Comparator {

	/**
	 * 比较两张牌的大小 考虑了牌级
	 * 
	 * @param thisPoker
	 *            :Poker类对象
	 * @param thatPoker
	 *            :Poker类对象
	 * @param grade
	 *            :当前牌局的牌级
	 * @return: 返回1表示thisPoker比thatPoker大 返回0表示一样大 返回-1则表示thisPoker比thatPoker小
	 */
	public static int comparePoker(Poker thisPoker, Poker thatPoker, int grade) {

		/**
		 * 比较流程如下: 1. 先看两张牌中是否有大小鬼,存在: a. 都是鬼; b. 一个是鬼,一个不是;
		 ************************************************ 
		 * 2. 若都不是鬼,看两张牌点数是否等于当前牌级,存在: a. 两张牌点数都等于当前牌级; b. 一个等于,一个不等于;
		 ************************************************* 
		 * 3. 若不等于当前牌级,则看两张牌中是否存在Ace,有: a. 两张牌都是Ace; b. 一张是,一张不是;
		 * 
		 * 注:这是设计时的一个失误,本来掼蛋中除鬼和主牌外Ace最大, 但由于设计者没搞清楚,误以为Ace最小,所以把Ace的点数定为1,
		 * 使得判断的时候需要多加这样一道工序 ************************************************ 4.
		 * 若都不是Ace,则可以纯粹根据牌的点数比较了
		 */

		/**
		 * 1. 鬼牌存在的情况
		 */
		if (thisPoker.pattern == PokerPattern.JOKER
				&& thatPoker.pattern == PokerPattern.JOKER) {
			if (thisPoker.points > thatPoker.points)
				return 1;
			else if (thisPoker.points < thatPoker.points)
				return -1;
			else
				return 0;
		} else if (thisPoker.pattern == PokerPattern.JOKER) {
			return 1;
		} else if (thatPoker.pattern == PokerPattern.JOKER) {
			return -1;
		}

		/**
		 * 2. 存在主牌的情况
		 */
		else if (thisPoker.points == grade && thatPoker.points == grade) {
			return 0;
		} else if (thisPoker.points == grade) {
			return 1;
		} else if (thatPoker.points == grade) {
			return -1;
		}

		/**
		 * 3. 存在Ace的情况
		 */
		else if (thisPoker.points == PokerPoints.ACE
				&& thatPoker.points == PokerPoints.ACE)
			return 0;
		else if (thisPoker.points == PokerPoints.ACE)
			return 1;
		else if (thatPoker.points == PokerPoints.ACE)
			return -1;

		/**
		 * 4. 纯粹比较点数的情况
		 */
		else {
			if (thisPoker.points > thatPoker.points)
				return 1;
			else if (thisPoker.points < thatPoker.points)
				return -1;
			else
				return 0;
		}
	}

	/**
	 * 
	 * @param thisPokers
	 * @param thatPokers
	 * @return: 1:前者大于后者,而且二者是可比的 0:前者不大于后者,二者是可比的 -1:二者不可比
	 * 
	 * 
	 */
	public static final int comparePokers(ArrayList<Poker> thisPokers,
			ArrayList<Poker> thatPokers, int grade) {

		/**
		 * 对牌的排序还要搞一搞啊 比如三带二,三张牌要放左边 而不是看牌的大小
		 * 
		 * 还是该从小到大排序
		 */

		int type1 = Parser.getPokerType(thisPokers, grade)[0];
		int type2 = Parser.getPokerType(thatPokers, grade)[0];

		if (type1 == PokerType.INVALID_TYPE || type2 == PokerType.INVALID_TYPE)
			return -1;

		/**
		 * 牌型不同时可比较的情况有: 四王牌,其他 其他,四王 炸弹,其他 其他,炸弹 同花顺,其他 其他,同花顺
		 */
		else if (type1 != type2) {
			if (type1 == PokerType.FOUR_JOKER)
				return 1;
			else if (type2 == PokerType.FOUR_JOKER)
				return 0;
			else if (type1 == PokerType.BOMB) {
				if (type2 == PokerType.STRAIGHT_FLUSH && thisPokers.size() <= 5)
					return 0;
				else
					return 1;
			} else if (type2 == PokerType.BOMB) {
				if (type1 == PokerType.STRAIGHT_FLUSH && thatPokers.size() <= 5)
					return 1;
				else
					return 0;
			} else if (type1 == PokerType.STRAIGHT_FLUSH) {
				return 1;
			} else if (type2 == PokerType.STRAIGHT_FLUSH) {
				return 0;
			} else
				return -1;
		}
		/**
		 * 牌型一样,先看牌的张数是否一样: 1. 只有炸弹时才可能出现牌的张数不一样 2. 否则从左到右一张一张比较
		 */
		else {
			if ((type1 == PokerType.BOMB)
					&& (thisPokers.size() != thatPokers.size())) {
				if (thisPokers.size() > thatPokers.size())
					return 1;
				else
					return 0;
			} else {
				/**
				 * 先将二者排序 调用sortPokers方法
				 */
				thisPokers = Sorter.sortPokers(thisPokers, type1, grade);
				thatPokers = Sorter.sortPokers(thatPokers, type2, grade);

				/**
				 * 单张,对子,炸弹,三张,三带二,钢板 比较第一张牌即可得出大小
				 */
				if (type1 == PokerType.SINGLE || type1 == PokerType.DOUBLE
						|| type1 == PokerType.TRIPLE || type1 == PokerType.BOMB
						|| type1 == PokerType.TRIPLE_WITH_DOUBLE
						|| type1 == PokerType.DOUBLE_TRIPLE_STRAIGHT) {
					Poker thisPoker = thisPokers.get(0);
					Poker thatPoker = thatPokers.get(0);

					int result = comparePoker(thisPoker, thatPoker, grade);
					if (result == 1)
						return 1;
					else
						return 0;
				}
				/**
				 * 三连对,顺子,同花顺 从大到小一张一张比较 并且前三张比较过之后一定能得出结果
				 * 因为这3中牌型之中可能出现"配牌"在头两张的情形
				 */
				else {
					int current = 0;
					while (current <= 1) {
						Poker thisPoker = thisPokers.get(current);
						Poker thatPoker = thatPokers.get(current);

						if (Helper.isMasterCard(thisPoker, grade)
								|| Helper.isMasterCard(thatPoker, grade)) {
							current++;
						} else {
							int result = comparePoker(thisPoker, thatPoker,
									grade);
							if (result == 1)
								return 1;
							else
								return 0;
						}
					}

					/**
					 * 前2张没比较出来 比较第三张即可
					 */
					int result = comparePoker(thisPokers.get(2),
							thatPokers.get(2), grade);
					if (result == 1)
						return 1;
					else
						return 0;
				}
			}
		}

	}
}
