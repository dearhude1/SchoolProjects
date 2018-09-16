package guandan.helper;

import java.util.ArrayList;

import guandan.constants.PokerPoints;
import guandan.constants.PokerType;
import guandan.game.Poker;

public class Sorter {

	public static ArrayList<Poker> sortPokers_reverse(
			ArrayList<Poker> pokerList, int pokerType, int grade) {
		if (pokerType == PokerType.INVALID_TYPE
				|| pokerType == PokerType.SINGLE)
			return pokerList;

		ArrayList<Poker> sortedPokers = new ArrayList<Poker>();
		ArrayList<Poker> revSortedPokers = new ArrayList<Poker>();

		/**
		 * 先正向排序
		 */
		sortedPokers.addAll(sortPokers(pokerList, pokerType, grade));

		if (pokerType == PokerType.DOUBLE || pokerType == PokerType.TRIPLE
				|| pokerType == PokerType.BOMB
				|| pokerType == PokerType.TRIPLE_WITH_DOUBLE
				|| pokerType == PokerType.FOUR_JOKER)
			revSortedPokers.addAll(sortedPokers);

		/**
		 * 同花顺直接翻转
		 */
		else if (pokerType == PokerType.STRAIGHT
				|| pokerType == PokerType.STRAIGHT_FLUSH) {
			for (int i = sortedPokers.size() - 1; i >= 0; i--)
				revSortedPokers.add(sortedPokers.get(i));
		}
		/**
		 * 三连对分三对翻转
		 */
		else if (pokerType == PokerType.TRIPLE_DOUBLE_STRAIGHT) {
			revSortedPokers.add(sortedPokers.get(4));
			revSortedPokers.add(sortedPokers.get(5));
			revSortedPokers.add(sortedPokers.get(2));
			revSortedPokers.add(sortedPokers.get(3));
			revSortedPokers.add(sortedPokers.get(0));
			revSortedPokers.add(sortedPokers.get(1));
		}
		/**
		 * 钢板分两个三翻转
		 */
		else if (pokerType == PokerType.DOUBLE_TRIPLE_STRAIGHT) {
			revSortedPokers.add(sortedPokers.get(3));
			revSortedPokers.add(sortedPokers.get(4));
			revSortedPokers.add(sortedPokers.get(5));
			revSortedPokers.add(sortedPokers.get(0));
			revSortedPokers.add(sortedPokers.get(1));
			revSortedPokers.add(sortedPokers.get(2));
		} else
			revSortedPokers.addAll(sortedPokers);

		sortedPokers.clear();

		return revSortedPokers;
	}

	/**
	 * 对玩家打出的一手牌进行整理排序,以便其显示出来或者方便AI_Player处理
	 * 
	 * 整理之后其内部顺序应满足: 普通牌:从左到右点数下降,例如:554433, AAKKQQ, AKQJ10, 3322AA
	 * 三带二:三张牌必须左边,例如:QQQ44, 888KK 另外主牌必须插入相应被配牌的右边,例如:77配33,
	 * 
	 * @param pokerList
	 * @param grade
	 */
	public static ArrayList<Poker> sortPokers(ArrayList<Poker> pokerList,
			int pokerType, int grade) {
		if (pokerType == PokerType.INVALID_TYPE
				|| pokerType == PokerType.SINGLE)
			return pokerList;

		/**
		 * 先将主牌抽出 然后剩下的牌排序,分两张情况: 若牌型是顺子类型,则纯粹按照点数排序 非顺子类型,调用comparePoker来排序
		 * 
		 * 最后再将主牌插入到剩下的牌中
		 */

		/**
		 * 先抽出主牌
		 */
		int MASTER_CARD = 0;
		int NON_MASTER_CARD = 1;
		ArrayList<Poker>[] splitPokers = Helper.splitPokers(pokerList, grade);
		int masterCardNum = splitPokers[MASTER_CARD].size();
		ArrayList<Poker> sortedPokers = new ArrayList<Poker>();

		/**
		 * 再将剩下的部分排序 对于顺子牌型纯粹按照点数大小排序; 非顺子牌型则调用comparePoker来排序
		 * 
		 * 从左到右按照点数下降
		 */
		if (pokerType != PokerType.TRIPLE_DOUBLE_STRAIGHT
				&& pokerType != PokerType.DOUBLE_TRIPLE_STRAIGHT
				&& pokerType != PokerType.STRAIGHT
				&& pokerType != PokerType.STRAIGHT_FLUSH) {
			for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++) {
				int maxIndex = i;
				for (int j = i + 1; j < splitPokers[NON_MASTER_CARD].size(); j++) {
					if (Comparator.comparePoker(
							splitPokers[NON_MASTER_CARD].get(j),
							splitPokers[NON_MASTER_CARD].get(maxIndex), grade) == 1) {
						maxIndex = j;
					}
				}
				if (maxIndex != i) {
					Poker poker_i = splitPokers[NON_MASTER_CARD].get(i);
					Poker poker_max = splitPokers[NON_MASTER_CARD]
							.get(maxIndex);
					Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
					poker_i.pattern = poker_max.pattern;
					poker_i.points = poker_max.points;
					poker_max.pattern = tempPoker.pattern;
					poker_max.points = tempPoker.points;
				}
			}
		} else {
			for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++) {
				int maxIndex = i;
				for (int j = i + 1; j < splitPokers[NON_MASTER_CARD].size(); j++) {
					if (splitPokers[NON_MASTER_CARD].get(j).points > splitPokers[NON_MASTER_CARD]
							.get(maxIndex).points)
						maxIndex = j;
				}
				if (maxIndex != i) {
					Poker poker_i = splitPokers[NON_MASTER_CARD].get(i);
					Poker poker_max = splitPokers[NON_MASTER_CARD]
							.get(maxIndex);
					Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
					poker_i.pattern = poker_max.pattern;
					poker_i.points = poker_max.points;
					poker_max.pattern = tempPoker.pattern;
					poker_max.points = tempPoker.points;
				}
			}
		}

		/**
		 * 若没有主牌,则排好序的splitPokers[NON_MASTER_CARD]赋给sortedPokers 只不过要处理三带二的情况
		 * 
		 * 以及存在Ace的顺子
		 */
		if (masterCardNum == 0) {

			/**
			 * 处理下三带二 将三张放前面啊 看第二张牌和第三张牌是否一样 一样则三张在前 否则三张在后面,需要调到前面来
			 */
			if (pokerType == PokerType.TRIPLE_WITH_DOUBLE) {
				Poker poker1 = splitPokers[NON_MASTER_CARD].get(0);
				Poker poker2 = splitPokers[NON_MASTER_CARD].get(1);
				Poker poker3 = splitPokers[NON_MASTER_CARD].get(2);
				if (Comparator.comparePoker(poker2, poker3, grade) != 0) {
					for (int i = 2; i < splitPokers[NON_MASTER_CARD].size(); i++)
						sortedPokers.add(splitPokers[NON_MASTER_CARD].get(i));
					sortedPokers.add(poker1);
					sortedPokers.add(poker2);
				} else {
					for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++)
						sortedPokers.add(splitPokers[NON_MASTER_CARD].get(i));
				}
			}

			/**
			 * 为何没有处理顺子牌中Ace的情况
			 */
			else if (pokerType == PokerType.TRIPLE_DOUBLE_STRAIGHT) {
				Poker poker1 = splitPokers[NON_MASTER_CARD].get(0);
				Poker poker5 = splitPokers[NON_MASTER_CARD].get(4);
				if (poker1.points == PokerPoints.KING
						&& poker5.points == PokerPoints.ACE) {
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(4));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(5));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(0));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(1));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(2));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(3));

				} else {
					for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++)
						sortedPokers.add(splitPokers[NON_MASTER_CARD].get(i));
				}

			} else if (pokerType == PokerType.DOUBLE_TRIPLE_STRAIGHT) {
				Poker poker1 = splitPokers[NON_MASTER_CARD].get(0);
				Poker poker4 = splitPokers[NON_MASTER_CARD].get(3);
				if (poker1.points == PokerPoints.KING
						&& poker4.points == PokerPoints.ACE) {
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(3));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(4));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(5));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(0));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(1));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(2));

				} else {
					for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++)
						sortedPokers.add(splitPokers[NON_MASTER_CARD].get(i));
				}
			} else if (pokerType == PokerType.STRAIGHT
					|| pokerType == PokerType.STRAIGHT_FLUSH) {
				Poker poker1 = splitPokers[NON_MASTER_CARD].get(0);
				Poker poker5 = splitPokers[NON_MASTER_CARD].get(4);
				if (poker1.points == PokerPoints.KING
						&& poker5.points == PokerPoints.ACE) {
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(4));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(0));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(1));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(2));
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(3));
				} else {
					for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++)
						sortedPokers.add(splitPokers[NON_MASTER_CARD].get(i));
				}
			}

			else {
				for (int i = 0; i < splitPokers[NON_MASTER_CARD].size(); i++)
					sortedPokers.add(splitPokers[NON_MASTER_CARD].get(i));
			}
		}

		/**
		 * Double,Triple,Bomb 直接将主牌加到非主牌后面即可
		 */
		else if (pokerType == PokerType.DOUBLE || pokerType == PokerType.TRIPLE
				|| pokerType == PokerType.BOMB) {
			sortedPokers.addAll(splitPokers[NON_MASTER_CARD]);
			sortedPokers.addAll(splitPokers[MASTER_CARD]);
		}

		/**
		 * 三带二情况,插入主牌按照如下流程:
		 * 
		 */
		else if (pokerType == PokerType.TRIPLE_WITH_DOUBLE) {
			/**
			 * 将非主牌分为两部分
			 */
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();
			ArrayList<Poker> triplePokers;
			ArrayList<Poker> doublePokers;

			Poker firstPoker = splitPokers[NON_MASTER_CARD].get(0);
			pokerList1.add(firstPoker);

			for (int i = 1; i < splitPokers[NON_MASTER_CARD].size(); i++) {
				Poker pk = splitPokers[NON_MASTER_CARD].get(i);
				if (Parser.isDouble(firstPoker, pk))
					pokerList1.add(pk);
				else
					pokerList2.add(pk);
			}

			if (Parser.isDouble(pokerList1) && Parser.isDouble(pokerList2)) {
				if (!Helper.hasJoker(pokerList1)) {
					triplePokers = pokerList1;
					triplePokers.add(splitPokers[MASTER_CARD].remove(0));
					doublePokers = pokerList2;
				} else {
					triplePokers = pokerList2;
					triplePokers.add(splitPokers[MASTER_CARD].remove(0));
					doublePokers = pokerList1;
				}
			} else if (Parser.isTriple(pokerList1)) {
				triplePokers = pokerList1;
				doublePokers = pokerList2;
				doublePokers.add(splitPokers[MASTER_CARD].remove(0));
			} else if (Parser.isTriple(pokerList2)) {
				triplePokers = pokerList2;
				doublePokers = pokerList1;
				doublePokers.add(splitPokers[MASTER_CARD].remove(0));
			} else if (Parser.isDouble(pokerList1)) {
				if (!Helper.hasJoker(pokerList1)) {
					triplePokers = pokerList1;
					triplePokers.add(splitPokers[MASTER_CARD].remove(0));
					doublePokers = pokerList2;
					doublePokers.add(splitPokers[MASTER_CARD].remove(0));
				} else {
					triplePokers = pokerList2;
					triplePokers.add(splitPokers[MASTER_CARD].remove(0));
					triplePokers.add(splitPokers[MASTER_CARD].remove(0));
					doublePokers = pokerList1;
				}
			} else {
				/**
				 * 这种情况pokerList2肯定不是一对鬼 因为pokerList2中元素要比pokerList1中的要小
				 */
				triplePokers = pokerList1;
				triplePokers.add(splitPokers[MASTER_CARD].remove(0));
				triplePokers.add(splitPokers[MASTER_CARD].remove(0));
				doublePokers = pokerList2;
			}

			/**
			 * 将triplePokers和doublePokers组合起来
			 */
			sortedPokers.addAll(triplePokers);
			sortedPokers.addAll(doublePokers);
		}

		/**
		 * 三连对,插入主牌: 在需要对子的地方插入即可
		 * 
		 * 少两张主牌的时候麻烦一点 因为要处理QQAA,KKAA
		 * 
		 * 将主牌插入之后,以下情况均可: I+2,I+2,I+1,I+1,I,I
		 * 
		 * 对于存在Ace的三连对,满足: AAKKQQ 3322AA
		 * 
		 */
		else if (pokerType == PokerType.TRIPLE_DOUBLE_STRAIGHT) {
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList3 = new ArrayList<Poker>();

			if (masterCardNum == 1) {
				Poker poker1 = splitPokers[NON_MASTER_CARD].get(0);
				Poker poker2 = splitPokers[NON_MASTER_CARD].get(1);
				Poker poker3 = splitPokers[NON_MASTER_CARD].get(2);
				Poker poker4 = splitPokers[NON_MASTER_CARD].get(3);
				Poker poker5 = splitPokers[NON_MASTER_CARD].get(4);

				pokerList1.add(poker1);
				if (Parser.isDouble(poker1, poker2)) {
					pokerList1.add(poker2);

					pokerList2.add(poker3);
					if (Parser.isDouble(poker3, poker4)) {
						pokerList2.add(poker4);
						pokerList3.add(poker5);
						pokerList3.add(splitPokers[MASTER_CARD].get(0));
					} else {
						pokerList2.add(splitPokers[MASTER_CARD].get(0));
						pokerList3.add(poker4);
						pokerList3.add(poker5);
					}
				} else {
					pokerList1.add(splitPokers[MASTER_CARD].get(0));
					pokerList2.add(poker2);
					pokerList2.add(poker3);
					pokerList3.add(poker4);
					pokerList3.add(poker5);
				}
			} else {
				Poker poker1 = splitPokers[NON_MASTER_CARD].get(0);
				Poker poker2 = splitPokers[NON_MASTER_CARD].get(1);
				Poker poker3 = splitPokers[NON_MASTER_CARD].get(2);
				Poker poker4 = splitPokers[NON_MASTER_CARD].get(3);

				if (Parser.isDouble(poker1, poker2)
						&& Parser.isDouble(poker3, poker4)) {
					/**
					 * 分以下四种情况: 两对非主牌点数差1,或者_ _ Q Q A A 两对非主牌点数差2,或者K K _ _ A A
					 */
					if ((poker1.points - poker3.points == 1)
							|| (poker1.points - poker3.points == 11)) {
						pokerList1.add(splitPokers[MASTER_CARD].remove(0));
						pokerList1.add(splitPokers[MASTER_CARD].remove(0));
						pokerList2.add(poker1);
						pokerList2.add(poker2);
						pokerList3.add(poker3);
						pokerList3.add(poker4);
					} else if ((poker1.points - poker3.points == 2)
							|| (poker1.points - poker3.points == 12)) {
						pokerList1.add(poker1);
						pokerList1.add(poker2);
						pokerList2.add(splitPokers[MASTER_CARD].remove(0));
						pokerList2.add(splitPokers[MASTER_CARD].remove(0));
						pokerList3.add(poker3);
						pokerList3.add(poker4);
					}
				} else if (Parser.isDouble(poker1, poker2)) {
					pokerList1.add(poker1);
					pokerList1.add(poker2);
					pokerList2.add(poker3);
					pokerList2.add(splitPokers[MASTER_CARD].remove(0));
					pokerList3.add(poker4);
					pokerList3.add(splitPokers[MASTER_CARD].remove(0));
				} else if (Parser.isDouble(poker3, poker4)) {
					pokerList1.add(poker1);
					pokerList1.add(splitPokers[MASTER_CARD].remove(0));
					pokerList2.add(poker2);
					pokerList2.add(splitPokers[MASTER_CARD].remove(0));
					pokerList3.add(poker3);
					pokerList3.add(poker4);
				} else {
					pokerList1.add(poker1);
					pokerList1.add(splitPokers[MASTER_CARD].remove(0));
					pokerList2.add(poker2);
					pokerList2.add(poker3);
					pokerList3.add(poker4);
					pokerList3.add(splitPokers[MASTER_CARD].remove(0));
				}
			}

			/**
			 * 最后将pokerList1-3添加到sortedPokers中 如果是KKQQAA的情况,注意将AA放到前面
			 */
			if (pokerList1.get(0).points == PokerPoints.KING
					|| pokerList2.get(0).points == PokerPoints.QUEEN) {
				sortedPokers.addAll(pokerList3);
				sortedPokers.addAll(pokerList1);
				sortedPokers.addAll(pokerList2);
			} else {
				sortedPokers.addAll(pokerList1);
				sortedPokers.addAll(pokerList2);
				sortedPokers.addAll(pokerList3);
			}
		}

		/**
		 * 钢板,插入主牌 这个比较容易,将splitPokers[NON_MASTER_CARD]分为两部分 再进行插入即可
		 * 
		 * 由于已经是钢板了,所以通过各部分的元素个数就能知其牌型
		 */
		else if (pokerType == PokerType.DOUBLE_TRIPLE_STRAIGHT) {
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();
			Poker firstPoker = splitPokers[NON_MASTER_CARD].get(0);
			pokerList1.add(firstPoker);

			for (int i = 1; i < splitPokers[NON_MASTER_CARD].size(); i++) {
				Poker pk = splitPokers[NON_MASTER_CARD].get(i);
				if (Parser.isDouble(firstPoker, pk))
					pokerList1.add(pk);
				else
					pokerList2.add(pk);
			}
			if (pokerList1.size() == 2 && pokerList2.size() == 2) {
				pokerList1.add(splitPokers[MASTER_CARD].remove(0));
				pokerList2.add(splitPokers[MASTER_CARD].remove(0));
			} else if (pokerList1.size() == 2 && pokerList2.size() == 3) {
				pokerList1.add(splitPokers[MASTER_CARD].remove(0));
			} else if (pokerList1.size() == 3 && pokerList2.size() == 2) {
				pokerList2.add(splitPokers[MASTER_CARD].remove(0));
			} else if (pokerList1.size() == 3) {
				pokerList2.add(splitPokers[MASTER_CARD].remove(0));
				pokerList2.add(splitPokers[MASTER_CARD].remove(0));
			} else {
				pokerList1.add(splitPokers[MASTER_CARD].remove(0));
				pokerList1.add(splitPokers[MASTER_CARD].remove(0));
			}

			/**
			 * 最后将pokerList1,2添加到sortedPokers中 如果是KKKAAA的情况,注意将AAA放到前面
			 */
			if (pokerList1.get(0).points == PokerPoints.KING) {
				sortedPokers.addAll(pokerList2);
				sortedPokers.addAll(pokerList1);
			} else {
				sortedPokers.addAll(pokerList1);
				sortedPokers.addAll(pokerList2);
			}
		}

		/**
		 * 顺子或同花顺,插入主牌
		 * 
		 * 
		 * 
		 */
		else if (pokerType == PokerType.STRAIGHT
				|| pokerType == PokerType.STRAIGHT_FLUSH) {
			/**
			 * 预处理 如果是AKQJ10这个顺子 先将末尾的A去掉
			 * 在splitPoker[NON_MASTER_CARD]之前加一个点数为14的牌
			 * 
			 * 等到配牌插入后再把14变为Ace的点数
			 */
			int size = splitPokers[NON_MASTER_CARD].size();
			Poker lastPoker = splitPokers[NON_MASTER_CARD].get(size - 1);
			Poker firstPoker = splitPokers[NON_MASTER_CARD].get(0);
			if (lastPoker.points == PokerPoints.ACE
					&& (firstPoker.points == PokerPoints.KING
							|| firstPoker.points == PokerPoints.QUEEN || firstPoker.points == PokerPoints.JACK)) {
				Poker anotherAcePoker = new Poker(firstPoker.pattern,
						PokerPoints.ACE);
				anotherAcePoker.points = 14;

				splitPokers[NON_MASTER_CARD].remove(size - 1);
				splitPokers[NON_MASTER_CARD].add(0, anotherAcePoker);
			}

			/**
			 * 按空隙插入配牌,插入方法如下: 循环遍历链表splitPokers[NON_MASTER_CARD]
			 * 找到点数不连续的牌,在点数不连续的两张牌中插入相应张数的配牌
			 * 
			 * 
			 */
			int count = masterCardNum;
			int current = 0;
			int next = 1;

			while (next <= splitPokers[NON_MASTER_CARD].size() - 1) {
				Poker currentPoker = splitPokers[NON_MASTER_CARD].get(current);
				Poker nextPoker = splitPokers[NON_MASTER_CARD].get(next);

				int currentDiv = currentPoker.points - nextPoker.points;

				if (currentDiv > 1) {
					while (currentDiv > 1) {
						splitPokers[NON_MASTER_CARD].add(next,
								splitPokers[MASTER_CARD].remove(0));
						currentDiv--;
						next++;
					}
				}
				current = next;
				next++;
			}
			/**
			 * 循环一遍后还有剩余主牌 将主牌插入最前面 但如果最前面是Ace,则插入最后面
			 */
			if (count > 0) {
				if (splitPokers[NON_MASTER_CARD].get(0).points == 14) {
					while (splitPokers[MASTER_CARD].size() > 0) {
						splitPokers[NON_MASTER_CARD]
								.add(splitPokers[MASTER_CARD].remove(0));
					}
				} else {
					while (splitPokers[MASTER_CARD].size() > 0) {
						splitPokers[NON_MASTER_CARD].add(0,
								splitPokers[MASTER_CARD].remove(0));
					}
				}
			}
			/**
			 * 若插入配牌后第一张牌点数为14 则说明经过了预处理的 将其改回Ace
			 */
			if (splitPokers[NON_MASTER_CARD].get(0).points == 14)
				splitPokers[NON_MASTER_CARD].get(0).points = PokerPoints.ACE;

			sortedPokers.addAll(splitPokers[NON_MASTER_CARD]);
		}

		pokerList.clear();
		pokerList.addAll(sortedPokers);

		return pokerList;
	}

}
