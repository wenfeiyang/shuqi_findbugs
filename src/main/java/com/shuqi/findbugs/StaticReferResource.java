package com.shuqi.findbugs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.ObjectType;

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
	private static Set<ObjectType> PERMITTED_STATIC_REFER_BASE_TYPE = new LinkedHashSet<ObjectType>();
	
	private static String valid_prefix_pattern = "com\\..*|android\\..*|org\\..*";
	
	private static List<ClassDescriptor> permitted_static_ref_class = new ArrayList<ClassDescriptor>();
	
	static {
		for (String base_name : PERMITTED_STATIC_REFER_BASE_NAME) {
			ObjectType type = ObjectType.getInstance(base_name);
			if (type != null) {
				 PERMITTED_STATIC_REFER_BASE_TYPE.add(type);
			}
		}
	}
	
	 BugReporter bugReporter;

	 public StaticReferResource(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }
	 
	 private boolean isAllowedStaticField(String field_dot_class_name) {
		 if (field_dot_class_name.matches(valid_prefix_pattern)) {
			 ObjectType target_type = ObjectType.getInstance(field_dot_class_name);
			 if (target_type != null) {
				 for (ObjectType base_type: PERMITTED_STATIC_REFER_BASE_TYPE) {
					 try {
						 if (DEBUG) {
							 System.out.println("base type " + base_type.getClassName());
							 System.out.println("target type " + target_type.getClassName());
						 }
						if (target_type.isAssignmentCompatibleWith(base_type)) {	
							 return false;
						 }
					} catch (ClassNotFoundException e) {
						 if (DEBUG) {
							 e.printStackTrace();
						 }
					}
				 }
			 }
			 for (String base_name: PERMITTED_STATIC_REFER_BASE_NAME) {
				 if (field_dot_class_name == base_name) {
					 return false;
				 }
			 }
		 }
		 return true;
	 }
	 
	 private void reportBug(Field obj) {
			BugInstance bug = new BugInstance(this,"OOM_STATIC_REFER_RESOURCE", HIGH_PRIORITY)
								.addClass(this)
								.addField(getClassName(), obj.getName(), obj.getSignature(), true);
			bug.addInt(getPC());
			bugReporter.reportBug(bug);	
	 }
	 
	 private void reportBug() {
			BugInstance bug = new BugInstance(this,"OOM_STATIC_REFER_RESOURCE", HIGH_PRIORITY)
								.addClassAndMethod(this).addSourceLine(this, getPC());
			bug.addInt(getPC());
			bugReporter.reportBug(bug);
			
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
		        		reportBug();
		        	}
	        	}
        	}
        	break;
        default:
            break;
        }
		
	}
	
	
}
