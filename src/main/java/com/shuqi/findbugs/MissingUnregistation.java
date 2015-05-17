package com.shuqi.findbugs;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.StatelessDetector;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;

import org.apache.bcel.classfile.ConstantNameAndType;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class MissingUnregistation extends OpcodeStackDetector implements StatelessDetector {
	
	 private String REGISTER_RECEIVER = "registerReceiver";
	 private String UNREGISTER_RECEIVER = "unregisterReceiver";
	 private String[] VALID_REGISTER_SIGS = new String[] {
			 "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;",
			 "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;Ljava/lang/String;Landroid/os/Handler;)Landroid/content/Intent;"
	 };
	 private String VALID_UNREGISTER_SIG = "(Landroid/content/BroadcastReceiver;)V";
	 private List<BugInstance> bugs = new ArrayList<BugInstance>();
	 private int register_count = 0;
	 private int unregister_count = 0;
	
	 BugReporter bugReporter;
	 private boolean should_visit_code = false;
	 
	 public MissingUnregistation(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }
	 
	 private boolean isValidRegisterSignature(String signature) {
		 for (String item : VALID_REGISTER_SIGS) {
			 if (item.equals(signature)) {
				 return true;
			 }
		 }
		 return false;
	 }

	@Override
	public boolean shouldVisitCode(Code obj) {
		return should_visit_code;
	}


	@Override
	public boolean shouldVisit(JavaClass obj) {
		return super.shouldVisit(obj);
	}

	@Override
	public void visit(ConstantNameAndType obj) {
		if (REGISTER_RECEIVER.equals(obj.getName(getConstantPool()))) {
			should_visit_code = true;
		}
		super.visit(obj);
	}
	
	@Override
	public void visit(Code obj) {
		super.visit(obj);
	}
	
	private BugInstance createBugInstance() {
		BugInstance bug =new BugInstance(this,"OOM_MISSING_UNREGISTATION", HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(this, getPC());
		bug.addInt(getPC());
		return bug;
	}

	@Override
	public void sawOpcode(int seen) {	
		switch(seen) {
		case INVOKEVIRTUAL:
			MethodDescriptor des = getMethodDescriptorOperand();
			if (REGISTER_RECEIVER.equals(des.getName())
					&& isValidRegisterSignature(des.getSignature())) {
				bugs.add(createBugInstance());
				register_count++;
			} else if (UNREGISTER_RECEIVER.equals(des.getName())
					&& VALID_UNREGISTER_SIG.equals(des.getSignature())) {
				unregister_count++;
			}
			break;
		default:
			break;
		}	
	}
	
	@Override
	public void visitAfter(JavaClass obj) {
		reportMissingUnregisterBug();
		super.visitAfter(obj);
	}
	
	private void reportMissingUnregisterBug() {
		if (register_count > unregister_count) {
			for (int i = unregister_count; i < bugs.size(); i++) {
				bugReporter.reportBug(bugs.get(i));
			}
		}
		bugs = new ArrayList<BugInstance>();
		register_count = 0;
		unregister_count = 0;
	}

}
