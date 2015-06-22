package com.shuqi.findbugs;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class ContextMisuse extends OpcodeStackDetector implements StatelessDetector{
	
	static final boolean DEBUG = SystemProperties.getBoolean("cm.debug");
	static final String ANDROID_CONTEXT_SIG = "android.content.Context";
	static final String ANDROID_ACTIVITY_BASE = "android.app.Activity";
	static final String ANDROID_DIALOG_BASE = "android.app.Dialog";
	static final String[] CLASS_FILTER_LIST = new String[] {
		"Dialog", "Activity"
	};
	static final Set<String> APP_CONTEXT_EXCLUDE_CLASSES = new HashSet<String>();
	
	static {
		try {
			String value = DetectorConfig.getProperty("ContextMisuse.app.exculde_classes");
			if (value != null) {
				for (String class_name: value.split(",")) {
					APP_CONTEXT_EXCLUDE_CLASSES.add(class_name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	BugReporter bugReporter;
	 
	 public ContextMisuse(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }
	 
	 private boolean isDialogClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 String name = desc.getDottedClassName();
		 return name.equals("android.app.AlertDialog$Builder") || 
				name.endsWith("Dialog") || 
				Util.isSubType(ANDROID_DIALOG_BASE, name);
	 }
	 
	 private boolean isActivityClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 String name = desc.getDottedClassName();
		 return name.endsWith("Activity") || 
				Util.isSubType(ANDROID_ACTIVITY_BASE, name);
	 }
	 
	 private boolean isWidgetClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 String className = desc.getDottedClassName();
		 return className.startsWith("android.widget") || 
				 className.startsWith("android.view") ||
				 className.startsWith("android.webkit") ||
				 className.startsWith("android.content.Intent") ||
				 Util.isSubType("android.view.View", className) ||
				 Util.isSubType("android.widget.BaseAdapter", className);
	 }
	 
	 private void reportBug(String pattern) {
			BugInstance bug = new BugInstance(this, pattern, HIGH_PRIORITY)
							.addClass(this).addMethod(getMethodDescriptorOperand()).addSourceLine(this, getPC());
			bug.addInt(getPC());
			bugReporter.reportBug(bug);
			
	 }
  
	@Override
	public void sawOpcode(int seen) {
		switch(seen) {
		case INVOKESPECIAL:
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
			MethodDescriptor method_desc = getMethodDescriptorOperand();
			String signature = method_desc.getSignature();
			try {
				if (Util.hasParamTypeInMethod(signature, ANDROID_CONTEXT_SIG)) {
					int slot = Util.getParameterStackSlot(signature, ANDROID_CONTEXT_SIG);
					Item item = stack.getStackItem(slot);
					boolean is_oper_dialog = isDialogClass(method_desc.getClassDescriptor());
					if (is_oper_dialog) {
						// Dialog should use Activity Context
						if (!(isActivityClass(Util.toClassDescriptor(item.getSignature())))) {
							int num_sig = Util.getParameterCount(getMethodDescriptor().getSignature());
							int register = item.getRegisterNumber();
							XMethod method_returned = item.getReturnValueOf();
							if (method_returned == null ||
									!"android.view.View".equals(method_returned.getClassName()) || 
									!"getContext".equals(method_returned.getName())) {
								// Context is not passed through method parameter
								if (register < 0 || register >= (seen == INVOKESTATIC ? num_sig : num_sig+1)) {
									reportBug("OOM_MISUSE_ACTIVITY_CONTEXT");
								}
							}
						}	
					} else if (!isWidgetClass(getMethodDescriptorOperand().getClassDescriptor())) {
						// Should use Application Context as much as possible
						if (!APP_CONTEXT_EXCLUDE_CLASSES.contains(method_desc.getClassDescriptor().getDottedClassName())) {
							if (isActivityClass(Util.toClassDescriptor(item.getSignature()))) {
								reportBug("OOM_MISUSE_APPLICATION_CONTEXT");
							}
						}
					}
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
			break;	
		default:
			break;
		}
	}

	@Override
	public boolean shouldVisit(JavaClass jclass) {
        for (int i = 0; i < jclass.getConstantPool().getLength(); ++i) {
            Constant constant = jclass.getConstantPool().getConstant(i);
            if (constant instanceof ConstantNameAndType) {
            	ConstantNameAndType cc = (ConstantNameAndType) constant;
            	if (cc.getSignature(getConstantPool()).contains("Landroid/content/Context;")) {
            		return true;
            	}
            }
        }
		return super.shouldVisit(jclass);
	}

}
