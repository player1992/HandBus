package com.leo.bus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Date:2020/7/17.3:15 PM</p>
 * <p>Author:leo</p>
 * <p>Desc:</p>
 */
public class HandBus {

    static {
        HandLogger.logWarn("init HandBus");
    }


    //外部类加载的时候不会加载内部类
    private static class Holder {
        static {
            HandLogger.logWarn("init HandBus.Holder");
        }

        static HandBus INSTANCE = new HandBus();
    }

    private HandBus() {
    }

    public static HandBus getInstance() {
        HandLogger.logDebug("getInstance : " + Holder.INSTANCE.toString());
        return Holder.INSTANCE;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 容器
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 根据事件类型去找接收者，不必遍历
     *  Event - <Activity,Method>
     */
    private Map<String, List<EventHandler>> mEventMappings = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param receiver 添加事件接收者
     */
    public void register(Object receiver) {
        if (receiver == null) return;
        log("register receiver" + receiver.getClass().getCanonicalName());
        List<Method> eventMethods = findEventMethod(receiver.getClass());
        addReceiver(receiver, eventMethods);
        log("register ： "+ mEventMappings);
    }

    private void addReceiver(Object receiver, Collection<Method> eventMethods) {
        for (Method eventMethod : eventMethods) {
            Class<?>[] parameterTypes = eventMethod.getParameterTypes();
            //以事件类型维护映射关系
            String eventType = parameterTypes[0].toString();
            //已经有接受者，则继续添加新的接收者
            if (mEventMappings.containsKey(eventType)) {
                List<EventHandler> receivers = mEventMappings.get(eventType);
                // 可能尚未注册过 或者已经清空
                if (receivers == null || receivers.isEmpty()){
                    receivers = new ArrayList<>();
                    mEventMappings.put(eventType,receivers);
                }
                receivers.add(new EventHandler(receiver,eventMethod));
            } else {
                //没有接收者,新添加
                List<EventHandler> receivers = new ArrayList<>();
                receivers.add(new EventHandler(receiver,eventMethod));
                mEventMappings.put(eventType, receivers);
            }
        }
    }

    private List<Method> findEventMethod(Class<?> aClass) {
        List<Method> result = new ArrayList<>();
        Method[] methods = aClass.getDeclaredMethods();
        //遍历接受者的所有方法
        for (Method method : methods) {
            //方法确定是public的
            if (method.getAnnotation(Receive.class) != null) {
                log("find method " + method.getName()
                        + " for receiver " + aClass.getCanonicalName());
//                Receive annotation = method.getAnnotation(Receive.class);
                //方法必须public修饰
                if ((method.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                    throw new IllegalStateException("@Receive 修饰的方法必须为public");
                }
                //方法参数只能是1个
                if (method.getParameterTypes().length == 1) {
                    result.add(method);
                } else {
                    throw new IllegalStateException("@Receive 修饰的方法参数必须为1");
                }
            } else {
                log("method  " + method.getName() + " in " + aClass.getCanonicalName() + " ignored");
            }
        }
        return result;
    }

    /**
     * @param receiver 移除事件接受者
     */
    public void unregister(Object receiver) {
        if (receiver == null) return;
        log("unregister receiver" + receiver.getClass().getCanonicalName());
        for (String eventKey : mEventMappings.keySet()) {
            List<EventHandler> eventHandlers = mEventMappings.get(eventKey);
            if (eventHandlers == null || eventHandlers.size() == 0) continue;
            int receiverSize = eventHandlers.size();
            for (int i = 0; i < receiverSize; i++) {
                EventHandler eventHandler = eventHandlers.get(i);
                if (eventHandler.target.getClass().toString().equals(receiver.getClass().toString())) {
                    eventHandlers.remove(eventHandler);
                    i--;
                    receiverSize--;
                }
            }

        }
        log("unregister ： "+ mEventMappings);
    }


    public void post(Object event) {
        String eventKey = event.getClass().toString();
        List<EventHandler> receiverMaps = mEventMappings.get(eventKey);
        if (receiverMaps == null || receiverMaps.size() == 0) return;
        //所有可以处理该事件的接收者
        for (EventHandler receiver : receiverMaps) {
            log(event.getClass().toString() +" 事件处理 ：" + receiver.target.toString());
            try {
                receiver.method.invoke(receiver.target,event);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }

    private void log(String msg) {
        HandLogger.logDebug(msg);
    }

    private void logE(String msg) {
        HandLogger.logError(msg);
    }

    private void logW(String msg) {
        HandLogger.logWarn(msg);
    }
}
