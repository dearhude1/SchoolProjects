package guandan.player;

import java.util.ArrayList;
import java.util.ListIterator;

import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;
import guandan.constants.PokerType;
import guandan.game.GameView;
import guandan.game.Poker;

public class Easy_AI_Player extends AI_Player {

	public Easy_AI_Player(GameView gv, int turn, int AI) {
		super(gv, turn, 0);
		// TODO Auto-generated constructor stub
	}

	public ArrayList<Poker> lead() {
		ArrayList<Poker> currentPoker = gameView.getCurrentPokers();
		int currentWinner = gameView.getCurrentWinner();
		return easy_AI(currentPoker, currentWinner);
	}

	/**
	 * 简单AI策略
	 * 
	 * @return 简单AI应该出什么牌
	 */
	protected ArrayList<Poker> easy_AI(ArrayList<Poker> currentPoker,
			int currentWinner) {
		ArrayList<Poker> result = new ArrayList<Poker>();
		boolean haveFound = false;
		/**
		 * 判断是发牌还是压牌，相等的时候是发牌，反之是压牌
		 */
		if (currentWinner == this.myTurn) {
			/**
			 * 从最小的牌开始找起,2~k的循环，去除主牌
			 */
			for (int i = 2; i < 14; i++) {
				if (numList[i] > 0 && numList[i] < 4 && i != this.currentGrade) {
					haveFound = true;
					int pokerNum = numList[i];
					result = selectPokers(numList[i], -1, i, result);
					/**
					 * 是两张的话，看看是否能组成连对，只考虑从2到Q开始的连对，主牌不进入连队
					 */
					if (pokerNum == 2 && i < 13) {
						int k = ((i + 2) > 13) ? 1 : (i + 2);
						if (numList[i + 1] == 2 && numList[k] == 2) {
							if ((i + 1) != this.currentGrade
									&& k != this.currentGrade) {
								result = selectPokers(numList[i + 1], -1,
										i + 1, result);
								result = selectPokers(numList[k], -1, k, result);
							}
						}
					}
					/**
					 * 是三张的话，看看能否组成其他牌
					 */
					if (pokerNum == 3) {
						int k = ((i + 1) > 13) ? 1 : (i + 1);
						/**
						 * 能否组成钢板
						 */
						if (numList[k] == 3) {
							if (k != this.currentGrade) {
								result = selectPokers(numList[k], -1, k, result);
							}
						}
						/**
						 * 能否组成3+2
						 */
						else {
							/**
							 * 在一定范围内寻找对子
							 */
							for (int j = (i + 1); j < 14 && j < (i + 5); j++) {
								if (numList[j] == 2 && j != this.currentGrade) {
									result = selectPokers(numList[j], -1, j,
											result);
									break;
								}
							}
						}
					}
					break;
				}
			}
			/**
			 * 未找到，在A中寻找，且A不是主牌
			 */
			if (!haveFound && (numList[1] > 0 && numList[1] < 4)
					&& 1 != this.currentGrade) {
				haveFound = true;
				int pokerNum = numList[1];
				result = selectPokers(numList[1], -1, PokerPoints.ACE, result);
				/**
				 * 看看能否组成3+2
				 */
				if (pokerNum == 3) {
					/**
					 * 除去逢人配，主牌正好有两张
					 */
					if ((numList[this.currentGrade] - pokerList0[2][this.currentGrade]) == 2) {
						result = selectPokers(2, -1, this.currentGrade, result);

					}
					/**
					 * 或者两张小王
					 */
					else if (numList[0] == 2 && numList[14] != 2) {
						result = selectPokers(numList[0], PokerPattern.JOKER,
								PokerPoints.LITTLE_JOKER, result);
					}
					/**
					 * 或者两张大王
					 */
					else if (numList[14] == 2 && numList[0] != 2) {
						result = selectPokers(numList[14], PokerPattern.JOKER,
								PokerPoints.OLD_JOKER, result);
					}
				}
			}
			/**
			 * 查询一下主牌以下还有牌没,1代表有牌（也只会是炸弹）
			 */
			int onlyFire = 0;
			for (int i = 1; i < 14; i++) {
				if (i != this.currentGrade && numList[i] != 0) {
					onlyFire = 1;
					break;
				}
			}
			/**
			 * 如果之前未找到牌出，且主牌下还有炸弹，出主牌（不出逢人配）
			 */
			if (!haveFound && onlyFire == 1) {
				int pokerNum = numList[this.currentGrade]
						- pokerList0[2][this.currentGrade];
				/**
				 * 不是炸弹的话
				 */
				if (pokerNum > 0 && pokerNum < 4) {
					haveFound = true;
					result = selectPokers(pokerNum, -1, this.currentGrade,
							result);
					/**
					 * 看能不能和大小王组成3+2
					 */
					if (pokerNum == 3) {
						if (numList[0] == 2 && numList[14] != 2) {
							result = selectPokers(numList[0],
									PokerPattern.JOKER,
									PokerPoints.LITTLE_JOKER, result);
						} else if (numList[14] == 2 && numList[0] != 2) {
							result = selectPokers(numList[14],
									PokerPattern.JOKER, PokerPoints.OLD_JOKER,
									result);
						}
					}
				}
			}
			/**
			 * 如果为找打牌打，且主牌以下，所有牌已经打完的话，红桃主牌也出
			 */
			else if (!haveFound) {
				int pokerNum = numList[this.currentGrade];
				if (pokerNum > 0 && pokerNum < 4) {
					haveFound = true;
					result = selectPokers(numList[this.currentGrade]
							- pokerList0[2][this.currentGrade], -1,
							this.currentGrade, result);
					result = selectPokers(pokerList0[2][this.currentGrade],
							PokerPattern.HEART, this.currentGrade, result);
					if (pokerNum == 3) {
						if (numList[0] == 2 && numList[14] != 2) {
							result = selectPokers(numList[0],
									PokerPattern.JOKER,
									PokerPoints.LITTLE_JOKER, result);
						} else if (numList[14] == 2 && numList[0] != 2) {
							result = selectPokers(numList[14],
									PokerPattern.JOKER, PokerPoints.OLD_JOKER,
									result);
						}
					}
				}
			}
			/**
			 * 再不行就出大小王,先判定是否天王炸
			 */
			if (!haveFound && numList[0] == 2 && numList[14] == 2) {

			} else if (!haveFound && numList[0] != 0) {
				haveFound = true;
				result = selectPokers(numList[0], PokerPattern.JOKER,
						PokerPoints.LITTLE_JOKER, result);
			} else if (!haveFound && numList[14] != 0) {
				haveFound = true;
				result = selectPokers(numList[14], PokerPattern.JOKER,
						PokerPoints.OLD_JOKER, result);
			}
			/**
			 * 如果还没有找到，出炸弹
			 */
			if (!haveFound) {
				result = payFire();
				int k = pokerList.size() - numList[0] - numList[14];
				if (k == 1 || k == 2) {
					result = selectPokers(k, PokerPattern.HEART,
							this.currentGrade, result);
				}
			}
			/**
			 * 检查一下牌是否打完
			 */
			if (pokerList.size() == 0) {
				setFinished(true);
			}
			return result;
		} else {
			/**
			 * 如果是对家打的牌就不压
			 */
			if ((myTurn + currentWinner) % 2 != 0) {
				int pokerType[] = pokerType(currentPoker);
				Poker pokerNow;
				/**
				 * 建一个符合当前牌型大小的Poker，用以比较
				 */
				if (pokerType[1] == 0) {
					pokerNow = new Poker(PokerPattern.JOKER,
							PokerPoints.LITTLE_JOKER);
				} else if (pokerType[1] == 14) {
					pokerNow = new Poker(PokerPattern.JOKER,
							PokerPoints.OLD_JOKER);
				} else {
					pokerNow = new Poker(1, pokerType[1]);
				}
				/**
				 * 分牌型进行出牌规则制定
				 */
				switch (pokerType[0]) {
				case PokerType.SINGLE:
					/**
					 * 对2~k寻找正好为一张的
					 */
					for (int i = 2; i < 14; i++) {
						if (i != this.currentGrade && numList[i] == 1
								&& comparePoker(new Poker(1, i), pokerNow)) {
							haveFound = true;
							result = selectPokers(numList[i], -1, i, result);
							break;
						}
					}
					/**
					 * 没找到，寻找A为单张，A不为主牌
					 */
					if (!haveFound && 1 != this.currentGrade && numList[1] == 1
							&& comparePoker(new Poker(1, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(numList[1], -1, PokerPoints.ACE,
								result);
					}
					/**
					 * 没找到，寻找主牌为单张
					 */
					if (!haveFound
							&& (numList[this.currentGrade] - pokerList0[2][this.currentGrade]) == 1
							&& comparePoker(new Poker(1, this.currentGrade),
									pokerNow)) {
						haveFound = true;
						int j = numList[this.currentGrade]
								- pokerList0[2][this.currentGrade];
						result = selectPokers(j, -1, this.currentGrade, result);
					}
					/**
					 * 没找到，寻找小王
					 */
					if (!haveFound && numList[0] != 0
							&& comparePoker(new Poker(0, 0), pokerNow)) {
						haveFound = true;
						result = selectPokers(1, PokerPattern.JOKER,
								PokerPoints.LITTLE_JOKER, result);
					}
					/**
					 * 没找到，寻找大王
					 */
					if (!haveFound && numList[14] != 0
							&& comparePoker(new Poker(0, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(1, PokerPattern.JOKER,
								PokerPoints.OLD_JOKER, result);
					}
					/**
					 * 没找到，开始拆主牌
					 */
					if (!haveFound
							&& (numList[this.currentGrade] - pokerList0[2][this.currentGrade]) != 0
							&& comparePoker(new Poker(1, this.currentGrade),
									pokerNow)) {
						haveFound = true;
						result = selectPokers(1, -1, this.currentGrade, result);
					}
					/**
					 * 没找到，拆A
					 */
					if (!haveFound && 1 != this.currentGrade && numList[1] > 0
							&& numList[1] < 4
							&& comparePoker(new Poker(1, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(1, -1, PokerPoints.ACE, result);
					}
					/**
					 * 没找到，从大往小拆
					 */
					if (!haveFound) {
						for (int i = 13; i > 1; i--) {
							if (comparePoker(new Poker(1, i), pokerNow)) {
								if (i != this.currentGrade && numList[i] > 0
										&& numList[i] < 4) {
									haveFound = true;
									result = selectPokers(1, -1, i, result);
									break;
								}
							} else
								break;
						}
					}
					/**
					 * 没找到，出炸
					 */
					if (!haveFound) {
						result = payFire();
						/**
						 * 没炸，就打逢人配
						 */
						if (result.size() == 0
								&& pokerList0[2][this.currentGrade] > 0
								&& comparePoker(
										new Poker(1, this.currentGrade),
										pokerNow)) {
							haveFound = true;
							result = selectPokers(1, PokerPattern.HEART,
									this.currentGrade, result);
						}
					}
					break;
				case PokerType.DOUBLE:
					/**
					 * 2~k，寻找对子
					 */
					for (int i = 2; i < 14; i++) {
						if (i != this.currentGrade && numList[i] == 2
								&& comparePoker(new Poker(1, i), pokerNow)) {
							haveFound = true;
							result = selectPokers(numList[i], -1, i, result);
							break;
						}
					}
					/**
					 * 没找到，寻找对子A
					 */
					if (!haveFound && 1 != this.currentGrade && numList[1] == 2
							&& comparePoker(new Poker(1, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(numList[1], -1, 1, result);
					}
					/**
					 * 没找到，寻找主牌对子，不加逢人配
					 */
					if (!haveFound
							&& (numList[this.currentGrade] - pokerList0[2][this.currentGrade]) == 2
							&& comparePoker(new Poker(1, this.currentGrade),
									pokerNow)) {
						haveFound = true;
						result = selectPokers(2, -1, this.currentGrade, result);
					}
					/**
					 * 没找到，找对小王
					 */
					if (!haveFound && numList[0] == 2
							&& comparePoker(new Poker(0, 0), pokerNow)) {
						haveFound = true;
						result = selectPokers(numList[0], PokerPattern.JOKER,
								PokerPoints.LITTLE_JOKER, result);
					}
					/**
					 * 没找到，找大王
					 */
					if (!haveFound && numList[14] == 2
							&& comparePoker(new Poker(0, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(numList[14], PokerPattern.JOKER,
								PokerPoints.OLD_JOKER, result);
					}
					/**
					 * 没找到，拆主牌
					 */
					if (!haveFound
							&& (numList[this.currentGrade] - pokerList0[2][this.currentGrade]) > 1
							&& comparePoker(new Poker(1, this.currentGrade),
									pokerNow)) {
						haveFound = true;
						result = selectPokers(2, -1, this.currentGrade, result);
					}
					/**
					 * 没知道，拆A
					 */
					if (!haveFound && 1 != this.currentGrade && numList[1] > 1
							&& numList[1] < 4
							&& comparePoker(new Poker(1, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(2, -1, PokerPoints.ACE, result);
					}
					/**
					 * 没找到，从大往小拆
					 */
					if (!haveFound) {
						for (int i = 13; i > 1; i--) {
							if (comparePoker(new Poker(1, i), pokerNow)) {
								if (i != this.currentGrade && numList[i] > 1
										&& numList[i] < 4) {
									haveFound = true;
									result = selectPokers(2, -1, i, result);
									break;
								}
							} else
								break;
						}
					}
					/**
					 * 没找到，出炸
					 */
					if (!haveFound) {
						result = payFire();
						/**
						 * 无炸出，配牌
						 */
						if (result.size() == 0
								&& pokerList0[2][this.currentGrade] > 0) {
							/**
							 * 配A,A不是主
							 */
							if (numList[1] == 1 && 1 != this.currentGrade
									&& comparePoker(new Poker(1, 1), pokerNow)) {
								haveFound = true;
								result = selectPokers(numList[1], -1,
										PokerPoints.ACE, result);
								result = selectPokers(1, PokerPattern.HEART,
										this.currentGrade, result);
							}
							/**
							 * 从大到小配
							 */
							else {
								for (int i = 13; i > 1; i--) {
									if (comparePoker(new Poker(1, i), pokerNow)) {
										if (i != this.currentGrade
												&& numList[i] == 1) {
											haveFound = true;
											result = selectPokers(numList[i],
													-1, i, result);
											result = selectPokers(1,
													PokerPattern.HEART,
													this.currentGrade, result);
											break;
										}
									} else
										break;
								}
							}
							/**
							 * 还没找到，看一下主牌是否有对子
							 */
							if (!haveFound
									&& comparePoker(new Poker(1,
											this.currentGrade), pokerNow)) {
								int k = numList[this.currentGrade]
										- pokerList0[2][this.currentGrade];
								if (k == 1) {
									result = selectPokers(1, -1,
											this.currentGrade, result);
									result = selectPokers(1,
											PokerPattern.HEART,
											this.currentGrade, result);
								} else if (pokerList0[2][this.currentGrade] == 2) {
									result = selectPokers(2,
											PokerPattern.HEART,
											this.currentGrade, result);
								}
							}
						}
					}
					break;
				case PokerType.TRIPLE:
					/**
					 * 从2~K找匹配的三张
					 */
					for (int i = 2; i < 14; i++) {
						if (i != this.currentGrade && numList[i] == 3
								&& comparePoker(new Poker(1, i), pokerNow)) {
							haveFound = true;
							result = selectPokers(numList[i], -1, i, result);
							break;
						}
					}
					/**
					 * 没找到，找三张A
					 */
					if (!haveFound && 1 != this.currentGrade && numList[1] == 3
							&& comparePoker(new Poker(1, 1), pokerNow)) {
						haveFound = true;
						result = selectPokers(numList[1], -1, PokerPoints.ACE,
								result);
					}
					/**
					 * 没找到，找三张主牌
					 */
					if (!haveFound
							&& (numList[this.currentGrade] - pokerList0[2][this.currentGrade]) == 3
							&& comparePoker(new Poker(1, this.currentGrade),
									pokerNow)) {
						haveFound = true;
						result = selectPokers(3, -1, this.currentGrade, result);
					}
					/**
					 * 没找到，出火
					 */
					if (!haveFound) {
						result = payFire();
						/**
						 * 没有火，开始配牌
						 */
						if (result.size() == 0
								&& pokerList0[2][this.currentGrade] > 0) {
							if (numList[1] == 2 && 1 != this.currentGrade
									&& comparePoker(new Poker(1, 1), pokerNow)) {
								haveFound = true;
								result = selectPokers(numList[1], -1, 1, result);
								result = selectPokers(1, PokerPattern.HEART,
										this.currentGrade, result);
							}
							/**
							 * 从大到小组牌
							 */
							else {
								for (int i = 13; i > 1; i--) {
									if (comparePoker(new Poker(1, i), pokerNow)) {
										if (i != this.currentGrade
												&& numList[i] == 2) {
											haveFound = true;
											result = selectPokers(numList[i],
													-1, i, result);
											result = selectPokers(1,
													PokerPattern.HEART,
													this.currentGrade, result);
											break;
										}
									} else
										break;
								}
							}
						}
					}
					break;
				case PokerType.TRIPLE_DOUBLE_STRAIGHT:
					/**
					 * 寻找连对
					 */
					for (int i = pokerType[1] + 1; i < 13; i++) {
						int k = ((i + 2) > 13) ? 1 : (i + 2);
						if (numList[i] == 2 && numList[i + 1] == 2
								&& numList[k] == 2) {
							if (i != this.currentGrade
									&& (i + 1) != this.currentGrade
									&& k != this.currentGrade) {
								haveFound = true;
								result = selectPokers(numList[i], -1, i, result);
								result = selectPokers(numList[i + 1], -1,
										i + 1, result);
								result = selectPokers(numList[k], -1, k, result);
								break;
							}
						}
					}
					if (!haveFound) {
						result = payFire();
					}
					break;
				case PokerType.DOUBLE_TRIPLE_STRAIGHT:
					for (int i = pokerType[1] + 1; i < 14; i++) {
						int k = ((i + 1) > 13) ? 1 : (i + 1);
						if (numList[i] == 3 && numList[k] == 3) {
							if (i != this.currentGrade
									&& k != this.currentGrade) {
								haveFound = true;
								result = selectPokers(numList[i], -1, i, result);
								result = selectPokers(numList[k], -1, k, result);
								break;
							}
						}
					}
					if (!haveFound) {
						result = payFire();
					}
					break;
				case PokerType.TRIPLE_WITH_DOUBLE:
					/**
					 * 先查询是否有对子
					 */
					boolean haveCouple = false;
					for (int i = 1; i < 14; i++) {
						if (numList[i] == 2 && i != this.currentGrade) {
							haveCouple = true;
							break;
						}
					}
					/**
					 * 有对子
					 */
					if (haveCouple) {
						/**
						 * 从小到大找三张
						 */
						for (int i = 2; i < 14; i++) {
							if (i != this.currentGrade && numList[i] == 3
									&& comparePoker(new Poker(1, i), pokerNow)) {
								haveFound = true;
								result = selectPokers(numList[i], -1, i, result);
								break;
							}
						}
						/**
						 * 没找到，找AAA
						 */
						if (!haveFound && 1 != this.currentGrade
								&& numList[1] == 3
								&& comparePoker(new Poker(1, 1), pokerNow)) {
							haveFound = true;
							result = selectPokers(numList[1], -1,
									PokerPoints.ACE, result);
						}
						/**
						 * 没找到，找主牌
						 */
						if (!haveFound
								&& (numList[this.currentGrade] - pokerList0[2][this.currentGrade]) == 3
								&& comparePoker(
										new Poker(1, this.currentGrade),
										pokerNow)) {
							haveFound = true;
							result = selectPokers(3, -1, this.currentGrade,
									result);
						}
						/**
						 * 能够找到3张的话，加入对子
						 */
						if (haveFound) {
							boolean findCouple = false;
							/**
							 * 从小到大找对子
							 */
							for (int i = 2; i < 14; i++) {
								if (i != this.currentGrade && numList[i] == 2) {
									findCouple = true;
									result = selectPokers(numList[i], -1, i,
											result);
									break;
								}
							}
							/**
							 * 没找到小对子，找 AA
							 */
							if (!findCouple && 1 != this.currentGrade
									&& numList[1] == 2) {
								findCouple = true;
								result = selectPokers(numList[1], -1,
										PokerPoints.ACE, result);
							}
						}
					}
					if (!haveFound) {
						result = payFire();
					}
					break;
				case PokerType.STRAIGHT:
					result = payFire();
					break;
				case PokerType.STRAIGHT_FLUSH:
					/**
					 * 分为有无逢人配讨论
					 */
					if (pokerList0[2][this.currentGrade] == 0) {
						for (int j = 1; j < 14; j++) {
							/**
							 * 寻找最小的
							 */
							if (numList[j] > 5) {
								haveFound = true;
								result = selectPokers(numList[j], -1, j, result);
								break;
							}
						}
					} else {
						/**
						 * 先找一遍
						 */
						for (int j = 1; j < 14; j++) {
							/**
							 * 寻找最小的
							 */
							if (numList[j] > 5) {
								haveFound = true;
								result = selectPokers(numList[j], -1, j, result);
								break;
							}
						}
						/**
						 * 没找到加入逢人配，先加一张
						 */
						if (!haveFound) {
							for (int j = 1; j < 14; j++) {
								if (numList[j] > 4 && j != this.currentGrade) {
									haveFound = true;
									result = selectPokers(numList[j], -1, j,
											result);
									result = selectPokers(1,
											PokerPattern.HEART,
											this.currentGrade, result);
									break;
								}
							}
						}
						/**
						 * 找不到，如果有两张逢人配，加入两张逢人配
						 */
						if (!haveFound && pokerList0[2][this.currentGrade] == 2) {
							for (int j = 1; j < 14; j++) {
								if (numList[j] > 3 && j != this.currentGrade) {
									haveFound = true;
									result = selectPokers(numList[j], -1, j,
											result);
									result = selectPokers(2,
											PokerPattern.HEART,
											this.currentGrade, result);
									break;
								}
							}
						}
						/**
						 * 还没找到，看看主牌是否就是符合的炸弹
						 */
						if (!haveFound) {
							if (numList[this.currentGrade] > 5) {
								haveFound = true;
								int count = 0;
								ListIterator<Poker> listIterator;
								while ((numList[this.currentGrade]--) > 0) {
									listIterator = this.pokerList
											.listIterator(count);
									while (listIterator.hasNext()) {
										Poker poker = (Poker) listIterator
												.next();
										if (poker.pattern != 0
												&& poker.points == this.currentGrade) {
											break;
										}
										count++;
									}
									Poker poker = (Poker) this.pokerList
											.get(count);
									pokerList0[poker.pattern - 1][poker.points]--;
									this.pokerList.remove(count);
									result.add(poker);
								}
							}
						}
					}
					/**
					 * 还没找到，看是否有天王炸
					 */
					if (!haveFound && numList[0] == 2 && numList[14] == 2) {
						haveFound = true;
						result = selectPokers(numList[14], PokerPattern.JOKER,
								PokerPoints.OLD_JOKER, result);
						result = selectPokers(numList[0], PokerPattern.JOKER,
								PokerPoints.LITTLE_JOKER, result);
					}
					break;
				case PokerType.BOMB:
					if (Math.random() > 0.4) {
						if (pokerList0[2][this.currentGrade] == 0) {
							/**
							 * 先寻找最小的能压住炸弹
							 */
							for (int j = 1; j < 14; j++) {
								if (numList[j] == currentPoker.size()
										&& j != this.currentGrade
										&& comparePoker(new Poker(1, j),
												pokerNow)) {
									haveFound = true;
									result = selectPokers(numList[j], -1, j,
											result);
									break;
								}
							}
							/**
							 * 张数多一张的炸弹
							 */
							if (!haveFound) {
								outer: for (int i = currentPoker.size() + 1; i < 9; i++) {
									for (int j = 1; j < 14; j++) {
										if (numList[j] == i
												&& j != this.currentGrade) {
											haveFound = true;
											result = selectPokers(numList[j],
													-1, j, result);
											break outer;
										}
									}
								}
							}
						} else {
							/**
							 * 先寻找最小的能压住炸弹
							 */
							for (int j = 1; j < 14; j++) {
								if (numList[j] == currentPoker.size()
										&& j != this.currentGrade
										&& comparePoker(new Poker(1, j),
												pokerNow)) {
									haveFound = true;
									result = selectPokers(numList[j], -1, j,
											result);
									break;
								}
							}
							/**
							 * 张数多一张的炸弹
							 */
							if (!haveFound) {
								outer: for (int i = currentPoker.size() + 1; i < 9; i++) {
									for (int j = 1; j < 14; j++) {
										if (numList[j] == i
												&& j != this.currentGrade) {
											haveFound = true;
											result = selectPokers(numList[j],
													-1, j, result);
											break outer;
										}
									}
								}
							}
							/**
							 * 加入一张逢人配，张数一样多，牌点大的
							 */
							if (!haveFound) {
								for (int j = 1; j < 14; j++) {
									if (numList[j] == currentPoker.size() - 1
											&& j != this.currentGrade
											&& comparePoker(new Poker(1, j),
													pokerNow)) {
										haveFound = true;
										result = selectPokers(numList[j], -1,
												j, result);
										result = selectPokers(1,
												PokerPattern.HEART,
												this.currentGrade, result);
										break;
									}
								}
							}
							/**
							 * 加入逢人配后多一张的炸弹
							 */
							if (!haveFound) {
								outer: for (int i = currentPoker.size(); i < 9; i++) {
									for (int j = 1; j < 14; j++) {
										if (numList[j] == i
												&& j != this.currentGrade) {
											haveFound = true;
											result = selectPokers(numList[j],
													-1, j, result);
											result = selectPokers(1,
													PokerPattern.HEART,
													this.currentGrade, result);
											break outer;
										}
									}
								}
							}

							if (!haveFound
									&& pokerList0[2][this.currentGrade] == 2) {
								for (int j = 1; j < 14; j++) {
									if (numList[j] == currentPoker.size() - 2
											&& j != this.currentGrade
											&& comparePoker(new Poker(1, j),
													pokerNow)) {
										haveFound = true;
										result = selectPokers(numList[j], -1,
												j, result);
										result = selectPokers(2,
												PokerPattern.HEART,
												this.currentGrade, result);
										break;
									}
								}
								if (!haveFound) {
									outer: for (int i = currentPoker.size() - 1; i < 9; i++) {
										for (int j = 1; j < 14; j++) {
											if (numList[j] == i
													&& j != this.currentGrade) {
												haveFound = true;
												result = selectPokers(
														numList[j], -1, j,
														result);
												result = selectPokers(2,
														PokerPattern.HEART,
														this.currentGrade,
														result);
												break outer;
											}
										}
									}
								}
							}
							/**
							 * 看下主牌本身是否符合
							 */
							if (!haveFound) {
								if (numList[this.currentGrade] > 5) {
									haveFound = true;
									int count = 0;
									ListIterator<Poker> listIterator;
									while ((numList[this.currentGrade]--) > 0) {
										listIterator = this.pokerList
												.listIterator(count);
										while (listIterator.hasNext()) {
											Poker poker = (Poker) listIterator
													.next();
											if (poker.pattern != 0
													&& poker.points == this.currentGrade) {
												break;
											}
											count++;
										}
										Poker poker = (Poker) this.pokerList
												.get(count);
										pokerList0[poker.pattern - 1][poker.points]--;
										this.pokerList.remove(count);
										result.add(poker);
									}
								}
							}
						}
						/**
						 * 看下是否有天王炸
						 */
						if (!haveFound && numList[0] == 2 && numList[14] == 2) {
							haveFound = true;
							result = selectPokers(numList[14],
									PokerPattern.JOKER, PokerPoints.OLD_JOKER,
									result);
							result = selectPokers(numList[0],
									PokerPattern.JOKER,
									PokerPoints.LITTLE_JOKER, result);
						}
					}
					break;
				case PokerType.FOUR_JOKER:
					break;
				}
				if (pokerList.size() == 0) {
					setFinished(true);
				}
				return result;
			}
		}
		return result;
	}

	/**
	 * 出火
	 */
	protected ArrayList<Poker> payFire() {
		ArrayList<Poker> result = new ArrayList<Poker>();
		boolean haveFound = false;
		/**
		 * 从4张的炸弹开始循环起，不考虑逢人配
		 */
		outer: for (int i = 4; i < 9; i++) {
			for (int j = 1; j < 14; j++) {
				if (numList[j] == i && j != this.currentGrade) {
					haveFound = true;
					result = selectPokers(numList[j], -1, j, result);
					break outer;
				}
			}
		}
		/**
		 * 还未找到炸弹，开始考虑逢人配
		 */
		if (!haveFound && this.pokerList0[2][this.currentGrade] > 1) {
			for (int j = 1; j < 14; j++) {
				/**
				 * 3+1成炸
				 */
				if (numList[j] == 3 && j != this.currentGrade) {
					haveFound = true;
					result = selectPokers(numList[j], -1, j, result);

					result = selectPokers(1, PokerPattern.HEART,
							this.currentGrade, result);
					break;
				}
			}
		}
		/**
		 * 还没有找到炸弹，2+2成炸
		 */
		if (!haveFound && this.pokerList0[2][this.currentGrade] == 2) {
			for (int j = 1; j < 14; j++) {
				if (numList[j] == 2 && j != this.currentGrade) {
					haveFound = true;
					result = selectPokers(numList[j], -1, j, result);

					result = selectPokers(numList[this.currentGrade],
							PokerPattern.HEART, this.currentGrade, result);
					break;
				}
			}
		}
		/**
		 * 还没找到，看看主牌是否就是炸弹
		 */
		if (!haveFound && numList[this.currentGrade] > 4) {
			haveFound = true;
			result = selectPokers(numList[this.currentGrade]
					- pokerList0[2][this.currentGrade], -1, this.currentGrade,
					result);
			result = selectPokers(pokerList0[2][this.currentGrade],
					PokerPattern.HEART, this.currentGrade, result);
		}
		/**
		 * 还没找到，看是否有天王炸
		 */
		if (!haveFound && numList[0] == 2 && numList[14] == 2) {
			haveFound = true;
			result = selectPokers(numList[14], PokerPattern.JOKER,
					PokerPoints.OLD_JOKER, result);
			result = selectPokers(numList[0], PokerPattern.JOKER,
					PokerPoints.LITTLE_JOKER, result);
		}
		return result;
	}

	/**
	 * 回贡
	 * 
	 * @return 回贡的牌
	 */
	public Poker payBack() {
		ArrayList<Poker> result = new ArrayList<Poker>();
		for (int i = 1; i < 9; i++) {
			for (int j = 2; j < 14; j++) {
				if (numList[j] == i && this.currentGrade != j) {
					result = selectPokers(1, -1, j, result);
					return result.get(0);
				}
			}
		}
		return null;
	}
}
