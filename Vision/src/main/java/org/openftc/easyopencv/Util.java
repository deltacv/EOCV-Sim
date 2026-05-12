/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import java.util.concurrent.CountDownLatch;

public class Util
{
    public static void joinUninterruptibly(Thread thread)
    {
        boolean interrupted = false;

        while (true)
        {
            try
            {
                thread.join();
                break;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                interrupted = true;
            }
        }

        if (interrupted)
        {
            Thread.currentThread().interrupt();
        }
    }

    public static void acquireUninterruptibly(CountDownLatch latch)
    {
        boolean interrupted = false;

        while (true)
        {
            try
            {
                latch.await();
                break;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                interrupted = true;
            }
        }

        if (interrupted)
        {
            Thread.currentThread().interrupt();
        }
    }
}
