package com.shuqi.findbugs;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.InnerClass;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;

public class InnerClassMisuse  extends OpcodeStackDetector implements StatelessDetector {
	
	static final boolean DEBUG = SystemProperties.getBoolean("icm.debug");
	static Map<String, String> OUTER_CLASS_PERMITTED_MAP = new LinkedHashMap<String, String>() {{
		put("android.app.Activity", "OOM_NON_STATIC_INNER_CLASS_IN_ACTIVITY");
		put("android.view.View", "OOM_NON_STATIC_INNER_CLASS_IN_VIEW");
		put("android.app.Dialog", "OOM_NON_STATIC_INNER_CLASS_IN_DIALOG");
		put("android.widget.Toast", "OOM_NON_STATIC_INNER_CLASS_IN_TOAST");
	}};
	
	private BugReporter bugReporter;
	
	public InnerClassMisuse(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	
	private void reportBug(String bug_pattern, String class_name) {
		bugReporter.reportBug(
				new BugInstance(this, bug_pattern, HIGH_PRIORITY)
				.addClass(class_name));
	}

	@Override
	public void sawOpcode(int seen) {
		
	}
	
	private boolean isStatic(InnerClass obj) {
		return ((obj.getInnerAccessFlags() & Constants.ACC_STATIC) != 0);
	}

	@Override
	public void visit(InnerClass obj) {
		if (!isStatic(obj) && !getClassDescriptor().isAnonymousClass()) {
			try {
				String inner_class = getConstantPool().getConstantString(obj.getInnerClassIndex(), CONSTANT_Class);
				String outer_class = getConstantPool().getConstantString(obj.getOuterClassIndex(), CONSTANT_Class);
				if (outer_class != null && outer_class.equals(getClassName())) {
							for (String base_type: OUTER_CLASS_PERMITTED_MAP.keySet()) {
								if (Util.isSubType(base_type, ClassName.toDottedClassName(outer_class))) {
									reportBug(OUTER_CLASS_PERMITTED_MAP.get(base_type), inner_class);
									return;
								}
							}
						}
				} catch(ClassFormatException e) {
					if (DEBUG) {
						e.printStackTrace();
					}
				} catch (ClassNotFoundException e) {
					AnalysisContext.reportMissingClass(e);
				}
		}
	}
}
