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
	 * ���е����г��Ʋ���
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
	 * ���ݵ�ǰ���ƣ���ͨAI���������еĲ���
	 */
	protected void normalAnalyze(){
		ArrayList<ArrayList<Poker[]>> straightFlushList = removeStraightFlush(this.numList,this.pokerList0);
		/**
		 * ��ʼ��������ͬ��˳����
		 */
		for(int i = 0;i<straightFlushList.size();i++){
			int[] numList1 = this.numList.clone();
			int[][] pokerList01 = copyArray(this.pokerList0);
			changeList(numList1,pokerList01,straightFlushList.get(i));
			/**
			 * ����ȥ��ͬ����ʣ����ƣ�����ȥ���ӻ�˳�Ĳ���
			 */
			ArrayList<ArrayList<Poker[]>> straightList = removeStraight(numList1,pokerList01);
			/**
			 *��ʼ���������ӻ�˳����
			 */
			for(int j = 0;j<straightList.size();j++){
				int[] numList2 = numList1.clone();
				int[][] pokerList02 = copyArray(pokerList01);
				changeList(numList2,pokerList02,straightList.get(j));
				/**
				 * ����Ȼʣ�µ��Ƽ�������
				 */
				AI_Strategy ai_Strategy = new AI_Strategy(0.0,numList2,pokerList02,straightFlushList.get(i),straightList.get(j),null,null);
				ai_Strategy = calculateHands(ai_Strategy);
				/**
				 * ����ʣ���������²���
				 */
				if(ai_Strategy.getHands()<this.leftHands){
					/**
					 * ��ɾ���ɲ��ԣ�����2�ı�ɾ��
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
		 * �����һ��sizeΪ0�Ķ��󣬷�������������
		 */
		result.add(new ArrayList<Poker[]>());
		/**
		 * ���ݲ�ͬ�Ļ�ɫ��������С����ʼ����
		 */
		for(int i = 0; i < 4;i++){
			/**
			 * ���浱ǰ��ɫ�Ĳ���
			 */
			strategyList0 = new ArrayList<ArrayList<Poker[]>>();
			strategyList0.add(new ArrayList<Poker[]>());
			/**
			 * ����ָ�������Ʋ���ͬ��˳
			 */
			int start = PokerPoints.ACE;
			int end = PokerPoints.ACE;
			/**
			 * begin�������Ʋ�ȱ��ͬ��˳�Ŀ�ʼ
			 * count��������begin��end����ͬ��������
			 */
			int begin = PokerPoints.ACE;
			int count = 0;
			/**
			 * �����Ƿ��Ѿ�����һ��ѭ��
			 */
			boolean haveCircle = false;
			do{
				/**
				 * end��14��ʱ��Ҫ���A����have��ֵΪtrue��ʾ�Ѿ�ѭ��һ��
				 */
				if(end == 14){
					end = PokerPoints.ACE;
					haveCircle = true;
				}
				/**
				 * �鿴��ǰend�Ƿ������ƣ���������������ͬ��˳�Ŀ���
				 */
				if(pokerList0[i][end] == 0){
					start = end+1;
					/**
					 * ����10��ʱ�򲻿�������˳����
					 */
					if (start > PokerPoints.TEN){
						haveCircle = true;
					}
				}
				else{
					/**
					 * ��ǰend���գ���end-start = 4��-9������ͬ��˳
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
						 * ��������������һ���ο����������ͬ��˳
						 */
						if(pokerList0[i][start] == 2 && pokerList0[i][start+1] == 2 &&
								pokerList0[i][start+2] == 2 && pokerList0[i][start+3] == 2&&
								pokerList0[i][end] == 2){
							strategyList0 = addToStraightFlushStrategy(strategyList0,straightFlush.clone(),numList,pokerList0);
						}
						/**
						 * start�ƽ�
						 */
						start++;
					}
				}
				/**
				 * ���Ҳ�ȱ��ͬ��˳
				 */
				if(pokerList0[i][end] > 0){
					count++;					
				}
				if((end - begin == 4) || (end - begin == -9)){
					/**
					 * begin��ָ���Ƶ�������Ϊ0����֤˳�Ӿ����ܴ󣬵���beginΪ10��������⣬��Ϊ˳�Ӳ����ٴ���
					 */
					if(pokerList0[i][begin]>0||begin == PokerPoints.TEN){
						/**
						 * ���Բ��Ƶ����
						 */
						if((count  == 3 && pokerList0[PokerPattern.HEART-1][this.currentGrade] == 2)||
								(count  == 4 && pokerList0[PokerPattern.HEART-1][this.currentGrade] > 0)){
							Poker[] straightFlush = new Poker[5];
							/**
							 * ���ݲ�ȱ״����ȫ˳��
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
							 * ������������count��Ϊ4���Ҹ��ƶ�Ϊ2�ţ��ҷ�����������������������ͬ��˳
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
						 * ��Ϊbegin���ƽ����ҵ�ǰbegin�����Ƶģ�����10,û������ν�ˣ���countҪ��1
						 */
						count--;
					}
					begin++;
				}
				/**
				 * end�ƽ�
				 */
				end++;
			}while(!haveCircle);
			/**
			 * ������ɫ�Ĳ��Լ���result
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
					 * ����ָ���������м���������
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
					 * ����ָ���������м���������
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
						 * ����������ͻ
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
	 * ȥ�������е��ӻ�˳
	 * @return ����ȥ���ӻ��Ĳ���
	 */
	protected ArrayList<ArrayList<Poker[]>> removeStraight(int[] numList,int[][] pokerList0){
		ArrayList<ArrayList<Poker[]>> result = new ArrayList<ArrayList<Poker[]>>();
		/**
		 * �����һ��sizeΪ0�Ķ��󣬷�������������
		 */
		result.add(new ArrayList<Poker[]>());
		/**
		 * ����ָ�������Ʋ���ͬ��˳
		 */
		int start = PokerPoints.ACE;
		int end = PokerPoints.ACE;
		/**
		 * ��start~end�䵥����
		 */
		int count = 0;
		/**
		 * �����Ƿ��Ѿ�����һ��ѭ��
		 */
		boolean haveCircle = false;
		do{
			/**
			 * end��14��ʱ��Ҫ���A����have��ֵΪtrue��ʾ�Ѿ�ѭ��һ��
			 */
			if(end == 14){
				end = PokerPoints.ACE;
				haveCircle = true;
			}
			/**
			 * ���������������ŵ��Ʋ�����˳ ���Ʋ����뵥����
			 */
			if((numList[end] == 0)||(end == this.currentGrade && (numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade] == 0))){
				count = 0;
				start = end+1;
				/**
				 * ����10��ʱ���ٿ�����˳��
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
				 * end-startΪ4��-9�ſ�����˳�� ����һ���������������ŵ��Ʋ���˳
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
					 * start�ƽ�
					 */
					start++;
				}
			}
			/**
			 * end�ƽ�
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
		 * ��ͬ��˳�з����������,��λ��
		 */
		int count = 0;
		int[] position = {-1,-1};
		for(int i = 0 ;i< straightFlush.length;i++){
			if(straightFlush[i].pattern == PokerPattern.HEART && straightFlush[i].points == this.currentGrade){
				/**
				 * �������������������أ�������Ϊ�ڼ������ͬ��˳ʱ��������˺�������
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
			 * �������������������أ�������Ϊ�ڼ������ͬ��˳ʱ��������˺�������
			 */
			return strategyList;
		}
		/**
		 * �����в��Խ��и���
		 */
		for(int i = 0;i < length; i++){
			strategy = strategyList.get(i);
			/**
			 * ���������еķ���������
			 */
			int countAll = 0;
			/**
			 * ���ԭ����Ϊ�գ����²��Լ��뼴��
			 */
			if(strategy.size() == 0){
				ArrayList<Poker[]> newStrategy = new ArrayList<Poker[]>();
				newStrategy.add(straightFlush);
				strategyList.add(newStrategy);
			}
			/**
			 * ԭ���Բ�Ϊ�գ����ҳ�ͻ���ٸ���
			 */
			else{
				/**
				 * ����Ƿ��ͻ
				 */
				/**
				 * ��ʼ��
				 */
				for(int t=0;t<15;t++){
					judgeList[t] = 0;
				}
				for(int k  = 0;k < strategy.size();k++){
					/**
					 * �ж����鸳ֵ
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
				 * ����ͻ�ż������
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
		 * ˳���еķ����������,��λ��
		 */
		int count = 0;
		int[] position = {-1,-1};
		for(int i = 0 ;i< straight.length;i++){
			if(straight[i].pattern == PokerPattern.HEART && straight[i].points == this.currentGrade){
				/**
				 * �������������������أ���ֹ������˺�������
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
			 * �������������������أ�������Ϊ�ڼ������ͬ��˳ʱ��������˺�������
			 */
			return strategyList;
		}
		/**
		 * �����в��Խ��и���
		 */
		for(int i = 0;i < length; i++){
			strategy = strategyList.get(i);
			/**
			 * ���������еķ���������
			 */
			int countAll = 0;
			/**
			 * ���ԭ����Ϊ�գ����²��Լ��뼴��
			 */
			if(strategy.size() == 0){
				ArrayList<Poker[]> newStrategy = new ArrayList<Poker[]>();
				newStrategy.add(straight);
				strategyList.add(newStrategy);
			}
			/**
			 * ԭ���Բ�Ϊ�գ����ҳ�ͻ���ٸ���
			 */
			else{
				/**
				 * �ж������ʼ��
				 */
				for(int j = 0; j < 5;j++){
					judgeList[j]= 0;
				}
				/**
				 * ����Ƿ��ͻ
				 */
				for(int k  = 0;k < strategy.size();k++){
					/**
					 * ���ܳ�ͻ��˳�ӷ�Χ
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
					 * ����������
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
				 * ����ͻ�ż������
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
		 * �����ź����ŵ���Ͻ��е�������
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
	 * ����ѡ���Ĳ��ԣ�����Ӧ����������������
	 * @param numList
	 * @param pokerList0
	 * @param straightFlush
	 */
	protected void changeList(int[] nl,int[][] pL,ArrayList<Poker[]> straightFlush){
		for(int i = 0;i<straightFlush.size();i++){
			for(int j = 0;j<straightFlush.get(i).length;j++){
				nl[straightFlush.get(i)[j].points]--;
				/**
				 * �ӻ�˳���Ƶ�pattern������-1�ǵģ�ͬ��˳����ʲôpattern����ʲôpattern
				 * ����ζ��������ӻ�˳���ԣ����˺������ƣ�pL�ǲ����µ�
				 */
				if(straightFlush.get(i)[j].pattern!=-1){
					pL[straightFlush.get(i)[j].pattern-1][straightFlush.get(i)[j].points]--;
				}
			}
		}
	}
	/**
	 * ����ѡ���Ĳ��ԣ�����Ӧ�������������ӷ�
	 * @param numList
	 * @param pokerList0
	 * @param straightFlush
	 */
	protected void addList(int[] nl,int[][] pL,ArrayList<Poker[]> straightFlush){
		for(int i = 0;i<straightFlush.size();i++){
			for(int j = 0;j<straightFlush.get(i).length;j++){
				nl[straightFlush.get(i)[j].points]++;
				/**
				 * �ӻ�˳���Ƶ�pattern������-1�ǵģ�ͬ��˳����ʲôpattern����ʲôpattern
				 * ����ζ��������ӻ�˳���ԣ����˺������ƣ�pL�ǲ����µ�
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
	 * ����
	 */
	protected ArrayList<Poker> payFirst(ArrayList<Poker> result){
		boolean haveFound = false;
		AI_Strategy chooseStrategy = null;
		/**
		 * �¼����������¼��������¼�����
		 */
		int control1 = gameView.getPlayerPokerNum()[(this.myTurn + 1) % 4];
		int control2 = gameView.getPlayerPokerNum()[(this.myTurn + 2) % 4];
		int control3 = gameView.getPlayerPokerNum()[(this.myTurn + 3) % 4];
		/**
		 * ѡ��һ������
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
		 * �����ջصĵ���
		 */
		if(control1==1||control3==1){}
		else if(!haveFound && (numList[0]>0||numList[14]>0||((outNumList[0]+outNumList[14]>2)&&(numList[this.currentGrade]-pokerList0[PokerPattern.HEART-1][this.currentGrade]>0)))){
			/**
			 * ��2~kѰ������Ϊһ�ŵ�
			 */
			for(int i = 2;i<14;i++){
				if(i!= this.currentGrade && numList[i] == 1){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
		    /**
		     * û�ҵ���Ѱ��AΪ���ţ�A��Ϊ����
		     */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 1){
				haveFound = true;
				result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			}
		}
		
		/**
		 * �����ջص�3�Ż�3+2
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
				 * ���ܲ��ܼ������
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
		 * �����ջصĶ���
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
		 * ���ر��������ԣ��ְ壬�ӻ�˳
		 */
		addList(numList,pokerList0,chooseStrategy.getDoubleTriple());
		addList(numList,pokerList0,chooseStrategy.getTripleDouble());
		addList(numList,pokerList0,chooseStrategy.getStraight());
		/**
		 * ���ӻ�˳
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
		 * ������
		 */
		if(!haveFound){
			if(chooseStrategy.getTripleDouble().size()!=0){
				Poker[] pokers = chooseStrategy.getTripleDouble().get(0);
				/**
				 * ����һ�²��ô�����Թ�����
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
		 * ���ְ�
		 */
		if(!haveFound){
			if(chooseStrategy.getDoubleTriple().size()!=0){
				Poker[] pokers = chooseStrategy.getDoubleTriple().get(0);
				/**
				 * ����һ�£����ù���ĸְ�̫����
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
		 * ����С���ƿ�ʼ����,2~k��ѭ����ȥ������
		 */
		if(!haveFound){
			for(int i = 2; i<14;i++){
				if(numList[i] > 0 && numList[i] < 4  && i != this.currentGrade){
					int pokerNum = numList[i];
					/**
					 * ���Ʋ�������Ҫ����
					 */
					if((numList[i] == 1||numList[i] == 2) &&(control1 == numList[i] || control3 == numList[i])){
						
					}
					else if (pokerNum == 1){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
					/**
					 * �����ŵĻ��������Ƿ���������ԣ���3+2
					 */
					else if(pokerNum == 2){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+2)>13)?1:(i+2);
						/**
						 * �Ƿ�����
						 */
						if(i<13 && numList[i+1] == 2 && numList[k]==2){
							if((i+1)!= this.currentGrade && k !=this.currentGrade){
								result = selectPokers(numList[i+1],-1,i+1,result);
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * �ܲ������3+2
						 */
						else{
							/**
							 * ��һ����Χ��Ѱ������
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
					 * �����ŵĻ��������ܷ����������
					 */
					else if(pokerNum == 3){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+1)>13)?1:(i+1);
						/**
						 * �ܷ���ɸְ�
						 */
						if(numList[k]==3){
							if(k !=this.currentGrade){							
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * �ܷ����3+2
						 */
						else{
							/**
							 * ��һ����Χ��Ѱ�Ҷ���
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
		 * �����δ�ҵ�����ǰ��Ϊ�˿��ƶ���ʣ������ƽ��д���
		 */
		if(!haveFound && (control1==1||control1==2||control3==1||control3==2)){
			/**
			 * һ��ʣһ�ţ�һ��ʣ2��
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
			 * ��ʣ��1�ţ���һ�����꣬��һ��ʣ��1��,���Ը�ʣ��1�ţ�һ�����кܶ�����
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
			 * ��ʣ��2�ţ���һ�����꣬��һ��ʣ��2��,���Ը�ʣ��2�ţ�һ�����кܶ�����
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
		 * δ�ҵ�����A��Ѱ�ң���A��������
		 */
		if(!haveFound &&(numList[1] > 0 && numList[1] < 4)&& 1!= this.currentGrade){
			haveFound = true;
			int pokerNum = numList[1];
			result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			/**
			 * �����ܷ����3+2
			 */
			if (pokerNum == 3){
				/**
				 * ��ȥ�����䣬��������������
				 */
				if ((numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2){
					result = selectPokers(2,-1,this.currentGrade,result);

				}
				/**
				 * ��������С��
				 */
				else if (numList[0] == 2 && numList[14]!=2){
					result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
				}
				/**
				 * �������Ŵ���
				 */
				else if (numList[14] == 2 && numList[0] != 2){
					result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
				}
			}
		}
		/**
		 * ��ѯһ���������»�����û,1��������
		 */
		int havePoker = 0;
		for(int i = 1;i<14;i++){
			if(i!=this.currentGrade&&numList[i]!=0){
				havePoker =1;
				break;
			}
		}
		/**
		 * ���֮ǰδ�ҵ��Ƴ����������»���ը���������ƣ����������䣩
		 */
		if(!haveFound && havePoker == 1){
			int pokerNum = numList[this.currentGrade] - pokerList0[2][this.currentGrade];
			/**
			 * ����ը���Ļ�
			 */
			if(pokerNum>0 && pokerNum<4){
				haveFound = true;
				result = selectPokers(pokerNum,-1,this.currentGrade,result);
				/**
				 * ���ܲ��ܺʹ�С�����3+2
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
		 * ���Ϊ�Ҵ��ƴ����������£��������Ѿ�����Ļ�����������Ҳ��
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
		 * �ٲ��оͳ���С��,���ж��Ƿ�����ը
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
		 * �����û���ҵ�����ը��
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
		 * û�ҵ������¶���û�����ƵĴ�С����������ʱ��Ӧ��ֻʣ��Ϊ����ʣ�µ����һ����
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
					 * �����ŵĻ��������Ƿ���������ԣ���3+2
					 */
					else if(pokerNum == 2){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+2)>13)?1:(i+2);
						/**
						 * �Ƿ�����
						 */
						if(i<13 && numList[i+1] == 2 && numList[k]==2){
							if((i+1)!= this.currentGrade && k !=this.currentGrade){
								result = selectPokers(numList[i+1],-1,i+1,result);
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * �ܲ������3+2
						 */
						else{
							/**
							 * ��һ����Χ��Ѱ������
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
					 * �����ŵĻ��������ܷ����������
					 */
					else if(pokerNum == 3){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						int k = ((i+1)>13)?1:(i+1);
						/**
						 * �ܷ���ɸְ�
						 */
						if(numList[k]==3){
							if(k !=this.currentGrade){							
								result = selectPokers(numList[k],-1,k,result);
							}
						}
						/**
						 * �ܷ����3+2
						 */
						else{
							/**
							 * ��һ����Χ��Ѱ�Ҷ���
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
		 * ���һ�����Ƿ����
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
	 * ѹ��
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
		 * ѹ�Լ���
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
		 * ѡ��һ������
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
		 * ��һ�����ϵ�ǰ���ʹ�С��Poker�����ԱȽ�
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
		 * �����ͽ��г��ƹ����ƶ�
		 */
		switch(pokerType[0]){
		case PokerType.SINGLE:
			/**
			 * ���ѹ��֮�����ô�����ѹ
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * �Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5���Ÿ����Լ�����ԭ���������õ���ѹ����
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * ��2~kѰ������Ϊһ�ŵ�
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
			    /**
			     * û�ҵ���Ѱ��AΪ���ţ�A��Ϊ����
			     */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
				/**
				 * û�ҵ���Ѱ������Ϊ����
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					int j = numList[this.currentGrade]-pokerList0[2][this.currentGrade];
					result = selectPokers(j,-1,this.currentGrade,result);
				}
				/**
				 * û�ҵ���Ѱ��С��
				 */
				if(!haveFound && numList[0] != 0 && comparePoker(new Poker(0,0),pokerNow)){
					haveFound = true;
					result = selectPokers(1,PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
				}
				/**
				 * û�ҵ���Ѱ�Ҵ���
				 */
				if(!haveFound && numList[14] != 0 && comparePoker(new Poker(0,1),pokerNow)){
					haveFound = true;
					result = selectPokers(1,PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
				}
				
			}
			/**
			 * ����ѹ�ƴ�С������A
			 */
			else{
				/**
				 * ��2~kѰ������Ϊһ�ŵ�
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
			    /**
			     * û�ҵ���Ѱ��AΪ���ţ�A��Ϊ����
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * �Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5����������ѹ������ֻѹС����
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * 2~k��Ѱ�Ҷ���
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 2 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
				/**
				 * û�ҵ���Ѱ�Ҷ���A
				 */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 2 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,1,result);
				}
				/**
				 * û�ҵ���Ѱ�����ƶ��ӣ����ӷ�����
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					result = selectPokers(2,-1,this.currentGrade,result);
				}
				/**
				 * û�ҵ����Ҷ�С��
				 */
				if(!haveFound && numList[0] == 2 && comparePoker(new Poker(0,0),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
				}
				/**
				 * û�ҵ����Ҵ���
				 */
				if(!haveFound && numList[14] == 2 && comparePoker(new Poker(0,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
				}
			}
			else{
				/**
				 * 2~10��Ѱ�Ҷ���
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * �Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5����ѹ
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * ��2~K��ƥ�������
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
				/**
				 * û�ҵ���������A
				 */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
				/**
				 * û�ҵ�������������
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * ����Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5��ѹ
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * ����Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5��ѹ
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * �Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5����û�����Ƶ�ѹ������ֻ����С��3+2
			 */
			else if(chooseStrategy.getHands()<3 && gameView.getPlayerPokerNum()[currentWinner] > 3
					&& gameView.getPlayerPokerNum()[currentWinner] != 5){
				/**
				 * �Ȳ�ѯ�Ƿ��ж���
				 */
				boolean haveCouple = false;
				for(int i = 1;i<14;i++){
					if(numList[i] == 2 && i!= this.currentGrade){
						haveCouple = true;
						break;
					}
				}
				/**
				 * �ж���
				 */
				if(haveCouple){
					/**
					 * ��С����������
					 */
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 *  û�ҵ�����AAA
					 */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
					/**
					 * û�ҵ���������
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						result = selectPokers(3,-1,this.currentGrade,result);
					}
					/**
					 * �ܹ��ҵ�3�ŵĻ����������
					 */
					if(haveFound){
						boolean findCouple = false;
						/**
						 * ��С�����Ҷ���
						 */
						for(int i = 2;i<14;i++){
							if(i!= this.currentGrade && numList[i] == 2){
								findCouple = true;
								result = selectPokers(numList[i],-1,i,result);
								break;
							}
						}
						/**
						 * û�ҵ�С���ӣ��� AA
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
				 * �Ȳ�ѯ�Ƿ��ж���
				 */
				boolean haveCouple = false;
				for(int i = 1;i<11;i++){
					if(numList[i] == 2 && i!= this.currentGrade){
						haveCouple = true;
						break;
					}
				}
				/**
				 * �ж���
				 */
				if(haveCouple){
					/**
					 * ��С����������
					 */
					for(int i = 2;i<11;i++){
						if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 * �ܹ��ҵ�3�ŵĻ����������
					 */
					if(haveFound){
						/**
						 * ��С�����Ҷ���
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
			 * �Լ���������С��4����ѹ
			 */
			else if(gameView.getPlayerPokerNum()[currentWinner]<4){
				
			}
			/**
			 * ����Լ�����С��3���ҶԼ����ƴ���3�Ҳ�����5��ѹ
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
		 * ���������������ٵ��࣬������õ�����ƥ��
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
				 * ��һ�����ϵ�ǰ���ʹ�С��Poker�����ԱȽ�
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
				 * �����ͽ��г��ƹ����ƶ�
				 */
				switch(pokerType[0]){
				case PokerType.SINGLE:
					/**
					 * ��2~kѰ������Ϊһ�ŵ�
					 */
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 1 && comparePoker(new Poker(1,i),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
				    /**
				     * û�ҵ���Ѱ��AΪ���ţ�A��Ϊ����
				     */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
					/**
					 * û�ҵ���Ѱ������Ϊ����
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						int j = numList[this.currentGrade]-pokerList0[2][this.currentGrade];
						result = selectPokers(j,-1,this.currentGrade,result);
					}
					/**
					 * û�ҵ���Ѱ��С��
					 */
					if(!haveFound && numList[0] != 0 && comparePoker(new Poker(0,0),pokerNow)){
						haveFound = true;
						result = selectPokers(1,PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
					}
					/**
					 * û�ҵ���Ѱ�Ҵ���
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
					 * û�ҵ���Ѱ�Ҷ���A
					 */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 2 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,1,result);
					}
					/**
					 * û�ҵ���Ѱ�����ƶ��ӣ����ӷ�����
					 */
					if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
						haveFound = true;
						result = selectPokers(2,-1,this.currentGrade,result);
					}
					/**
					 * û�ҵ����Ҷ�С��
					 */
					if(!haveFound && numList[0] == 2 && comparePoker(new Poker(0,0),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
					}
					/**
					 * û�ҵ����Ҵ���
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
					 * û�ҵ���������A
					 */
					if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
					}
					/**
					 * û�ҵ�������������
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
					 * �Ȳ�ѯ�Ƿ��ж���
					 */
					boolean haveCouple = false;
					for(int i = 1;i<14;i++){
						if(numList[i] == 2 && i!= this.currentGrade){
							haveCouple = true;
							break;
						}
					}
					/**
					 * �ж���
					 */
					if(haveCouple){
						/**
						 * ��С����������
						 */
						for(int i = 2;i<14;i++){
							if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[i],-1,i,result);
								break;
							}
						}
						/**
						 *  û�ҵ�����AAA
						 */
						if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
						}
						/**
						 * û�ҵ���������
						 */
						if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
							haveFound = true;
							result = selectPokers(3,-1,this.currentGrade,result);
						}
						/**
						 * �ܹ��ҵ�3�ŵĻ����������
						 */
						if(haveFound){
							boolean findCouple = false;
							/**
							 * ��С�����Ҷ���
							 */
							for(int i = 2;i<14;i++){
								if(i!= this.currentGrade && numList[i] == 2){
									findCouple = true;
									result = selectPokers(numList[i],-1,i,result);
									break;
								}
							}
							/**
							 * û�ҵ�С���ӣ��� AA
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
		 * ѡ��һ������
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
		 * ��һ�����ϵ�ǰ���ʹ�С��Poker�����ԱȽ�
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
		 * �����ͽ��г��ƹ����ƶ�
		 */
		switch(pokerType[0]){
		case PokerType.SINGLE:
			if(pokerList.size() == 2 && pokerType(new ArrayList(pokerList))[0]==PokerType.DOUBLE 
					&& gameView.getPlayerPokerNum()[(this.myTurn+2)%4]>7){
				haveFound = true;
			}
			/**
			 * ��2~kѰ������Ϊһ�ŵ�
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
		     * û�ҵ���Ѱ��AΪ���ţ�A��Ϊ����
		     */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 1 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			}
			/**
			 * û�ҵ���Ѱ������Ϊ����
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				int j = numList[this.currentGrade]-pokerList0[2][this.currentGrade];
				result = selectPokers(j,-1,this.currentGrade,result);
			}
			/**
			 * û�ҵ���Ѱ��С��
			 */
			if(!haveFound && numList[0] != 0 && comparePoker(new Poker(0,0),pokerNow)){
				haveFound = true;
				result = selectPokers(1,PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
			}
			/**
			 * û�ҵ���Ѱ�Ҵ���
			 */
			if(!haveFound && numList[14] != 0 && comparePoker(new Poker(0,1),pokerNow)){
				haveFound = true;
				result = selectPokers(1,PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
			}
			/**
			 * û�ҵ�����ʼ������
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) != 0 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(1,-1,this.currentGrade,result);
			}
			/**
			 * û�ҵ�����A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] > 0 && numList[1] < 4 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(1,-1,PokerPoints.ACE,result);
			}
			if(gameView.getPlayerPokerNum()[currentWinner]<11){
				/**
				 * û�ҵ����Ӵ���С��
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
			 * û�ҵ�����ը
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
				 * ûը���ʹ������
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
			 * 2~k��Ѱ�Ҷ���
			 */
			for(int i = 2;i<14;i++){
				if(i!= this.currentGrade && numList[i] == 2 && comparePoker(new Poker(1,i),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
			/**
			 * û�ҵ���Ѱ�Ҷ���A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 2 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[1],-1,1,result);
			}
			/**
			 * û�ҵ���Ѱ�����ƶ��ӣ����ӷ�����
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 2 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(2,-1,this.currentGrade,result);
			}
			/**
			 * û�ҵ����Ҷ�С��
			 */
			if(!haveFound && numList[0] == 2 && comparePoker(new Poker(0,0),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[0],PokerPattern.JOKER,PokerPoints.LITTLE_JOKER,result);
			}
			/**
			 * û�ҵ����Ҵ���
			 */
			if(!haveFound && numList[14] == 2 && comparePoker(new Poker(0,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[14],PokerPattern.JOKER,PokerPoints.OLD_JOKER,result);
			}
			/**
			 * û�ҵ���������
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) > 1 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(2,-1,this.currentGrade,result);
			}
			/**
			 * û�ҵ�����A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] > 1 && numList[1] < 4 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(2,-1,PokerPoints.ACE,result);
			}
			if(gameView.getPlayerPokerNum()[currentWinner]<15){
				/**
				 * û�ҵ����Ӵ���С��
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
			 * û�ҵ�����ը
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
					 * ��ը��������
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
							 * ��A,A������
							 */
							if(numList[1] == 1&&1!=this.currentGrade && comparePoker(new Poker(1,1),pokerNow)){
								haveFound = true;
								result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
								result = selectPokers(1,PokerPattern.HEART,this.currentGrade,result);
							}
							/**
							 * �Ӵ�С��
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
							 * ��û�ҵ�����һ�������Ƿ��ж���
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
			 * ��2~K��ƥ�������
			 */
			for(int i = 2;i<14;i++){
				if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[i],-1,i,result);
					break;
				}
			}
			/**
			 * û�ҵ���������A
			 */
			if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
				haveFound = true;
				result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
			}
			/**
			 * û�ҵ�������������
			 */
			if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
				haveFound = true;
				result = selectPokers(3,-1,this.currentGrade,result);
			}
			/**
			 * û�ҵ�������
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
					 * û�л𣬿�ʼ����
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
							 * �Ӵ�С����
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
			 * Ѱ������
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
			 * �Ȳ�ѯ�Ƿ��ж���
			 */
			boolean haveCouple = false;
			for(int i = 1;i<14;i++){
				if(numList[i] == 2 && i!= this.currentGrade){
					haveCouple = true;
					break;
				}
			}
			/**
			 * �ж���
			 */
			if(haveCouple){
				/**
				 * ��С����������
				 */
				for(int i = 2;i<14;i++){
					if(i!= this.currentGrade && numList[i] == 3 && comparePoker(new Poker(1,i),pokerNow)){
						haveFound = true;
						result = selectPokers(numList[i],-1,i,result);
						break;
					}
				}
				/**
				 *  û�ҵ�����AAA
				 */
				if(!haveFound && 1!= this.currentGrade && numList[1] == 3 && comparePoker(new Poker(1,1),pokerNow)){
					haveFound = true;
					result = selectPokers(numList[1],-1,PokerPoints.ACE,result);
				}
				/**
				 * û�ҵ���������
				 */
				if(!haveFound && (numList[this.currentGrade]-pokerList0[2][this.currentGrade]) == 3 && comparePoker(new Poker(1,this.currentGrade),pokerNow)){
					haveFound = true;
					result = selectPokers(3,-1,this.currentGrade,result);
				}
				/**
				 * �ܹ��ҵ�3�ŵĻ����������
				 */
				if(haveFound){
					boolean findCouple = false;
					/**
					 * ��С�����Ҷ���
					 */
					for(int i = 2;i<14;i++){
						if(i!= this.currentGrade && numList[i] == 2){
							findCouple = true;
							result = selectPokers(numList[i],-1,i,result);
							break;
						}
					}
					/**
					 * û�ҵ�С���ӣ��� AA
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
				 * ��Ϊ���޷���������
				 */
				if(!haveFound){
					changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
					changeList(numList,pokerList0,chooseStrategy.getStraight());
					if(pokerList0[2][this.currentGrade] ==0){
						for(int j = 1;j<14;j++){
							/**
							 * Ѱ����С��
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
						 * ��ը��
						 */
						for(int j = 1;j<14;j++){
							/**
							 * Ѱ����С��
							 */
							if(numList[j] >5){
								haveFound = true;
								result = selectPokers(numList[j],-1,j,result);
								break;
							}
						}
						/**
						 * û�ҵ���������䣬�ȼ�һ��
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
						 * �Ҳ�������������ŷ����䣬�������ŷ�����
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
						 * ��û�ҵ������������Ƿ���Ƿ��ϵ�ը��
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
				 * ��û�ҵ������Ƿ�������ը
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
					 * ��Ѱ����С����ѹסը��
					 */
					for(int j = 1;j<14;j++){
						if(numList[j] == currentPoker.size() && j!= this.currentGrade&& comparePoker(new Poker(1,j),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[j],-1,j,result);
							break;
						}
					}
					/**
					 * ������һ�ŵ�ը��
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
					 * ��Ѱ����С����ѹסը��
					 */
					for(int j = 1;j<14;j++){
						if(numList[j] == currentPoker.size() && j!= this.currentGrade&& comparePoker(new Poker(1,j),pokerNow)){
							haveFound = true;
							result = selectPokers(numList[j],-1,j,result);
							break;
						}
					}
					/**
					 * ������һ�ŵ�ը��
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
					 * ����һ�ŷ����䣬����һ���࣬�Ƶ���
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
					 * �����������һ�ŵ�ը��
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
					 * �������Ʊ����Ƿ����
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
				 * �����Ƿ�������ը
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
	 * ��ը��
	 */
	protected ArrayList<Poker> payFire(ArrayList<Poker[]> sF){
		ArrayList<Poker> result = new ArrayList<Poker>();
		boolean haveFound = false;
		/**
		 * ��4�ŵ�ը����ʼѭ���𣬲����Ƿ�����
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
		 * ͬ��˳
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
	 * ��δ�ҵ�ը������ʼ���Ƿ�����
	 */
	    if(!haveFound && this.pokerList0[2][this.currentGrade]>1){
	    	for(int j = 1;j<14;j++){
	    		/**
	    		 * 3+1��ը
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
			 * ��6�ŵ�ը����ʼѭ���𣬲����Ƿ�����
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
	     * ��û���ҵ�ը����2+2��ը
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
	     * ��û�ҵ������������Ƿ����ը��
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
	     * ��û�ҵ������Ƿ�������ը
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
	    		 * 3+1��ը
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
	 * �ع�
	 * @return �ع�����
	 */
	public Poker payBack(){
		/**
		 * ��ʼ��������������
		 */
		availStrategy  = new LinkedList<AI_Strategy>();
		this.leftHands = 100;
		normalAnalyze();
		/**
		 * ѡ��һ������
		 */
		AI_Strategy chooseStrategy = null;
		/**
		 * ���Ƽ�������
		 */
		int[] numListCopy = this.numList.clone();
		int[][] pokerList0Copy = copyArray(this.pokerList0);
		for(int i = 0;i<availStrategy.size();i++){
			if(availStrategy.get(i).getHands() == this.leftHands){
				chooseStrategy = availStrategy.get(i);
				/**
				 * ������ȥ��ͬ��˳���ӻ�˳
				 */
				changeList(numList,pokerList0,chooseStrategy.getStraightFlush());
				changeList(numList,pokerList0,chooseStrategy.getStraight());
				break;
			}
		}
		boolean haveFound = false;
		int currentChoose = -1;
		/**
		 * �����ǻ��������ֻ��ǶԼ�
		 */
		int[] orderRecord = gameView.getOrderRecord();
		if((orderRecord[0]+orderRecord[1])%2 == 0 || ((orderRecord[0]+orderRecord[3])%2 == 1)&&orderRecord[0] == this.myTurn){
			ArrayList<Poker> result = new ArrayList<Poker>();
			/**
			 * ��2�ҵ�k�ҵ���
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
			 * �ҵ���û�ҵ�����2��J��3�ŵ�
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
			 * 3��Ҳû�ҵ�����2~Q�Ҷ���
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
			 * ��û�ҵ�����5�ţ����������ϵ�ը��
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
			 * ����ͬ��˳�ӻ�˳
			 */
			addList(numList,pokerList0,chooseStrategy.getStraightFlush());
			addList(numList,pokerList0,chooseStrategy.getStraight());
			/**
			 * ��û�ҵ���������С��
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
		 * �������Լ�
		 */
		else{
			ArrayList<Poker> result = new ArrayList<Poker>();
			/**
			 * ��2�ҵ�k�ҵ���
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
			 * ����Ҳû�ҵ�����1~K�Ҷ���
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
			 * ��û�ҵ���������С��
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
