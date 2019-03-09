/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2019, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.event;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import org.sensorhub.api.common.Event;
import org.vast.util.Asserts;


public class SubscriberConsumerAdapter<E extends Event> implements Subscriber<E>
{       
    Consumer<? super Subscription> onSubscribe;
    Consumer<? super E> onNext;
    Consumer<Throwable> onError;
    Runnable onComplete;
    
    
    public SubscriberConsumerAdapter(Consumer<? super Subscription> onSubscribe, Consumer<? super E> onNext)
    {
        Asserts.checkNotNull(onSubscribe, "onSubscribe");
        Asserts.checkNotNull(onNext, "onNext");
        
        this.onSubscribe = onSubscribe;
        this.onNext = onNext;
    }
    
    
    public SubscriberConsumerAdapter(Consumer<? super Subscription> onSubscribe, Consumer<? super E> onNext, Consumer<Throwable> onError)
    {
        this(onSubscribe, onNext);
        this.onError = onError;
    }
    
    
    public SubscriberConsumerAdapter(Consumer<? super Subscription> onSubscribe, Consumer<? super E> onNext, Consumer<Throwable> onError, Runnable onComplete)
    {
        this(onSubscribe, onNext, onError);
        this.onComplete = onComplete;
    }
    
    
    @Override
    public void onSubscribe(Subscription sub)
    {
        sub.request(Long.MAX_VALUE);
        onSubscribe.accept(sub);
    }
    
    
    @Override
    public void onNext(E e)
    {
        onNext.accept(e);
    }
    

    @Override
    public void onError(Throwable e)
    {
        if (onError != null)
            onError.accept(e);
    }
    
    
    @Override
    public void onComplete()
    {
        if (onComplete != null)
            onComplete.run();
    }
}