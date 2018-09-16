package guandan.player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import android.util.Log;

import guandan.constants.AIType;
import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;
import guandan.constants.PokerType;
import guandan.game.GameView;
import guandan.game.Poker;

public class Normal_AI_Player extends AI_Player{

	/**
	 * 可行的所有出牌策略
	 */
	protected LinkedList<AI_Strategy> availStrategy = null;
	
	public Normal_AI_Player(GameView bv, int turn, int AI) {
		super(bv, turn, AIType.normal_AI);
		// TODO Auto-generated constructor stub
	}
	public ArrayList<Poker> lead() {
		ArrayList<Poker> currentPoker = gameView.getCurrentPokers();
		int currentWinner = gameView.getCurrentWinner();
		return normal_AI(currentPoker, currentWinner);
	}

	protected ArrayList<Poker> normal_AI(ArrayList<Poker> currentPoker,int currentWinner){
		availStrategy  = new LinkedList<AI_Strategy>();
		this.leftHands = 100;
		normalAnalyze();
		
		ArrayList<Poker> result = new ArrayList<Poker>();
		Log.i("currentWinner", currentWinner+"");
		if(currentWinner == this.myTurn){
			result = payFirst(result);
		}
		else{
			//if((myTurn + currentWinner)%2 !=0){}
			result = payOthers(currentPoker,currentWinner,result);
			
		} 
//		ArrayList<Poker[]> straightFlush = this.availStrategy.get(availStrategy.size()-1).getStraightFlush();
//		if(straightFlush.size() != 0){
//			
//			Poker[] pokers = straightFlush.get(0);
//			Log.i("ttttt"+this.myTurn,pokers[0].pattern+ " " + pokers[0].points + " " +pokers[1].pattern+ " " + pokers[1].points
//					+ " " +pokers[2].pattern+ " " + pokers[2].points
//					+ " " +pokers[3].pattern+ " " + pokers[3].points
//					+ " " +pokers[4].pattern+ " " + pokers[4].points);
//			result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
//			result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
//			result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
//			result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
//			result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
//			numList[pokers[0].points]--;
//			numList[pokers[1].points]--;
//			numList[pokers[2].points]--;
//			numList[pokers[3].points]--;
//			numList[pokers[4].points]--;
//		}
//		else {
//			straightFlush = this.availStrategy.get(availStrategy.size()-1).getStraight();
//			if(straightFlush.size() != 0){
//				
//				Poker[] pokers = straightFlush.get(0);
//				Log.i("ttttt"+this.myTurn,pokers[0].pattern+ " " + pokers[0].points + " " +pokers[1].pattern+ " " + pokers[1].points
//						+ " " +pokers[2].pattern+ " " + pokers[2].points
//						+ " " +pokers[3].pattern+ " " + pokers[3].points
//						+ " " +pokers[4].pattern+ " " + pokers[4].points);
//				result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
//				result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
//				result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
//				result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
//				result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
//				numList[pokers[0].points]--;
//				numList[pokers[1].points]--;
//				numList[pokers[2].points]--;
//				numList[pokers[3].points]--;
//				numList[pokers[4].points]--;
//			}
//		}
		Log.i("sssssssss", "" + leftHands);
		
		return result;		
	}
	/**
	 * 根据当前手牌，普通AI分析出可行的策略
	 */
	protected void normalAnalyze(){
		ArrayList<ArrayList<Poker[]>> straightFlushList = removeStraightFlush(this.numList,this.pokerList0);
		/**
		 * 开始遍历所有同花顺策略
		 */
		for(int i = 0;i<straightFlushList.size();i++){
			int[] numList1 = this.numList.clone();
			int[][] pokerList01 = copyArray(this.pokerList0);
			changeList(numList1,pokerList01,straightFlushList.get(i));
			/**
			 * 根据去除同花后剩余的牌，生成去除杂花顺的策略
			 */
			ArrayList<ArrayList<Poker[]>> straightList = removeStraight(numList1,pokerList01);
			/**
			 *开始遍历所有杂花顺策略
			 */
			for(int j = 0;j<straightList.size();j++){
				int[] numList2 = numList1.clone();
				int[][] pokerList02 = copyArray(pokerList01);
				changeList(numList2,pokerList02,straightList.get(j));
				/**
				 * 对仍然剩下的牌计算手数
				 */
				AI_Strategy ai_Strategy = new AI_Strategy(0.0,numList2,pokerList02,straightFlushList.get(i),straightList.get(j),null,null);
				ai_Strategy = calculateHands(ai_Strategy);
				/**
				 * 根据剩余手数更新策略
				 */
				if(ai_Strategy.getHands()<this.leftHands){
					/**
					 * 先删除旧策略，大于2的被删除
					 */
					int t = 0;
					ListIterator<AI_Strategy> listIterator = this.availStrategy.listIterator();
					while(listIterator.hasNext()){
						if(listIterator.next().getHands() > ai_Strategy.getHands()+1){
							this.availStrategy.remove(t);
							listIterator = this.availStrategy.listIterator(t);
						}
						else{
							t++;
						}
					}
					
					this.availStrategy.add(ai_Strategy);
					this.leftHands = ai_Strategy.getHands();
				}
				else if((ai_Strategy.getHands() == this.leftHands)||(ai_Strategy.getHands() == this.leftHands+0.5)|| (ai_Strategy.getHands() == this.leftHands+1)){
					this.availStrategy.add(ai_Strategy);
				}
			}
			
		}
	}
	protected ArrayList<ArrayList<Poker[]>> removeStraightFlush(int[] numList,int[][] pokerList0){
		ArrayList<ArrayList<Poker[]>> result = new ArrayList<ArrayList<Poker[]>>();
		ArrayList<ArrayList<Poker[]>> strategyList0;
		/**
		 * 添加了一个size为0的对象，方便组合所有情况
		 */
		result.add(new ArrayList<Poker[]>());
		/**
		 * 根据不同的花色，点数从小到大开始查找
		 */
		for(int i = 0; i < 4;i++){
			/**
			 * 保存当前花色的策略
			 */
			strategyList0 = new ArrayList<ArrayList<Poker[]>>();
			strategyList0.add(new ArrayList<Poker[]>());
			/**
			 * 两个指针来控制查找同花顺
			 */
			int start = PokerPoints.ACE;
			int end = PokerPoints.ACE;
			/**
			 * begin用来控制残缺的同花顺的开始
			 * count用来计数begin到end区段同花的张数
			 */
			int begin = PokerPoints.ACE;
			int count = 0;
			/**
			 * 控制是否已经进行一遍循环
			 */
			boolean haveCircle = false;
			do{
				/**
				 * end到14的时候要变回A，且have赋值为true表示已经循环一遍
				 */
				if(end == 14){
					end = PokerPoints.ACE;
					haveCircle = true;
				}
				/**
				 * 查看当前end是否能有牌，这样才有完整的同花顺的可能
				 */
				if(pokerList0[i][end] == 0){
					start = end+1;
					/**
					 * 大于10的时候不可能再有顺子了
					 */
					if (start > PokerPoints.TEN){
						haveCircle = true;
					}
				}
				else{
					/**
					 * 当前end不空，且end-start = 4或-9表明有同花顺
					 */
					if((end - start == 4) || (end - start == -9)){
						Poker[] straightFlush = new Poker[5];
						straightFlush[0] = new Poker(i+1,start);
						straightFlush[1] = new Poker(i+1,start+1);
						straightFlush[2] = new Poker(i+1,start+2);
						straightFlush[3] = new Poker(i+1,start+3);
						straightFlush[4] = new Poker(i+1,end);
						strategyList0 = addToStraightFlushStrategy(strategyList0,straightFlush,numList,pokerList0);
						/**
						 * 检查特殊情况：这一区段可以组成两个同花顺
						 */
						if(pokerList0[i][start] == 2 && pokerList0[i][start+1] == 2 &&
								pokerList0[i][start+2] == 2 && pokerList0[i][start+3] == 2&&
								pokerList0[i][end] == 2){
							strategyList0 = addToStraightFlushStrategy(strategyList0,straightFlush.clone(),numList,pokerList0);
						}
						/**
						 * start推进
						 */
						start++;
					}
				}
				/**
				 * 查找残缺的同花顺
				 */
				if(pokerList0[i][end] > 0){
					count++;					
				}
				if((end - begin == 4) || (end - begin == -9)){
					/**
					 * begin所指的牌的数量不为0，保证顺子尽可能大，但是begin为10的情况除外，因为顺子不能再大了
					 */
					if(pokerList0[i][begin]>0||begin == PokerPoints.TEN){
						/**
						 * 可以补牌的情况
						 */
						if((count  == 3 && pokerList0[PokerPattern.HEART-1][this.currentGrade] == 2)||
								(count  == 4 && pokerList0[PokerPattern.HEART-1][this.currentGrade] > 0)){
							Poker[] straightFlush = new Poker[5];
							/**
							 * 根据残缺状况补全顺子
							 */
							if(pokerList0[i][begin]>0){
								straightFlush[0] = new Poker(i+1,begin);
							}
							else{
								straightFlush[0] = new Poker(PokerPattern.HEART,this.currentGrade);
							}
							if(pokerList0[i][begin+1]>0){
								straightFlush[1] = new Poker(i+1,begin+1);
							}
							else{
								straightFlush[1] = new Poker(PokerPattern.HEART,this.currentGrade);
							}
							if(pokerList0[i][begin+2]>0){
								straightFlush[2] = new Poker(i+1,begin+2);
							}
							else{
								straightFlush[2] = new Poker(PokerPattern.HEART,this.currentGrade);
							}
							if(pokerList0[i][begin+3]>0){
								straightFlush[3] = new Poker(i+1,begin+3);
							}
							else{
								straightFlush[3] = new Poker(PokerPattern.HEART,this.currentGrade);
							}
							if(pokerList0[i][end]>0){
								straightFlush[4] = new Poker(i+1,end);
							}
							else{
								straightFlush[4] = new Poker(PokerPattern.HEART,this.currentGrade);
							}
							strategyList0 = addToStraightFlushStrategy(strategyList0,straightFlush,numList,pokerList0);
							/**
							 * 检查特殊情况，count数为4，且该牌都为2张，且逢人配有两个，则个组成两个同花顺
							 */
							if(count == 4 && pokerList0[PokerPattern.HEART-1][this.currentGrade] == 2){
								int count2 = 0;
								if(pokerList0[i][begin] == 2){
									count2 ++;
								}
								if(pokerList0[i][begin+1] == 2){
									count2 ++;
								}
								if(pokerList0[i][begin+2] == 2){
									count2 ++;
								}
								if(pokerList0[i][begin+3] == 2){
									count2 ++;
								}
								if(pokerList0[i][end] == 2){
									count2 ++;
								}
								if(count2 ==4){
									strategyList0 = addToStraightFlushStrategy(strategyList0,straightFlush.clone(),numList,pokerList0);
								}
							}
						}
						/**
						 * 因为begin的推进，且当前begin是有牌的（最后的10,没牌无所谓了），count要减1
						 */
						count--;
					}
					begin++;
				}
				/**
				 * end推进
				 */
				end++;
			}while(!haveCircle);
			/**
			 * 将各花色的策略加入result
			 */
			if(result.size() == 1){
				result = (ArrayList<ArrayList<Poker[]>>) strategyList0.clone();
			}
			else{
				int length = result.size();
				int[] count1 = new int[length];
				int[] count2 = new int[strategyList0.size()];
				for(int j = 0;j<length;j++){
					/**
					 * 计数指定策略中有几个分人配
					 */
					for(int  q= 0;q<result.get(j).size();q++){
						for(int t = 0;t<5;t++){
							if(result.get(j).get(q)[t].pattern==PokerPattern.HEART&&result.get(j).get(q)[t].points==this.currentGrade){
								count1[j]++;
							}
						}
					}
				}
				for(int j = 0;j<strategyList0.size();j++){
					/**
					 * 计数指定策略中有几个分人配
					 */
					for(int  q= 0;q<strategyList0.get(j).size();q++){
						for(int t = 0;t<5;t++){
							if(strategyList0.get(j).get(q)[t].pattern==PokerPattern.HEART&&strategyList0.get(j).get(q)[t].points==this.currentGrade){
								count2[j]++;
							}
						}
					}
				}
				for(int j = 0;j<length;j++){
					for(int k = 0;k<strategyList0.size();k++){
						/**
						 * 避免逢人配冲突
						 */
						if(count1[j]+count2[k]<pokerList0[PokerPattern.HEART-1][this.currentGrade]+1){
							if(strategyList0.get(k).size() == 0){	
							}
							else{
								ArrayList<Poker[]> newStrategy = (ArrayList<Poker[]>) result.get(j).clone();
								ArrayList<Poker[]> strategy = strategyList0.get(k);
								for(int h = 0;h<strategy.size();h++){
									Poker[] newPokers = strategy.get(h).clone();
									newStrategy.add(newPokers);
								}
								result.add(newStrategy);
							}
						}
					}
				}
			}
		}
		return result;
	}
	/**
	 * 去除手牌中的杂花顺
	 * @return 所有去除杂花的策略
	 */
	protected ArrayList<ArrayList<Poker[]>> removeStraight(int[] numList,int[][] pokerList0){
		ArrayList<ArrayList<Poker[]>> result = new ArrayList<ArrayList<Poker[]>>();
		/**
		 * 添加了一个size为0的对象，方便组合所有情况
		 */
		result.add(new ArrayList<Poker[]>());
		/**
		 * 两个指针来控制查找同花顺
		 */
		int start = PokerPoints.ACE;
		int end = PokerPoints.ACE;
		/**
		 * 记start~end间单牌数
		 */
		int count = 0;
		/**
		 * 控制是否已经进行一遍循环
		 */
		boolean haveCircle = false;
		do{
			/**
			 * end到14的时候要变回A，且have赋值为true表示已经循环一遍
			 */
			if(end == 14){
				end = PokerPoints.ACE;
				haveCircle = true;
			}
			/**
			 * 区间内至少有两张单牌才能组顺 主牌不计入单牌数
			 */
			if((numList[end] == 0)||(end == this.currentGrade && (numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade] == 0))){
				count = 0;
				start = end+1;
				/**
				 * 大于10的时候不再可能有顺子
				 */
				if (start > PokerPoints.TEN){
					haveCircle = true;
				}
			}
			else {
				if(numList[end] == 1 && end != this.currentGrade){
					count++;
				}
				/**
				 * end-start为4或-9才可能是顺子 且这一区间内有至少两张单牌才组顺
				 */
				if(((end - start == 4) || (end - start == -9))&&count>1){
					Poker[] straightFlush = new Poker[5];
					straightFlush[0] = new Poker(-1,start);
					straightFlush[1] = new Poker(-1,start+1);
					straightFlush[2] = new Poker(-1,start+2);
					straightFlush[3] = new Poker(-1,start+3);
					straightFlush[4] = new Poker(-1,end);
					result = addToStraightStrategy(result,straightFlush,numList,pokerList0);
					/**
					 * start推进
					 */
					start++;
				}
			}
			/**
			 * end推进
			 */
			end++;
		}while(!haveCircle);
		return result;
	}
	protected ArrayList<ArrayList<Poker[]>> addToStraightFlushStrategy(ArrayList<ArrayList<Poker[]>> strategyList,Poker[] straightFlush,int[] numList,int[][] pokerList0){
		int[] judgeList = new int[15];
		ArrayList<Poker[]> strategy = new ArrayList<Poker[]>();
		int length = strategyList.size();
		/**
		 * 新同花顺中逢人配的数量,及位置
		 */
		int count = 0;
		int[] position = {-1,-1};
		for(int i = 0 ;i< straightFlush.length;i++){
			if(straightFlush[i].pattern == PokerPattern.HEART && straightFlush[i].points == this.currentGrade){
				/**
				 * 逢人配数量超出，返回，这是因为在计算红桃同花顺时，多计算了红桃主牌
				 */
				if(count == 2){
					return strategyList;
				}
				position[count] = i;
				count++;
			}
		}
		if(count > pokerList0[PokerPattern.HEART-1][this.currentGrade]){
			/**
			 * 逢人配数量超出，返回，这是因为在计算红桃同花顺时，多计算了红桃主牌
			 */
			return strategyList;
		}
		/**
		 * 对所有策略进行更新
		 */
		for(int i = 0;i < length; i++){
			strategy = strategyList.get(i);
			/**
			 * 计数策略中的逢人配数量
			 */
			int countAll = 0;
			/**
			 * 如果原策略为空，将新策略加入即可
			 */
			if(strategy.size() == 0){
				ArrayList<Poker[]> newStrategy = new ArrayList<Poker[]>();
				newStrategy.add(straightFlush);
				strategyList.add(newStrategy);
			}
			/**
			 * 原策略不为空，查找冲突，再更新
			 */
			else{
				/**
				 * 检查是否冲突
				 */
				/**
				 * 初始化
				 */
				for(int t=0;t<15;t++){
					judgeList[t] = 0;
				}
				for(int k  = 0;k < strategy.size();k++){
					/**
					 * 判断数组赋值
					 */
					for(int p = 0;p<5;p++){
						if(strategy.get(k)[p].pattern == PokerPattern.HEART && strategy.get(k)[p].points == this.currentGrade){
							countAll++;
						}
						else{
							judgeList[strategy.get(k)[p].points]++;
						}
					}
					
				}
				boolean ifConflict = false;
				if(countAll+ count > pokerList0[PokerPattern.HEART-1][this.currentGrade]){
					ifConflict = true;
				}
				for(int j = 0;j< 5;j++){
					if(straightFlush[j].pattern == PokerPattern.HEART && straightFlush[j].points==this.currentGrade){						
					}
					else{
						if(judgeList[straightFlush[j].points]>pokerList0[straightFlush[j].pattern-1][straightFlush[j].points]-1){
							ifConflict = true;
							break;
						}
					}
				}
				/**
				 * 不冲突才加入策略
				 */
				if(!ifConflict){
					ArrayList<Poker[]> newStrategy = (ArrayList<Poker[]>) strategy.clone();
					newStrategy.add(straightFlush);
					strategyList.add(newStrategy);
				}
				
			}
		}
		return strategyList;
		
	}
	protected ArrayList<ArrayList<Poker[]>> addToStraightStrategy(ArrayList<ArrayList<Poker[]>> strategyList,Poker[] straight,int[] numList,int[][] pokerList0){
		int[] judgeList = new int[5];
		ArrayList<Poker[]> strategy = new ArrayList<Poker[]>();
		int length = strategyList.size();
		/**
		 * 顺子中的逢人配的数量,及位置
		 */
		int count = 0;
		int[] position = {-1,-1};
		for(int i = 0 ;i< straight.length;i++){
			if(straight[i].pattern == PokerPattern.HEART && straight[i].points == this.currentGrade){
				/**
				 * 逢人配数量超出，返回，防止多计算了红桃主牌
				 */
				if(count == 2){
					return strategyList;
				}
				position[count] = i;
				count++;
			}
		}
		if(count > pokerList0[PokerPattern.HEART-1][this.currentGrade]){
			/**
			 * 逢人配数量超出，返回，这是因为在计算红桃同花顺时，多计算了红桃主牌
			 */
			return strategyList;
		}
		/**
		 * 对所有策略进行更新
		 */
		for(int i = 0;i < length; i++){
			strategy = strategyList.get(i);
			/**
			 * 计数策略中的逢人配数量
			 */
			int countAll = 0;
			/**
			 * 如果原策略为空，将新策略加入即可
			 */
			if(strategy.size() == 0){
				ArrayList<Poker[]> newStrategy = new ArrayList<Poker[]>();
				newStrategy.add(straight);
				strategyList.add(newStrategy);
			}
			/**
			 * 原策略不为空，查找冲突，再更新
			 */
			else{
				/**
				 * 判断数组初始化
				 */
				for(int j = 0; j < 5;j++){
					judgeList[j]= 0;
				}
				/**
				 * 检查是否冲突
				 */
				for(int k  = 0;k < strategy.size();k++){
					/**
					 * 可能冲突的顺子范围
					 */
					switch(strategy.get(k)[0].points-straight[0].points){
					case -4:
						judgeList[0]++;
						break;
					case -3:
						judgeList[0]++;
						judgeList[1]++;
						break;
					case -2:
						judgeList[0]++;
						judgeList[1]++;
						judgeList[2]++;
						break;
					case -1:
						judgeList[0]++;
						judgeList[1]++;
						judgeList[2]++;
						judgeList[3]++;
						break;
					case 0:
						judgeList[0]++;
					    judgeList[1]++;
					    judgeList[2]++;
						judgeList[3]++;
						judgeList[4]++;
						   break;
					case 1:
						judgeList[1]++;
					    judgeList[2]++;
						judgeList[3]++;
						judgeList[4]++;
						break;
					case 2:
						judgeList[2]++;
						judgeList[3]++;
						judgeList[4]++;
						break;
					case 3:
						judgeList[3]++;
						judgeList[4]++;
						break;
					case 4:
						judgeList[4]++;
						break;
					}
					/**
					 * 计数逢人配
					 */
					for(int h = 0;h<5;h++){
						if(strategy.get(k)[h].pattern == PokerPattern.HEART && strategy.get(k)[h].points ==this.currentGrade){
							countAll++;
						}
					}
				}
				boolean ifConflict = false;
				if(countAll+ count >pokerList0[PokerPattern.HEART-1][this.currentGrade]){
					ifConflict = true;
				}
				for(int j = 0;j< 5;j++){
					if(j!=position[0]&& j!=position[1]){
						if(straight[j].points == this.currentGrade){
							if(judgeList[j]>numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]-1){
								ifConflict = true;
								break;
							}
						}
						else if(judgeList[j]>numList[straight[j].points]-1){
							ifConflict = true;
							break;
						}
					}
				}
				/**
				 * 不冲突才加入策略
				 */
				if(!ifConflict){
					ArrayList<Poker[]> newStrategy = (ArrayList<Poker[]>) strategy.clone();
					newStrategy.add(straight);
					strategyList.add(newStrategy);
				}
				
			}
		}
		return strategyList;
		
	}
	protected AI_Strategy calculateHands(AI_Strategy ai_Strategy){
		double hands = 0;
		int[] numList = ai_Strategy.getNumList();
		int[][] pokerList0 = ai_Strategy.getPokerList0();
		if(numList[0]==2&&numList[14]==2){
			hands-=1;
		}
		else{
			if(numList[0]!=0){
				hands+=1;
			}
			if(numList[14]!=0){
				hands+=0.5;
			}
		}
		int doublePoker = 0;
		int triplePoker = 0;
		for(int i = 1;i<14;i++){
			int judge = 0;
			if(i!=this.currentGrade){
				judge = numList[i];
				
			}
			else{
				judge = numList[i] - pokerList0[PokerPattern.HEART-1][this.currentGrade];
			}
			switch(judge){
			case 0:break;
			case 1:hands+=1;
			       break;
			case 2:doublePoker++;
			       break;
			case 3:triplePoker++;
			       break;
			case 4:hands-=0.5;
			       break;
			case 5:
			case 6:
			case 7:hands-=1;
			       break;
			case 8:hands-=1.5;
			}
		}
		hands =hands -pokerList0[PokerPattern.HEART-1][this.currentGrade]*0.5;
		
		/**
		 * 对三张和两张的组合进行单独计算
		 */
		int maxHands = Math.max(doublePoker, triplePoker);
		int special = 0;
		ArrayList<Poker[]> tD = new ArrayList<Poker[]>();
		ArrayList<Poker[]> dT = new ArrayList<Poker[]>();
		if(doublePoker>2){
			for(int i = 1;i<13;i++){
				int k = ((i+2)>13)?1:(i+2);
				if(numList[i]== 2&& numList[i+1] == 2 && numList[k]==2){
					if(i!= this.currentGrade && (i+1)!= this.currentGrade && k !=this.currentGrade){
						doublePoker-=3;
						special ++;
						Poker[] pokers = new Poker[6];
						pokers[0] = new Poker(-1,i);
						pokers[1] = new Poker(-1,i);
						pokers[2] = new Poker(-1,i+1);
						pokers[3] = new Poker(-1,i+1);
						pokers[4] = new Poker(-1,k);
						pokers[5] = new Poker(-1,k);
						tD.add(pokers);
						if(i==12){
							break;
						}
						i=k;
					}
				}
			}
		}
		if(triplePoker>1){
			for(int i = 1;i<14;i++){
				int k = ((i+1)>13)?1:(i+1);
				if(numList[i]== 3&& numList[k] == 3){
					if(i!= this.currentGrade && k !=this.currentGrade){
						triplePoker-=2;
						special++;
						Poker[] pokers = new Poker[6];
						pokers[0] = new Poker(-1,i);
						pokers[1] = new Poker(-1,i);
						pokers[2] = new Poker(-1,i);
						pokers[3] = new Poker(-1,k);
						pokers[4] = new Poker(-1,k);
						pokers[5] = new Poker(-1,k);
						dT.add(pokers);
						if(i==13){
							break;
						}
						i = k;
					}
				}
			}
		}
		hands+= Math.min(Math.max(doublePoker, triplePoker)+special, maxHands);
		hands = hands + ai_Strategy.getStraight().size() - ai_Strategy.getStraightFlush().size();
		ai_Strategy.setHands(hands);
		ai_Strategy.setTripleDouble(tD);
		ai_Strategy.setDoubleTriple(dT);
		return ai_Strategy;
	}
	/**
	 * 根据选定的策略，对相应计数的数组作减法
	 * @param numList
	 * @param pokerList0
	 * @param straightFlush
	 */
	protected void changeList(int[] nl,int[][] pL,ArrayList<Poker[]> straightFlush){
		for(int i = 0;i<straightFlush.size();i++){
			for(int j = 0;j<straightFlush.get(i).length;j++){
				nl[straightFlush.get(i)[j].points]--;
				/**
				 * 杂花顺的牌的pattern都是以-1记的，同花顺则是什么pattern就是什么pattern
				 * 这意味着如果是杂花顺策略，除了红心主牌，pL是不更新的
				 */
				if(straightFlush.get(i)[j].pattern!=-1){
					pL[straightFlush.get(i)[j].pattern-1][straightFlush.get(i)[j].points]--;
				}
			}
		}
	}
	/**
	 * 根据选定的策略，对相应计数的数组作加法
	 * @param numList
	 * @param pokerList0
	 * @param straightFlush
	 */
	protected void addList(int[] nl,int[][] pL,ArrayList<Poker[]> straightFlush){
		for(int i = 0;i<straightFlush.size();i++){
			for(int j = 0;j<straightFlush.get(i).length;j++){
				nl[straightFlush.get(i)[j].points]++;
				/**
				 * 杂花顺的牌的pattern都是以-1记的，同花顺则是什么pattern就是什么pattern
				 * 这意味着如果是杂花顺策略，除了红心主牌，pL是不更新的
				 */
				if(straightFlush.get(i)[j].pattern!=-1){
					pL[straightFlush.get(i)[j].pattern-1][straightFlush.get(i)[j].points]++;
				}
			}
		}
	}
	protected int[][] copyArray(int[][] s){
		int[][] k = new int[s.length][s[0].length];
		for(int i = 0;i<s.length;i++){
			for(int j = 0;j<s[0].length;j++){
				k[i][j] = s[i][j];
			}
		}
		return k;
	}
	/**
	 * 出牌
	 */
	protected ArrayList<Poker> payFirst(ArrayList<Poker> result){
		boolean haveFound = false;
		AI_Strategy chooseStrategy = null;
		/**
		 * 下家手牌与下下家与下下下家手牌
		 */
		int control1 = gameView.getPlayerPokerNum()[(this.myTurn + 1) % 4];
		int control2 = gameView.getPlayerPokerNum()[(this.myTurn + 2) % 4];
		int control3 = gameView.getPlayerPokerNum()[(this.myTurn + 3) % 4];
		/**
		 * 选定一个策略
		 */
		for(int i = 0;i<availStrategy.size();i++){
			if(availStrategy.get(i).getHands() == this.leftHands){
				chooseStrategy = availStrategy.get(i);
				changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
				changeList(numList,pokerList0,chooseStrategy.getStraight());
				changeList(numList,pokerList0,chooseStrategy.getDoubleTriple());
				changeList(numList,pokerList0,chooseStrategy.getTripleDouble());
				break;
			}
		}
		if(pokerList.size()<7 && pokerType(new ArrayList<Poker>(pokerList))[0]!=PokerType.INVALID_TYPE){
			haveFound =true;
			int length = pokerList.size();
			for(int i = 0;i<length;i++){
				result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
			}
		}
		/**
		 * 找能收回的单张
		 */
		if(control1==1||control3==1){}
		else if(!haveFound && (numList[0]>0||numList[14]>0||((outNumList[0]+outNumList[14]>2)&&(numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]>0)))){
			/**
			 * 对2~k寻找正好为一张的
			 */
			for(int i = 2;i<14;i++){
				if(i!= this.currentGrade && numList[i] == 1){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
		    /**
		     * 没找到，寻找A为单张，A不为主牌
		     */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 1){
				haveFound = true;
				result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			}
		}
		
		/**
		 * 找能收回的3张或3+2
		 */
		if(!haveFound && ((numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]==3)||(numList[1]==3)||(numList[13]==3))){
			for(int i = 2;i<13;i++){
				if(numList[i] == 3 && i!= this.currentGrade){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
			if(haveFound){
				/**
				 * 看能不能加入对子
				 */
				int count = 0;
				for(int i = 2;i<14;i++){
					if(numList[i] == 2 && i!= this.currentGrade){
						count++;
					}
				}
				if(count>1){
					for(int i = 2;i<14;i++){
						if(numList[i] == 2 && i!= this.currentGrade){
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
				}
			}
		}
		/**
		 * 找能收回的对子
		 */
		if(!haveFound && (control1==2||control3==2)){}
		else if(!haveFound && ((numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]==2)||(numList[1]==2)||(numList[13]==2))){
			for(int i = 2;i<14;i++){
				if(numList[i] == 2 && i!= this.currentGrade){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
		}
		/**
		 * 补回保留的连对，钢板，杂花顺
		 */
		addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
		addList(numList,pokerList0,chooseStrategy.getTripleDouble());
		addList(numList,pokerList0,chooseStrategy.getStraight());
		/**
		 * 出杂花顺
		 */
		if(!haveFound){
			if(chooseStrategy.getStraight().size()!=0){
				haveFound=true;
				Poker[] pokers = chooseStrategy.getStraight().get(0);
				result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
				result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
				result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
				result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
				result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
			}
		}
		/**
		 * 出连对
		 */
		if(!haveFound){
			if(chooseStrategy.getTripleDouble().size()!=0){
				Poker[] pokers = chooseStrategy.getTripleDouble().get(0);
				/**
				 * 控制一下不让大的连对过早打出
				 */
				if(pokers[0].points>PokerPoints.TEN && pokerList.size()> 19){
					
				}
				else if(pokers[0].points == PokerPoints.QUEEN && pokerList.size()> 15){
					
				}
				else{
					haveFound=true;
					result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
					result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
					result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
					result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
					result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
					result= selectPokers(1,pokers[5].pattern,pokers[5].points,result);
				}
			}
		}
		/**
		 * 出钢板
		 */
		if(!haveFound){
			if(chooseStrategy.getDoubleTriple().size()!=0){
				Poker[] pokers = chooseStrategy.getDoubleTriple().get(0);
				/**
				 * 控制一下，不让过大的钢板太早打出
				 */
				if(pokers[0].points>PokerPoints.TEN && pokerList.size()> 21){
					
				}
				else if(pokers[0].points>PokerPoints.JACK && pokerList.size()> 15){
					
				}
                else if(pokers[0].points>PokerPoints.QUEEN && pokerList.size()> 13){
					
				}
				else{
					haveFound=true;
					result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
					result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
					result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
					result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
					result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
					result= selectPokers(1,pokers[5].pattern,pokers[5].points,result);
				}
				
			}
		}
		/**
		 * 从最小的牌开始找起,2~k的循环，去除主牌
		 */
		if(!haveFound){
			for(int i = 2; i<14;i++){
				if(numList[i] > 0 && numList[i] < 4  && i != this.currentGrade){
					int pokerNum = numList[i];
					/**
					 * 控制不出对手要的牌
					 */
					if((numList[i] == 1||numList[i] == 2) &&(control1 == numList[i] || control3 == numList[i])){
						
					}
					else if (pokerNum == 1){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
					/**
					 * 是两张的话，看看是否能组成连对，和3+2
					 */
					else if(pokerNum == 2){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+2)>13)?1:(i+2);
						/**
						 * 是否连队
						 */
						if(i<13 && numList[i+1] == 2 && numList[k]==2){
							if((i+1)!= this.currentGrade && k !=this.currentGrade){
								result = selectPokers(numList[i+1],-1,i+1,result);
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * 能不能组成3+2
						 */
						else{
							/**
							 * 在一定范围内寻找三张
							 */
							for(int j = 2; j <14 && j<(i+6);j++){
								if (numList[j] == 3 && j!= this.currentGrade){
									result = selectPokers(numList[j],-1,j,result);
									break;
								}
							}
						}
						break;
					}
					/**
					 * 是三张的话，看看能否组成其他牌
					 */
					else if(pokerNum == 3){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+1)>13)?1:(i+1);
						/**
						 * 能否组成钢板
						 */
						if(numList[k]==3){
							if(k !=this.currentGrade){							
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * 能否组成3+2
						 */
						else{
							/**
							 * 在一定范围内寻找对子
							 */
							for(int j = 2; j <14 && j<(i+5);j++){
								if (numList[j] == 2 && j!= this.currentGrade){
									result = selectPokers(numList[j],-1,j,result);
									break;
								}
							}
						}
						break;
					}
				}
			}
		}
		/**
		 * 如果还未找到，对前面为了控制对手剩余的手牌进行处理
		 */
		if(!haveFound && (control1==1||control1==2||control3==1||control3==2)){
			/**
			 * 一个剩一张，一个剩2张
			 */
			if(control1+control3 == 3){
				if(control2>4){
					if(pokerList0[PokerPattern.HEART-1][this.currentGrade]!=0){
						for(int i = 2; i<14;i++){
							if(numList[i]== 2  && i != this.currentGrade){
								haveFound = true;
								result = selectPokers(2,-1,i,result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
								for(int j = i+1;j<15;j++){
									int current = (j==14)?1:j;
									if(numList[current]== 2  && current != this.currentGrade){
										result = selectPokers(2,-1,current,result);
										break;
									}
								}
								break;
							}
						}
					}
				}
				else if(control1 == 1 && control2 ==2){
					for(int i = 2; i<14;i++){
						if(numList[i]== 2  && i != this.currentGrade){
							haveFound = true;
							result = selectPokers(2,-1,i,result);
							break;
						}
					}
					if(!haveFound && pokerList0[PokerPattern.HEART-1][this.currentGrade]!=0){
						for(int i = 2; i<14;i++){
							if(numList[i]== 1  && i != this.currentGrade){
								haveFound = true;
								result = selectPokers(1,-1,i,result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
								break;
							}
						}
					}
				}
				if(!haveFound){
					int count = 0;
					int[] numP = new int[13];
					for(int i = 2;i<14;i++){
						if(i!=this.currentGrade&& numList[i]==1){
							numP[count] = i;
							count++;
						}
					}
					if(1!= this.currentGrade && numList[1] == 1){
						numP[count] = 1;
						count++;
					}
					if(outNumList[0]+outNumList[14]+numList[0]+numList[14] != 4){
						if(numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]==1){
							numP[count] = this.currentGrade;
							count++;
						}
					}
					if(control2 == 0 || control1 != 1){
						if(count==2){
							if(pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
								haveFound = true;
								result = selectPokers(1,-1,numP[0],result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
							else{
								haveFound = true;
								result = selectPokers(1,-1,numP[1],result);
							}					
						}
						if(!haveFound && count>2){
							if(pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
								haveFound = true;
								result = selectPokers(1,-1,numP[0],result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
							else{
								haveFound = true;
								result = selectPokers(1,-1,numP[2],result);
							}
						}
					}
					else{
						if(count>0){
							if(pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
								haveFound = true;
								result = selectPokers(1,-1,numP[0],result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
							else{
								haveFound = true;
								result = selectPokers(1,-1,numP[count-1],result);
							}
						}
					}
				}
				if(!haveFound){
					for(int i = 2; i<14;i++){
						if(numList[i]== 2  && i != this.currentGrade){
							haveFound = true;
							result = selectPokers(1,-1,i,result);
							break;
						}
					}
				}
			}
			/**
			 * 都剩余1张，或一个打完，另一个剩余1张,或以个剩余1张，一个还有很多的情况
			 */
			else if(control1 == 1|| control3 == 1){
				int count = 0;
				int[] numP = new int[13];
				for(int i = 2;i<14;i++){
					if(i!=this.currentGrade&& numList[i]==1){
						numP[count] = i;
						count++;
					}
				}
				if(1!= this.currentGrade && numList[1] == 1){
					numP[count] = 1;
					count++;
				}
				if(outNumList[0]+outNumList[14]+numList[0]+numList[14] != 4){
					if(numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]==1){
						numP[count] = this.currentGrade;
						count++;
					}
				}
				if(control2 == 0 || control1 != 1){
					if(count==2){
						if(pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
							haveFound = true;
							result = selectPokers(1,-1,numP[0],result);
							result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
						}
						else{
							haveFound = true;
							result = selectPokers(1,-1,numP[1],result);
						}					
					}
					if(!haveFound && count>2){
						if(pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
							haveFound = true;
							result = selectPokers(1,-1,numP[0],result);
							result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
						}
						else{
							haveFound = true;
							result = selectPokers(1,-1,numP[2],result);
						}
					}
				}
				else{
					if(count>0){
						if(pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
							haveFound = true;
							result = selectPokers(1,-1,numP[0],result);
							result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
						}
						else{
							haveFound = true;
							result = selectPokers(1,-1,numP[count-1],result);
						}
					}
				}
			}
			/**
			 * 都剩余2张，或一个打完，另一个剩余2张,或以个剩余2张，一个还有很多的情况
			 */
			else{
				if(pokerList0[PokerPattern.HEART-1][this.currentGrade] == 0){
					for(int i = 2; i<14;i++){
						if(numList[i]== 2  && i != this.currentGrade){
							haveFound = true;
							result = selectPokers(1,-1,i,result);
							break;
						}
					}
				}
				else{
					for(int i = 2; i<14;i++){
						if(numList[i]== 2  && i != this.currentGrade){
							haveFound = true;
							result = selectPokers(2,-1,i,result);
							result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							for(int j = i+1;j<15;j++){
								int current = (j==14)?1:j;
								if(numList[current]== 2  && current != this.currentGrade){
									result = selectPokers(2,-1,current,result);
									break;
								}
							}
							break;
						}
					}
				}
			}
				
		}
		/**
		 * 未找到，在A中寻找，且A不是主牌
		 */
		if(!haveFound &&(numList[1] > 0 && numList[1] < 4)&& 1!= this.currentGrade){
			haveFound = true;
			int pokerNum = numList[1];
			result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			/**
			 * 看看能否组成3+2
			 */
			if (pokerNum == 3){
				/**
				 * 除去逢人配，主牌正好有两张
				 */
				if ((numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2){
					result = selectPokers(2,-1,this.currentGrade,result);

				}
				/**
				 * 或者两张小王
				 */
				else if (numList[0] == 2 && numList[14]!=2){
					result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
				}
				/**
				 * 或者两张大王
				 */
				else if (numList[14] == 2 && numList[0] != 2){
					result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
				}
			}
		}
		/**
		 * 查询一下主牌以下还有牌没,1代表有牌
		 */
		int havePoker = 0;
		for(int i = 1;i<14;i++){
			if(i!=this.currentGrade&&numList[i]!=0){
				havePoker =1;
				break;
			}
		}
		/**
		 * 如果之前未找到牌出，且主牌下还有炸弹，出主牌（不出逢人配）
		 */
		if(!haveFound && havePoker == 1){
			int pokerNum = numList[this.currentGrade] - pokerList0[2][this.currentGrade];
			/**
			 * 不是炸弹的话
			 */
			if(pokerNum>0 && pokerNum<4){
				haveFound = true;
				result = selectPokers(pokerNum,-1,this.currentGrade,result);
				/**
				 * 看能不能和大小王组成3+2
				 */
				if (pokerNum == 3){
				    if (numList[0] == 2 && numList[14] !=2){
				    	result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
					}
					else if (numList[14] == 2 && numList[0]!=2){
						result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
					}
				}
			}
		}
		/**
		 * 如果为找打牌打，且主牌以下，所有牌已经打完的话，红桃主牌也出
		 */
		else if(!haveFound){
			int pokerNum = numList[this.currentGrade];
			if(pokerNum>0 && pokerNum<4){
				haveFound = true;
				result = selectPokers(numList[this.currentGrade]-pokerList0[2][this.currentGrade],-1,this.currentGrade,result);
				result = selectPokers(pokerList0[2][this.currentGrade],PokerPattern.HEART,this.currentGrade,result);
				if (pokerNum == 3){
				    if (numList[0] == 2 && numList[14] != 2){
				    	result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
					}
					else if (numList[14] == 2 && numList[0]!= 2){
						result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
					}
				}
			}
		}
		/**
		 * 再不行就出大小王,先判定是否天王炸
		 */
		if(!haveFound && numList[0]==2&&numList[14]==2){
			
		}
		else if (!haveFound && numList[0] != 0){
			haveFound = true;
			result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
		}
		else if (!haveFound && numList[14] != 0){
			haveFound = true;
			result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
		}
		/**
		 * 如果还没有找到，出炸弹
		 */
		if(!haveFound){
			result = payFire(chooseStrategy.getStraightFlush());
			if(result.size() != 0){
				haveFound = true;
			}
			if(pokerType(result)[0] == PokerType.STRAIGHT_FLUSH){	
			}
			else if(pokerType(result)[0] != PokerType.INVALID_TYPE){
				int k = pokerList.size() - numList[0]- numList[14];
				if(k==2&&pokerList0[PokerPattern.HEART-1][this.currentGrade]>0){
				    result = selectPokers(pokerList0[PokerPattern.HEART-1][this.currentGrade],PokerPattern.HEART,this.currentGrade,result);
				}
				else if(k==1&&pokerList0[PokerPattern.HEART-1][this.currentGrade]==1){
					result = selectPokers(pokerList0[PokerPattern.HEART-1][this.currentGrade],PokerPattern.HEART,this.currentGrade,result);
				}
			}
		}
		else addList(numList,pokerList0,chooseStrategy.getStraightFlush());
		/**
		 * 没找到，重新对牌没有限制的从小到大清理，这时候应该只剩下为控制剩下的最后一手牌
		 */
		if(!haveFound){
			for(int i = 2; i<14;i++){
				if(numList[i] > 0 && numList[i] < 4  && i != this.currentGrade){
					int pokerNum = numList[i];
					if (pokerNum == 1){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
					/**
					 * 是两张的话，看看是否能组成连对，和3+2
					 */
					else if(pokerNum == 2){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+2)>13)?1:(i+2);
						/**
						 * 是否连队
						 */
						if(i<13 && numList[i+1] == 2 && numList[k]==2){
							if((i+1)!= this.currentGrade && k !=this.currentGrade){
								result = selectPokers(numList[i+1],-1,i+1,result);
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * 能不能组成3+2
						 */
						else{
							/**
							 * 在一定范围内寻找三张
							 */
							for(int j = 2; j <14 && j<(i+6);j++){
								if (numList[j] == 3 && j!= this.currentGrade){
									result = selectPokers(numList[j],-1,j,result);
									break;
								}
							}
						}
						break;
					}
					/**
					 * 是三张的话，看看能否组成其他牌
					 */
					else if(pokerNum == 3){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+1)>13)?1:(i+1);
						/**
						 * 能否组成钢板
						 */
						if(numList[k]==3){
							if(k !=this.currentGrade){							
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * 能否组成3+2
						 */
						else{
							/**
							 * 在一定范围内寻找对子
							 */
							for(int j = 2; j <14 && j<(i+5);j++){
								if (numList[j] == 2 && j!= this.currentGrade){
									result = selectPokers(numList[j],-1,j,result);
									break;
								}
							}
						}
						break;
					}
				}
			}
		}
		
		/**
		 * 检查一下牌是否打完
		 */
		if(pokerList.size()==0){
			setFinished(true);
		}
		if(result == null || result.size() == 0){
			String s = "";
			for(int i = 0;i<pokerList.size();i++){
				s+=pokerList.get(i).pattern;
				s+=" ";
				s+=pokerList.get(i).points;
				s+="  "; 
			}
			Log.i("wrong", s);
		}
		return result;
	}
	/**
	 * 压牌
	 */
	protected ArrayList<Poker> payOthers(ArrayList<Poker> currentPoker,int currentWinner,ArrayList<Poker> result){
		boolean haveFound = false;
		AI_Strategy chooseStrategy = null;
		if((myTurn + currentWinner)%2 !=0){
			result = payEnemyBest(currentPoker,currentWinner,result);
			if(result == null||result.size() == 0){
				result = payEnemyMust(currentPoker,currentWinner,result);
			}
			if(pokerList.size()==0){
				setFinished(true);
			}
			return result;
		}
		/**
		 * 压对家牌
		 */
		else{
			result = payFriend(currentPoker,currentWinner,result);
			if(pokerList.size()==0){
				setFinished(true);
			}
			return result;
		}
           
	}
	protected ArrayList<Poker> payFriend(ArrayList<Poker> currentPoker,
			int currentWinner, ArrayList<Poker> result) {
		boolean haveFound = false;
		AI_Strategy chooseStrategy = null;
		/**
		 * 选定一个策略
		 */
		for(int i = 0;i<availStrategy.size();i++){
			if(availStrategy.get(i).getHands() == this.leftHands){
				chooseStrategy = availStrategy.get(i);
				changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
				changeList(numList,pokerList0,chooseStrategy.getStraight());
				changeList(numList,pokerList0,chooseStrategy.getTripleDouble());
				changeList(numList,pokerList0,chooseStrategy.getDoubleTriple());
				break;
			}
		}
		int pokerType[] = pokerType(currentPoker);
		Poker pokerNow;
		/**
		 * 建一个符合当前牌型大小的Poker，用以比较
		 */
		if(pokerType[1] == 0){
			pokerNow = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
		}
		else if(pokerType[1] == 14){
			pokerNow = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
		}
		else{
			pokerNow = new Poker(1,pokerType[1]);
		}
		/**
		 * 分牌型进行出牌规则制定
		 */
		switch(pokerType[0]){
		case PokerType.SINGLE:
			/**
			 * 如果压了之后正好打完则压
			 */
			if(pokerList.size()==1 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.SINGLE){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 自己手数小于3，且对家手牌大于3且不等于5，才根据自己有利原则无限制用单张压单张
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * 对2~k寻找正好为一张的
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
			    /**
			     * 没找到，寻找A为单张，A不为主牌
			     */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
				/**
				 * 没找到，寻找主牌为单张
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					int j = numList[this.currentGrade]-pokerList0[2][this.currentGrade];
					result = selectPokers(j,-1,this.currentGrade,result);
				}
				/**
				 * 没找到，寻找小王
				 */
				if(!haveFound && numList[0] != 0 && comparePoker(new Poker(0,0),pokerNow)){
					haveFound = true;
					result = selectPokers(1,PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
				}
				/**
				 * 没找到，寻找大王
				 */
				if(!haveFound && numList[14] != 0 && comparePoker(new Poker(0,1),pokerNow)){
					haveFound = true;
					result = selectPokers(1,PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
				}
				
			}
			/**
			 * 否则压牌大小不超过A
			 */
			else{
				/**
				 * 对2~k寻找正好为一张的
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
			    /**
			     * 没找到，寻找A为单张，A不为主牌
			     */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
			}
			
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		case PokerType.DOUBLE:
			if(pokerList.size()==2 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.DOUBLE){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 自己手数小于3，且对家手牌大于3且不等于5，才无限制压，否则只压小对子
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * 2~k，寻找对子
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 2 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
				/**
				 * 没找到，寻找对子A
				 */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 2 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,1,result);
				}
				/**
				 * 没找到，寻找主牌对子，不加逢人配
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					result = selectPokers(2,-1,this.currentGrade,result);
				}
				/**
				 * 没找到，找对小王
				 */
				if(!haveFound && numList[0] == 2 && comparePoker(new Poker(0,0),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
				}
				/**
				 * 没找到，找大王
				 */
				if(!haveFound && numList[14] == 2 && comparePoker(new Poker(0,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
				}
			}
			else{
				/**
				 * 2~10，寻找对子
				 */
				for(int i = 2;i<11;i++){
					if(i!= this.currentGrade && numList[i] == 2 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
			}
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		case PokerType.TRIPLE:
			if(pokerList.size()==3 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.TRIPLE){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 自己手数小于3，且对家手牌大于3且不等于5，才压
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * 从2~K找匹配的三张
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
				/**
				 * 没找到，找三张A
				 */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
				/**
				 * 没找到，找三张主牌
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					result = selectPokers(3,-1,this.currentGrade,result);
				}
			}

			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		case PokerType.TRIPLE_DOUBLE_STRAIGHT:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			if(pokerList.size()==6 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.TRIPLE_DOUBLE_STRAIGHT){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 如果自己手数小于3，且对家手牌大于3且不等于5才压
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] >3 
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				for(int i = 0;i<chooseStrategy.getTripleDouble().size();i++){
					Poker[] pokers = chooseStrategy.getTripleDouble().get(0);
					ArrayList<Poker> poker = new ArrayList<Poker>();
					poker.add(pokers[0]);
					poker.add(pokers[1]);
					poker.add(pokers[2]);
					poker.add(pokers[3]);
					poker.add(pokers[4]);
					poker.add(pokers[5]);
					if(pokerType(poker)[1]>pokerNow.points){
						haveFound=true;
						
						result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
						result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
						result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
						result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
						result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
						result= selectPokers(1,pokers[5].pattern,pokers[5].points,result);
						break;
					}
				}
			}
			break;
		case PokerType.DOUBLE_TRIPLE_STRAIGHT:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			if(pokerList.size()==6 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.DOUBLE_TRIPLE_STRAIGHT){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 如果自己手数小于3，且对家手牌大于3且不等于5才压
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] >3 
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				for(int i = 0;i<chooseStrategy.getDoubleTriple().size();i++){
					Poker[] pokers = chooseStrategy.getDoubleTriple().get(0);
					ArrayList<Poker> poker = new ArrayList<Poker>();
					poker.add(pokers[0]);
					poker.add(pokers[1]);
					poker.add(pokers[2]);
					poker.add(pokers[3]);
					poker.add(pokers[4]);
					poker.add(pokers[5]);
					if(pokerType(poker)[1]>pokerNow.points){
						haveFound=true;
						
						result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
						result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
						result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
						result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
						result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
						result= selectPokers(1,pokers[5].pattern,pokers[5].points,result);
						break;
					}
				}
			}
			break;
		case PokerType.TRIPLE_WITH_DOUBLE:
			if(pokerList.size()==6 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.TRIPLE_DOUBLE_STRAIGHT){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 自己手数小于3，且对家手牌大于3且不等于5，才没有限制的压，否则只能用小的3+2
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * 先查询是否有对子
				 */
				boolean haveCouple = false;
				for(int i = 1;i<14;i++){
					if(numList[i] == 2 && i!= this.currentGrade){
						haveCouple = true;
						break;
					}
				}
				/**
				 * 有对子
				 */
				if(haveCouple){
					/**
					 * 从小到大找三张
					 */
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 *  没找到，找AAA
					 */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
					/**
					 * 没找到，找主牌
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						result = selectPokers(3,-1,this.currentGrade,result);
					}
					/**
					 * 能够找到3张的话，加入对子
					 */
					if(haveFound){
						boolean findCouple = false;
						/**
						 * 从小到大找对子
						 */
						for(int i = 2;i<14;i++){
							if(i!= this.currentGrade && numList[i] == 2){
								findCouple = true;
								result = selectPokers(numList[i],-1,i,result);
								break;
							}
						}
						/**
						 * 没找到小对子，找 AA
						 */
						if(!findCouple && 1!= this.currentGrade && numList[1] == 2){
							findCouple = true;
							result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
						}
					}
				}
			}
			else{
				/**
				 * 先查询是否有对子
				 */
				boolean haveCouple = false;
				for(int i = 1;i<11;i++){
					if(numList[i] == 2 && i!= this.currentGrade){
						haveCouple = true;
						break;
					}
				}
				/**
				 * 有对子
				 */
				if(haveCouple){
					/**
					 * 从小到大找三张
					 */
					for(int i = 2;i<11;i++){
						if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 * 能够找到3张的话，加入对子
					 */
					if(haveFound){
						/**
						 * 从小到大找对子
						 */
						for(int i = 2;i<11;i++){
							if(i!= this.currentGrade && numList[i] == 2){
								result = selectPokers(numList[i],-1,i,result);
								break;
							}
						}
					}
				}
			}
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		case PokerType.STRAIGHT:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			if(pokerList.size()==5 && pokerType(new ArrayList<Poker>(pokerList))[0]== PokerType.STRAIGHT){
				int num =  pokerType(new ArrayList<Poker>(pokerList))[1];
				Poker poker;
				if(num == 0){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(num == 14){
					poker = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					poker = new Poker(1,num);
				}
				if(comparePoker(poker,pokerNow)){
					haveFound =true;
					int length = pokerList.size();
					for(int i = 0;i<length;i++){
						result = selectPokers(1,pokerList.get(0).pattern,pokerList.get(0).points,result);
					}
				}
			}
			/**
			 * 对家手牌数量小于4，不压
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * 如果自己手数小于3，且对家手牌大于3且不等于5才压
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] >3 
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				for(int i = 0;i<chooseStrategy.getStraight().size();i++){
					Poker[] pokers = chooseStrategy.getStraight().get(0);
					ArrayList<Poker> poker = new ArrayList<Poker>();
					poker.add(pokers[0]);
					poker.add(pokers[1]);
					poker.add(pokers[2]);
					poker.add(pokers[3]);
					poker.add(pokers[4]);
					if(pokerType(poker)[1]>pokerNow.points){
						haveFound=true;
						
						result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
						result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
						result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
						result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
						result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
						break;
					}
				}
			}
			break;
		case PokerType.STRAIGHT_FLUSH:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		case PokerType.BOMB:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		case PokerType.FOUR_JOKER:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			addList(numList,pokerList0,chooseStrategy.getTripleDouble());
			addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
			break;
		}
		return result;
	}
	protected ArrayList<Poker> payEnemyBest(ArrayList<Poker> currentPoker,
			int currentWinner, ArrayList<Poker> result) {
		boolean haveFound = false;
		AI_Strategy chooseStrategy = null;
		double currentHands = this.leftHands;
		/**
		 * 按策略手数，从少到多，进行最好的牌型匹配
		 */
		Outer:
		for(int circle = 0;circle<3;circle++){
			for(int numS = 0;numS<availStrategy.size();numS++){
				if(availStrategy.get(numS).getHands() == currentHands){
					chooseStrategy = availStrategy.get(numS);
					changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
					changeList(numList,pokerList0,chooseStrategy.getStraight());
					changeList(numList,pokerList0,chooseStrategy.getTripleDouble());
					changeList(numList,pokerList0,chooseStrategy.getDoubleTriple());
				}
				else{
					continue;
				}
				int pokerType[] = pokerType(currentPoker);
				Poker pokerNow;
				/**
				 * 建一个符合当前牌型大小的Poker，用以比较
				 */
				if(pokerType[1] == 0){
					pokerNow = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
				}
				else if(pokerType[1] == 14){
					pokerNow = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
				}
				else{
					pokerNow = new Poker(1,pokerType[1]);
				}
				/**
				 * 分牌型进行出牌规则制定
				 */
				switch(pokerType[0]){
				case PokerType.SINGLE:
					/**
					 * 对2~k寻找正好为一张的
					 */
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
				    /**
				     * 没找到，寻找A为单张，A不为主牌
				     */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
					/**
					 * 没找到，寻找主牌为单张
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						int j = numList[this.currentGrade]-pokerList0[2][this.currentGrade];
						result = selectPokers(j,-1,this.currentGrade,result);
					}
					/**
					 * 没找到，寻找小王
					 */
					if(!haveFound && numList[0] != 0 && comparePoker(new Poker(0,0),pokerNow)){
						haveFound = true;
						result = selectPokers(1,PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
					}
					/**
					 * 没找到，寻找大王
					 */
					if(!haveFound && numList[14] != 0 && comparePoker(new Poker(0,1),pokerNow)){
						haveFound = true;
						result = selectPokers(1,PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
					}
					
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				case PokerType.DOUBLE:
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 2 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 * 没找到，寻找对子A
					 */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 2 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,1,result);
					}
					/**
					 * 没找到，寻找主牌对子，不加逢人配
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						result = selectPokers(2,-1,this.currentGrade,result);
					}
					/**
					 * 没找到，找对小王
					 */
					if(!haveFound && numList[0] == 2 && comparePoker(new Poker(0,0),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
					}
					/**
					 * 没找到，找大王
					 */
					if(!haveFound && numList[14] == 2 && comparePoker(new Poker(0,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
					}
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				case PokerType.TRIPLE:
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 * 没找到，找三张A
					 */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
					/**
					 * 没找到，找三张主牌
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						result = selectPokers(3,-1,this.currentGrade,result);
					}
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				case PokerType.TRIPLE_DOUBLE_STRAIGHT:
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					for(int i = 0;i<chooseStrategy.getTripleDouble().size();i++){
						Poker[] pokers = chooseStrategy.getTripleDouble().get(0);
						ArrayList<Poker> poker = new ArrayList<Poker>();
						poker.add(pokers[0]);
						poker.add(pokers[1]);
						poker.add(pokers[2]);
						poker.add(pokers[3]);
						poker.add(pokers[4]);
						poker.add(pokers[5]);
					    if(pokerType(poker)[1]>pokerNow.points){
							haveFound=true;
							
							result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
							result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
							result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
							result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
							result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
							result= selectPokers(1,pokers[5].pattern,pokers[5].points,result);
							break;
						}
					}
					break;
				case PokerType.DOUBLE_TRIPLE_STRAIGHT:
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					for(int i = 0;i<chooseStrategy.getDoubleTriple().size();i++){
						Poker[] pokers = chooseStrategy.getDoubleTriple().get(0);
						ArrayList<Poker> poker = new ArrayList<Poker>();
						poker.add(pokers[0]);
						poker.add(pokers[1]);
						poker.add(pokers[2]);
						poker.add(pokers[3]);
						poker.add(pokers[4]);
						poker.add(pokers[5]);
					    if(pokerType(poker)[1]>pokerNow.points){
							haveFound=true;
							
							result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
							result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
							result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
							result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
							result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
							result= selectPokers(1,pokers[5].pattern,pokers[5].points,result);
							break;
						}
					}
					break;
				case PokerType.TRIPLE_WITH_DOUBLE:
					/**
					 * 先查询是否有对子
					 */
					boolean haveCouple = false;
					for(int i = 1;i<14;i++){
						if(numList[i] == 2 && i!= this.currentGrade){
							haveCouple = true;
							break;
						}
					}
					/**
					 * 有对子
					 */
					if(haveCouple){
						/**
						 * 从小到大找三张
						 */
						for(int i = 2;i<14;i++){
							if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[i],-1,i,result);
								break;
							}
						}
						/**
						 *  没找到，找AAA
						 */
						if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
						}
						/**
						 * 没找到，找主牌
						 */
						if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
							haveFound = true;
							result = selectPokers(3,-1,this.currentGrade,result);
						}
						/**
						 * 能够找到3张的话，加入对子
						 */
						if(haveFound){
							boolean findCouple = false;
							/**
							 * 从小到大找对子
							 */
							for(int i = 2;i<14;i++){
								if(i!= this.currentGrade && numList[i] == 2){
									findCouple = true;
									result = selectPokers(numList[i],-1,i,result);
									break;
								}
							}
							/**
							 * 没找到小对子，找 AA
							 */
							if(!findCouple && 1!= this.currentGrade && numList[1] == 2){
								findCouple = true;
								result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
							}
						}
					}
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				case PokerType.STRAIGHT:
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					for(int i = 0;i<chooseStrategy.getStraight().size();i++){
						Poker[] pokers = chooseStrategy.getStraight().get(0);
						ArrayList<Poker> poker = new ArrayList<Poker>();
						poker.add(pokers[0]);
						poker.add(pokers[1]);
						poker.add(pokers[2]);
						poker.add(pokers[3]);
						poker.add(pokers[4]);
					    if(pokerType(poker)[1]>pokerNow.points){
							haveFound=true;
							
							result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
							result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
							result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
							result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
							result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
							break;
						}
					}
					break;
				case PokerType.STRAIGHT_FLUSH:
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				case PokerType.BOMB:
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				case PokerType.FOUR_JOKER:
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					addList(numList,pokerList0,chooseStrategy.getTripleDouble());
					addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
					break;
				}
				if(haveFound){
					break Outer;
				}
		    }
			if(haveFound){
				break Outer;
			}
			else{
				currentHands+=0.5;
			}
		}
		
		return result;
	}
	protected ArrayList<Poker> payEnemyMust(ArrayList<Poker> currentPoker,
			int currentWinner, ArrayList<Poker> result) {
		boolean haveFound = false;
		AI_Strategy chooseStrategy = null;
		boolean tagSF = false;
		/**
		 * 选定一个策略
		 */
		for(int i = 0;i<availStrategy.size();i++){
			if(availStrategy.get(i).getHands() == this.leftHands){
				chooseStrategy = availStrategy.get(i);
				changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
				changeList(numList,pokerList0,chooseStrategy.getStraight());
				break;
			}
		}
		int pokerType[] = pokerType(currentPoker);
		Poker pokerNow;
		/**
		 * 建一个符合当前牌型大小的Poker，用以比较
		 */
		if(pokerType[1] == 0){
			pokerNow = new Poker(PokerPattern.JOKER,PokerPoints.LITTLE_JOKER); 
		}
		else if(pokerType[1] == 14){
			pokerNow = new Poker(PokerPattern.JOKER,PokerPoints.OLD_JOKER);
		}
		else{
			pokerNow = new Poker(1,pokerType[1]);
		}
		/**
		 * 分牌型进行出牌规则制定
		 */
		switch(pokerType[0]){
		case PokerType.SINGLE:
			if(pokerList.size() == 2 && pokerType(new ArrayList(pokerList))[0]==PokerType.DOUBLE 
					&& gameView.getPlayerPokerNum()[(this.myTurn+2)%4]>7){
				haveFound = true;
			}
			/**
			 * 对2~k寻找正好为一张的
			 */
			if(haveFound){
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
			}
		    /**
		     * 没找到，寻找A为单张，A不为主牌
		     */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			}
			/**
			 * 没找到，寻找主牌为单张
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				int j = numList[this.currentGrade]-pokerList0[2][this.currentGrade];
				result = selectPokers(j,-1,this.currentGrade,result);
			}
			/**
			 * 没找到，寻找小王
			 */
			if(!haveFound && numList[0] != 0 && comparePoker(new Poker(0,0),pokerNow)){
				haveFound = true;
				result = selectPokers(1,PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
			}
			/**
			 * 没找到，寻找大王
			 */
			if(!haveFound && numList[14] != 0 && comparePoker(new Poker(0,1),pokerNow)){
				haveFound = true;
				result = selectPokers(1,PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
			}
			/**
			 * 没找到，开始拆主牌
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) != 0 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(1,-1,this.currentGrade,result);
			}
			/**
			 * 没找到，拆A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] > 0 && numList[1] < 4 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(1,-1,PokerPoints.ACE,result);
			}
			if(gameView.getPlayerPokerNum()[currentWinner]<11){
				/**
				 * 没找到，从大往小拆
				 */
				if(!haveFound){
					for(int i = 13;i>1;i--){
						if(comparePoker(new Poker(1,i),pokerNow)){
							if(i!=this.currentGrade && numList[i]>0 && numList[i]<4){
								haveFound = true;
								result = selectPokers(1,-1,i,result);
								break;
							}
						}
						else break;
					}
				}
			}
			/**
			 * 没找到，出炸
			 */
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					tagSF = true;
					if(result.size() != 0){
						haveFound = true;
					}
				}
				/**
				 * 没炸，就打逢人配
				 */
				if(!haveFound){
					if(comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						if(pokerList0[PokerPattern.HEART-1][this.currentGrade] == 2){
							int count =0;
							for(int i = 1;i<14;i++){
								if(numList[i]>1){
									count++;
								}
							}
							if(count==0){
								haveFound = true;
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
						}
						
						else if(pokerList0[PokerPattern.HEART-1][this.currentGrade] == 1){
							int count =0;
							for(int i = 1;i<14;i++){
								if(numList[i]>2){
									count++;
								}
							}
							if(count==0){
								haveFound = true;
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
						}
					}
				}
			}
			if(!tagSF){
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.DOUBLE:
			/**
			 * 2~k，寻找对子
			 */
			for(int i = 2;i<14;i++){
				if(i!= this.currentGrade && numList[i] == 2 && comparePoker(new Poker(1,i),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
			/**
			 * 没找到，寻找对子A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 2 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[1],-1,1,result);
			}
			/**
			 * 没找到，寻找主牌对子，不加逢人配
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(2,-1,this.currentGrade,result);
			}
			/**
			 * 没找到，找对小王
			 */
			if(!haveFound && numList[0] == 2 && comparePoker(new Poker(0,0),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
			}
			/**
			 * 没找到，找大王
			 */
			if(!haveFound && numList[14] == 2 && comparePoker(new Poker(0,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
			}
			/**
			 * 没找到，拆主牌
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) > 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(2,-1,this.currentGrade,result);
			}
			/**
			 * 没找到，拆A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] > 1 && numList[1] < 4 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(2,-1,PokerPoints.ACE,result);
			}
			if(gameView.getPlayerPokerNum()[currentWinner]<15){
				/**
				 * 没找到，从大往小拆
				 */
				if(!haveFound){
					for(int i = 13;i>1;i--){
						if(comparePoker(new Poker(1,i),pokerNow)){
							if(i!=this.currentGrade && numList[i]>1 && numList[i]<4){
								haveFound = true;
								result = selectPokers(2,-1,i,result);
								break;
							}
						}
						else break;
					}
				}
			}
			/**
			 * 没找到，出炸
			 */
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					if(result.size()!=0){
						haveFound = true;
					}
					tagSF = true;
				}
				
				if(!haveFound){
					/**
					 * 无炸出，配牌
					 */
					if(result.size() ==0 && pokerList0[2][this.currentGrade]>0){
						int count = 0;
						for(int i = 1;i<14;i++){
							if(i!=this.currentGrade && numList[i] == 3){
								count++;
							}
						}
						if(count!=0){
							/**
							 * 配A,A不是主
							 */
							if(numList[1] == 1&&1!=this.currentGrade && comparePoker(new Poker(1,1),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
							/**
							 * 从大到小配
							 */
							else{
								for(int i = 13;i>1;i--){
									if(comparePoker(new Poker(1,i),pokerNow)){
										if(i!=this.currentGrade && numList[i]==1){
											haveFound = true;
											result = selectPokers(numList[i],-1,i,result);
											result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
											break;
										}
									}
									else break;
								}
							}
							/**
							 * 还没找到，看一下主牌是否有对子
							 */
							if(!haveFound && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
								int k = numList[this.currentGrade] - pokerList0[2][this.currentGrade];
								if(k == 1){
									result = selectPokers(1,-1,this.currentGrade,result);
									result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
								}
								else if(pokerList0[2][this.currentGrade]==2){
									result = selectPokers(2,PokerPattern.HEART,this.currentGrade,result);
								}
							}
						}
					}
				}
			}
			if(!tagSF){
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.TRIPLE:
			/**
			 * 从2~K找匹配的三张
			 */
			for(int i = 2;i<14;i++){
				if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
			/**
			 * 没找到，找三张A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			}
			/**
			 * 没找到，找三张主牌
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(3,-1,this.currentGrade,result);
			}
			/**
			 * 没找到，出火
			 */
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					if(result.size()!=0){
						haveFound = true;
					}
					tagSF = true;
				}
				if(!haveFound){
					/**
					 * 没有火，开始配牌
					 */
					if( result.size()==0 && pokerList0[2][this.currentGrade]>0){
						int count = 0;
						for(int i = 1;i<14;i++){
							if(i!=this.currentGrade && numList[i] ==3){
								count++;
							}
						}
						if(count!=0){
							if(numList[1] == 2 && 1!=this.currentGrade && comparePoker(new Poker(1,1),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[1],-1,1,result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
							/**
							 * 从大到小组牌
							 */
							else{
								for(int i = 13;i>1;i--){
									if(comparePoker(new Poker(1,i),pokerNow)){
										if(i!=this.currentGrade && numList[i]==2){
											haveFound = true;
											result = selectPokers(numList[i],-1,i,result);
											result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
											break;
										}
									}
									else break;
								}
							}
						}
					}
				}
			}
			if(!tagSF){
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.TRIPLE_DOUBLE_STRAIGHT:
			/**
			 * 寻找连对
			 */
			for(int i = pokerType[1]+1;i<13;i++){
				int k = ((i+2)>13)?1:(i+2);
				if(numList[i]== 2&& numList[i+1] == 2 && numList[k]==2){
					if(i!= this.currentGrade && (i+1)!= this.currentGrade && k !=this.currentGrade){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						result = selectPokers(numList[i+1],-1,i+1,result);
						result = selectPokers(numList[k],-1,k,result);
						break;
					}
				}
			}
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					if(result.size()!=0){
						haveFound = true;
					}
					tagSF = true;
				}
			}
			if(!tagSF){
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.DOUBLE_TRIPLE_STRAIGHT:
			for(int i = pokerType[1]+1;i<14;i++){
				int k = ((i+1)>13)?1:(i+1);
				if(numList[i]== 3&& numList[k] == 3){
					if(i!= this.currentGrade && k !=this.currentGrade){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						result = selectPokers(numList[k],-1,k,result);
						break;
					}
				}
			}
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					if(result.size()!=0){
						haveFound = true;
					}
					tagSF = true;
				}
			}
			if(!tagSF){
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.TRIPLE_WITH_DOUBLE:
			/**
			 * 先查询是否有对子
			 */
			boolean haveCouple = false;
			for(int i = 1;i<14;i++){
				if(numList[i] == 2 && i!= this.currentGrade){
					haveCouple = true;
					break;
				}
			}
			/**
			 * 有对子
			 */
			if(haveCouple){
				/**
				 * 从小到大找三张
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
				/**
				 *  没找到，找AAA
				 */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
				/**
				 * 没找到，找主牌
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					result = selectPokers(3,-1,this.currentGrade,result);
				}
				/**
				 * 能够找到3张的话，加入对子
				 */
				if(haveFound){
					boolean findCouple = false;
					/**
					 * 从小到大找对子
					 */
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 2){
							findCouple = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 * 没找到小对子，找 AA
					 */
					if(!findCouple && 1!= this.currentGrade && numList[1] == 2){
						findCouple = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
				}
			}
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					if(result.size()!=0){
						haveFound = true;
					}
					tagSF = true;
				}
			}
			if(!tagSF){
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.STRAIGHT:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			for(int i = 0;i<chooseStrategy.getStraight().size();i++){
				Poker[] pokers = chooseStrategy.getStraight().get(0);
				ArrayList<Poker> poker = new ArrayList<Poker>();
				poker.add(pokers[0]);
				poker.add(pokers[1]);
				poker.add(pokers[2]);
				poker.add(pokers[3]);
				poker.add(pokers[4]);
			    if(pokerType(poker)[1]>pokerNow.points){
					haveFound=true;
					
					result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
					result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
					result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
					result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
					result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
					break;
				}
			}
			if(!haveFound){
				if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
						||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
					changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
					changeList(numList,pokerList0,chooseStrategy.getStraight());
					result = payFire(chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
					if(result.size()!=0){
						haveFound = true;
					}
					tagSF = true;
				}						
			}
			break;
		case PokerType.STRAIGHT_FLUSH:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
					||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
					||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
					||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
				for(int i = 0;i<chooseStrategy.getStraightFlush().size();i++){
					Poker[] pokers = chooseStrategy.getStraightFlush().get(0);
					ArrayList<Poker> poker = new ArrayList<Poker>();
					poker.add(pokers[0]);
					poker.add(pokers[1]);
					poker.add(pokers[2]);
					poker.add(pokers[3]);
					poker.add(pokers[4]);
				    if(pokerType(poker)[1]>pokerNow.points){
						haveFound=true;
						
						result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
						result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
						result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
						result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
						result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
						break;
					}
				}
				/**
				 * 分为有无逢人配讨论
				 */
				if(!haveFound){
					changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
					changeList(numList,pokerList0,chooseStrategy.getStraight());
					if(pokerList0[2][this.currentGrade] ==0){
						for(int j = 1;j<14;j++){
							/**
							 * 寻找最小的
							 */
							if(numList[j] >5){
								haveFound = true;
								result = selectPokers(numList[j],-1,j,result);
								break;
							}
						}
					}
					else {
						
						/**
						 * 找炸弹
						 */
						for(int j = 1;j<14;j++){
							/**
							 * 寻找最小的
							 */
							if(numList[j] >5){
								haveFound = true;
								result = selectPokers(numList[j],-1,j,result);
								break;
							}
						}
						/**
						 * 没找到加入逢人配，先加一张
						 */
						if(!haveFound){
							for(int j = 1;j<14;j++){
								if(numList[j] >4 && j!= this.currentGrade){
									haveFound = true;
									result = selectPokers(numList[j],-1,j,result);
									result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
									break;
								}
							}
						}
						/**
						 * 找不到，如果有两张逢人配，加入两张逢人配
						 */
						if(!haveFound && pokerList0[2][this.currentGrade] ==2){
							for(int j = 1;j<14;j++){
								if(numList[j] >3 && j!=this.currentGrade){
									haveFound = true;
									result = selectPokers(numList[j],-1,j,result);
									result = selectPokers(2,PokerPattern.HEART,this.currentGrade,result);
									break;
								}
							}
						}
						/**
						 * 还没找到，看看主牌是否就是符合的炸弹
						 */
						if(!haveFound){
							if(numList[this.currentGrade]>5){
								haveFound = true;
								int count = 0;
								ListIterator<Poker> listIterator;
								while((numList[this.currentGrade]--)>0){
									listIterator = this.pokerList.listIterator(count);
									while(listIterator.hasNext()){
										Poker poker = (Poker)listIterator.next();
										if(poker.pattern != 0&& poker.points == this.currentGrade){
											break;
										}
										count++;
									}
									Poker poker = (Poker)this.pokerList.get(count);
									pokerList0[poker.pattern-1][poker.points]--;
									this.pokerList.remove(count);
									result.add(poker);
								}
							}
						}
					}
					addList(numList,pokerList0,chooseStrategy.getStraightFlush());
					addList(numList,pokerList0,chooseStrategy.getStraight());
				}
				/**
				 * 还没找到，看是否有天王炸
				 */
				if(!haveFound && numList[0] == 2&& numList[14] ==2){
			    	haveFound = true;
			    	result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
					result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
			    }
			}			
			break;
		case PokerType.BOMB:
			if((calculateFire(chooseStrategy.getStraightFlush().size())>3)
					||(calculateFire(chooseStrategy.getStraightFlush().size())>2 && gameView.getPlayerPokerNum()[currentWinner]<22)
					||(calculateFire(chooseStrategy.getStraightFlush().size())>1 && gameView.getPlayerPokerNum()[currentWinner]<16)
					||(calculateFire(chooseStrategy.getStraightFlush().size())>0 && gameView.getPlayerPokerNum()[currentWinner]<10)){
				if(pokerList0[2][this.currentGrade] ==0){
					/**
					 * 先寻找最小的能压住炸弹
					 */
					for(int j = 1;j<14;j++){
						if(numList[j] == currentPoker.size() && j!= this.currentGrade&& comparePoker(new Poker(1,j),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[j],-1,j,result);
							break;
						}
					}
					/**
					 * 张数多一张的炸弹
					 */
					if(!haveFound){
						outer:
							for(int i = currentPoker.size()+1;i<9;i++){
								for(int j = 1;j<14;j++){
									if(numList[j] == i && j!= this.currentGrade){
										haveFound = true;
										result = selectPokers(numList[j],-1,j,result);
										break outer;
									}
								}
							}
					}
				}
				else {
					/**
					 * 先寻找最小的能压住炸弹
					 */
					for(int j = 1;j<14;j++){
						if(numList[j] == currentPoker.size() && j!= this.currentGrade&& comparePoker(new Poker(1,j),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[j],-1,j,result);
							break;
						}
					}
					/**
					 * 张数多一张的炸弹
					 */
					if(!haveFound){
						outer:
							for(int i = currentPoker.size()+1;i<9;i++){
								for(int j = 1;j<14;j++){
									if(numList[j] == i && j!= this.currentGrade){
										haveFound = true;
										result = selectPokers(numList[j],-1,j,result);
										break outer;
									}
								}
							}
					}
					/**
					 * 加入一张逢人配，张数一样多，牌点大的
					 */
					if(!haveFound){
						for(int j = 1;j<14;j++){
							if(numList[j] == currentPoker.size()-1 && j!= this.currentGrade&& comparePoker(new Poker(1,j),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[j],-1,j,result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
								break;
							}
						}
					}
					/**
					 * 加入逢人配后多一张的炸弹
					 */
				    if(!haveFound){
				    	outer:
							for(int i = currentPoker.size();i<9;i++){
								for(int j = 1;j<14;j++){
									if(numList[j] == i && j!= this.currentGrade){
										haveFound = true;
										result = selectPokers(numList[j],-1,j,result);
										result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
										break outer;
									}
								}
							}
				    }

					if(!haveFound && pokerList0[2][this.currentGrade] ==2){
						for(int j = 1;j<14;j++){
							if(numList[j] == currentPoker.size()-2 && j!=this.currentGrade&& comparePoker(new Poker(1,j),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[j],-1,j,result);
								result = selectPokers(2,PokerPattern.HEART,this.currentGrade,result);
								break;
							}
						}
						if(!haveFound){
							outer:
								for(int i = currentPoker.size()-1;i<9;i++){
									for(int j = 1;j<14;j++){
										if(numList[j] == i && j!=this.currentGrade){
											haveFound = true;
											result = selectPokers(numList[j],-1,j,result);
											result = selectPokers(2,PokerPattern.HEART,this.currentGrade,result);
											break outer;
										}
									}
								}
						}
					}
					/**
					 * 看下主牌本身是否符合
					 */
					if(!haveFound){
						if(numList[this.currentGrade] == currentPoker.size()&& comparePoker(new Poker(1,this.currentGrade),pokerNow)){
							haveFound = true;
					    	result = selectPokers(numList[this.currentGrade]-pokerList0[2][this.currentGrade],-1,this.currentGrade,result);
					    	result = selectPokers(pokerList0[2][this.currentGrade],PokerPattern.HEART,this.currentGrade,result);		
						}
						if(!haveFound&& numList[this.currentGrade]>currentPoker.size()){
							haveFound = true;
							if(pokerList.size() < 10){
								result = selectPokers(numList[this.currentGrade]-pokerList0[2][this.currentGrade],-1,this.currentGrade,result);
								result = selectPokers(pokerList0[2][this.currentGrade],PokerPattern.HEART,this.currentGrade,result);
							}
							else{
								result = selectPokers(numList[this.currentGrade]-pokerList0[2][this.currentGrade],-1,this.currentGrade,result);
								int leftNum=currentPoker.size()+1-result.size();
								while((leftNum--)>0){
									result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
								}
							}
						}
					}
				}
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
				if(!haveFound&&currentPoker.size()<6){
					if(chooseStrategy.getStraightFlush().size()!=0){
						haveFound=true;
						Poker[] pokers = chooseStrategy.getStraightFlush().get(0);
						result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
						result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
						result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
						result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
						result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
					}
				}
				/**
				 * 看下是否有天王炸
				 */
				if(!haveFound && numList[0] == 2&& numList[14] ==2){
			    	haveFound = true;
			    	result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
					result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
			    }
			}
			else{
				addList(numList,pokerList0,chooseStrategy.getStraightFlush());
				addList(numList,pokerList0,chooseStrategy.getStraight());
			}
			break;
		case PokerType.FOUR_JOKER:
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			break;
		}
		return result;
	}
	/**
	 * 出炸弹
	 */
	protected ArrayList<Poker> payFire(ArrayList<Poker[]> sF){
		ArrayList<Poker> result = new ArrayList<Poker>();
		boolean haveFound = false;
		/**
		 * 从4张的炸弹开始循环起，不考虑逢人配
		 */
		outer:
			for(int i = 4; i <6;i++){
				for(int j = 1;j<14;j++){
					if(numList[j] == i&&j!=this.currentGrade){
						haveFound = true;
						result = selectPokers(numList[j],-1,j,result);
						break outer;
					}
				}
			}
		/**
		 * 同花顺
		 */
		if(!haveFound){
			if(sF.size()!=0){
				haveFound = true;
				addList(numList,pokerList0,sF);
				Poker[] pokers = sF.get(0);
				result= selectPokers(1,pokers[0].pattern,pokers[0].points,result);
				result= selectPokers(1,pokers[1].pattern,pokers[1].points,result);
				result= selectPokers(1,pokers[2].pattern,pokers[2].points,result);
				result= selectPokers(1,pokers[3].pattern,pokers[3].points,result);
				result= selectPokers(1,pokers[4].pattern,pokers[4].points,result);
			}
		}
		else{
			addList(numList,pokerList0,sF);
		} 
	/**
	 * 还未找到炸弹，开始考虑逢人配
	 */
	    if(!haveFound && this.pokerList0[2][this.currentGrade]>1){
	    	for(int j = 1;j<14;j++){
	    		/**
	    		 * 3+1成炸
	    		 */
				if(numList[j] == 3&&j!=this.currentGrade){
					haveFound = true;
					result = selectPokers(numList[j],-1,j,result);
					result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
					break;
				}
			}
	    }
	    if(!haveFound){
	    	/**
			 * 从6张的炸弹开始循环起，不考虑逢人配
			 */
			outer:
				for(int i = 6; i <9;i++){
					for(int j = 1;j<14;j++){
						if(numList[j] == i&&j!=this.currentGrade){
							haveFound = true;
							result = selectPokers(numList[j],-1,j,result);
							break outer;
						}
					}
				}
	    }
	    /**
	     * 还没有找到炸弹，2+2成炸
	     */
	    if(!haveFound && this.pokerList0[2][this.currentGrade] == 2){
	    	for(int j = 1;j<14;j++){
				if(numList[j] == 2&&j!=this.currentGrade){
					haveFound = true;
					result = selectPokers(numList[j],-1,j,result);
					
					result = selectPokers(numList[this.currentGrade],PokerPattern.HEART,this.currentGrade,result);
					break;
				}
			}
	    }
	    /**
	     * 还没找到，看看主牌是否就是炸弹
	     */
	    if(!haveFound && numList[this.currentGrade]>3){
	    	haveFound = true;
	    	if(pokerList.size()<10){
				result = selectPokers(numList[this.currentGrade]-pokerList0[2][this.currentGrade],-1,this.currentGrade,result);
				result = selectPokers(pokerList0[2][this.currentGrade],PokerPattern.HEART,this.currentGrade,result);
			}
			else{
				result = selectPokers(numList[this.currentGrade]-pokerList0[2][this.currentGrade],-1,this.currentGrade,result);
				int leftNum= 4 - result.size();
				while((leftNum--)>0){
					result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
				}
			}
	    }
	    
	    /**
	     * 还没找到，看是否有天王炸
	     */
	    if(!haveFound && numList[0] == 2&& numList[14] ==2){
	    	haveFound = true;
	    	result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
	    	result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
	    }
		return result;
	}
	protected int calculateFire(int numOfStraightFlush){
		int result = 0;
		int num = pokerList0[PokerPattern.HEART-1][this.currentGrade];
		for(int i = 4; i <9;i++){
			for(int j = 1;j<14;j++){
				if(numList[j] == i&&j!=this.currentGrade){
					result++;
				}
			}
		}
		if(num!=0){
			for(int j = 1;j<14;j++){
	    		/**
	    		 * 3+1成炸
	    		 */
				if(numList[j] == 3&&j!=this.currentGrade){
					result++;
					num--;
				}
				if(num==0){
					break;
				}
			}
		}
		if(num==2){
			for(int j = 1;j<14;j++){
				if(numList[j] == 2&&j!=this.currentGrade){
					result ++;
					num=0;
					break;
				}
			}
		}
		if(numList[this.currentGrade]-(pokerList0[PokerPattern.HEART - 1][this.currentGrade]-num)>3){
			result++;
		}
		if(numList[0] == 2&& numList[14] ==2){
			result++;
		}
		result+=numOfStraightFlush;
		return result;
	}
	/**
	 * 回贡
	 * @return 回贡的牌
	 */
	public Poker payBack(){
		/**
		 * 初始化，并分析策略
		 */
		availStrategy  = new LinkedList<AI_Strategy>();
		this.leftHands = 100;
		normalAnalyze();
		/**
		 * 选定一个策略
		 */
		AI_Strategy chooseStrategy = null;
		/**
		 * 复制计数数组
		 */
		int[] numListCopy = this.numList.clone();
		int[][] pokerList0Copy = copyArray(this.pokerList0);
		for(int i = 0;i<availStrategy.size();i++){
			if(availStrategy.get(i).getHands() == this.leftHands){
				chooseStrategy = availStrategy.get(i);
				/**
				 * 计数中去除同花顺和杂花顺
				 */
				changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
				changeList(numList,pokerList0,chooseStrategy.getStraight());
				break;
			}
		}
		boolean haveFound = false;
		int currentChoose = -1;
		/**
		 * 分析是还贡给对手还是对家
		 */
		int[] orderRecord = gameView.getOrderRecord();
		if((orderRecord[0]+orderRecord[1])%2 == 0 || ((orderRecord[0]+orderRecord[3])%2 == 1)&&orderRecord[0] == this.myTurn){
			ArrayList<Poker> result = new ArrayList<Poker>();
			/**
			 * 从2找到k找单张
			 */
			for(int j = 2;j < 14;j++){
				if(numList[j] == 1 && this.currentGrade != j){
					if(currentChoose == -1){
						currentChoose = j;
					}
					else{
						if(numListCopy[j]>numList[currentChoose]){
							currentChoose = j;
						}
					}
				}
			}
			if(currentChoose != -1){
				haveFound = true;
				result = selectPokers(1,-1,currentChoose,result);
			}
			/**
			 * 找单张没找到，从2到J找3张的
			 */
			if(!haveFound){
				for(int j = 2;j < 12;j++){
					if(numList[j] == 3 && this.currentGrade != j){
						if(currentChoose == -1){
							currentChoose = j;
						}
						else{
							if(numListCopy[j]>numList[currentChoose]){
								currentChoose = j;
							}
						}
					}
				}
				if(currentChoose != -1){
					haveFound = true;
					result = selectPokers(1,-1,currentChoose,result);
				}
			}
			/**
			 * 3张也没找到，从2~Q找对子
			 */
			if(!haveFound){
				for(int j = 2;j < 13;j++){
					if(numList[j] == 2 && this.currentGrade != j){
						if(currentChoose == -1){
							currentChoose = j;
						}
						else{
							if(numListCopy[j]>numList[currentChoose]){
								currentChoose = j;
							}
						}
					}
				}
				if(currentChoose != -1){
					haveFound = true;
					result = selectPokers(1,-1,currentChoose,result);
				}
			}
			/**
			 * 还没找到，找5张（包括）以上的炸弹
			 */
			if(!haveFound){
				for(int j = 2;j < 14;j++){
					if(numList[j] >4  && this.currentGrade != j){
						currentChoose = j;
						break;
					}
				}
				if(currentChoose != -1){
					haveFound = true;
					result = selectPokers(1,-1,currentChoose,result);
				}
			}
			/**
			 * 补回同花顺杂花顺
			 */
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			/**
			 * 还没找到，返回最小牌
			 */
			if(!haveFound){
				for(int j = 2;j < 14;j++){
					if(numList[j] !=0  && this.currentGrade != j){
					    currentChoose = j;
						break;
					}
				}
				if(currentChoose != -1){
					haveFound = true;
					result = selectPokers(1,-1,currentChoose,result);
				}
			}
			return result.get(0);
		}
		/**
		 * 还贡给对家
		 */
		else{
			ArrayList<Poker> result = new ArrayList<Poker>();
			/**
			 * 从2找到k找单张
			 */
			for(int j = 2;j < 14;j++){
				if(numList[j] == 1){
					if(currentChoose == -1){
						currentChoose = j;
					}
					else{
						if(numListCopy[j]<numList[currentChoose]){
							currentChoose = j;
						}
					}
				}
			}
			if(currentChoose != -1){
				haveFound = true;
				result = selectPokers(1,-1,currentChoose,result);
			}
			/**
			 * 单张也没找到，从1~K找对子
			 */
			if(!haveFound){
				for(int j = 1;j < 14;j++){
					if(numList[j] == 2){
						if(currentChoose == -1){
							currentChoose = j;
						}
						else{
							if(numListCopy[j]<numList[currentChoose]){
								currentChoose = j;
							}
						}
					}
				}
				if(currentChoose != -1){
					haveFound = true;
					result = selectPokers(1,-1,currentChoose,result);
				}
			}
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			/**
			 * 还没找到，返回最小牌
			 */
			if(!haveFound){
				for(int j = 2;j < 14;j++){
					if(numList[j] !=0  && this.currentGrade != j){
					    currentChoose = j;
						break;
					}
				}
				if(currentChoose != -1){
					haveFound = true;
					result = selectPokers(1,-1,currentChoose,result);
				}
			}
			return result.get(0);
		}
	}
	public void someoneLead(ArrayList<Poker> currentPokers,int turn){
    	super.someoneLead(currentPokers, turn);
    	super.setOutNumList(currentPokers);
    }
}
