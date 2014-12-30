// ----------------------------------------------------------------------------
// Copyright 2007-2014, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Thread pool manager
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/03  Martin D. Flynn
//     -Removed reference to JavaMail api imports
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2006/11/28  Martin D. Flynn
//     -Added method "setMaxPoolSize(size)"
//  2013/04/08  Martin D. Flynn
//     -Added global "StopThreads(...)" method to stop all active threads in all
//      ThreadPools.
//  2013/09/20  Martin D. Flynn
//     -Support property override for "maximumPoolSize"/"maximumIdleSeconds"
//      Property: ThreadPool.GROUP_NAME.maximumPoolSize=SIZE
//      Property: ThreadPool.GROUP_NAME.maximumIdleSeconds=SECONDS
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** Thread pool manager
**/

public class ThreadPool
{
    
    // ------------------------------------------------------------------------

    private static final int    DFT_POOL_SIZE           = 20;
    private static final int    DFT_MAX_IDLE_AGE_SEC    = 0;
    private static final long   DFT_MAX_IDLE_AGE_MS     = (long)DFT_MAX_IDLE_AGE_SEC * 1000L;
    private static final int    DFT_MAX_QUEUE_SIZE      = 0;

    public  static final int    STOP_WAITING            = -1;
    public  static final int    STOP_NEVER              = 0;
    public  static final int    STOP_NOW                = 1;

    // ------------------------------------------------------------------------

    private static boolean                globalStopThreadsNow = false;
    private static Map<ThreadPool,String> threadPoolList       = new WeakHashMap<ThreadPool,String>();

    /**
    *** Adds a ThreadPool to a global list
    *** @param tp  A ThreadPool
    **/
    private static void _AddThreadPool(ThreadPool tp)
    {
        if (tp != null) {
            synchronized (ThreadPool.threadPoolList) {
                try {
                    if (!ThreadPool.threadPoolList.containsKey(tp)) {
                        ThreadPool.threadPoolList.put(tp,"");
                    }
                } catch (Throwable th) {
                    // not sure what could be thrown here for weak-references, but catch just in case
                    Print.logException("ThreadPool weak reference list", th);
                }
            }
        }
    }

    /**
    *** Tell all active threads to stop 
    *** @param stopNow  True to stop threads, even if jobs are still queued.  False
    ***                 to stop only after all jobs have been processed. (note that 
    ***                 jobs currently being processed will continue until they are
    ***                 done).
    **/
    public static void StopThreads(boolean stopNow)
    {
        synchronized (ThreadPool.threadPoolList) {
            if (stopNow) {
                ThreadPool.globalStopThreadsNow = true;
            }
            for (ThreadPool tp : ThreadPool.threadPoolList.keySet()) {
                tp.stopThreads(stopNow);
            }
        }
    }

    /**
    *** Tell all active threads to stop 
    **/
    public static int GetTotalThreadCount()
    {
        int count = 0;
        synchronized (ThreadPool.threadPoolList) {
            for (ThreadPool tp : ThreadPool.threadPoolList.keySet()) {
                count += tp.getPoolSize();
            }
        }
        return count;
    }

    /**
    *** Tell all active threads to stop 
    **/
    public static int PrintThreadCount()
    {
        int count = 0;
        synchronized (ThreadPool.threadPoolList) {
            for (ThreadPool tp : ThreadPool.threadPoolList.keySet()) {
                String n = tp.getName();
                int    s = tp.getPoolSize();
                Print.logInfo("ThreadPool '" + n + "' size=" + s);
                count += s;
            }
        }
        return count;
    }
    
    /**
    *** Gets the ThreadPool state
    **/
    public static StringBuffer GetThreadPoolState(StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        synchronized (ThreadPool.threadPoolList) {
            sb.append("ThreadPools:\n");
            if (!ListTools.isEmpty(ThreadPool.threadPoolList)) {
                for (ThreadPool tp : ThreadPool.threadPoolList.keySet()) {
                    String name     = tp.getName();
                    int    pSize    = tp.getPoolSize();
                    int    maxPSize = tp.getMaxPoolSize();
                    int    active   = tp.getActiveCount();
                    int    qSize    = tp.getQueueSize();
                    int    maxQSize = tp.getMaxQueueSize();
                    sb.append("  ");
                    sb.append("Name=").append(name).append(" ");
                    for (int s=18, n=name.length(); s>n; s--) {sb.append(" ");}
                    sb.append("MaxPoolSize=" ).append(maxPSize).append("  ");
                    sb.append("PoolSize="    ).append(pSize   ).append("  ");
                    sb.append("Active="      ).append(active  ).append("  ");
                    sb.append("MaxQueueSize=").append(maxQSize).append("  ");
                    sb.append("QueueSize="   ).append(qSize   ).append("  ");
                    sb.append("\n");
                }
            } else {
                sb.append("  ");
                sb.append("None");
                sb.append("\n");
            }
        }
        return sb;
    }

    // ------------------------------------------------------------------------

    /* The ThreadGroup for this pool */
    private ThreadGroup                 poolGroup       = null;

    /* the maximum number of allowed threads in this pool */
    private int                         maxPoolSize     = DFT_POOL_SIZE;

    /* the maximum allowed idle time of a thread before it is removed from the pool */
    private long                        maxIdleAgeMS    = DFT_MAX_IDLE_AGE_MS;

    /* the list of threads in this pool */
    private java.util.List<ThreadJob>   jobThreadPool   = null;

    /* the sequence id used for naming individual threads */
    private int                         threadId        = 1;

    /* the list of yet-to-be-processed jobs */
    private java.util.List<Runnable>    jobQueue        = null;

    /* the maximum number of waiting jobs (ie. in "jobQueue") */
    private int                         maxQueueSize    = DFT_MAX_QUEUE_SIZE;

    /* semiphore used to indicate waiting for job */
    private int                         waitingCount    = 0;

    /* true to gracefully stop/remove threads from this pool */
    private int                         stopThreads     = STOP_NEVER;

    /**
    *** Constuctor
    *** @param name The name of the thread pool
    **/
    public ThreadPool(String name)
    {
        this(name, 
            DFT_POOL_SIZE, DFT_MAX_IDLE_AGE_SEC, DFT_MAX_QUEUE_SIZE);
    }

    /**
    *** Constructor
    *** @param name The name of the thread pool
    *** @param maxPoolSize The maximum number of threads in the thread pool
    **/
    public ThreadPool(String name, 
        int maxPoolSize)
    {
        this(name, 
            maxPoolSize, DFT_MAX_IDLE_AGE_SEC, DFT_MAX_QUEUE_SIZE);
    }

    /**
    *** Constructor
    *** @param name The name of the thread pool
    *** @param maxPoolSize   The maximum number of threads in the thread pool
    *** @param maxIdleSec    The maximum number of seconds a thread is allowed to remain
    ***                      idle before it self-terminates.
    *** @param maxQueueSize  The maximum number of jobs allowed in queue
    **/
    public ThreadPool(String name, 
        int maxPoolSize, int maxIdleSec, int maxQueueSize)
    {
        this(name,
            RTKey.valueOf(!StringTools.isBlank(name)?("ThreadPool."+name):null),
            maxPoolSize, maxIdleSec, maxQueueSize);
    }

    /**
    *** Constructor
    *** @param name          The name of the thread pool
    *** @param propPfx_      The property key prefix from which the default attributes
    ***                      for this ThreadPool will be obtained.
    *** @param maxPoolSize   The maximum number of threads in the thread pool ("maximumPoolSize")
    *** @param maxIdleSec    The maximum number of seconds a thread is allowed to remain
    ***                      idle before it self-terminates ("maximumIdleSeconds")
    *** @param maxQueueSize  The maximum number of jobs allowed in queue ("maximumQueueSize")
    **/
    public ThreadPool(String name, 
        RTKey propPfx_,
        int maxPoolSize, int maxIdleSec, int maxQueueSize)
    {
        super();

        /* init vars */
        String groupName   = !StringTools.isBlank(name)? name.trim() : "ThreadPool";
        this.poolGroup     = new ThreadGroup(groupName);
        this.jobThreadPool = new Vector<ThreadJob>();
        this.jobQueue      = new Vector<Runnable>();
        this.stopThreads   = ThreadPool.globalStopThreadsNow? STOP_NOW : STOP_NEVER;

        /* set maxPoolSize/maxIdleSec */
        if (!RTKey.isBlank(propPfx_)) {
            // IE:
            //  ThreadPool.PoolName.maximumPoolSize=50
            //  ThreadPool.PoolName.maximumIdleSeconds=0
            //  ThreadPool.PoolName.maximumQueueSize=0
            this.setMaxPoolSize( propPfx_.rtSuffix("maximumPoolSize"   ), maxPoolSize );
            this.setMaxIdleSec(  propPfx_.rtSuffix("maximumIdleSeconds"), maxIdleSec  );
            this.setMaxQueueSize(propPfx_.rtSuffix("maximumQueueSize"  ), maxQueueSize);
        } else {
            this.setMaxPoolSize( maxPoolSize );
            this.setMaxIdleSec(  maxIdleSec  );
            this.setMaxQueueSize(maxQueueSize);
        }

        /* add to global manager */
        ThreadPool._AddThreadPool(this);

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of the thread pool
    *** @return The name of the thread pool
    **/
    public String getName()
    {
        return this.getThreadGroup().getName();
    }
    
    /**
    *** Returns the name of the thread pool
    *** @return The name of the thread pool
    **/
    public String toString()
    {
        return this.getName();
    }
    
    /**
    *** Returns true if this object is equal to <code>other</code>. This will
    *** only return true if they are the same object
    *** @param other The object to check equality with
    *** @return True if <code>other</code> is the same object
    **/
    public boolean equals(Object other)
    {
        return (this == other); // equals only if same object
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the thread group of the Threads in this pool
    *** @return The thread group of the Threads in this pool
    **/
    public ThreadGroup getThreadGroup()
    {
        return this.poolGroup;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the number of currently active jobs
    **/
    public int getActiveCount()
    {
        int cnt = 0;
        synchronized (this.jobThreadPool) {
            for (ThreadJob tj : this.jobThreadPool) {
                if (tj.isRunning()) {
                    cnt++;
                }
            }
        }
        return cnt;
    }


    /**
    *** Gets the current size of this thread pool
    *** @return The number of thread jobs in this thread pool
    **/
    public int getPoolSize()
    {
        int size = 0;
        synchronized (this.jobThreadPool) {
            size = this.jobThreadPool.size();
        }
        return size;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum size of this thread pool
    *** @param maxSize The maximum size of the thread pool
    **/
    public void setMaxPoolSize(int maxSize)
    {
        this.maxPoolSize = (maxSize > 0)? maxSize : DFT_POOL_SIZE;
    }

    /**
    *** Sets the maximum size of this thread pool
    *** @param propKey    The property key name to use for looking up the overriding 
    ***                   value in the runtime configuration properties.
    *** @param dftMaxSize The maximum size of the thread pool
    **/
    public void setMaxPoolSize(RTKey propKey, int dftMaxSize)
    {
        int propMps = (propKey != null)? RTConfig.getInt(propKey.toString(),-1) : -1;
        if (propMps > 0) {
            this.maxPoolSize = propMps;
        } else
        if (dftMaxSize > 0) {
            this.maxPoolSize = dftMaxSize;
        } else {
            this.maxPoolSize = DFT_POOL_SIZE;
        }
        Print.logDebug("["+this.getName()+"] ThreadPool 'maximumPoolSize': " + this.maxPoolSize);
    }

    /**
    *** Gets the maximum size of this thread pool
    *** @return The maximum size of the thread pool
    **/
    public int getMaxPoolSize()
    {
        return this.maxPoolSize;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum number of seconds that a thread is allowed to remain idle
    *** before it self-terminates.
    *** @param maxIdleSec The maximum number of idle seconds
    **/
    public void setMaxIdleSec(int maxIdleSec)
    {
        this.setMaxIdleMS((long)maxIdleSec * 1000L);
    }

    /**
    *** Sets the maximum number of seconds that a thread is allowed to remain idle
    *** before it self-terminates.
    *** @param propKeySec   The propery name from which the maximum number of idle
    ***                     seconds will attempt to be retrieved.
    *** @param dftMaxIdleMS The default maximum number of idle seconds.
    **/
    public void setMaxIdleSec(RTKey propKeySec, int dftMaxIdleSec)
    {
        int propMidSec = (propKeySec != null)? RTConfig.getInt(propKeySec.toString(),-1) : -1;
        if (propMidSec >= 0) {
            this.setMaxIdleMS((long)propMidSec * 1000L);
        } else
        if (dftMaxIdleSec >= 0) {
            this.setMaxIdleMS((long)dftMaxIdleSec * 1000L);
        } else {
            this.setMaxIdleMS(DFT_MAX_IDLE_AGE_MS);
        }
        Print.logDebug("["+this.getName()+"] ThreadPool 'maximumIdleSec': " + this.getMaxIdleMS()/1000L);
    }

    /**
    *** Sets the maximum number of milliseconds that a thread is allowed to remain idle
    *** before it self terminates.
    *** @param maxIdleMS The maximum number of idle milliseconds
    **/
    public void setMaxIdleMS(long maxIdleMS)
    {
        this.maxIdleAgeMS = (maxIdleMS >= 0L)? maxIdleMS : DFT_MAX_IDLE_AGE_MS;
    }

    /**
    *** Gets the maximum number of milliseconds that a thread is allowed to remain idle
    *** before it self terminates.
    *** @return The maximum idle milliseconds
    **/
    public long getMaxIdleMS()
    {
        return this.maxIdleAgeMS;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum allowed number of waiting jobs (in "jobQueue")
    *** @param maxQSize The maximum allowed number of waiting jobs
    **/
    public void setMaxQueueSize(int maxQSize)
    {
        this.maxQueueSize = (maxQSize >= 0)? maxQSize : DFT_MAX_QUEUE_SIZE;
    }

    /**
    *** Sets the maximum allowed number of waiting jobs (in "jobQueue")
    *** @param propKey     The property key name to use for looking up the overriding 
    ***                    value in the runtime configuration properties.
    *** @param dftMaxQSize The maximum allowed number of waiting jobs
    **/
    public void setMaxQueueSize(RTKey propKey, int dftMaxQSize)
    {
        int propMqs = (propKey != null)? RTConfig.getInt(propKey.toString(),-1) : -1;
        if (propMqs > 0) {
            this.maxQueueSize = propMqs;
        } else
        if (dftMaxQSize > 0) {
            this.maxQueueSize = dftMaxQSize;
        } else {
            this.maxQueueSize = DFT_MAX_QUEUE_SIZE;
        }
        Print.logDebug("["+this.getName()+"] ThreadPool 'maximumQueueSize': " + this.maxQueueSize);
    }

    /**
    *** Gets the maximum allowed number of waiting jobs (in "jobQueue")
    *** @return The maximum allowed number of waiting jobs (in "jobQueue")
    **/
    public int getMaxQueueSize()
    {
        return this.maxQueueSize;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds a new job to the thread pool's queue
    *** @param job The job to add to the queue
    **/
    public boolean run(Runnable job)
    {
        if (job == null) {
            // ignore null jobs
            return false;
        } else
        if (this.stopThreads == STOP_NOW) {
            // ignore job if this ThreadPool is in the process of stopping now.
            return false;
        } else {
            int maxQueueSize = this.getMaxQueueSize();
            boolean addedJob = false;
            synchronized (this.jobThreadPool) { // <-- modification of threadPool is likely
                synchronized (this.jobQueue) { // <-- modification of job queue mandatory
                    if ((maxQueueSize <= 0) || (this.jobQueue.size() < maxQueueSize)) {
                        // It's possible that we may end up adding more threads than we need if this
                        // section executes multiple times before the newly added thread has a chance 
                        // to pull a job off the queue.
                        this.jobQueue.add(job);
                        if ((this.waitingCount == 0) && (this.jobThreadPool.size() < this.maxPoolSize)) {
                            String    tn = StringTools.format(this.threadId++,"000").trim();
                            ThreadJob tj = new ThreadJob(this, (this.getName() + "_" + tn));
                            this.jobThreadPool.add(tj);
                            Print.logDebug("New Thread: " + tj.getName() + " [" + this.getMaxPoolSize() + "]");
                        }
                        this.jobQueue.notify(); // notify a waiting thread
                        addedJob = true;
                    }
                }
            }
            return addedJob;
        }
    }

    /**
    *** Gets the job queue size (jobs not yet processed)
    **/
    public int getQueueSize()
    {
        int qsize = 0;
        synchronized (this.jobQueue) {
            qsize = this.jobQueue.size();
        }
        return qsize;
    }

    // ------------------------------------------------------------------------

    /**
    *** Stops all threads in this pool once queued jobs are complete
    **/
    public void stopThreads()
    {
        this.stopThreads(false); // stop when jobs are done
    }

    /**
    *** Stops all threads in this pool once queued jobs are complete
    *** @param stopNow  True to stop threads, even if jobs are still queued.  False
    ***                 to stop only after all jobs have been processed. (note that 
    ***                 jobs currently being processed will continue until they are
    ***                 done).
    **/
    public void stopThreads(boolean stopNow)
    {
        synchronized (this.jobQueue) {
            this.stopThreads = stopNow? STOP_NOW : STOP_WAITING;
            this.jobQueue.notifyAll();
        }
    }
    
    /**
    *** Removes the specified worker thread from the pool
    *** @param thread The thread to remove from the pool
    **/
    protected void _removeThreadJob(ThreadJob thread)
    {
        if (thread != null) {
            synchronized (this.jobThreadPool) {
                //Print.logDebug("Removing thread: " + thread.getName());
                this.jobThreadPool.remove(thread);
            }
        }
    }

    // ------------------------------------------------------------------------

    private static class ThreadJob
        extends Thread
    {

        /* ThreadPool to which this thread belongs */
        private ThreadPool  threadPool = null;

        /* the current job being executed */
        private Runnable    job = null;

        /* timestamps */
        private long        creationTimeMS = 0L;
        private long        lastUsedTimeMS = 0L;

        public ThreadJob(ThreadPool pool, String name) {
            super(pool.getThreadGroup(), name);
            this.threadPool = pool;
            this.creationTimeMS = DateTime.getCurrentTimeMillis();
            this.lastUsedTimeMS = this.creationTimeMS;
            this.start(); // auto start thread
        }

        public void run() {

            /* loop forever (or until stopped) */
            while (true) {

                /* get next job */
                // 'this.job' is always null here
                boolean stop = false;
                synchronized (this.threadPool.jobQueue) {
                    //Print.logDebug("Thread checking for jobs: " + this.getName());
                    while (this.job == null) {
                        if (this.threadPool.stopThreads == STOP_NOW) {
                            // stop now, no more jobs
                            stop = true;
                            break;
                        } else
                        if (this.threadPool.jobQueue.size() > 0) { // this.jobQueue
                            // run next job
                            this.job = this.threadPool.jobQueue.remove(0); // Runnable
                        } else
                        if (this.threadPool.stopThreads == STOP_WAITING) {
                            // stop after all jobs have completed
                            stop = true;
                            break;
                        } else
                        if ((this.threadPool.maxIdleAgeMS > 0L) &&
                            ((DateTime.getCurrentTimeMillis() - this.lastUsedTimeMS) > this.threadPool.maxIdleAgeMS)) {
                            // stop due to excess idle time
                            stop = true;
                            break;
                        } else {
                            // wait for next job notification
                            int tmoMS = 20000; // maximum wait (should probably be higher)
                            // TODO: adjust 'tmpMS' to coincide with remaining 'maxIdleAgeMS'
                            this.threadPool.waitingCount++;
                            try { this.threadPool.jobQueue.wait(tmoMS); } catch (InterruptedException ie) {}
                            this.threadPool.waitingCount--;
                            // continue next loop
                        }
                    } // while (this.job == null)
                }
                if (stop) { break; }

                /* run job */
                //Print.logDebug("Thread running: " + this.getName());
                this.job.run();
                synchronized (this.threadPool.jobQueue) {
                    this.job = null;
                }
                this.lastUsedTimeMS = DateTime.getCurrentTimeMillis();

            } // while (true)

            /* remove thread from pool */
            this.threadPool._removeThreadJob(this);

        }

        public boolean isRunning() {
            boolean rtn = false;
            synchronized (this.threadPool.jobQueue) {
                rtn = (this.job != null)? true : false;
            }
            return rtn;
        }

    } // class ThreadJob
    
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        // ThreadPool.Test_1.maximumPoolSize=30
        // ThreadPool.Test_1.maximumIdleSeconds=30
        RTKey propPfx = RTKey.valueOf(RTConfig.getString("prop",null));
        ThreadPool pool_1 = new ThreadPool("Test_1", propPfx, 3/*pool*/,-1/*idleSec*/,-1/*queueSize*/); 
        ThreadPool pool_2 = new ThreadPool("Test_2", 3);

        for (int i = 0; i < 15; i++) {
            final int n = i;
            Print.logInfo("Job " + i);
            Runnable job = new Runnable() {
                int num = n;
                public void run() {
                    Print.logInfo("Start Job: " + this.getName());
                    try { Thread.sleep(2000 + (num * 89)); } catch (Throwable t) {}
                    Print.logInfo("Stop  Job:                " + this.getName());
                }
                public String getName() {
                    return "[" + Thread.currentThread().getName() + "] " + num;
                }
            };
            if ((i & 1) == 0) {
                pool_1.run(job);
            } else {
                pool_2.run(job);
            }
            try { Thread.sleep(100); } catch (Throwable t) {}
        }

        Print.logInfo("Stop Threads");
        ThreadPool.StopThreads(true); // Stop now
        for (int i = 0; i < 20; i++) {
            Print.sysPrintln("---------------------------");
            int cnt = ThreadPool.PrintThreadCount();
            if (cnt <= 0) { break; }
            try { Thread.sleep(1000); } catch (Throwable t) {}
        }
        Print.sysPrintln("Total Thread Count: " + ThreadPool.GetTotalThreadCount());
        
    }
    
}
