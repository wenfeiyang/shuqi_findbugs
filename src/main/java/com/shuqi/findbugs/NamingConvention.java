package com.shuqi.findbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;

public class NamingConvention implements Detector, StatelessDetector {
	
	private final static String ACTIVITY_BASE = "android.app.Activity";
	private final static String SERVICE_BASE = "android.app.Service";
	private final static String DIALOG_BASE = "android.app.Dialog";
	private final static String BROADCASTRECEIVER_BASE = "android.content.BroadcastReceiver";
	private final static String CONTENTPROVIDER_BASE = "android.content.ContentProvider";
	private final static String TOAST_BASE = "android.widget.Toast";
	private final static String VIEW_BASE = "android.view.View";
	
	static final boolean DEBUG = SystemProperties.getBoolean("nc.debug");
	
	private final BugReporter bugReporter;
	
	public NamingConvention(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visitClassContext(ClassContext classContext) {
		String className = classContext.getClassDescriptor().getDottedClassName();
		try {
			if (Util.isSubType(ACTIVITY_BASE, className)) {
				if (! className.endsWith("Activity")) {
					bugReporter.reportBug(new BugInstance(this, "NM_ACTIVITY_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}
			} else if(Util.isSubType(SERVICE_BASE, className)) {
				if (! className.endsWith("Service")) {
					bugReporter.reportBug(new BugInstance(this, "NM_SERVICE_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}
			} else if(Util.isSubType(BROADCASTRECEIVER_BASE, className)) {
				if (! className.endsWith("BroadcastReceiver")) {
					bugReporter.reportBug(new BugInstance(this, "NM_BROADCASTRECEIVER_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}	
			} else if(Util.isSubType(CONTENTPROVIDER_BASE, className)) {
				if (! className.endsWith("ContentProvider")) {
					bugReporter.reportBug(new BugInstance(this, "NM_CONTENTPROVIDER_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}	
			} else if(Util.isSubType(DIALOG_BASE, className)) {
				if (! className.endsWith("Dialog")) {
					bugReporter.reportBug(new BugInstance(this, "NM_DIALOG_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}
			} else if(Util.isSubType(TOAST_BASE, className)) {
				if (! className.endsWith("Toast")) {
					bugReporter.reportBug(new BugInstance(this, "NM_TOAST_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}
			} else if(Util.isSubType(VIEW_BASE, className)) {
				if (! className.endsWith("View")) {
					bugReporter.reportBug(new BugInstance(this, "NM_VIEW_CLASS_NAMING", NORMAL_PRIORITY)
					.addClass(classContext.getClassDescriptor()));
				}
			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}		
	}

	@Override
	public void report() {
		
	}
	
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

}
