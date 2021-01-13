package com.atex.onecms.scripting;

import com.atex.onecms.app.dam.policy.DamContentPolicy;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;

public class ScriptListPolicy extends DamContentPolicy {
    public ScriptListPolicy(final CmClient cmClient, final Application fileServiceClient) throws IllegalApplicationStateException {
        super(cmClient, fileServiceClient);
    }

    @Override
    public void preCommitSelf() throws CMException {
        super.preCommitSelf();

        ScriptList bean = (ScriptList) this.getContentData();
        if (bean == null) {
            return;
        }

        updateFromBean(bean, this.getContentId());
    }
}
