AJS.toInit(function() {
	var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
	
	function populateForm() {
	    AJS.$.ajax({
	      url: baseUrl + "/rest/jira-acceptance-admin/1.0/",
	      dataType: "json",
	      success: function(config) {
	    	  AJS.$("#url").attr("value", config.url);
	          AJS.$("#login").attr("value", config.login);
	          AJS.$("#password").attr("value", config.password);
	      }
	    });
	  }
     
  function updateConfig() {
	  AJS.$.ajax({
	    url: baseUrl + "/rest/jira-acceptance-admin/1.0/",
	    type: "PUT",
	    contentType: "application/json",
	    data: '{ "url": "' + AJS.$("#url").attr("value") + '",' +
		'"login": "' + AJS.$("#login").attr("value") + '",' +
		'"password": "' + AJS.$("#password").attr("value") + '"	}',
	    processData: false,
	    fail:urlErrorMessage(),
	    error: urlErrorMessage(),
	    success:urlSuccessMessage()
	  });
	}
  
  
  populateForm();
  
  AJS.$("#admin").submit(function(e) {
	    e.preventDefault();
	    updateConfig();
	});
  
  function urlSuccessMessage(){
	  
	  AJS.$('#aui-message-bar').html("");
	  AJS.messages.success({
	   title:"Success.",	   
	   body: "<p> The settings have been updated.</p>"		   
	});
  }
  
  function urlErrorMessage(){
	  
	  AJS.$('#aui-message-bar').html("");
	  AJS.messages.error({
	   title:"Error.",
	   fadeout: true,
	   delay: 2000,
	   duration: 10000,
	   body: "<p> Something went wrong.</p>"			   
	});		
  }

});
