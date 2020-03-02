package com.yccx.livebuslib.data;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;

import com.yccx.livebuslib.wrapper.WrapperObserver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BusMutableLiveData<T> extends MutableLiveData<T> {

    private Map<Observer, Observer> observerMap = new HashMap<>();

    /**
     * 在给定的观察者的生命周期内将给定的观察者添加到观察者列表所有者。事件是在主线程上分派的。
     * 如果LiveData已经有数据集合，它将被传递给观察者。
     * @param owner                                 owner
     * @param observer                              observer
     */
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
        super.observe(owner, observer);
        try {
            hook(observer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将给定的观察者添加到观察者列表中。
     * 这个调用类似于{@link LiveData#observe(LifecycleOwner, Observer)}和一个LifecycleOwner,
     * which总是积极的。这意味着给定的观察者将接收所有事件，并且永远不会被自动删除。
     * 您应该手动调用{@link #removeObserver(Observer)}来停止观察这LiveData。
     * @param observer                              observer
     */
    @Override
    public void observeForever(@NonNull Observer<T> observer) {
        if (!observerMap.containsKey(observer)) {
            observerMap.put(observer, new WrapperObserver(observer));
        }
        super.observeForever(observerMap.get(observer));
    }

    /**
     * 从观察者列表中删除给定的观察者。
     * @param observer                              observer
     */
    @Override
    public void removeObserver(@NonNull Observer<T> observer) {
        Observer realObserver = null;
        if (observerMap.containsKey(observer)) {
            realObserver = observerMap.remove(observer);
        } else {
            realObserver = observer;
        }
        super.removeObserver(realObserver);
    }

    /**
     * 利用反射修改属性
     * @param observer                              observer
     */
    private void hook(@NonNull Observer<T> observer) throws Exception {
        //get wrapper's version
        Class<LiveData> classLiveData = LiveData.class;
        Field fieldObservers = classLiveData.getDeclaredField("mObservers");
        fieldObservers.setAccessible(true);
        Object objectObservers = fieldObservers.get(this);
        Class<?> classObservers = objectObservers.getClass();
        Method methodGet = classObservers.getDeclaredMethod("get", Object.class);
        methodGet.setAccessible(true);
        Object objectWrapperEntry = methodGet.invoke(objectObservers, observer);
        Object objectWrapper = null;
        if (objectWrapperEntry instanceof Map.Entry) {
            objectWrapper = ((Map.Entry) objectWrapperEntry).getValue();
        }
        if (objectWrapper == null) {
            throw new NullPointerException("Wrapper can not be bull!");
        }
        Class<?> classObserverWrapper = objectWrapper.getClass().getSuperclass();
        Field fieldLastVersion = null;
        if (classObserverWrapper != null) {
            fieldLastVersion = classObserverWrapper.getDeclaredField("mLastVersion");
            fieldLastVersion.setAccessible(true);
            //get livedata's version
            Field fieldVersion = classLiveData.getDeclaredField("mVersion");
            fieldVersion.setAccessible(true);
            Object objectVersion = fieldVersion.get(this);
            //set wrapper's version
            fieldLastVersion.set(objectWrapper, objectVersion);
        }
    }
}