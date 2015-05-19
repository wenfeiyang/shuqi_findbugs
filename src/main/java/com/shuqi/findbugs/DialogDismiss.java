package com.shuqi.findbugs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;


import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class DialogDismiss extends OpcodeStackDetector implements StatelessDetector {
	
	private static class ProtentialMisuse {
		private MethodDescriptor method;
		private SourceLineAnnotation sourceline;
		
		public ProtentialMisuse(MethodDescriptor method, SourceLineAnnotation location) {
			this.method = method;
			this.sourceline = location;
		}
	}
	
	static final boolean DEBUG = SystemProperties.getBoolean("dd.debug");
	
	private static final String COMMON_METHOD_SIG = "()V";
	private static final String DISMISS_METHOD_NAME = "dismiss";
	private static final String CANCEL_METHOD_NAME = "cancel";
	private static final String SHOW_METHOD_NAME = "show";
	private static final String DIALOG_BASE_CLASS = "android.app.Dialog";
	
	private BugReporter bugReporter;
	private Map<String, Boolean> show_tracking_map;
	private Map<String, Boolean> cancel_tracking_map;
	private Map<String, Boolean> dismiss_tracking_map;
	private Map<String, List<ProtentialMisuse>> possible_bug_map;
	private String last_invoked_field_sig;
	
 	
	@Override
	public void visitAfter(JavaClass obj) {
		reportBugPerClass();
		initialize();
		super.visitAfter(obj);
	}
	
	private void reportBugPerClass() {
		if (DEBUG) {
			if (show_tracking_map.keySet().size() > 0) {
				System.out.println(show_tracking_map);
				System.out.println(cancel_tracking_map);
				System.out.println(dismiss_tracking_map);
			}
		}
		for (String sig: show_tracking_map.keySet()) {
			if (!cancel_tracking_map.containsKey(sig) 
					&& !dismiss_tracking_map.containsKey(sig)) {
				List<ProtentialMisuse> misuse_points  = possible_bug_map.get(sig);
				if (misuse_points.size() > 0) {
					BugInstance bug =new BugInstance(this,"OOM_DIALOG_MISSING_DISMISS_OR_CANCEL", HIGH_PRIORITY)
									.addClass(getClassDescriptor());
					for (ProtentialMisuse misuse_point : misuse_points) {
						bug.addMethod(misuse_point.method).addSourceLine(misuse_point.sourceline);
					}
					bugReporter.reportBug(bug);
				}
			}
		}
	}
	
	private void initialize() {
		show_tracking_map = new HashMap<String, Boolean>();
		cancel_tracking_map = new HashMap<String, Boolean>();
		dismiss_tracking_map = new HashMap<String, Boolean>();
		possible_bug_map = new HashMap<String, List<ProtentialMisuse>>();
		last_invoked_field_sig = null;
	}
 
	public DialogDismiss(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		initialize();
	}
	
	@Override
	public void visit(Code code) {
		last_invoked_field_sig = null;
		super.visit(code);
	}

	@Override
	public void sawOpcode(int seen) {
		switch(seen) {
        case GETSTATIC:
        case GETFIELD:
        	last_invoked_field_sig = getClassConstantOperand() + "/" + getFieldDescriptorOperand().getName();
        	break;
		case INVOKEVIRTUAL:
			MethodDescriptor md = getMethodDescriptorOperand();
			try {
				if (Util.isSubType(DIALOG_BASE_CLASS, md.getClassDescriptor().getDottedClassName())
						&& COMMON_METHOD_SIG.equals(md.getSignature()) && !md.isStatic()) {
						if (last_invoked_field_sig != null) {						
							if (SHOW_METHOD_NAME.equals(md.getName())) {
								show_tracking_map.put(last_invoked_field_sig, true);
			            		if (!possible_bug_map.containsKey(last_invoked_field_sig)) {
			            			possible_bug_map.put(last_invoked_field_sig, new LinkedList<ProtentialMisuse>());
			            		}
			            		possible_bug_map.get(last_invoked_field_sig).add(new ProtentialMisuse(
			            				getMethodDescriptor(), SourceLineAnnotation.fromVisitedInstruction(getClassContext(),
			                                    this, getPC())));
							} else if(CANCEL_METHOD_NAME.equals(md.getName())) {
								cancel_tracking_map.put(last_invoked_field_sig, true);
							} else if(DISMISS_METHOD_NAME.equals(md.getName())) {
								dismiss_tracking_map.put(last_invoked_field_sig, true);
							}
						}		
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
			break;
		default:
			last_invoked_field_sig = null;
			break;
		}
		
	}

}
