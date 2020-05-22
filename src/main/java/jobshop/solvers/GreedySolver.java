
package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.Task;
import jobshop.encodings.ResourceOrder;
import sun.security.x509.OtherName;

public class GreedySolver implements Solver{

    private String h ;

    public GreedySolver (String heuristique){
        this.h=heuristique;
    }



    @Override
    public Result solve(Instance instance, long deadline) {


        ResourceOrder sol = new ResourceOrder(instance);

        Task nullTask = new Task (-1,-1);
        Task chosenTask = nullTask;

        Task[] realisableTask = new Task[instance.numJobs];
        Task[] availableTask = new Task[instance.numJobs];

        int[] LRPT = new int[instance.numJobs]; //remaining processing time of each job
        int [] nextFreeTimeJob = new int [instance.numJobs];
        int [] nextFreeTimeMachine = new int [instance.numMachines];

        for (int i=0;i<instance.numJobs;i++){
            realisableTask[i]=new Task(i,0);
            availableTask[i]=new Task(i,0);
            LRPT[i]=0;
            nextFreeTimeJob[i]=0;
            for (int t=0; t<instance.numTasks;t++){
                LRPT[i]+=instance.duration(i,t);
            }
        }

        for (int i=0;i<instance.numMachines;i++){
            nextFreeTimeMachine[i]=0;
        }
        int curD,curEST;
        Task curTask;
        int EST = Integer.MAX_VALUE;
        for (int r=0;r<instance.numMachines;r++){
            for (int j=0; j< instance.numJobs;j++){
                EST = Integer.MAX_VALUE;
                availableTask= realisableTask.clone();
                if ("EST_SPT".equals(this.h) || "EST_LRPT".equals(this.h)){
                    //on trouve EST le plus petit earliest start time
                    for (int i=0; i<instance.numJobs; i++) {
                        curTask = realisableTask[i];
                        if (!curTask.equals(nullTask)) {
                            curEST = Integer.max(nextFreeTimeJob[i], nextFreeTimeMachine[instance.machine(curTask)]);
                            if (curEST < EST) {
                                EST = curEST;
                            }
                        }
                    }
                    //on garde dans availableTask seulement les taches avec le plus petit est
                    for (int i=0; i<instance.numJobs; i++) {
                        curTask = realisableTask[i];
                        if (!curTask.equals(nullTask)) {
                            curEST = Integer.max(nextFreeTimeJob[i], nextFreeTimeMachine[instance.machine(curTask)]);
                            if (curEST > EST) {
                                availableTask[i] = nullTask;
                            }
                        }
                    }

                }
                if ("SPT".equals(this.h) || "EST_SPT".equals(this.h)) {
                    //SPT heuristique
                    curD = Integer.MAX_VALUE;
                    for (int i = 0; i < instance.numJobs; i++) {
                        curTask = availableTask[i];
                        if (!curTask.equals(nullTask) && instance.duration(curTask) < curD) {
                            chosenTask = curTask;
                            curD= instance.duration(chosenTask);
                        }
                    }
                } else if ("LRPT".equals(this.h) || "EST_LRPT".equals(this.h)) {
                    //LRPT heuristique
                    curD = Integer.MIN_VALUE;
                    for (int i = 0; i < instance.numJobs; i++) {
                        curTask = availableTask[i];
                        if (!curTask.equals(nullTask) && LRPT[curTask.job] > curD) {
                            chosenTask = curTask;
                            curD=LRPT[curTask.job];
                        }
                    }

                    LRPT[chosenTask.job] -= instance.duration(chosenTask);
                }

                //tache trouvee task chosenTask

                int m=instance.machine(chosenTask);
                if (sol.nextFreeSlot[m]<instance.numJobs) {
                    sol.tasksByMachine[m][sol.nextFreeSlot[m]] = chosenTask;
                    sol.nextFreeSlot[m]+=1;
                }

                nextFreeTimeJob[chosenTask.job]=EST+instance.duration(chosenTask);
                nextFreeTimeMachine[m]=EST+instance.duration(chosenTask);

                if (chosenTask.task <instance.numTasks-1){
                    realisableTask[chosenTask.job] = new Task(chosenTask.job,chosenTask.task +1);
                } else { realisableTask[chosenTask.job]=nullTask; }
            }
        }




        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }
}