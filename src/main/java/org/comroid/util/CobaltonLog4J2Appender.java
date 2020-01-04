package org.comroid.util;

import java.io.Serializable;
import java.util.function.Function;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.javacord.api.util.logging.ExceptionLogger;

@Plugin(
        name = "CobaltonLog4J2Appender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class CobaltonLog4J2Appender extends AbstractAppender {
    private final Function<Throwable, ?> logger;

    public CobaltonLog4J2Appender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);

        //noinspection RedundantTypeArguments
        this.logger = ExceptionLogger.<Object>get();
    }

    @Override
    public void append(LogEvent event) {
        final Throwable throwable = event.getThrown();

        if (throwable != null)
            logger.apply(throwable);
    }

    @PluginFactory
    public static CobaltonLog4J2Appender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new CobaltonLog4J2Appender(name, filter, layout);
    }
}
