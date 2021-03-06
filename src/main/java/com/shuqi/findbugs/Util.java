package com.shuqi.findbugs;

import javax.annotation.CheckForNull;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

public class Util {
	
	public static ClassDescriptor[] getMethodParameterTypes(String signature) {
		SignatureParser parser = new SignatureParser(signature);
		ClassDescriptor[] names = new ClassDescriptor[parser.getTotalArgumentSize()];
		for (int i = 0 ; i< names.length; i++) {
			names[i] = DescriptorFactory.createClassDescriptorFromSignature(parser.getParameter(i));
		}
		return names;
	}
	
	public static ClassDescriptor getMethodReturnType(String signature) {
		SignatureParser parser = new SignatureParser(signature);
		return DescriptorFactory.createClassOrObjectDescriptorFromSignature(parser.getReturnTypeSignature());
	}
	
	public static boolean isSubType(String baseType, String possibleSubtype) 
			throws ClassNotFoundException {
			return Hierarchy.isSubtype(possibleSubtype, baseType);
	}
	
	public static String[] getMethodParameterClassNames(String signature) {
		SignatureParser parser = new SignatureParser(signature);
		String[] names = new String[parser.getNumParameters()];
		for (int i = 0 ; i< names.length; i++) {
			names[i] = Util.toDotClassName(parser.getParameter(i));
		}
		return names;
	}
	
	public static int getParameterFirstSlot(String method_sig, String target_param_type) {
		String[] param_types = Util.getMethodParameterClassNames(method_sig);
		for (int i = 0 ; i < param_types.length; i++) {
			if (target_param_type.equals(param_types[i])) {
				return i;
			}
		}
		return -1;
	}
	
	public static int getParameterStackSlot(String method_sig, String target_param_type) {
		String[] param_types = Util.getMethodParameterClassNames(method_sig);
		for (int i = 0 ; i < param_types.length; i++) {
			if (target_param_type.equals(param_types[i])) {
				return param_types.length - i -1;
			}
		}
		return -1;
	}
	
	public static int getParameterCount(String method_sig) {
		SignatureParser parser = new SignatureParser(method_sig);
		return parser.getNumParameters();
	}
	
	public static boolean hasParamTypeInMethod(String method_sig, String target_param_type) {
		return Util.getParameterFirstSlot(method_sig, target_param_type) >= 0;
	}

	
	public static String toDotClassName(String signature) {
		return DescriptorFactory.createClassOrObjectDescriptorFromSignature(signature).getDottedClassName();
	}
	
	public static ClassDescriptor toClassDescriptor(String signature) {
		return DescriptorFactory.createClassOrObjectDescriptorFromSignature(signature);
	}
	
    @CheckForNull
    public static JavaClass getOuterClass(JavaClass obj) throws ClassNotFoundException {
        for (Attribute a : obj.getAttributes()) {
            if (a instanceof InnerClasses) {
                for (InnerClass ic : ((InnerClasses) a).getInnerClasses()) {
                    if (obj.getClassNameIndex() == ic.getInnerClassIndex()) {
                        ConstantClass oc = (ConstantClass) obj.getConstantPool().getConstant(ic.getOuterClassIndex());
                        if (oc != null) {
	                        String ocName = oc.getBytes(obj.getConstantPool());
	                        return Repository.lookupClass(ocName);
                        }
                    }
                }
            }
        }
        return null;
    }
}
