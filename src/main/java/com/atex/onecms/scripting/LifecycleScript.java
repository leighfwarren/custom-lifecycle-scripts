package com.atex.onecms.scripting;

import com.atex.onecms.app.dam.DamContentBean;
import org.apache.commons.beanutils.PropertyUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LifecycleScript extends DamContentBean implements Serializable {

    private static final long serialVersionUID = 7224250021104323898L;
    private static final String INPUT_TEMPLATE = "p.LifecycleScript";
    private static final String OBJECT_TYPE = "LifecycleScript";

    @XmlElement
    private String id;

    @XmlElement
    private String event;

    @XmlElement
    private String scriptType;

    @XmlElement
    private String description;

    @XmlElement
    private String script;

    public LifecycleScript() {
        super.setObjectType(OBJECT_TYPE);
        super.setInputTemplate(INPUT_TEMPLATE);
        super.set_type(this.getClass().getName());
    }

    public LifecycleScript(final LifecycleScript orig) {
        try {
            PropertyUtils.copyProperties(this, orig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unique identifier e.g. atex.oncecms.scripting.setworkflowparentid
     *
     * @return The ID for the script.
     */
    public String getId() {
        return id;
    }


    public void setId(final String id) {
        this.id = id;
    }

    /**
     * The lifecycle stage to run this script.
     *
     * @return The stage.
     */
    public String getEvent() {
        return event;
    }

    public void setEvent(final String event) {
        this.event = event;
    }

    /**
     * The content type or other filter to run the script.
     *
     * @return The content type or filter for this script.
     */
    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(final String scriptType) {
        this.scriptType = scriptType;
    }

    /**
     * Description of script.
     *
     * @return The description of this script.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * The JavaScript to run.
     *
     * @return The code that this script runs.
     */
    public String getScript() {
        return script;
    }

    public void setScript(final String script) {
        this.script = script;
    }

}
