package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.*;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */

        public void applyOn(ResourceOrder order) {
            Task aux = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2] ;
            order.tasksByMachine[machine][t2] = aux ;
        }
    }

    @Override
    public Result solve(Instance instance, long deadline) {

        Solver solv = new GreedySolver("EST_LRPT");
        Result bestSol = solv.solve(instance,deadline);
        int curMakespan=bestSol.schedule.makespan();
        ResourceOrder order = new ResourceOrder(bestSol.schedule);

        List<Block> blocks;
        List<Swap> swaps;
        ResourceOrder bestN, curN;
        Schedule sched;
        boolean improve=true;
        int iter =0;
        int maxIter=10000;
        int makeCurN,makeBestN;

        while (improve && iter<maxIter) {

            improve=false;
            iter++;

            blocks = blocksOfCriticalPath(order);
            bestN = order.copy();
           for(Block block : blocks) {
                swaps = neighbors(block);
                for (Swap curSwap : swaps) {
                    curN = order.copy();
                    curSwap.applyOn(curN);
                    makeBestN=bestN.toSchedule().makespan();
                    makeCurN=curN.toSchedule().makespan();
                    if ( makeCurN < makeBestN) {
                        bestN = curN.copy();
                    }
                }
            }

            sched = bestN.toSchedule();
            if (sched.makespan() < curMakespan) {
                curMakespan = sched.makespan();
                improve = true;
                order = bestN.copy();
                bestSol = new Result(instance, sched, Result.ExitCause.Blocked);
            }

        }
        return bestSol;
    }



    /** Returns a list of all blocks of the critical path. */
   List<Block> blocksOfCriticalPath(ResourceOrder order) {
        Schedule sched = order.toSchedule();
        List<Task> critic = sched.criticalPath();
        List<Block> sol = new ArrayList<>();
        int[] curBlock = new int [2];
        int curIndex =0;
        int preResource=-1;
       List<Task> orderMachine;
        int m;
        Block newBlock;
        for(Task curTask : critic){
            m = order.instance.machine(curTask);
            if (m==preResource){
                curBlock[1]+=1;
            } else {
                if (curBlock[0]!=curBlock[1]){
                    orderMachine = Arrays.asList(order.tasksByMachine[preResource]) ;
                    newBlock = new Block(preResource,orderMachine.indexOf(critic.get(curBlock[0])),orderMachine.indexOf(critic.get(curBlock[1])));
                    sol.add(newBlock);
                }
                curBlock[0]=curIndex;
                curBlock[1]=curBlock[0];
                preResource=m;
            }
            curIndex+=1;

        }
        if (curBlock[0]!=curBlock[1]) {
            orderMachine = Arrays.asList(order.tasksByMachine[preResource]) ;
            newBlock = new Block(preResource, orderMachine.indexOf(critic.get(curBlock[0])),orderMachine.indexOf(critic.get(curBlock[1])));
            sol.add(newBlock);
        }
        return sol;
    }
    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        Swap newSwap;
        List<Swap> sol = new ArrayList<>();
        if(block.firstTask+1==block.lastTask){
             newSwap = new Swap(block.machine,block.firstTask,block.lastTask);
            sol.add(newSwap);
        } else {
            newSwap = new Swap(block.machine,block.firstTask,block.firstTask+1);
            sol.add(newSwap);
            newSwap = new Swap(block.machine,block.lastTask-1,block.lastTask);
            sol.add(newSwap);
        }
        return sol;
     }

}
