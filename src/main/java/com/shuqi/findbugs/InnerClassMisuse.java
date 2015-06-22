package com.shuqi.findbugs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	static Map<String, String> PERMITED_OUTER_CLASS_MAP = new LinkedHashMap<String, String>() {{
		put("android.app.Activity", "ACTIVITY");
		put("android.view.View", "VIEW");
		put("android.app.Dialog", "DIALOG");
		put("android.widget.Toast", "TOAST");
	}};
	
	private BugReporter bugReporter;
	
	public InnerClassMisuse(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	private void reportBug(String outer_base, boolean is_anonymous, String class_name, int priority) {
		String pattern = "OOM_" + (is_anonymous ? "ANONYMOUS_" : "NON_STATIC_") + "INNER_CLASS_IN_" + PERMITED_OUTER_CLASS_MAP.get(outer_base);
		bugReporter.reportBug(
				new BugInstance(this, pattern, priority).addClass(class_name));
	}

	@Override
	public void sawOpcode(int seen) {
		
	}
	
	private boolean isStatic(InnerClass obj) {
		return ((obj.getInnerAccessFlags() & Constants.ACC_STATIC) != 0);
	}

	@Override
	public void visit(InnerClass obj) {
		if (!isStatic(obj)) {
			try {
				String inner_class = getConstantPool().getConstantString(obj.getInnerClassIndex(), CONSTANT_Class);
				Matcher m = Pattern.compile("(.*)\\$[0-9]*$").matcher(inner_class);
				String outer_class = m.matches() ? m.group(1) : getConstantPool().getConstantString(obj.getOuterClassIndex(), CONSTANT_Class);
				boolean isAnonymousInnerClass = m.matches();
				if (getClassName().equals(outer_class)) {
					if (Util.isSubType("android.os.Handler", inner_class)) {
						bugReporter.reportBug(
								new BugInstance(this, "OOM_NON_STATIC_INNER_HANDLER", HIGH_PRIORITY).addClass(inner_class));
					} else {
						for (String base_type: PERMITED_OUTER_CLASS_MAP.keySet()) {
							if (Util.isSubType(base_type, ClassName.toDottedClassName(outer_class))) {
								reportBug(base_type, isAnonymousInnerClass, inner_class, NORMAL_PRIORITY);
								return;
							}
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
