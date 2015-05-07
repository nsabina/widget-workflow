//constructor for Workflow Manager ( not yet remained to stay in sync with released prototypes 
WorkflowManager = {
	workflows : [],  //list of all workflows
	activeWorkflow : null, //list of all active workflows
	backgroundWorkflows : [], // list of all background workflows
	workflowQueue : [],  //workflow Queue
	mainView : null,  //main frame for modal dialog content
	secondaryView : null, //secondary frame for non-modal dialog content
    secondaryViewLoaded: false,
    statusWidgetUrl : "http://localhost:8000/check-progress.html"
};

var DEFAULT_MAIN_WIDGET_WIDTH = 500; //370;
var DEFAULT_MAIN_WIDGET_HEIGHT = 400; //150;

var DEFAULT_SECONDARY_WIDGET_WIDTH = 200; //370;
var DEFAULT_SECONDARY_WIDGET_HEIGHT = 100; //150;

var TOP_MARGIN = 50;

//Initialize stub for listener to post message to a widget
WidgetsApp = {};


//Workflow Manager initialization

WorkflowManager.init = function() {
	
	
    this.mainView = this.createView("WidgetsAppMainView",
                (screen.width - DEFAULT_MAIN_WIDGET_WIDTH) / 2,
                (screen.height - DEFAULT_MAIN_WIDGET_HEIGHT) / 2,
                DEFAULT_MAIN_WIDGET_WIDTH, DEFAULT_MAIN_WIDGET_HEIGHT,
                this.onMainViewLoaded);
    

    this.secondaryView = this.createView("WidgetsAppSecondaryView",screen.width - DEFAULT_SECONDARY_WIDGET_WIDTH, TOP_MARGIN,
                                           DEFAULT_SECONDARY_WIDGET_WIDTH, DEFAULT_SECONDARY_WIDGET_HEIGHT, 
                                           this.onSecondaryViewLoaded);
	
	// Adding listener to trigger SDK function when event is posted by widget
	function receiveStubMessage(event) {
		WorkflowManager[event.data.function].apply(this, event.data.parameters);
	}
	  
	window.addEventListener("message", receiveStubMessage.bind(this), false);
	
	
}

//create view
WorkflowManager.createView = function(viewId, leftMargin, topMargin, width, height, onViewLoadHandler) {
	
	/*
	 * Create div, create iframe inside of the div.
	 * Add div to the document.
	 */
	var div = document.createElement('div');
	div.id = viewId;
	div.style.position = 'absolute';
	div.style.width = width;
	div.style.height = height;
	div.style.margin.left = leftMargin;
	div.style.margin.top = topMargin;
	
	var iframe = document.createElement('iframe');
	iframe.frameBorder = 0;
	iframe.width = width;
	// bind iframe onload a handler
	iframe.onload = onViewLoadHandler.bind(this);
	
	div.appendChild(iframe);
	document.body.appendChild(div);
	
	return {"div" : div, "frame" : iframe};
}


//Main SDK function invoked from 3rd party integration page on button click 
WorkflowManager.startWorkflow = function(workflowUrl, context) {

	var workflowId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
			    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r&0x3|0x8);
			    return v.toString(16);
		  });

	
    var workflow = {
    	"workflowId" : workflowId,
    	"context": context,
    	"nextStepUrl" : workflowUrl,
    	"contextType" : "application/object"
    };
    this.workflows.push(workflow);
    if(!this.activeWorkflow) {
        this.activeWorkflow = workflow;
        //TODO mainWebView->setVisible(true);
        this.mainView.frame.src =  workflowUrl;
    } else {
        workflowQueue.unshift(workflow);
    }
}


WorkflowManager.next = function(workflowId, nextStepUrl, context, contextType) {

	var workflow = this.getWorkflow(workflowId);

    if(workflow) {
        workflow.nextStepUrl=nextStepUrl;
        workflow.context =context;
        workflow.contextType = contextType;

        if(!this.activeWorkflow) {
            // if there is no active workflow,
            // remove from secondary view and background workflows
            //make this workflow active
            this.unregisterBackgroundWorkflow(workflow);
            this.activeWorkflow = workflow;
        }

        if(workflow == this.activeWorkflow) {
            // if this workflow is currently active, proceed to the next step
            this.mainView.frame.src = nextStepUrl;
            //connect(mainWebView, SIGNAL(loadFinished(bool)), this, SLOT(workflowContinued()));
        } else {
            // if this workflow is not currently active, enqueue it
            this.workflowQueue.unshift(workflow);
        }

    }

  };
  
  /**
   * Moves the current workflow to the background.
   *
   * @brief WorkflowManager::waitAndCheckProgress
   * @param workflowId
   * @param progressUrl
   * @param context
   */
  WorkflowManager.waitAndCheckProgress = function(workflowId, progressUrl, context, contextType)
  {
      console.log("Checking current progress for workflow " + workflowId
                + " using callback url " + progressUrl);

  	 var workflow = this.getWorkflow(workflowId);

      if(workflow) {
    	  //this.secondaryView.div.visible=true;
          workflow.progressUrl=progressUrl;
          workflow.context =context;
          workflow.contextType = contextType;

          this.backgroundWorkflows.push(workflow);

          // if secondary view is not initalized, initialize it and connect to our workflow
          // otherwise simply execute javascript to register our workflow with the secondary view
          if(!this.secondaryViewLoaded) {
              //connect(secondaryWebView, SIGNAL(loadFinished(bool)), this, SLOT(onSecondaryViewLoaded()));
              this.secondaryView.frame.src = this.statusWidgetUrl;
          } else {
              this.registerBackgroundWorkflow(workflow);
          }

          // If the current workflow is active
          // make it inactive and activate next workflow in the queue
          if(workflow == this.activeWorkflow) {
              this.activeWorkflow = 0;
              this.dequeWaitingWorkflowAndRun();
          }
      }
  }
  
 
  WorkflowManager.dequeWaitingWorkflowAndRun = function()  {
      if (!this.workflowQueue.length === 0) {
          this.activeWorkflow = this.workflowQueue.pop();
          this.mainView.frame.src =  this.activeWorkflow.nextStepUrl;
          //mainWebView->setVisible(true);
      } else {
          this.activeWorkflow = null;
          //mainWebView->setVisible(false);
      }
  }
  

  WorkflowManager.unregisterBackgroundWorkflow = function(workflow) {
	  
   	  WidgetsApp.setContext(workflow.workflowId, null,null, this.secondaryView);

  }
  
 
  WorkflowManager.registerBackgroundWorkflow = function(workflow) {
  
	    WidgetsApp.setContext(workflow.workflowId, workflow.context, workflow.contextType, this.secondaryView);

  }  
  
  WorkflowManager.onMainViewLoaded = function() {

	    if (this.activeWorkflow) {
			var workflowId = this.activeWorkflow.workflowId;
			var result = WidgetsApp.setContext(workflowId, this.activeWorkflow.context,
					this.activeWorkflow.contextType, this.mainView);
			if (!workflowId) {
				this.activeWorkflow.workflowId = result;
			}
		} 
	}

	WorkflowManager.onSecondaryViewLoaded = function() {

	    if (this.secondaryView && this.secondaryView.frame.src) {
			this.secondaryViewLoaded = true;
			// for each workflow in background workflows
			//   register the workflow with the secondary view by executing javascript setContext or addBackgroundWorkflow
			for (var i = 0; i < this.backgroundWorkflows.length; i++) {
				this.registerBackgroundWorkflow(this.backgroundWorkflows[i]);

			}
		} 
	}
  

  //***********TODO****************//
  WorkflowManager.adjustMainWindow = function(width, height) {

	//TODO alert("adjustMainWindow");
  };
  
  //SDK is posting an event to set context for a specific workflow wigdet 
  WidgetsApp.setContext = function(workflowId, contextData, contextType, view) {
		var message = { "function": "setContext",
				"parameters" : [workflowId, contextData, contextType] };
		view.frame.contentWindow.postMessage(message, '*');
  }
  
  WorkflowManager.getWorkflow = function(workflowId) {
	  var result = null;
	  for (var i = 0; i < this.workflows.length; i++) {
		    var workflow = this.workflows[i];
		    if(workflow.workflowId === workflowId) {
		    	result = workflow;
		    	break;
		    }
		} 
	  return result;
  }
  
  /**
   * Removes workflow from the secondary(background) view.
   * If this workflow is currently active, removes it from the main view.
   *
   * If the workflow queue is not empty, retrieves one workflow from the queue and makes it active.
   *
   *
   * @brief WorkflowManager::completeWorkflow
   * @param workflowId
   */
  WorkflowManager.completeWorkflow = function(workflowId) {
      //qDebug() << "Completing workflow " << workflowId;

      var workflow = this.getWorkflow(workflowId);

      if(workflow == this.activeWorkflow) {
          this.activeWorkflow = 0;
      }

      if(workflow) {
          removeAllInstances(this.workflowQueue, workflow);
          removeAllInstances(this.workflows, workflow);
          this.unregisterBackgroundWorkflow(workflow);

      }

      // If there is no active workflow, deque next one
      if(!this.activeWorkflow) {
          this.dequeWaitingWorkflowAndRun();
      }
  }
  
  function removeAllInstances(arr, item) {
      for(var i = arr.length; i--;) {
          if(arr[i] === item) {
              arr.splice(i, 1);
          }
      }
  }
