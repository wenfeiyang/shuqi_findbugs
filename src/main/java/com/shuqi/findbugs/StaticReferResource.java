package com.shuqi.findbugs;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;

public class StaticReferResource extends OpcodeStackDetector implements StatelessDetector {
	
	static final boolean DEBUG = SystemProperties.getBoolean("srr.debug");
	
	private static String[] PERMITTED_STATIC_REFER_BASE_NAME = new String[] {
		"android.app.Activity",
		"android.graphics.Bitmap",
		"android.graphics.drawable.Drawable"
	};
	
	@Override
	public boolean shouldVisit(JavaClass jclass) {
        for (int i = 0; i < jclass.getConstantPool().getLength(); ++i) {
            Constant constant = jclass.getConstantPool().getConstant(i);
            if (constant instanceof ConstantNameAndType) {
            	ConstantNameAndType cc = (ConstantNameAndType) constant;
            	String signature = cc.getSignature(getConstantPool());
            	if (signature.contains("Activity") ||
            			signature.contains("Bitmap") ||
            			signature.contains("Drawable")) {
            		return true;
            	}
            }
        }
		return super.shouldVisit(jclass);
	}

	private final BugAccumulator accumulator;

	 public StaticReferResource(BugReporter bugReporter) {
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

	@Override
	public void sawOpcode(int seen) {
        switch(seen) {
        case PUTSTATIC:
        	if (ACONST_NULL != getPrevOpcode(1)) {
	        	FieldDescriptor field_descriptor = getFieldDescriptorOperand();
	        	if (field_descriptor.getSignature().endsWith(";")) {
		        	ClassDescriptor class_descriptor = DescriptorFactory.createClassDescriptorFromFieldSignature(field_descriptor.getSignature());
		        	if (DEBUG) {
		        		System.out.println("statci field type " + class_descriptor.getDottedClassName());
		        	}
		        	if (!isAllowedStaticField(class_descriptor.getDottedClassName())) {
		        		accumulator.accumulateBug(new BugInstance(this,"OOM_STATIC_REFER_RESOURCE", HIGH_PRIORITY)
		        		.addClassAndMethod(this).addSourceLine(this, getPC()), this);
		        	}
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
