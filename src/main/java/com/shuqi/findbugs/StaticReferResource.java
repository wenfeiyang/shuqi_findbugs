package com.shuqi.findbugs;

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.util.ClassName;

public class StaticReferResource extends OpcodeStackDetector implements StatelessDetector {
	
	static final boolean DEBUG = SystemProperties.getBoolean("srr.debug");
	
	private static String[] PERMITTED_STATIC_REFER_BASE_NAME = new String[] {
		"android.app.Activity",
		"android.graphics.Bitmap",
		"android.graphics.drawable.Drawable",
		"android.widget.ImageView",
		"android.app.Dialog"
	};
	private Map<ClassDescriptor, BugInstance> protential_bug_map = new HashMap<ClassDescriptor, BugInstance>();
	
	@Override
	public boolean shouldVisit(JavaClass jclass) {
        for (int i = 0; i < jclass.getConstantPool().getLength(); ++i) {
            Constant constant = jclass.getConstantPool().getConstant(i);
            if (constant instanceof ConstantNameAndType) {
            	ConstantNameAndType cc = (ConstantNameAndType) constant;
            	String signature = cc.getSignature(getConstantPool());
            	if (signature.contains("Activity") ||
            			signature.contains("Bitmap") ||
            			signature.contains("Drawable") ||
            			signature.contains("Image")) {
            		return true;
            	}
            }
        }
		return super.shouldVisit(jclass);
	}

	private final BugAccumulator accumulator;
	private final BugReporter reporter;

	 public StaticReferResource(BugReporter bugReporter) {
		 reporter = bugReporter;
		 accumulator = new BugAccumulator(bugReporter);
	 }
	 
	 private boolean isAllowedStaticField(String field_dot_class_name) {
		 for (String base_name: PERMITTED_STATIC_REFER_BASE_NAME) {
			 if (field_dot_class_name == base_name) {
				 return false;
			 }
		 }
		 for (String base_type: PERMITTED_STATIC_REFER_BASE_NAME) {
			 try {
				if (Util.isSubType(base_type, field_dot_class_name)) {	
					 return false;
				 }
			} catch (ClassNotFoundException e) {
				 if (DEBUG) {
					 e.printStackTrace();
				 }
			}
		 }
		 return true;
	 }
	 
	 

//	@Override
//	public void visit(Field obj) {
//		AnalysisContext context = AnalysisContext.currentAnalysisContext();
//		context.getFieldSummary().getSummary(getXField());
//		XField xfield = XFactory.createXField(getClassName(), obj);
//		if (xfield.isResolved() && xfield.isReferenceType() && xfield.isStatic()) {
//			try {
//				Item item = context.getFieldSummary().getSummary(xfield);
//				JavaClass jclass = item.getJavaClass();
//				if (item != null) {
//					for (Field field : jclass.getFields()) {
//						XField rfield = XFactory.createXField(jclass, field);
//						if (rfield.isReferenceType()) {
//							ClassDescriptor class_desc = DescriptorFactory.createClassDescriptorFromFieldSignature(obj.getSignature());
//							if (class_desc != null && !isAllowedStaticField(class_desc.getDottedClassName())) {
//								reporter.reportBug(
//										new BugInstance(this,"OOM_STATIC_REFER_RESOURCE", HIGH_PRIORITY)
//										.addClass(getClassName()).addField(xfield).addSourceLine(this, getPC()));
//							}
//						}
//					}
//				}
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//	}

	@Override
	public void sawOpcode(int seen) {
        switch(seen) {
        case PUTSTATIC:
        	XField xfield = getXFieldOperand();
        	if (ACONST_NULL != getPrevOpcode(1) && xfield.isResolved() && xfield.isReferenceType()) {
	        	ClassDescriptor class_descriptor = DescriptorFactory
	        			.createClassDescriptorFromFieldSignature(xfield.getSignature());
	        	if (DEBUG) {
	        		System.out.println("statci field type " + class_descriptor.getDottedClassName());
	        	}
	        	if (class_descriptor != null && !isAllowedStaticField(class_descriptor.getDottedClassName())) {
	        		accumulator.accumulateBug(new BugInstance(this,"OOM_STATIC_REFER_RESOURCE", HIGH_PRIORITY)
	        		.addClassAndMethod(this).addField(xfield).addSourceLine(this, getPC()), this);
	        	}
        	}
        	break;
        default:
            break;
        }
		
	}

	@Override
	public void visitAfter(JavaClass obj) {
		accumulator.reportAccumulatedBugs();
		super.visitAfter(obj);
	}
}
