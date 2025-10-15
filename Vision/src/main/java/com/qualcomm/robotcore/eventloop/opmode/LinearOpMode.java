/* Copyright (c) 2014 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.eventloop.opmode;

import io.github.deltacv.vision.external.source.ThreadVisionSourceProvider;
import io.github.deltacv.vision.external.source.VisionSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LinearOpMode extends OpMode {

    protected final Object lock = new Object();
    private LinearOpModeHelperThread helper = new LinearOpModeHelperThread(this, ThreadVisionSourceProvider.getCurrentProvider());

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
    }

    @Override
    public final void stop() {
        /*
         * Get out of dodge. Been here, done this.
         */
        if(stopRequested) { return; }

        stopRequested = true;

        helper.interrupt();

        try {
            helper.join();
        } catch (InterruptedException ignored) {
        }
    }

    private static class LinearOpModeHelperThread extends Thread {

        LinearOpMode opMode;
        VisionSourceProvider provider;

        static Logger logger = LoggerFactory.getLogger(LinearOpModeHelperThread.class);

        public LinearOpModeHelperThread(LinearOpMode opMode, VisionSourceProvider provider) {
            super("Thread-LinearOpModeHelper-" + opMode.getClass().getSimpleName());

            this.opMode = opMode;
            this.provider = provider;
        }

        @Override
        public void run() {
            ThreadVisionSourceProvider.register(provider);

            logger.info("{}: starting", opMode.getClass().getSimpleName());

            try {
                opMode.runOpMode();
                Thread.sleep(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("{}: interrupted", opMode.getClass().getSimpleName());
            } catch (RuntimeException e) {
                opMode.notifier.notify(e);
            }

            logger.info("{}: stopped", opMode.getClass().getSimpleName());
        }

    }

}
