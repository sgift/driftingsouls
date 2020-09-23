package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.framework.Configuration;

public class TickPartExecutor extends AbstractTickExecutor {
    private final Class<? extends TickController> tickControllerClazz;
    private final String logBaseName;

    public TickPartExecutor(Class<? extends TickController> tickControllerClazz, String logBaseName) {
        this.tickControllerClazz = tickControllerClazz;
        this.logBaseName = logBaseName;
    }

    @Override
    protected void executeTicks() {
        execTick(tickControllerClazz, false);
    }

    @Override
    protected void prepare() {
        setName("");
        setLogPath(Configuration.getLogPath()+logBaseName + "/");
    }
}
