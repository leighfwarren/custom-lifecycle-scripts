package com.atex.onecms.scripting.api;

import com.atex.onecms.content.Content;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.aspects.Aspect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A ContentResultFacade is a wrapper for a ContentResult that restructures and exposes fields for the NashornJS environment.
 * Notably, contentData is moved into the aspects list and can be referenced on an instance as: <code>content.aspects.contentData</code>.
 */
public class ContentResultFacade extends BaseJSObject<ContentResult<?>> {
    /**
     * Member for storing the current state of the ContentResult.
     */
    private ContentResult<?> content;

    /**
     * Create ContentResultFacade from a ContentResult object. The facade will be mutable in JS scripts.
     * @param content The ContentResult to create the facade from.
     */
    public ContentResultFacade(final ContentResult<?> content) {
        this(content, false);
    }


    /**
     * Create a ContentResultFacade from a ContentResult object.
     * @param content The ContentResult to create the facade from.
     * @param immutable true if the facade should be immutable in JS script code, otherwise false.
     */
    public ContentResultFacade(final ContentResult<?> content, final boolean immutable) {
        super(content, immutable);
        if (content == null) {
            return;
        }
        this.content = content;
        properties.put("aspects", buildAspectMap(content.getContent().getAspects(), content.getContent().getContentData()));
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

    /**
     * Get the underlying Content.
     * @return The underlying Content.
     */
    public Content<?> getContent() {
        return content.getContent();
    }
}
