package net.driftingsouls.ds2.server.framework;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;

public class WebApplicationCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getBeanFactory() != null &&
                WebApplicationContext.class.isAssignableFrom(context.getBeanFactory().getClass());
    }
}

