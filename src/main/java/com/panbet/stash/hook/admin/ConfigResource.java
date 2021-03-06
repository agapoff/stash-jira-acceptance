package com.panbet.stash.hook.admin;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

@Path("/")
public final class ConfigResource {

	private final UserManager userManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Config resources constructor.
	 * @param userManager
	 * @param pluginSettingsFactory
	 * @param transactionTemplate
	 */
	public ConfigResource(UserManager userManager, PluginSettingsFactory pluginSettingsFactory,	TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Get the config settings from the database and return it as a Json object.
	 * @param request. The request object.
	 * @return configAsJason. The required config object as Json.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request)
	{
	  String username = userManager.getRemoteUsername(request);
	  if (username == null || !userManager.isSystemAdmin(username))
	  {
	    return Response.status(Status.UNAUTHORIZED).build();
	  }
	 
	  return Response.ok(transactionTemplate.execute(new TransactionCallback()
	  {
	    public Object doInTransaction()
	    {
	      PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
	      Config config = new Config();
	      config.setUrl((String) settings.get(Config.class.getName() + ".url"));                 
	      config.setLogin((String) settings.get(Config.class.getName() + ".login"));
	      config.setPassword((String) settings.get(Config.class.getName() + ".password"));
	      
	      return config;
	    }
	  })).build();
	}

	/**
	 * Update the config values in the database. 
	 * @param config. The updated config object containing the values to save.
	 * @param request. The request object.
	 * @return reponseStatus. Whether the update was successful or not. 
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final Config config, @Context HttpServletRequest request)
	{
	  String username = userManager.getRemoteUsername(request);
	  if (username == null || !userManager.isSystemAdmin(username))
	  {
	    return Response.status(Status.UNAUTHORIZED).build();
	  }
	 
	  transactionTemplate.execute(new TransactionCallback()
	  {
	    public Object doInTransaction()
	    {
	      PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	      pluginSettings.put(Config.class.getName() + ".url", config.getUrl());
	      pluginSettings.put(Config.class.getName() + ".login", config.getLogin());
	      pluginSettings.put(Config.class.getName() + ".password", config.getPassword());
	      return Response.ok().build();
	    }
	  });
	  return Response.noContent().build();
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class Config
	{
	  @XmlElement private String url;
	  @XmlElement private String login;
	  @XmlElement private String password;
	         
	  public String getUrl()
	  {
	    return url;
	  }
	         
	  public void setUrl(String url)
	  {
	    this.url = url;
	  }	  

          public String getLogin()
          {
            return login;
          }

          public void setLogin(String login)
          {
            this.login = login;
          }
          public String getPassword()
          {
            return password;
          }

          public void setPassword(String password)
          {
            this.password = password;
          }

	}
}
