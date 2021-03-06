package com.panbet.stash.hook;


import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.history.HistoryService;
import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.util.PageRequestImpl;
import com.atlassian.stash.setting.*;
import java.util.List;
import com.google.common.collect.Lists;
import java.net.URL;

import org.apache.xmlrpc.client.*;
import org.apache.xmlrpc.util.*;
import java.util.Vector;

import com.atlassian.stash.hook.*;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import java.util.Collection;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import com.panbet.stash.hook.admin.ConfigResource.Config;

public class JiraPreReceiveHook implements PreReceiveRepositoryHook, RepositorySettingsValidator
{

    private static final PageRequestImpl PAGE_REQUEST = new PageRequestImpl(0, 100);
    private final HistoryService historyService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final TransactionTemplate transactionTemplate;
    public JiraPreReceiveHook(HistoryService historyService, PluginSettingsFactory pluginSettingsFactory, TransactionTemplate transactionTemplate) {
        this.historyService = historyService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public boolean onReceive(RepositoryHookContext context, Collection<RefChange> refChanges, HookResponse hookResponse) {
        List<Changeset> badChangesets = Lists.newArrayList();



		Config globalConfig = (Config) transactionTemplate.execute(new TransactionCallback()
		  {
		    public Object doInTransaction()
		    {
		      PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		      Config globalConfig = new Config();
		      globalConfig.setUrl((String) settings.get(Config.class.getName() + ".url"));                 
                      globalConfig.setLogin((String) settings.get(Config.class.getName() + ".login"));
                      globalConfig.setPassword((String) settings.get(Config.class.getName() + ".password"));
		      return globalConfig;
		    }
		  });

        //String url = context.getSettings().getString("jiraBaseURL");
	String url = globalConfig.getUrl();
        String RPC_PATH  = "/rpc/xmlrpc";
        String jiraLogin = globalConfig.getLogin();
        String jiraPassword = globalConfig.getPassword();
        String projectKey = context.getSettings().getString("projectKey");
	String magicWord = context.getSettings().getString("magicWord");

        if (url != null) {
          for (RefChange refChange : refChanges) {
            for (Changeset changeset : historyService.getChangesetsBetween(context.getRepository(), refChange.getFromHash(), refChange.getToHash(), PAGE_REQUEST).getValues()) {
		if (changeset.getMessage().equals(magicWord)) { continue; }
                try {
                    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                    config.setServerURL(new URL(url + RPC_PATH));
                    XmlRpcClient rpcClient = new XmlRpcClient();
                    rpcClient.setConfig(config);
                    Vector jiraParams = new Vector(5);
                    jiraParams.add(jiraLogin);
                    jiraParams.add(jiraPassword);
                    jiraParams.add("admin");
                    jiraParams.add(projectKey);
                    jiraParams.add(changeset.getMessage());
                    String result = (String) rpcClient.execute("commitacc.acceptCommit", jiraParams);

                    hookResponse.out().println("Result: " + result);
                    if (!result.matches("true(.*)")) {
                       badChangesets.add(changeset);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
          }
          if (badChangesets.isEmpty()) {
              hookResponse.out().println("Jira Commit Acceptance Passed");
              return true;
          }
          for (Changeset changeset : badChangesets) {
              hookResponse.err().println(String.format("Bad changeset '%s' with message '%s'", changeset.getId(), changeset.getMessage()));
          }
          return false;
        }
        hookResponse.out().println("Jira URL is empty");
        return false;
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString("jiraBaseURL", "").isEmpty()) {
            errors.addFieldError("jiraBaseURL", "jiraBaseURL field is blank, please supply one");
        }
        if (settings.getString("projectKey", "").isEmpty()) {
            errors.addFieldError("projectKey", "projectKey field is blank, please supply one");
        }
    }
}

