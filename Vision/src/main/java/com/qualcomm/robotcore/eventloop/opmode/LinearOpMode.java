package com.qualcomm.robotcore.eventloop.opmode;

import io.github.deltacv.vision.external.source.ThreadSourceHander;

public abstract class LinearOpMode extends OpMode {
    protected final Object lock = new Object();
    private LinearOpModeHelperThread helper = new LinearOpModeHelperThread(this);
    private RuntimeException catchedException = null;

    public LinearOpMode() {
    }

    //------------------------------------------------------------------------------------------------
    // Operations
    //------------------------------------------------------------------------------------------------

    /**
     * Override this method and place your code here.
     * <p>
     * Please do not swallow the InterruptedException, as it is used in cases
     * where the op mode needs to be terminated early.
     * @throws InterruptedException
     */
    abstract public void runOpMode() throws InterruptedException;

    /**
     * Pauses the Linear Op Mode until start has been pressed or until the current thread
     * is interrupted.
     */
    public void waitForStart() {
        while (!isStarted() && !Thread.currentThread().isInterrupted()) { idle(); }
    }

    /**
     * Puts the current thread to sleep for a bit as it has nothing better to do. This allows other
     * threads in the system to run.
     *
     * <p>One can use this method when you have nothing better to do in your code as you await state
     * managed by other threads to change. Calling idle() is entirely optional: it just helps make
     * the system a little more responsive and a little more efficient.</p>
     *
     * @see #opModeIsActive()
     */
    public final void idle() {
        // Otherwise, yield back our thread scheduling quantum and give other threads at
        // our priority level a chance to run
        Thread.yield();
    }

    /**
     * Sleeps for the given amount of milliseconds, or until the thread is interrupted. This is
     * simple shorthand for the operating-system-provided {@link Thread#sleep(long) sleep()} method.
     *
     * @param milliseconds amount of time to sleep, in milliseconds
     * @see Thread#sleep(long)
     */
    public final void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Answer as to whether this opMode is active and the robot should continue onwards. If the
     * opMode is not active, the OpMode should terminate at its earliest convenience.
     *
     * <p>Note that internally this method calls {@link #idle()}</p>
     *
     * @return whether the OpMode is currently active. If this returns false, you should
     *         break out of the loop in your {@link #runOpMode()} method and return to its caller.
     * @see #runOpMode()
     * @see #isStarted()
     * @see #isStopRequested()
     */
    public final boolean opModeIsActive() {
        boolean isActive = !this.isStopRequested() && this.isStarted();
        if (isActive) {
            idle();
        }
        return isActive;
    }

    /**
     * Can be used to break out of an Init loop when false is returned. Touching
     * Start or Stop will return false.
     *
     * @return Whether the OpMode is currently in Init. A return of false can exit
     *         an Init loop and proceed with the next action.
     */
    public final boolean opModeInInit() {
        return !isStarted() && !isStopRequested();
    }

    /**
     * Has the opMode been started?
     *
     * @return whether this opMode has been started or not
     * @see #opModeIsActive()
     * @see #isStopRequested()
     */
    public final boolean isStarted() {
        return this.isStarted || Thread.currentThread().isInterrupted();
    }

    /**
     * Has the the stopping of the opMode been requested?
     *
     * @return whether stopping opMode has been requested or not
     * @see #opModeIsActive()
     * @see #isStarted()
     */
    public final boolean isStopRequested() {
        return this.stopRequested || Thread.currentThread().isInterrupted();
    }


    //------------------------------------------------------------------------------------------------
    // OpMode inheritance
    //------------------------------------------------------------------------------------------------

    @Override
    public final void init() {
        isStarted = false;
        stopRequested = false;

        ThreadSourceHander.register(helper, ThreadSourceHander.threadHander());

        helper.start();
    }

    @Override
    public final void init_loop() { }

    @Override
    public final void start() {
        stopRequested = false;
        isStarted = true;
    }

    @Override
    public final void loop() {
        synchronized (lock) {
            if (catchedException != null) {
                throw catchedException;
            }
        }
    }

    @Override
    public final void stop() {
        /*
         * Get out of dodge. Been here, done this.
         */
        if(stopRequested)  { return; }

        stopRequested = true;

        helper.interrupt();
    }

    private static class LinearOpModeHelperThread extends Thread {

        LinearOpMode opMode;

        public LinearOpModeHelperThread(LinearOpMode opMode) {
            super("Thread-LinearOpModeHelper-" + opMode.getClass().getSimpleName());

            this.opMode = opMode;
        }

        @Override
        public void run() {
            try {
                opMode.runOpMode();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                synchronized (opMode.lock) {
                    opMode.catchedException = e;
                }
            }
        }

    }

}
