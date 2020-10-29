package server.plans;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class PlanJob implements Job {

    private Plan plan;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        plan.createClient();
        plan.scanForFiles();
        plan.closeClient();
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }
}