package com.atex.onecms.scripting;

import com.atex.onecms.app.dam.DamContentBean;
import com.polopoly.cm.ExternalContentId;
import org.apache.commons.beanutils.PropertyUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ScriptList extends DamContentBean implements Serializable {

    private static final String OBJECT_TYPE = "ScriptList";
    private static final String INPUT_TEMPLATE = "p.ScriptList";
    public static final String EXTERNAL_ID    = "com.atex.plugins.scripting.configuration";
    private static final long serialVersionUID = -5598989618519776032L;

    @XmlElement
    private List<ExternalContentId> scripts = null;

    public ScriptList() {
        super.setObjectType(OBJECT_TYPE);
        super.setInputTemplate(INPUT_TEMPLATE);
        super.set_type(this.getClass().getName());
    }

    public ScriptList(final ScriptList orig) {
        try {
            PropertyUtils.copyProperties(this, orig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ExternalContentId> getScripts() {
        return scripts;
    }

    public void setScripts(final List<ExternalContentId> scripts) {
        this.scripts = scripts;
    }
}
