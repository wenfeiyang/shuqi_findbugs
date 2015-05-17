package com.shuqi.findbugs;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;

public class ContextMisuse extends OpcodeStackDetector implements StatelessDetector{
	
	static final boolean DEBUG = SystemProperties.getBoolean("cm.debug");
	static final String ANDROID_CONTEXT_SIG = "android.content.Context";
	static final String ANDROID_ACTIVITY_BASE = "android.app.Activity";
	static final String ANDROID_DIALOG_BASE = "android.app.Dialog";
	static final String[] CLASS_FILTER_LIST = new String[] {
		"Dialog", "Activity"
	};
	
	BugReporter bugReporter;

	private static String[] PRESCREEN_CLASS_LIST = new String[] {
		"android.content.ContextWrapper"
	};
	 
	 public ContextMisuse(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }
	 
	 private void checkReferSelfContext() {
		 //this.getClassDescriptor();
		 //Util.isSubType(baseType, possibleSubtype)
	 }
	 
	 private boolean isDialogClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 return Util.isSubType(ANDROID_DIALOG_BASE, desc.getDottedClassName());
	 }
	 
	 private boolean isActivityClass(ClassDescriptor desc) 
			 throws ClassNotFoundException {
		 return Util.isSubType(ANDROID_ACTIVITY_BASE, desc.getDottedClassName());
	 }
	 
	 private boolean shouldVisit(ClassDescriptor desc) {
		 String name = desc.getDottedClassName();
		 for (String key : CLASS_FILTER_LIST) {
			 if (name.contains(key)) {
				 return true;
			 }
		 }
		 return false;
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
			if (shouldVisit(getMethodDescriptorOperand().getClassDescriptor())) {
				String signature = getMethodDescriptorOperand().getSignature();
				if (Util.hasParamTypeInMethod(signature, ANDROID_CONTEXT_SIG)) {
					int slot = Util.getParameterFirstSlot(signature, ANDROID_CONTEXT_SIG);
					int depth = stack.getStackDepth();
					int stack_slot = (seen == INVOKESTATIC ? (depth-1-slot) : (depth-2-slot));
	//				System.out.println(signature);
	//				System.out.println(stack_slot);
	//				System.out.println(Util.toClassDescriptor(item.getSignature()));
					try {
						Item item = stack.getStackItem(stack_slot);
						boolean is_oper_dialog = isDialogClass(getMethodDescriptorOperand().getClassDescriptor());
						if (is_oper_dialog) {
							// Dialog should use Activity Context
							if (!(isActivityClass(Util.toClassDescriptor(item.getSignature())))) {
								reportBug("OOM_MISUSE_APPLICATION_CONTEXT");
							}
							
						} else {
							// Activity should use Application Context
							if (isActivityClass(Util.toClassDescriptor(item.getSignature()))) {
								reportBug("OOM_MISUSE_ACTIVITY_CONTEXT");
							}
						}				
					} catch (ClassNotFoundException e) {
						//System.out.println(signature);
						System.out.println(getMethodDescriptorOperand());
						System.out.println(getClassName());
	//					if (DEBUG) {
							e.printStackTrace();
	//					}
					}
				}
			}
			break;
			
		default:
			break;
		}
		//this.getStack().getStackItem(0).getRegisterNumber();
	}

//	@Override
//	public boolean shouldVisit(JavaClass jclass) {
//        boolean sawTargetClass = false;
//        for (int i = 0; i < jclass.getConstantPool().getLength(); ++i) {
//            Constant constant = jclass.getConstantPool().getConstant(i);
//            String className = null;
//            if (constant instanceof ConstantMethodref) {
//                ConstantMethodref cmr = (ConstantMethodref) constant;
//
//                int classIndex = cmr.getClassIndex();
//                className = jclass.getConstantPool().getConstantString(classIndex, Constants.CONSTANT_Class);
//            } else if (constant instanceof ConstantInterfaceMethodref) {
//                ConstantInterfaceMethodref cmr = (ConstantInterfaceMethodref) constant;
//
//                int classIndex = cmr.getClassIndex();
//                className = jclass.getConstantPool().getConstantString(classIndex, Constants.CONSTANT_Class);
//            }
//
//            if (className != null) {
////                if (DEBUG) {
////                    System.out.println("FindOpenDataSource: saw class " + className);
////                }
//
//                for (String aPRESCREEN_CLASS_LIST : PRESCREEN_CLASS_LIST ) {
//                    if (ClassName.toDottedClassName(className).equals(aPRESCREEN_CLASS_LIST)) {
//                        sawTargetClass = true;
//                        break;
//                    }
//                }
//            }
//        }
//        return sawTargetClass;
//	}

}
