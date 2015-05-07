package com.nsabina.samples.oneclickshare.javafx;

public class Workflow {
	
	private String workflowId;
	private String nextStepUrl;
	private String progressCheckUrl;
	
	public Workflow(Object context) {
		this.context = context;
		
	}
	
	private Object context;
	public Object getContext() {
		return context;
	}

	public void setContext(Object context) {
		this.context = context;
	}

	public String getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}

	public String getNextStepUrl() {
		return nextStepUrl;
	}

	public void setNextStepUrl(String nextStepUrl) {
		this.nextStepUrl = nextStepUrl;
	}

	public String getProgressCheckUrl() {
		return progressCheckUrl;
	}

	public void setProgressCheckUrl(String progressCheckUrl) {
		this.progressCheckUrl = progressCheckUrl;
	}	

}
