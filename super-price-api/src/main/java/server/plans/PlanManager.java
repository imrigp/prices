/*
 *
 *  * Copyright 2020 Imri
 *  *
 *  * This application is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package server.plans;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import server.connection.DownloaderThread;
import server.xml.XmlDownload;
import server.xml.XmlFile;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class PlanManager {

    private final Scheduler scheduler;
    int id;
    private final ThreadPoolExecutor downloadExecutor;
    private final BlockingQueue<XmlDownload> queue;
    private final Consumer<XmlFile> xmlConsumer;

    public PlanManager(Consumer<XmlFile> xmlConsumer, int threads) throws SchedulerException {
        this.xmlConsumer = xmlConsumer;
        scheduler = StdSchedulerFactory.getDefaultScheduler();

        queue = new LinkedBlockingDeque<>();
        Consumer<XmlDownload> onDownloadReady = XmlDownload::submitDownload;

        // prepare DownloaderThreads
        downloadExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            downloadExecutor.execute(new DownloaderThread<>(queue, onDownloadReady));
        }
    }

    public void addPlan(Plan plan) throws SchedulerException {
        plan.setDownloadQueue(queue);
        plan.setXmlConsumer(xmlConsumer);
        JobDetail job = newJob(PlanJob.class)
                .withIdentity("job" + id, "plans")
                .build();
        job.getJobDataMap().put("plan", plan);

        Trigger trigger = newTrigger()
                .withIdentity("trigger" + id++, "plans")
                .startNow()
                .withSchedule(simpleSchedule()
                        //.withIntervalInHours(24)
                        .withIntervalInMinutes(60)
                        //.withIntervalInSeconds(30)
                        .withMisfireHandlingInstructionIgnoreMisfires()
                        .repeatForever())
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public void start() throws SchedulerException {
        scheduler.start();
    }

    public void shutdown() throws SchedulerException {
        downloadExecutor.shutdown();
        scheduler.shutdown();
    }

    public boolean isStarted() throws SchedulerException {
        return scheduler.isStarted();
    }

    public boolean isShutdown() throws SchedulerException {
        return scheduler.isShutdown();
    }
}
