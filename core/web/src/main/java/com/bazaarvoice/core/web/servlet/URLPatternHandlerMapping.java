package com.bazaarvoice.core.web.servlet;

import com.bazaarvoice.core.web.util.URLPattern;
import com.bazaarvoice.core.web.util.URLPatternPredicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.beans.factory.annotation.Required;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Spring HandlerMapping interface that maps HTTP
 * request URLs to the handler (usually a Controller) that handles the request.
 * <p/>
 * There are two big differences between this HandlerMapping and the standard
 * Spring SimpleUrlHandlerMapping class:
 * <ol>
 * <li> This class uses the BV URLPattern class which extends the Spring Ant
 *      path-based patterns with JDK 1.4-style regular expressions.  See the
 *      URLPattern javadoc for details.
 *
 * <li> This class uses the order of the entries in the handler map to
 *      determine which handler applies if a request URL matches multiple
 *      URL patterns.  It assumes that entries are listed from least specific
 *      to most specific, so the last entry wins.  In contrast,
 *      SimpleUrlHandlerMapping picks the entry with the longest URL pattern.
 * </ol>
 */
public class URLPatternHandlerMapping extends AbstractHandlerMapping {

    private Handler[] _urlMapEntries;
    private Map<String, URLPatternPredicate> _predicateMap;

    @Required
    public void setUrlMap(Map<String, Object> handlerMap) {
        // rely on Spring behavior of using a LinkedHashMap w/JDK 1.4+ so the handlerMap
        // will preserve the order of the entries as specified in the XML file.

        List<Handler> list = new ArrayList<Handler>();
        for (Map.Entry<String, Object> entry : handlerMap.entrySet()) {
            list.add(new Handler(new URLPattern(entry.getKey(), _predicateMap), entry.getValue()));
        }

        // the last entry to match wins, so reverse the list so as we iterate
        // through it we can return the first matching entry
        Collections.reverse(list);

        _urlMapEntries = list.toArray(new Handler[list.size()]);
    }

    public void setPredicateMap(Map<String, URLPatternPredicate> predicateMap) {
        _predicateMap = predicateMap;
    }

    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
        // use the remaining servlet-specific path
        String pathName = request.getPathInfo();

        if (StringUtils.isNotEmpty(pathName)) {
            //noinspection unchecked
            Map<String, String[]> parameters = request.getParameterMap();

            for (Handler handler : _urlMapEntries) {
                if (handler.getUrlPattern().matches(pathName, parameters)) {
                    return handler.getHandler();
                }
            }
        }

        return null;
    }

    private static class Handler {
        private final URLPattern _urlPattern;
        private final Object _handler;

        public Handler(URLPattern urlPattern, Object handler) {
            _urlPattern = urlPattern;
            _handler = handler;
        }

        public URLPattern getUrlPattern() {
            return _urlPattern;
        }

        public Object getHandler() {
            return _handler;
        }
    }
}
