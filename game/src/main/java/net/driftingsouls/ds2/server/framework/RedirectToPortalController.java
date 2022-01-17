package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

@Module(name = "redirectToPortal", defaultModule = true)
public class RedirectToPortalController extends Controller {
    @Action(ActionType.DEFAULT)
    public TemplateEngine defaultAction() {
        getResponse().redirectTo("./portal");
        return null;
    }
}
