package org.spigotmc.utils;

import java.awt.EventQueue;
import java.util.concurrent.Executor;

public class AwtExecutor implements Executor
{

    public static final Executor INSTANCE = new AwtExecutor();

    private AwtExecutor()
    {
    }

    @Override
    public void execute(Runnable command)
    {
        if ( EventQueue.isDispatchThread() )
        {
            command.run();
        } else
        {
            EventQueue.invokeLater( command );
        }
    }
}
