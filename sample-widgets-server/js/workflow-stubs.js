if(typeof(WorkflowManager) === 'undefined') {
	WorkflowManager = {
			"stub": true
	};
}
  
//Stub calls SDK's next function upon receipt of event from widget
 WorkflowManager.next = function(workflowId, nextStepUrl, contextData, contextType) {
	var message = { "function": "next",
					"parameters" : [workflowId, nextStepUrl, contextData, contextType]};
	window.parent.window.postMessage(message, '*');
  };  

//Stub calls SDK's waitAndCheckProgress function upon receipt of event from widget
  WorkflowManager.waitAndCheckProgress = function(workflowId, progressWidgetUrl, contextData, contextType) {
	var message = { "function": "waitAndCheckProgress",
					"parameters" : [workflowId, progressWidgetUrl, contextData, contextType]};
	window.parent.window.postMessage(message, '*');
  };

//Stub calls SDK's completeWorkflow function upon receipt of event from widget
  WorkflowManager.completeWorkflow = function(workflowId) {
	var message = { "function": "completeWorkflow",
					"parameters" : [workflowId]};
	window.parent.window.postMessage(message, '*');
  };

//Stub calls SDK's adjustMainWindow function upon receipt of event from widget  
  WorkflowManager.adjustMainWindow = function(width, height) {
	var message = { "function": "adjustMainWindow",
					"parameters" : [width, height]};
	window.parent.window.postMessage(message, '*');
  };

  if(typeof(WidgetsApp) === 'undefined') {
	  WidgetsApp = {};
  }
 
  // Adding stub listener to invoke widget's WidgetsApp functions on event posted by SDK 
  WidgetsApp.initialize = function() {
	function receiveStubMessage(event) {
		//alert("event.data " + event.data);
		WidgetsApp[event.data.function].apply(this, event.data.parameters);
	}
	  
	window.addEventListener("message", receiveStubMessage.bind(this), false);
}