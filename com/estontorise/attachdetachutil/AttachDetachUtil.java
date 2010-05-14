package com.estontorise.attachdetachutil;

import java.lang.reflect.InvocationTargetException;

public interface AttachDetachUtil {

	public Object detach(Object o) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;
	public Object attach(Object o) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;
	
}
