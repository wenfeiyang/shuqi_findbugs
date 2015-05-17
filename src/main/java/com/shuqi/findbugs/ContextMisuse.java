package com.shuqi.findbugs;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class ContextMisuse extends OpcodeStackDetector implements StatelessDetector{
	
	static final boolean DEBUG = SystemProperties.getBoolean("cm.debug");
	static final String ANDROID_CONTEXT_SIG = "android.content.Context";
	static final String ANDROID_ACTIVITY_BASE = "android.app.Activity";
	static final String ANDROID_DIALOG_BASE = "android.app.Dialog";
	static final String[] CLASS_FILTER_LIST = new String[] {
		"Dialog", "Activity"
	};
	
	BugReporter bugReporter;
	 
	 public ContextMisuse(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }
	 
	 private boolean isDialogClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 String name = desc.getDottedClassName();
		 return name.equals("android.app.AlertDialog.Builder") || 
				name.endsWith("Dialog") || 
				Util.isSubType(ANDROID_DIALOG_BASE, name);
	 }
	 
	 private boolean isActivityClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 String name = desc.getDottedClassName();
		 return name.endsWith("Activity") || 
				Util.isSubType(ANDROID_ACTIVITY_BASE, name);
	 }
	 
	 private boolean isWidgetClass(ClassDescriptor desc) {
		 return desc.getDottedClassName().startsWith("android.widget") || 
				 desc.getDottedClassName().startsWith("android.view") ||
				 desc.getDottedClassName().startsWith("android.webkit");
	 }
	 
	 private void reportBug(String pattern) {
			BugInstance bug = new BugInstance(this, pattern, HIGH_PRIORITY)
								.addClassAndMethod(this).addSourceLine(this, getPC());
			bug.addInt(getPC());
			bugReporter.reportBug(bug);
			
	 }
  
	@Override
	public void sawOpcode(int seen) {
		switch(seen) {
		case INVOKESPECIAL:
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
			String signature = getMethodDescriptorOperand().getSignature();
			try {
				if (Util.hasParamTypeInMethod(signature, ANDROID_CONTEXT_SIG)) {
					int slot = Util.getParameterStackSlot(signature, ANDROID_CONTEXT_SIG);
						Item item = stack.getStackItem(slot);
						boolean is_oper_dialog = isDialogClass(getMethodDescriptorOperand().getClassDescriptor());
						if (is_oper_dialog) {
							// Dialog should use Activity Context
							if (!(isActivityClass(Util.toClassDescriptor(item.getSignature())))) {
								int num_sig = Util.getParameterCount(getMethodDescriptor().getSignature());
								int register = item.getRegisterNumber();
								// Context is not passed through method parameter
								if (register < 0 || register >= (seen == INVOKESTATIC ? num_sig : num_sig+1)) {
									reportBug("OOM_MISUSE_ACTIVITY_CONTEXT");
								}
							}	
						} else if (! isWidgetClass(getMethodDescriptorOperand().getClassDescriptor())) {
							// Should use Application Context as much as possible
							if (isActivityClass(Util.toClassDescriptor(item.getSignature()))) {
								reportBug("OOM_MISUSE_APPLICATION_CONTEXT");
							}
					}
				}
			} catch (ClassNotFoundException e) {
				if (DEBUG) {
					e.printStackTrace();
				}
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
