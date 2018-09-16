package guandan.helper;

import java.util.ArrayList;

import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;
import guandan.constants.PokerType;
import guandan.game.Poker;

public class Parser {

	/**
	 * 获得一手牌的牌型和大小
	 * 
	 * 暂不考虑存在目标牌型的情况
	 * 
	 * @param pokerList
	 *            :需要判断的那手牌
	 * @param grade
	 *            :当前牌局的牌级
	 * @return:判断出的类型,是int值
	 */
	public static int[] getPokerType(ArrayList<Poker> pokerList, int grade) {
		/**
		 * 存放该函数的返回值 第一个元素存放牌的类型 第二个元素存放牌的大小
		 */
		int[] retArray = new int[2];

		int MASTER_CARD = 0;
		int NON_MASTER_CARD = 1;
		ArrayList<Poker>[] splitPokers = Helper.splitPokers(pokerList, grade);
		int masterCardNum = splitPokers[MASTER_CARD].size();

		if (pokerList == null || pokerList.size() == 0) {
			retArray[0] = PokerType.INVALID_TYPE;
		} else if (pokerList.size() == 1) {
			retArray[0] = PokerType.SINGLE;
		}

		/**
		 * 以下判断时 都是将牌分成两部分 一部分是主牌 另一部分非主牌
		 */
		else if (pokerList.size() == 2) {
			/**
			 * 两张牌检查是否是对子 注意配牌 也注意牌配不和大小鬼搭配
			 */
			if (masterCardNum == 0) {
				if (isDouble(splitPokers[NON_MASTER_CARD])) {
					retArray[0] = PokerType.DOUBLE;
				} else {
					retArray[0] = PokerType.INVALID_TYPE;
				}
			} else if (masterCardNum == 1) {
				if (splitPokers[NON_MASTER_CARD].get(0).pattern != PokerPattern.JOKER) {
					retArray[0] = PokerType.DOUBLE;
				} else {
					retArray[0] = PokerType.INVALID_TYPE;
				}

			} else
				retArray[0] = PokerType.DOUBLE;
		}
		/**
		 * 3张即检查是否是相同数值 注意配牌 不能配鬼
		 */
		else if (pokerList.size() == 3) {
			if (masterCardNum == 0) {
				if (isTriple(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.TRIPLE;
				else
					retArray[0] = PokerType.INVALID_TYPE;
			} else if (masterCardNum == 1) {
				if (isDouble(splitPokers[NON_MASTER_CARD])) {
					if (splitPokers[NON_MASTER_CARD].get(0).pattern != PokerPattern.JOKER)
						retArray[0] = PokerType.TRIPLE;
					else
						retArray[0] = PokerType.INVALID_TYPE;
				} else
					retArray[0] = PokerType.INVALID_TYPE;
			} else {
				if (splitPokers[NON_MASTER_CARD].get(0).pattern != PokerPattern.JOKER)
					retArray[0] = PokerType.TRIPLE;
				else
					retArray[0] = PokerType.INVALID_TYPE;
			}
		}
		/**
		 * 4张牌,检验: 四王牌 炸弹
		 */
		else if (pokerList.size() == 4) {
			if (masterCardNum == 0) {
				if (isBomb(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.BOMB;

				else if (isFourJoker(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.FOUR_JOKER;

				else
					retArray[0] = PokerType.INVALID_TYPE;
			} else if (masterCardNum == 1) {
				if (isTriple(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.BOMB;

				else
					retArray[0] = PokerType.INVALID_TYPE;
			} else {
				if (isDouble(splitPokers[NON_MASTER_CARD])
						&& splitPokers[NON_MASTER_CARD].get(0).pattern != PokerPattern.JOKER)
					retArray[0] = PokerType.BOMB;

				else
					retArray[0] = PokerType.INVALID_TYPE;
			}
		}
		/**
		 * 5张牌,需要检验以下牌型: 顺子 同花顺 三代二 五炸弹
		 */
		else if (pokerList.size() == 5) {
			if (masterCardNum == 0) {
				if (isStraight(splitPokers[NON_MASTER_CARD])) {
					if (Helper.isSamePattern(splitPokers[NON_MASTER_CARD]))
						retArray[0] = PokerType.STRAIGHT_FLUSH;

					else
						retArray[0] = PokerType.STRAIGHT;
				} else if (isBomb(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.BOMB;

				else if (isTriple_With_Double(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.TRIPLE_WITH_DOUBLE;

				else
					retArray[0] = PokerType.INVALID_TYPE;
			} else {
				if (canBeStraight(splitPokers[NON_MASTER_CARD])) {
					if (Helper.isSamePattern(splitPokers[NON_MASTER_CARD]))
						retArray[0] = PokerType.STRAIGHT_FLUSH;

					else
						retArray[0] = PokerType.STRAIGHT;
				} else if (isBomb(splitPokers[NON_MASTER_CARD])
						|| isTriple(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.BOMB;

				else if (canBeTriple_With_Double(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.TRIPLE_WITH_DOUBLE;

				else
					retArray[0] = PokerType.INVALID_TYPE;
			}
		}
		/**
		 * 6张牌,检验三种牌型: 钢板 三连对 六炸弹
		 */
		else if (pokerList.size() == 6) {
			if (masterCardNum == 0) {
				if (isBomb(splitPokers[NON_MASTER_CARD]))
					retArray[0] = PokerType.BOMB;
				else if (isTriple_Double_Straight(splitPokers[NON_MASTER_CARD])) {
					retArray[0] = PokerType.TRIPLE_DOUBLE_STRAIGHT;
				} else if (isDouble_Triple_Straight(splitPokers[NON_MASTER_CARD])) {
					retArray[0] = PokerType.DOUBLE_TRIPLE_STRAIGHT;
				} else
					retArray[0] = PokerType.INVALID_TYPE;
			} else {
				if (isBomb(splitPokers[NON_MASTER_CARD])) {
					retArray[0] = PokerType.BOMB;
				} else if (canBe_Triple_Double_Straight(splitPokers[NON_MASTER_CARD])) {
					retArray[0] = PokerType.TRIPLE_DOUBLE_STRAIGHT;
				} else if (canBe_Double_Triple_Straight(splitPokers[NON_MASTER_CARD])) {
					retArray[0] = PokerType.DOUBLE_TRIPLE_STRAIGHT;
				}
				/**
				 * 缺乏既能成为钢板也能成为三连对的处理
				 */

				else
					retArray[0] = PokerType.INVALID_TYPE;
			}
		}
		/**
		 * 大等于7张牌以上的只可能是炸弹
		 */
		else if (pokerList.size() >= 7 && pokerList.size() <= 10) {
			if (isBomb(splitPokers[NON_MASTER_CARD]))
				retArray[0] = PokerType.BOMB;
			else
				retArray[0] = PokerType.INVALID_TYPE;
		}
		/**
		 * 炸弹最多也只有10张牌 10张以上必定是无效牌型
		 */
		else
			retArray[0] = PokerType.INVALID_TYPE;

		/**
		 * 牌型获取后 获取牌的大小
		 * 
		 * 将牌进行排序,取其第一张牌的点数即可
		 */
		if (retArray[0] == PokerType.INVALID_TYPE)
			retArray[1] = -1;
		else {
			ArrayList<Poker> cpPokerList = new ArrayList<Poker>();
			for (int i = 0; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				Poker newPoker = new Poker(pk.pattern, pk.points);
				cpPokerList.add(newPoker);
			}

			cpPokerList = Sorter.sortPokers_reverse(cpPokerList, retArray[0],
					grade);

			if (cpPokerList.get(0).pattern == PokerPattern.JOKER
					&& cpPokerList.get(0).points == PokerPoints.OLD_JOKER)
				retArray[1] = 14;
			else
				retArray[1] = cpPokerList.get(0).points;
		}

		return retArray;
	}

	/**
	 * 一手去除掉主牌之后的牌,检验其是否是对子
	 * 
	 * @param pokerList
	 *            :去除掉主牌之后的一手牌
	 * @return:布尔值
	 */
	public static boolean isDouble(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 2)
			return false;
		else {
			Poker poker1 = pokerList.get(0);
			Poker poker2 = pokerList.get(1);

			return isDouble(poker1, poker2);
		}
	}

	/**
	 * 不考虑主牌因素,检查两张牌是否构成对子
	 * 
	 * @param poker1
	 * @param poker2
	 * @return
	 */
	public static boolean isDouble(Poker poker1, Poker poker2) {
		if (poker1.pattern == PokerPattern.JOKER
				&& poker2.pattern == PokerPattern.JOKER) {
			if (poker1.points == poker2.points)
				return true;
			else
				return false;
		} else if (poker1.pattern == PokerPattern.JOKER
				|| poker2.pattern == PokerPattern.JOKER)
			return false;
		else {
			if (poker1.points == poker2.points)
				return true;
			else
				return false;
		}
	}

	/**
	 * 一手去除掉主牌之后的牌,检验其是否是三张相同的牌
	 * 
	 * @param pokerList
	 *            :去除掉主牌之后的一手牌
	 * @return:布尔值
	 */
	public static boolean isTriple(ArrayList<Poker> pokerList) {

		if (pokerList.size() != 3)
			return false;
		else {
			Poker poker1 = pokerList.get(0);
			Poker poker2 = pokerList.get(1);
			Poker poker3 = pokerList.get(2);

			if (isDouble(poker1, poker2) && isDouble(poker1, poker3)
					&& isDouble(poker2, poker3))
				return true;
			else
				return false;
		}

	}

	/**
	 * 一手除掉主牌的牌,检验其是否是三带二
	 * 
	 * @param pokerList
	 * @return
	 */
	public static boolean isTriple_With_Double(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 5)
			return false;
		else {
			/**
			 * 先将牌整理为两部分,按照如下方法整理 若这手牌真的是三带二的话,则这两部分中: 一个必定是Triple,另一个必定是Double
			 */
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();

			Poker firstPoker = pokerList.get(0);
			pokerList1.add(firstPoker);

			for (int i = 1; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (isDouble(firstPoker, pk))
					pokerList1.add(pk);
				else
					pokerList2.add(pk);
			}

			if (isTriple(pokerList1) && isDouble(pokerList2))
				return true;
			else if (isTriple(pokerList2) && isDouble(pokerList1))
				return true;
			else
				return false;
		}
	}

	public static boolean isFourJoker(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 4)
			return false;
		else {
			Poker poker1 = pokerList.get(0);
			Poker poker2 = pokerList.get(1);
			Poker poker3 = pokerList.get(2);
			Poker poker4 = pokerList.get(3);

			if (poker1.pattern == PokerPattern.JOKER
					&& poker2.pattern == PokerPattern.JOKER
					&& poker3.pattern == PokerPattern.JOKER
					&& poker4.pattern == PokerPattern.JOKER)
				return true;
			else
				return false;
		}
	}

	/**
	 * 判断除掉主牌后的一手牌(三张或者四张),看其是否能够在 配一张主牌后成为三带二
	 * 
	 * @param pokerList
	 * @return:布尔值
	 */
	public static boolean canBeTriple_With_Double(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 4 && pokerList.size() != 3)
			return false;
		else if (isFourJoker(pokerList))
			return false;
		else {
			/**
			 * 将牌分成两部分
			 */
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();

			Poker firstPoker = pokerList.get(0);
			pokerList1.add(firstPoker);

			for (int i = 1; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (isDouble(firstPoker, pk))
					pokerList1.add(pk);
				else
					pokerList2.add(pk);
			}

			/**
			 * 4张牌时,可以是: 2,2 1,3 3,1
			 * 
			 * 3张牌时,可以是: 2,1 1,2
			 * 
			 * 注意单张牌不能是鬼牌 因为主牌不能配鬼
			 */
			if (pokerList.size() == 4) {
				if (isDouble(pokerList1) && isDouble(pokerList2))
					return true;
				else if (isTriple(pokerList1) && !Helper.hasJoker(pokerList2))
					return true;
				else if (isTriple(pokerList2) && !Helper.hasJoker(pokerList1))
					return true;
				else
					return false;
			} else {
				if (isDouble(pokerList1) && !Helper.hasJoker(pokerList2))
					return true;
				else if (isDouble(pokerList2) && !Helper.hasJoker(pokerList1))
					return true;
				else
					return false;
			}
		}
	}

	/**
	 * 一手去除掉主牌之后的牌,检验其是否是炸弹
	 * 
	 * @param pokerList
	 *            :去除掉主牌之后的一手牌
	 * @return:布尔值
	 */
	public static boolean isBomb(ArrayList<Poker> pokerList) {
		if (pokerList.size() < 4)
			return false;
		else {
			Poker firstPoker = pokerList.get(0);
			for (int i = 1; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (pk.points != firstPoker.points)
					return false;
			}
			return true;
		}
	}

	/**
	 * 一手除掉主牌之后的牌,检验其是否构成五张顺子
	 */
	public static boolean isStraight(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 5)
			return false;

		else if (Helper.hasJoker(pokerList)) {
			return false;
		} else {
			/**
			 * 先按照点数排序
			 * 
			 * 注意顺子是不考虑牌级的
			 */
			for (int i = 0; i < pokerList.size(); i++) {
				int minIndex = i;
				for (int j = i + 1; j < pokerList.size(); j++) {
					if (pokerList.get(j).points < pokerList.get(minIndex).points)
						minIndex = j;
				}

				if (minIndex != i) {
					Poker poker_i = pokerList.get(i);
					Poker poker_min = pokerList.get(minIndex);
					Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
					poker_i.pattern = poker_min.pattern;
					poker_i.points = poker_min.points;
					poker_min.pattern = tempPoker.pattern;
					poker_min.points = tempPoker.points;
				}
			}

			/**
			 * 根据最小的那张牌,得到可能的顺子组合 需要单独考虑有Ace的情形
			 */
			Poker firstPoker = pokerList.get(0);
			int[] potentialStraight1 = new int[5];
			int[] potentialStraight2 = new int[5];
			potentialStraight1[0] = firstPoker.points;
			for (int i = 1; i < 5; i++)
				potentialStraight1[i] = potentialStraight1[i - 1] + 1;
			if (pokerList.get(0).points == PokerPoints.ACE) {
				potentialStraight2[0] = PokerPoints.ACE;
				potentialStraight2[1] = PokerPoints.TEN;
				potentialStraight2[2] = PokerPoints.JACK;
				potentialStraight2[3] = PokerPoints.QUEEN;
				potentialStraight2[4] = PokerPoints.KING;
			} else {
				for (int i = 0; i < 5; i++)
					potentialStraight2[i] = potentialStraight1[i];
			}

			/**
			 * 最后检查pokerList是否满足可能的顺子中的一个
			 */
			boolean meet1 = true;
			boolean meet2 = true;
			for (int i = 0; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (pk.points != potentialStraight1[i]) {
					meet1 = false;
					break;
				}
			}
			for (int i = 0; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (pk.points != potentialStraight2[i]) {
					meet2 = false;
					break;
				}
			}
			if (meet1)
				return true;
			else if (meet2)
				return true;
			else
				return false;
		}
	}

	/**
	 * 对于除掉主牌的三张牌或四张牌,检验其是否是可以配成顺子
	 * 
	 * @param pokerList
	 *            :扑克牌链表
	 * @return:布尔值
	 */
	public static boolean canBeStraight(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 3 && pokerList.size() != 4)
			return false;
		else if (Helper.hasJoker(pokerList))
			return false;
		else {
			/**
			 * 先排序
			 */
			for (int i = 0; i < pokerList.size(); i++) {
				int minIndex = i;
				for (int j = i + 1; j < pokerList.size(); j++) {
					if (pokerList.get(j).points < pokerList.get(minIndex).points)
						minIndex = j;
				}

				if (minIndex != i) {
					Poker poker_i = pokerList.get(i);
					Poker poker_min = pokerList.get(minIndex);
					Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
					poker_i.pattern = poker_min.pattern;
					poker_i.points = poker_min.points;
					poker_min.pattern = tempPoker.pattern;
					poker_min.points = tempPoker.points;
				}
			}

			/**
			 * 分三张和四张处理
			 */
			if (pokerList.size() == 3) {
				Poker poker1 = pokerList.get(0);
				Poker poker2 = pokerList.get(1);
				Poker poker3 = pokerList.get(2);
				int div12 = poker2.points - poker1.points;
				int div13 = poker3.points - poker1.points;

				/**
				 * 只需检查三张牌能否填到五张顺子的对应位置即可
				 */
				if ((div12 == 1 && div13 == 2) || (div12 == 1 && div13 == 3)
						|| (div12 == 1 && div13 == 4)
						|| (div12 == 2 && div13 == 3)
						|| (div12 == 2 && div13 == 4)
						|| (div12 == 3 && div13 == 4)) {
					return true;
				} else if (poker1.points == PokerPoints.ACE) {
					/**
					 * 检验是否满足10,J,Q,K,A的情况即可 有以下几种情况: Ace,10,Jack Ace,10,Queen
					 * Ace,10,King Ace,Jack,Queen Ace,Jack,King Ace,Queen,King
					 */
					if ((div12 == 9 && div13 == 10)
							|| (div12 == 9 && div13 == 11)
							|| (div12 == 9 && div13 == 12)
							|| (div12 == 10 && div13 == 11)
							|| (div12 == 10 && div13 == 12)
							|| (div12 == 11 && div13 == 12)) {
						return true;
					} else
						return false;

				} else
					return false;
			} else {
				/**
				 * 四张牌仿照三张牌处理
				 */
				Poker poker1 = pokerList.get(0);
				Poker poker2 = pokerList.get(1);
				Poker poker3 = pokerList.get(2);
				Poker poker4 = pokerList.get(3);
				int div12 = poker2.points - poker1.points;
				int div13 = poker3.points - poker1.points;
				int div14 = poker4.points - poker1.points;

				if ((div12 == 1 && div13 == 2 && div14 == 3)
						|| (div12 == 1 && div13 == 2 && div14 == 4)
						|| (div12 == 1 && div13 == 3 && div14 == 4)
						|| (div12 == 2 && div13 == 3 && div14 == 4)) {
					return true;
				} else if (poker1.points == PokerPoints.ACE) {
					/**
					 * 检查是否满足10,J,Q,K,A 有以下情况: A,10,Jack,Queen A,10,Jack,King
					 * A,10,Queen,King A,Jack,Queen,King
					 */
					if ((div12 == 9 && div13 == 10 && div14 == 11)
							|| (div12 == 9 && div13 == 10 && div14 == 12)
							|| (div12 == 9 && div13 == 11 && div14 == 12)
							|| (div12 == 10 && div13 == 11 && div14 == 12)) {
						return true;
					} else
						return false;
				} else
					return false;
			}
		}
	}

	/**
	 * 除掉主牌的一手牌,看其是否是三连对
	 * 
	 * @param pokerList
	 * @return
	 */
	public static boolean isTriple_Double_Straight(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 6)
			return false;
		else if (Helper.hasJoker(pokerList))
			return false;
		else {
			/**
			 * 先排序 再检查(1,2),(3,4),(5,6)是否成对 如果成对,(1,3,5)还须连顺
			 */
			for (int i = 0; i < pokerList.size(); i++) {
				int minIndex = i;
				for (int j = i + 1; j < pokerList.size(); j++) {
					if (pokerList.get(j).points < pokerList.get(minIndex).points)
						minIndex = j;
				}

				if (minIndex != i) {
					Poker poker_i = pokerList.get(i);
					Poker poker_min = pokerList.get(minIndex);
					Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
					poker_i.pattern = poker_min.pattern;
					poker_i.points = poker_min.points;
					poker_min.pattern = tempPoker.pattern;
					poker_min.points = tempPoker.points;
				}
			}

			Poker poker1 = pokerList.get(0);
			Poker poker2 = pokerList.get(1);
			Poker poker3 = pokerList.get(2);
			Poker poker4 = pokerList.get(3);
			Poker poker5 = pokerList.get(4);
			Poker poker6 = pokerList.get(5);

			if (!(isDouble(poker1, poker2) && isDouble(poker3, poker4) && isDouble(
					poker5, poker6)))
				return false;
			else {
				int div13 = poker3.points - poker1.points;
				int div15 = poker5.points - poker1.points;
				if (div13 == 1 && div15 == 2)
					return true;
				else if (poker1.points == PokerPoints.ACE) {
					if (poker3.points == PokerPoints.QUEEN
							&& poker5.points == PokerPoints.KING)
						return true;
					else
						return false;
				} else
					return false;
			}
		}
	}

	/**
	 * 除掉主牌的一手牌,看其是否有可能在配牌之后 构成三连对
	 * 
	 * @param pokerList
	 * @return
	 */
	public static boolean canBe_Triple_Double_Straight(
			ArrayList<Poker> pokerList) {
		if (pokerList.size() != 4 && pokerList.size() != 5)
			return false;
		else if (Helper.hasJoker(pokerList))
			return false;
		else {
			/**
			 * 排序先
			 */
			for (int i = 0; i < pokerList.size(); i++) {
				int minIndex = i;
				for (int j = i + 1; j < pokerList.size(); j++) {
					if (pokerList.get(j).points < pokerList.get(minIndex).points)
						minIndex = j;
				}

				if (minIndex != i) {
					Poker poker_i = pokerList.get(i);
					Poker poker_min = pokerList.get(minIndex);
					Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
					poker_i.pattern = poker_min.pattern;
					poker_i.points = poker_min.points;
					poker_min.pattern = tempPoker.pattern;
					poker_min.points = tempPoker.points;
				}
			}

			/**
			 * 然后从这四张牌或五张牌中选择代表牌出来 四张牌可能选到:2-3张代表牌 五张牌必须要选到3张代表牌
			 */
			ArrayList<Poker> representList = new ArrayList<Poker>();
			Poker currentPoker = pokerList.get(0);
			int representIndex = 0;
			representList.add(pokerList.get(0));
			for (int i = 1; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (!isDouble(pk, currentPoker)) {
					representList.add(pk);
					currentPoker = pk;
					representIndex = i;
				}
				/**
				 * 排序之后,间隔为2的两张牌不可能是对子 否则这手牌之中就含有三张一样点数的牌 不可能成为三连对了
				 */
				else if ((i - representIndex) >= 2) {
					return false;
				}
			}

			/**
			 * 然后判断代表牌是否能够连成顺子
			 */
			if (pokerList.size() == 4) {
				if (representList.size() == 2) {
					Poker repPoker1 = representList.get(0);
					Poker repPoker2 = representList.get(1);
					int div12 = repPoker2.points - repPoker1.points;
					if (div12 == 1 || div12 == 2)
						return true;
					else if (repPoker1.points == PokerPoints.ACE) {
						if (repPoker2.points == PokerPoints.QUEEN
								|| repPoker2.points == PokerPoints.KING)
							return true;
						else
							return false;
					} else
						return false;
				} else if (representList.size() == 3) {
					Poker repPoker1 = representList.get(0);
					Poker repPoker2 = representList.get(1);
					Poker repPoker3 = representList.get(2);
					int div12 = repPoker2.points - repPoker1.points;
					int div13 = repPoker3.points - repPoker1.points;

					if (div12 == 1 && div13 == 2)
						return true;
					else if (repPoker1.points == PokerPoints.ACE) {
						if (repPoker2.points == PokerPoints.QUEEN
								&& repPoker3.points == PokerPoints.KING)
							return true;
						else
							return false;
					} else
						return false;
				} else
					return false;
			} else {
				if (representList.size() == 3) {
					Poker repPoker1 = representList.get(0);
					Poker repPoker2 = representList.get(1);
					Poker repPoker3 = representList.get(2);
					int div12 = repPoker2.points - repPoker1.points;
					int div13 = repPoker3.points - repPoker1.points;

					if (div12 == 1 && div13 == 2)
						return true;
					else if (repPoker1.points == PokerPoints.ACE) {
						if (repPoker2.points == PokerPoints.QUEEN
								&& repPoker3.points == PokerPoints.KING)
							return true;
						else
							return false;
					} else
						return false;
				} else
					return false;
			}
		}
	}

	/**
	 * 除掉主牌的一手牌,看其是否是钢板
	 * 
	 * @param pokerList
	 * @return
	 */
	public static boolean isDouble_Triple_Straight(ArrayList<Poker> pokerList) {
		if (pokerList.size() != 6)
			return false;
		else if (Helper.hasJoker(pokerList))
			return false;

		/**
		 * 判断方法如下: 将pokerList分为两部分 分成的两份必须要分别是Triple且点数相连
		 */
		else {
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();

			Poker firstPoker = pokerList.get(0);
			pokerList1.add(firstPoker);

			for (int i = 1; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (isDouble(firstPoker, pk))
					pokerList1.add(pk);
				else
					pokerList2.add(pk);
			}

			if (isTriple(pokerList1) && isTriple(pokerList2)) {
				Poker poker1 = pokerList1.get(0);
				Poker poker2 = pokerList2.get(0);

				if ((poker1.points - poker2.points == 1)
						|| (poker2.points - poker1.points == 1))
					return true;
				else if ((poker1.points == PokerPoints.ACE && poker2.points == PokerPoints.KING)
						|| (poker1.points == PokerPoints.KING && poker2.points == PokerPoints.ACE))
					return true;
				else
					return false;
			} else
				return false;

		}
	}

	/**
	 * 除掉主牌的一手牌,看其是否有可能在配牌之后 构成钢板
	 * 
	 * @param pokerList
	 * @return
	 */
	public static boolean canBe_Double_Triple_Straight(
			ArrayList<Poker> pokerList) {
		if (pokerList.size() != 5 && pokerList.size() != 4)
			return false;
		else if (Helper.hasJoker(pokerList))
			return false;
		else {
			/**
			 * 判断方法如下: 1.首先仍是将整个链表分为两部分,若这手牌真能配成钢板 则分出来的组合必定在如下情况中: a.
			 * pokerList1为Double,pokerList2也为Double b.
			 * pokerList1为Triple,pokerList2为单牌 c.
			 * pokerList1为Single,pokerList2为Triple d. pokerList为三带二,五张牌的情况必须是如此
			 * 
			 * 2.如果分出来的牌满足以上情况,则从两部分中选一个代表出来 看它们能否构成连牌,注意Ace,King的情况
			 */
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();

			Poker firstPoker = pokerList.get(0);
			pokerList1.add(firstPoker);

			for (int i = 1; i < pokerList.size(); i++) {
				Poker pk = pokerList.get(i);
				if (isDouble(firstPoker, pk))
					pokerList1.add(pk);
				else
					pokerList2.add(pk);
			}

			if (isTriple_With_Double(pokerList)
					|| (isDouble(pokerList1) && isDouble(pokerList2))
					|| (isTriple(pokerList1) && pokerList2.size() == 1)
					|| (isTriple(pokerList2) && pokerList1.size() == 1)) {
				Poker poker1 = pokerList1.get(0);
				Poker poker2 = pokerList2.get(0);

				if ((poker1.points - poker2.points == 1)
						|| (poker2.points - poker1.points == 1))
					return true;
				else if ((poker1.points == PokerPoints.ACE && poker2.points == PokerPoints.KING)
						|| (poker1.points == PokerPoints.KING && poker2.points == PokerPoints.ACE))
					return true;
				else
					return false;
			} else
				return false;
		}
	}

}
