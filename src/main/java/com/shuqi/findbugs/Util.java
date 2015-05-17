package com.shuqi.findbugs;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.util.ClassName;

public class Util {
	private static boolean DEBUG = true;
	
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
		String[] names = new String[parser.getTotalArgumentSize()];
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
	
	public static boolean hasParamTypeInMethod(String method_sig, String target_param_type) {
		return Util.getParameterFirstSlot(method_sig, target_param_type) >= 0;
	}

	
	public static String toDotClassName(String signature) {
		return DescriptorFactory.createClassOrObjectDescriptorFromSignature(signature).getDottedClassName();
	}
	
	public static ClassDescriptor toClassDescriptor(String signature) {
		return DescriptorFactory.createClassOrObjectDescriptorFromSignature(signature);
	}
	
	public static void main(String[] args) {
		for (String para : Util.getMethodParameterClassNames("(Landroid/content/Context;)V")) {
			System.out.println(para);
		}
	}


}
