package com.atex.onecms.scripting;

import com.atex.onecms.app.dam.policy.DamContentPolicy;
import com.atex.onecms.content.files.FileServiceAdapter;
import com.atex.onecms.content.files.FileServiceClient;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptListPolicy extends DamContentPolicy {

    private final Application application;
    private final FileServiceAdapter fileServiceAdapter;

    public ScriptListPolicy(final CmClient cmClient, final Application fileServiceClient) throws IllegalApplicationStateException {
        super(cmClient, fileServiceClient);
        this.application = fileServiceClient;
        this.fileServiceAdapter = new FileServiceAdapter(this, () -> application.getPreferredApplicationComponent(FileServiceClient.class));
    }

    @Override
    public void preCommitSelf() throws CMException {
// super.preCommitSelf();
        this.fileServiceAdapter.commitTemporaryFiles();
        this.getAspects();
        this.getContentData();
        String publishList = this.getComponent("publish", "name");
        String[] publishArray = publishList.split(",");

        ScriptList bean = (ScriptList) this.getContentData();
        if (bean == null) {
            return;
        }
        List<ExternalContentId> scripts = bean.getScripts();
        for (String externalId : publishArray) {
            boolean add = true;
            for (ExternalContentId script : scripts) {
                if (script.getExternalId().equals(externalId)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                ExternalContentId externalContentId = new ExternalContentId(externalId);
                bean.getScripts().add(externalContentId);
            }
        }
        updateFromBean(bean, this.getContentId());
    }

    private Map<String, ? extends Object> getAspects() throws CMException {
        HashMap<String, Object> aspects = new HashMap();
        String[] aspectNames = this.getContent().getComponentNames("aspects");
        String[] var3 = aspectNames;
        int var4 = aspectNames.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            String aspect = var3[var5];
            aspects.put(aspect, this.getAspect(aspect));
        }

        return aspects;
    }
}
