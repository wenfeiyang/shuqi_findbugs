package com.shuqi.findbugs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;


public class BitmapMisuse extends OpcodeStackDetector implements StatelessDetector {
	
	private List<Integer> options_obj_registers = new ArrayList<Integer>();
	private Map<String, List<ProtentialMisuse>> bitmap_field_obj_map = new HashMap<String, List<ProtentialMisuse>>();
	private Map<String, Boolean> bitmap_field_assigned_map = new HashMap<String, Boolean>();
	private Map<String, Boolean> bitmap_field_recycled_map = new HashMap<String, Boolean>();
	
	private static String BITMAP_CLASS_NAME = "android.graphics.Bitmap";
	private static String BITMAPFACTORY_CLASS_NAME = "android.graphics.BitmapFactory$Options";
	private String last_invoked_field_sig = null;
	
	static final boolean DEBUG = SystemProperties.getBoolean("cm.debug");

	private static String[] PRESCREEN_CLASS_LIST = new String[] {
		"Bitmap"
	};
	
	BugReporter bugReporter;
	
	private static class ProtentialMisuse {
		private MethodDescriptor method;
		private SourceLineAnnotation sourceline;
		
		public ProtentialMisuse(MethodDescriptor method, SourceLineAnnotation location) {
			this.method = method;
			this.sourceline = location;
		}
	}
	 
	 public BitmapMisuse(BugReporter bugReporter) {
		 this.bugReporter= bugReporter;
	 }
	
	private boolean isCallingBitmapFactoryDecodeMethod() {
		MethodDescriptor method_desc_oper = getMethodDescriptorOperand();
		return "android.graphics.BitmapFactory".equals(method_desc_oper.getClassDescriptor().getDottedClassName())
				&& ("decodeResource".equals(method_desc_oper.getName()) || "decodeFile".equals(method_desc_oper.getName()));	
	}
	
	private boolean isMissingOptionInBitmapFactoryDecodeMethod() {
		MethodDescriptor method_desc_oper = getMethodDescriptorOperand();
		return (! method_desc_oper.getSignature().contains("Landroid/graphics/BitmapFactory$Options;)"));

	}
	
	private void reportBug() {
		BugInstance bug =new BugInstance(this,"OOM_BITMAP_NO_SAMPLING", HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(this, getPC());
		bug.addInt(getPC());
		bugReporter.reportBug(bug);
	}
	
	private void clearState() {
		options_obj_registers.clear();
		options_obj_registers = new ArrayList<Integer>();
		last_invoked_field_sig = null;
	}
	

	@Override
	public boolean shouldVisit(JavaClass jclass) {
        boolean sawTargetClass = false;
        for (int i = 0; i < jclass.getConstantPool().getLength(); ++i) {
            Constant constant = jclass.getConstantPool().getConstant(i);
            String className = null;
            if (constant instanceof ConstantMethodref) {
                ConstantMethodref cmr = (ConstantMethodref) constant;

                int classIndex = cmr.getClassIndex();
                className = jclass.getConstantPool().getConstantString(classIndex, Constants.CONSTANT_Class);
            } else if (constant instanceof ConstantInterfaceMethodref) {
                ConstantInterfaceMethodref cmr = (ConstantInterfaceMethodref) constant;

                int classIndex = cmr.getClassIndex();
                className = jclass.getConstantPool().getConstantString(classIndex, Constants.CONSTANT_Class);
            }

            if (className != null) {
                if (DEBUG) {
                    System.out.println("FindOpenDataSource: saw class " + className);
                }

                for (String aPRESCREEN_CLASS_LIST : PRESCREEN_CLASS_LIST ) {
                    if (className.indexOf(aPRESCREEN_CLASS_LIST) >= 0) {
                    	sawTargetClass = true;
                        break;
                    }
                }
            }
        }
        return sawTargetClass;
	}
	
	
	@Override
	public void visit(Code code) {
		clearState();
		super.visit(code);
	}
 
	@Override
	public void sawOpcode(int seen) {
		
        switch(seen) {
        case PUTSTATIC:
        case PUTFIELD:
        	last_invoked_field_sig = getClassConstantOperand() + "/" + getFieldDescriptorOperand().getName();
            if (BITMAPFACTORY_CLASS_NAME.equals(getDottedClassConstantOperand())
            		&& "inSampleSize".equals(getFieldDescriptorOperand().getName())) {
            	options_obj_registers.add(stack.getStackItem(1).getRegisterNumber());
            }
            ClassDescriptor field_class_desc =DescriptorFactory.createClassDescriptorFromFieldSignature(getFieldDescriptorOperand().getSignature());
            if (field_class_desc != null && BITMAP_CLASS_NAME.equals(field_class_desc.getDottedClassName())) {
            	if (ACONST_NULL != getPrevOpcode(1) && getDottedClassName().equals(getDottedClassConstantOperand())) {
            		bitmap_field_assigned_map.put(last_invoked_field_sig, true);
            		if (!bitmap_field_obj_map.containsKey(last_invoked_field_sig)) {
            			bitmap_field_obj_map.put(last_invoked_field_sig, new LinkedList<ProtentialMisuse>());
            		}
            		bitmap_field_obj_map.get(last_invoked_field_sig).add(new ProtentialMisuse(
            				getMethodDescriptor(), SourceLineAnnotation.fromVisitedInstruction(getClassContext(),
                                    this, getPC())));
            	}
            }
        	break;
        case GETSTATIC:
        case GETFIELD:
        	last_invoked_field_sig = getClassConstantOperand() + "/" + getFieldDescriptorOperand().getName();
        	break;
        case INVOKEVIRTUAL:
        	MethodDescriptor recycle_method = getMethodDescriptorOperand();
        	if (recycle_method.getClassDescriptor().getDottedClassName().equals(BITMAP_CLASS_NAME)
        			&& "recycle".equals(recycle_method.getName())) {
        		if (GETFIELD == getPrevOpcode(1) && last_invoked_field_sig != null) {
        			bitmap_field_recycled_map.put(last_invoked_field_sig, true);
        		}
        	}
        	break;
        case INVOKESTATIC:
        	if (isCallingBitmapFactoryDecodeMethod()) {
	        	if (isMissingOptionInBitmapFactoryDecodeMethod()) {
	        		reportBug();
	        	} else {
	        		if (options_obj_registers.size() <= 0
	        				|| options_obj_registers.indexOf(stack.getStackItem(0).getRegisterNumber()) < -1) {
	        			reportBug();
	        		}
	        	}
        	}    	
        	break;
        default:
        	last_invoked_field_sig = null;
            break;
        }
		
	}

	@Override
	public void visitAfter(JavaClass obj) {
		reportNoRecycleBug();
		super.visitAfter(obj);
	}
	
	private void reportNoRecycleBug() {
		for (String field_sig : bitmap_field_assigned_map.keySet()) {
			if (!bitmap_field_recycled_map.containsKey(field_sig)) {
				List<ProtentialMisuse> misuse_points  = bitmap_field_obj_map.get(field_sig);
				if (misuse_points.size() > 0) {
					BugInstance bug =new BugInstance(this,"OOM_BITMAP_NO_EXPLICIT_RECYCLE", HIGH_PRIORITY)
									.addClass(getClassDescriptor());
					for (ProtentialMisuse misuse_point : misuse_points) {
						bug.addMethod(misuse_point.method).addSourceLine(misuse_point.sourceline);
					}
					bugReporter.reportBug(bug);
				}
			}
		}
		bitmap_field_obj_map = new HashMap<String, List<ProtentialMisuse>>();
		bitmap_field_assigned_map = new HashMap<String, Boolean>();
		bitmap_field_recycled_map = new HashMap<String, Boolean>();
	}

}
