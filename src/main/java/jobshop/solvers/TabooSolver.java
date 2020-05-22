package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;

import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class TabooSolver extends DescentSolver {



    private int maxIter;
    private int dureeTaboo;

 public TabooSolver(int maxIter, int dureeTaboo) {
      super();
      this.maxIter = maxIter ;
      this.dureeTaboo = dureeTaboo ;
  }

  @Override
  public Result solve(Instance instance, long deadline) {

      Solver solv = new GreedySolver("EST_LRPT");
      Result bestSol = solv.solve(instance,deadline);
      int curMakespan=bestSol.schedule.makespan();
      ResourceOrder order = new ResourceOrder(bestSol.schedule);
      List<Block> blocks;
      List<Swap> swaps;
      Swap winSwap;
      Task swap1, swap2, winSwap1, winSwap2;
      ResourceOrder bestN, curN;
      Schedule sched;
      int iter =0;
      int makeCurN;
      int bestMakeN;
      boolean changed=true;
      //each task(j,i) has a unique id : j*num_task+i
      int[][] Taboo = new int[instance.numJobs*instance.numTasks][instance.numJobs*instance.numTasks];
      for (int i=0;i<instance.numJobs*instance.numTasks;i++){
          for (int j=0;j<instance.numJobs*instance.numTasks;j++) {
              Taboo[i][j] = 0;
          }
      }

      //current best sol is in bestSol
      //order is current sol
      //bestN is current best neighbor

      while (iter<maxIter && changed) {
          changed=false;
          winSwap=null;
          bestN=new ResourceOrder(instance);
          blocks = blocksOfCriticalPath(order);
          bestMakeN=Integer.MAX_VALUE;
          for(Block block : blocks) {
              swaps = neighbors(block);
              for (Swap curSwap : swaps) {
                  swap1 = order.tasksByMachine[curSwap.machine][curSwap.t1];
                  swap2 = order.tasksByMachine[curSwap.machine][curSwap.t2];
                  curN = order.copy();
                  curSwap.applyOn(curN);
                  makeCurN = curN.toSchedule().makespan();
                      if ( (makeCurN < curMakespan) || (Taboo[swap1.job * instance.numTasks + swap1.task][swap2.job * instance.numTasks + swap2.task] <= iter) ){
                          if (makeCurN < bestMakeN) {
                              //System.out.println(" new bestN makespan "+makeCurN+ " < "+bestMakeN);
                              bestN = curN.copy();
                              bestMakeN = makeCurN;
                              winSwap = curSwap;
                              changed = true;
                          }

                  }
              }
          }
          if (winSwap !=null) {
              winSwap1 = order.tasksByMachine[winSwap.machine][winSwap.t1];
              winSwap2 = order.tasksByMachine[winSwap.machine][winSwap.t2];
              Taboo[winSwap2.job*instance.numTasks+winSwap2.task][winSwap1.job*instance.numTasks+winSwap1.task]=iter+dureeTaboo;
              sched = bestN.toSchedule();
              //System.out.println("best neighbor is "+sched.makespan()+" cur makespan taboo is "+order.toSchedule().makespan()+ " best makespan "+curMakespan);
              order = bestN.copy();
              if (curMakespan > sched.makespan()) {
                  bestSol = new Result(instance, sched, Result.ExitCause.Blocked);
                  curMakespan = sched.makespan();
              }
          }
          iter++;
      }
      //System.out.println("fin solution");
      return bestSol;
  }

}
