package com.estontorise.attachdetachutil.jpa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Id;

import com.estontorise.attachdetachutil.AttachDetachUtil;
import com.estontorise.attachdetachutil.annotations.FetchOnDetach;

public class JPAAttachDetachUtil implements AttachDetachUtil {

	private EntityManager entityManager;

	public JPAAttachDetachUtil(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@SuppressWarnings("unchecked")
	public <T> T detach(T o) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (T)detachCopy(o, new HashMap<Object, Object>());
	}
	
	private boolean isPrimitive(Class<?> cls) {
		if(cls.isPrimitive())
			return true;
		if(cls == String.class)
			return true;
		if(cls == Long.class)
			return true;
		if(cls == Integer.class)
			return true;
		if(cls == Date.class)
			return true;
		if(cls == Double.class)
			return true;
		if(cls == Float.class)
			return true;
		return false;
	}
	
	private boolean isDetachableList(Method m) {
		if(m.getReturnType() != List.class)
			return false;
		if(m.getAnnotation(FetchOnDetach.class) == null)
			return false;
		return true;
	}
	
	private boolean isDetachableObject(Method m) {
		if(m.getAnnotation(FetchOnDetach.class) == null)
			return false;
		return true;
	}
		
	@SuppressWarnings("unchecked")
	private Object detachCopy(Object o, Map<Object, Object> detachCache) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException {
		if(detachCache.containsKey(o))
			return detachCache.get(o);
		Object newInstance = o.getClass().newInstance();
		detachCache.put(o, newInstance);
		for(Method m : o.getClass().getMethods()) {
			String methodName = m.getName();
			if(methodName.startsWith("get")) {
				if(isPrimitive(m.getReturnType())) {
					Object value = m.invoke(o, new Object[] {});
					String setMethodName = "set" + methodName.substring(3);
					Method setMethod = newInstance.getClass().getMethod(setMethodName, value.getClass());
					setMethod.invoke(newInstance, value);
				} 
				else if(isDetachableList(m)) {
					List values = (List) m.invoke(o, new Object[] {});
					List detachedList = new ArrayList();
					for(Object value: values)
						detachedList.add(detachCopy(value, detachCache));
					String setMethodName = "set" + methodName.substring(3);
					Method setMethod = newInstance.getClass().getMethod(setMethodName, List.class);
					setMethod.invoke(newInstance, detachedList);
				} 
				else if(isDetachableObject(m)) {
					Object value = m.invoke(o, new Object[] {});
					value = detachCopy(value, detachCache);
					String setMethodName = "set" + methodName.substring(3);
					Method setMethod = newInstance.getClass().getMethod(setMethodName, value.getClass());
					setMethod.invoke(newInstance, value);
				}
			}
		}
		return newInstance;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T attach(T o) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (T)attachCopy(o, new HashMap<Object, Object>());
	}

	private boolean isIdMethod(Method m) {
		return m.getAnnotation(Id.class) != null;
	}

	private boolean isPersistent(Object id) {
		if(id instanceof Integer)
			return ((Integer)id).intValue() > 0;
		if(id instanceof Long)
			return ((Long)id).intValue() > 0;
		return false;
	}
		
	@SuppressWarnings("unchecked")
	private Object attachCopy(Object o, Map<Object, Object> attachCache) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException {
		if(attachCache.containsKey(o))
			return attachCache.get(o);
		Object newInstance = o.getClass().newInstance();
		attachCache.put(o, newInstance);
		Object id = null;
		for(Method m : o.getClass().getMethods()) {
			String methodName = m.getName();
			if(methodName.startsWith("get")) {
				if(isPrimitive(m.getReturnType())) {
					Object value = m.invoke(o, new Object[] {});
					if(isIdMethod(m))
						id = value;
					String setMethodName = "set" + methodName.substring(3);
					Method setMethod = newInstance.getClass().getMethod(setMethodName, m.getReturnType());
					setMethod.invoke(newInstance, value);
				} 
				else if(isDetachableList(m)) {
					List values = (List) m.invoke(o, new Object[] {});
					List detachedList = new ArrayList();
					for(Object value: values)
						detachedList.add(attachCopy(value, attachCache));
					String setMethodName = "set" + methodName.substring(3);
					Method setMethod = newInstance.getClass().getMethod(setMethodName, List.class);
					setMethod.invoke(newInstance, detachedList);
				} 
				else if(isDetachableObject(m)) {
					Object value = m.invoke(o, new Object[] {});
					value = attachCopy(value, attachCache);
					String setMethodName = "set" + methodName.substring(3);
					Method setMethod = newInstance.getClass().getMethod(setMethodName, value.getClass());
					setMethod.invoke(newInstance, value);
				}
			}
		}	
		if(isPersistent(id))
			entityManager.merge(newInstance);
		else
			entityManager.persist(newInstance);
		return newInstance;
	}
	
}
