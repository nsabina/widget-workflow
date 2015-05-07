package com.nsabina.samples.oneclickshare.javafx;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

import javax.swing.JFrame;

import netscape.javascript.JSObject;

public class WorkflowManager {

	static class WebView {

		private JFXPanel panel;
		private WebEngine engine;
		private void loadUrl(final String url) {
		    Platform.runLater(new Runnable() {
		        @Override 
		        public void run() {
		            String tmp = toURL(url);
		 
		            if (tmp == null) {
		                tmp = toURL("http://" + url);
		            }
		 
		            engine.load(tmp);
		        }
		    });
		}
	    private static String toURL(String str) {
	        try {
	            return new URL(str).toExternalForm();
	        } catch (MalformedURLException exception) {
	                return null;
	        }
	    }
		

	}

	private static int DEFAULT_LEFT_MARGIN = 200;
	private static int DEFAULT_TOP_MARGIN = 100;


	private static Color TRANSLUCENT_COLOR = new Color(1f, 0f, 0f, .5f);;

	private String statusWidgetUrl;
	private JFrame parentWindow;
	private Queue<Workflow> workflowQueue = new LinkedList<>();
	private Set<Workflow> workflows = new HashSet<>();
	private Set<Workflow> backgroundWorkflows = new HashSet<>();
	private Workflow activeWorkflow;

	private WebView mainWebView;
	private WebView secondaryWebView;
	private boolean secondaryViewLoaded;

	public WorkflowManager(JFrame parentWindow) {

		this.parentWindow = parentWindow;		

		mainWebView = createWebView(new ChangeListener<State>() {

			@Override
			public void changed(ObservableValue<? extends State> ov,
					State oldState, State newState) {
				if (newState == State.SUCCEEDED) {
					System.out.println("Load completed");
					addBridge(mainWebView);
					onMainViewLoaded();
				}
			}
		},
		200, 100, 0, 0, Color.GREEN);
		
		secondaryWebView = createWebView(new ChangeListener<State>() {

			@Override
			public void changed(ObservableValue<? extends State> ov,
					State oldState, State newState) {
				if (newState == State.SUCCEEDED) {
					System.out.println("Load completed");
					addBridge(secondaryWebView);
					onSecondaryViewLoaded();
				}
			}

		},
		1024 - 200, 50, 200, 100, Color.GREEN);

	}

	protected void onSecondaryViewLoaded() {
	    this.secondaryViewLoaded = true;
	    // for each workflow in background workflows
	    // register the workflow with the secondary view by executing javascript setContext or addBackgroundWorkflow
	    for (Workflow workflow:backgroundWorkflows) {
	        registerBackgroundWorkflow(workflow);
	    }
		
	}

	private void registerBackgroundWorkflow(Workflow workflow) {
	    String javascript = "WidgetsApp.setContext('" + workflow.getWorkflowId() + "', '" + workflow.getContext() + "')";
	    Object result = secondaryWebView.engine.executeScript(javascript);
	    if(result.toString() != workflow.getWorkflowId()) {
	    	System.out.println("Checking progress... Ouch! Workflow id " +  result.toString() + " does not match " + workflow.getWorkflowId());
	    }
		
	}

	private void addBridge(WebView webView) {
		JSObject jsobj = (JSObject) webView.engine.executeScript("window");
		jsobj.setMember("WorkflowManager", this);
	}
		
	private void onMainViewLoaded() {
		
		String workflowId = activeWorkflow.getWorkflowId();
		String javascript = "WidgetsApp.setContext(" + (workflowId!=null?"'" + workflowId + "'":"null") + ", '" + activeWorkflow.getContext() + "')";

		Object result = mainWebView.engine.executeScript(javascript);

		
		if(workflowId == null) {
			activeWorkflow.setWorkflowId(result.toString()); 
		} else if(result== null || !workflowId.equals(result.toString())) { 
			System.out.println("Workflow continued. But ouch! Workflow id " + result.toString() + " does not match " + workflowId);
		}
	}

	private WebView createWebView(final ChangeListener<State> onCompleteListener,
			Integer leftMargin,
			Integer topMargin, Integer width, Integer height,
			Color backgroundColor) {

		final WebView webView = new WebView();
		webView.panel = new JFXPanel();
		webView.panel.setBounds(leftMargin == null ? DEFAULT_LEFT_MARGIN
				: leftMargin, topMargin == null ? DEFAULT_TOP_MARGIN
				: topMargin, width, height);
		webView.panel.setBackground(backgroundColor == null ? TRANSLUCENT_COLOR
				: backgroundColor);
		webView.panel.setVisible(false);
		parentWindow.add(webView.panel);

		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				javafx.scene.web.WebView view = new javafx.scene.web.WebView();
				// view.setBlendMode(BlendMode.DIFFERENCE);
				webView.engine = view.getEngine();
				webView.engine.getLoadWorker().stateProperty()
						.addListener(onCompleteListener);

				Scene scene = new Scene(view);
				webView.panel.setScene(scene);				
			}
		});
		return webView;
	}

	public void startWorkflow(final String workflowUrl, String context) {
	    Workflow workflow = new Workflow(context);
	    workflow.setNextStepUrl(workflowUrl);
	    workflows.add(workflow);
	    if(activeWorkflow == null) {
	         activeWorkflow = workflow;
	         mainWebView.panel.setVisible(true); 
	         mainWebView.loadUrl(workflowUrl); 
	    } else {
	        workflows.add(workflow);
	    }
		
	}
	
	/**
	 * Executes next workflow step.
	 *
	 * @brief WorkflowManager::next
	 * @param workflowId
	 * @param nextStepUrl
	 * @param context
	 */
	public void next(String workflowId, String nextStepUrl, Object context, String contextType)
	{
	    System.out.println("Next step for workflow " + workflowId + " is  to load " + context + " from url " + nextStepUrl);

	    Workflow workflow = getWorkflow(workflowId);

	    if(workflow != null) {
	        workflow.setNextStepUrl(nextStepUrl);
	        workflow.setContext(context);

	        if(activeWorkflow == null) {
	            // if there is no active workflow,
	            // remove from secondary view and background workflows
	            //make this workflow active
	            unregisterBackgroundWorkflow(workflow);
	            activeWorkflow = workflow;
	        }

	        if(workflow == activeWorkflow) {
	            // if this workflow is currently active, proceed to the next step
	            mainWebView.loadUrl(nextStepUrl);
	        } else {
	            // if this workflow is not currently active, enqueue it
	            workflowQueue.add(workflow);
	        }

	        mainWebView.panel.setVisible(true);
	    }
	}
	
	
	/**
	 * Moves the current workflow to the background.
	 *
	 * @brief WorkflowManager.waitAndCheckProgress
	 * @param workflowId
	 * @param progressUrl
	 * @param context
	 */
	public void waitAndCheckProgress(String workflowId, String progressUrl, String context, String contextType)
	{
		System.out.println("Checking current progress for workflow " + workflowId + " using callback url " + progressUrl);


	    Workflow workflow = getWorkflow(workflowId);

	    if(workflow!=null) {
	        secondaryWebView.panel.setVisible(true);
	        workflow.setContext(context);

	        backgroundWorkflows.add(workflow);

	        // if secondary view is not initalized, initialize it and connect to our workflow
	        // otherwise simply execute javascript to register our workflow with the secondary view
	        if(!secondaryViewLoaded) {
	            secondaryWebView.loadUrl(statusWidgetUrl);
	        } else {
	            registerBackgroundWorkflow(workflow);
	        }

	        // If the current workflow is active
	        // make it inactive and activate next workflow in the queue
	        if(workflow == activeWorkflow) {
	            activeWorkflow = null;
	            dequeWaitingWorkflowAndRun();
	        }
	    }
	}
	
	private void dequeWaitingWorkflowAndRun() {
	    if (!workflowQueue.isEmpty()) {
	        activeWorkflow = workflowQueue.remove();
	        mainWebView.loadUrl(activeWorkflow.getNextStepUrl());
	        mainWebView.panel.setVisible(true);
	    } else {
	        activeWorkflow = null;
	        mainWebView.panel.setVisible(false); 
	    }
		
	}

	private void unregisterBackgroundWorkflow(Workflow workflow) {
	    if(backgroundWorkflows.remove(workflow)) {
	        // TODO: introduce function WidgetsApp.removeBackgroundWorkflow(workflowId) instead of using setContext(workflowId, null)
	        String javascript = "WidgetsApp.setContext('" + workflow.getWorkflowId() + "', null)";
	        System.out.println("Unregistering background workflow " +  workflow.getWorkflowId() + " with javascript " + javascript);
	        Object result = secondaryWebView.engine.executeScript(javascript);
	        if(!result.toString().equals(workflow.getWorkflowId())) {
	        	System.out.println("Checking progress... Ouch! Workflow id " + result.toString() + " does not match " + workflow.getWorkflowId());
	        }
	    }

	    if(backgroundWorkflows.isEmpty()) {
	        secondaryWebView.panel.setVisible(false);
	    }
		
	}

	private Workflow getWorkflow(String workflowId)
	{
	    Workflow result = null;
	    for(Workflow workflow:workflows) {
	        if(workflow.getWorkflowId().equals(workflowId)) {
	            result = workflow;
	            break;
	        }
	    }
	    return result;
	}
	
	public void adjustMainWindow(int width, int height, int position)
	{
	    System.out.println( "Adjusting width to " + width);
	    mainWebView.panel.setBounds(mainWebView.panel.getX(), mainWebView.panel.getY(), width, height);
	}

	public void setStatusWidgetUrl(String statusWidgetUrl) {
		this.statusWidgetUrl = statusWidgetUrl;
		
	}
	
	public void completeWorkflow (String workflowId) {
		System.out.println("Completing workflow " + workflowId);

	    Workflow workflow = getWorkflow(workflowId);

	    if(workflow == activeWorkflow) {
	        activeWorkflow = null;
	    }

	    if(workflow != null) {
	        workflowQueue.remove(workflow);
	        workflows.remove(workflow);
	        unregisterBackgroundWorkflow(workflow);

	        workflow = null;
	    }

	    // If there is no active workflow, deque next one
	    if(activeWorkflow == null) {
	        dequeWaitingWorkflowAndRun();
	    }
	}
	
}
