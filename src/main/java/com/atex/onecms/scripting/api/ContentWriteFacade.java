package com.atex.onecms.scripting.api;

import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.aspects.Aspect;
import jdk.nashorn.api.scripting.AbstractJSObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A ContentWriteFacade is a wrapper for a ContentWrite that restructures and exposes fields for the NashornJS environment.
 * Notably, contentData is moved into the aspects list and can be referenced on an instance as: <code>content.aspects.contentData</code>.
 */
public class ContentWriteFacade extends BaseJSObject<ContentWrite<?>> {
    /**
     * Member for storing the current state of the ContentWrite.
     */
    private ContentWrite<?> content;

    /**
     * Create ContentWriteFacade from a ContentWrite object. The facade will be mutable in JS scripts.
     * @param content The ContentWrite to create the facade from.
     */
    public ContentWriteFacade(final ContentWrite<?> content) {
        this(content, false);
    }


    /**
     * Create a ContentWriteFacade from a ContentWrite object.
     * @param content The ContentWrite to create the facade from.
     * @param immutable true if the facade should be immutable in JS script code, otherwise false.
     */
    public ContentWriteFacade(final ContentWrite<?> content, final boolean immutable) {
        super(content, immutable);
        if (content == null) {
            return;
        }
        this.content = content;
        properties.put("aspects", buildAspectMap(content.getAspects(), content.getContentData()));
        properties.put("getContentWrite", jsGetContent());
    }

    /**
     * Build a map of Aspects from content data.
     * @param aspects The list of aspects to build a map from.
     * @param contentData The content data to
     * @return A map of aspects including content data.
     */
    private Map<String, Aspect<?>> buildAspectMap(final Collection<Aspect> aspects, final Object contentData) {
        Map<String, Aspect<?>> aspectMap = new HashMap<>();
        for (Aspect<?> aspect : aspects) {
            aspectMap.put(aspect.getName(), aspect);
        }
        aspectMap.put("contentData", new Aspect("contentData", contentData));
        return aspectMap;
    }

    private AbstractJSObject jsGetContent() {
        return new AbstractJSObject() {
            @Override
            public Object call(final Object jsObj, final Object... args) {
                return content;
            }

            @Override
            public boolean isFunction() {
                return true;
            }
        };
    }
}
