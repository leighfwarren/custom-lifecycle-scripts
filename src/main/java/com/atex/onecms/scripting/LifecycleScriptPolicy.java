package com.atex.onecms.scripting;

import com.atex.onecms.app.dam.policy.DamContentPolicy;
import com.polopoly.application.Application;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;

public class LifecycleScriptPolicy extends DamContentPolicy {
    public LifecycleScriptPolicy(final CmClient cmClient, final Application fileServiceClient) throws Exception {
        super(cmClient, fileServiceClient);
    }

    @Override
    public void preCommitSelf() throws CMException {
        super.preCommitSelf();

        LifecycleScript bean = (LifecycleScript) this.getContentData();
        if (bean == null) {
            return;
        }

        updateFromBean(bean, this.getContentId());
    }
}
