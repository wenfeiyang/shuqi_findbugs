package com.shuqi.findbugs;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class UseAdapterConvertView extends OpcodeStackDetector implements StatelessDetector {
	
	private String VALID_GETVIEW_SIG = "(ILandroid/view/View;Landroid/view/ViewGroup;)Landroid/view/View;";
	private int last_load_register = -1;
	private boolean report_mask = true;
	private Set<Integer> tracking_registers = new HashSet<Integer>();

	@Override
	public void sawOpcode(int seen) {
		switch(seen) {
		case ALOAD_0:
		case ALOAD_1:
		case ALOAD_2:
		case ALOAD_3:
		case ALOAD:
			last_load_register = getRegisterOperand();
			break;
		case ASTORE:
		case ASTORE_0:
		case ASTORE_1:
		case ASTORE_2:
		case ASTORE_3:
			if (ALOAD == getPrevOpcode(1) || ALOAD_2 == getPrevOpcode(1)) {
				if (tracking_registers.contains(last_load_register)) {
					tracking_registers.add(getRegisterOperand());
				}
			}
			break;
		case IFNONNULL:
		case IFNULL:
			if (ALOAD == getPrevOpcode(1) || ALOAD_2 == getPrevOpcode(1)) {
				if (tracking_registers.contains(last_load_register)) {
					report_mask = false;
				}
			}
			break;
		default:
			break;
		}
	}
	
	 BugReporter bugReporter;

	 public UseAdapterConvertView(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }


	@Override
	public boolean shouldVisitCode(Code obj) {
		report_mask = true;
		tracking_registers = new HashSet<Integer>(){{
			add(2);
		}};	
		return "getView".equals(getMethodName()) && VALID_GETVIEW_SIG.equals(getMethodSig());
	}

	@Override
	public void visitAfter(Code obj) {
		if (report_mask) {
			BugInstance bug = new BugInstance(this,"OOM_NOT_USE_CONVERTVIEW", HIGH_PRIORITY)
					.addClassAndMethod(this).addSourceLine(this);
			bugReporter.reportBug(bug);
		}
		super.visitAfter(obj);
	}

	
}
