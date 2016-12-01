/*
 * Copyright 2016 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.rxjava2.operators;

import java.util.*;
import java.util.concurrent.*;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.schedulers.Schedulers;

/**
 * Additional operators in the form of {@link FlowableTransformer},
 * use {@link Flowable#compose(FlowableTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @since 0.7.2
 */
public final class FlowableTransformers {
    /** Utility class. */
    private FlowableTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source.
     * <p>Properties:
     * <ul>
     * <li>The operator starts with an open valve.</li>
     * <li>If the other Publisher completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * <li>The operator is a pass-through for backpressure and uses an internal unbounded buffer
     * of size {@link Flowable#bufferSize()} to hold onto values if the valve is closed.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @return the new FlowableTransformer instance
     * @throws NullPointerException if {@code other} is null
     * 
     * @since 0.7.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public static <T> FlowableTransformer<T, T> valve(Publisher<Boolean> other) {
        return valve(other, true, Flowable.bufferSize());
    }

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source and starts with the specivied valve state.
     * <p>Properties:
     * <ul>
     * <li>If the other Publisher completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * <li>The operator is a pass-through for backpressure and uses an internal unbounded buffer
     * of size {@link Flowable#bufferSize()} to hold onto values if the valve is closed.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @param defaultOpen should the valve start as open?
     * @return the new FlowableTransformer instance
     * @throws NullPointerException if {@code other} is null
     * 
     * @since 0.7.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public static <T> FlowableTransformer<T, T> valve(Publisher<Boolean> other, boolean defaultOpen) {
        return valve(other, defaultOpen, Flowable.bufferSize());
    }

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source and starts with the specivied valve state and the specified
     * buffer size hint.
     * <p>Properties:
     * <ul>
     * <li>If the other Publisher completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @param defaultOpen should the valve start as open?
     * @param bufferSize the buffer size hint (the chunk size of the underlying unbounded buffer)
     * @return the new FlowableTransformer instance
     * @throws IllegalArgumentException if bufferSize &lt;= 0
     * @throws NullPointerException if {@code other} is null
     * @since 0.7.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public static <T> FlowableTransformer<T, T> valve(Publisher<Boolean> other, boolean defaultOpen, int bufferSize) {
        ObjectHelper.requireNonNull(other, "other is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return new FlowableValve<T>(null, other, defaultOpen, bufferSize);
    }

    /**
     * Buffers elements into a List while the given predicate returns true; if the
     * predicate returns false for an item, a new buffer is created with the specified item.
     * @param <T> the source value type
     * @param predicate the predicate receiving the current value and if returns false,
     *                  a new buffer is created with the specified item
     * @return the new FlowableTransformer instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T> FlowableTransformer<T, List<T>> bufferWhile(Predicate<? super T> predicate) {
        return bufferWhile(predicate, Functions.<T>createArrayList(16));
    }

    /**
     * Buffers elements into a custom collection while the given predicate returns true; if the
     * predicate returns false for an item, a new collection is created with the specified item.
     * @param <T> the source value type
     * @param <C> the collection type
     * @param predicate the predicate receiving the current value and if returns false,
     *                  a new collection is created with the specified item
     * @param bufferSupplier the callable that returns a fresh collection
     * @return the new FlowableTransformer instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T, C extends Collection<? super T>> FlowableTransformer<T, C> bufferWhile(Predicate<? super T> predicate, Callable<C> bufferSupplier) {
        return new FlowableBufferPredicate<T, C>(null, predicate, false, bufferSupplier);
    }

    /**
     * Buffers elements into a List until the given predicate returns true at which
     * point a new empty buffer is started.
     * @param <T> the source value type
     * @param predicate the predicate receiving the current itam and if returns true,
     *                  the current buffer is emitted and a fresh empty buffer is created
     * @return the new FlowableTransformer instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T> FlowableTransformer<T, List<T>> bufferUntil(Predicate<? super T> predicate) {
        return bufferUntil(predicate, Functions.<T>createArrayList(16));
    }


    /**
     * Buffers elements into a custom collection until the given predicate returns true at which
     * point a new empty custom collection is started.
     * @param <T> the source value type
     * @param <C> the collection type
     * @param predicate the predicate receiving the current itam and if returns true,
     *                  the current collection is emitted and a fresh empty collection is created
     * @param bufferSupplier the callable that returns a fresh collection
     * @return the new Flowable instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T, C extends Collection<? super T>> FlowableTransformer<T, C> bufferUntil(Predicate<? super T> predicate, Callable<C> bufferSupplier) {
        return new FlowableBufferPredicate<T, C>(null, predicate, true, bufferSupplier);
    }

    /**
     * Inserts a time delay between emissions from the upstream source.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit) {
        return spanout(0L, betweenDelay, unit, Schedulers.computation(), false);
    }


    /**
     * Inserts a time delay between emissions from the upstream source.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit, Scheduler scheduler) {
        return spanout(0L, betweenDelay, unit, scheduler, false);
    }


    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit) {
        return spanout(initialDelay, betweenDelay, unit, Schedulers.computation(), false);
    }

    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit, Scheduler scheduler) {
        return spanout(initialDelay, betweenDelay, unit, scheduler, false);
    }

    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit, boolean delayError) {
        return spanout(0L, betweenDelay, unit, Schedulers.computation(), delayError);
    }


    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        return spanout(0L, betweenDelay, unit, scheduler, delayError);
    }


    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit, boolean delayError) {
        return spanout(initialDelay, betweenDelay, unit, Schedulers.computation(), delayError);
    }

    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new FlowableSpanout<T>(null, initialDelay, betweenDelay, unit, scheduler, delayError, Flowable.bufferSize());
    }

    /**
     * Allows mapping or filtering an upstream value through an emitter.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param consumer the consumer that is called for each upstream value and should call one of the doXXX methods
     * on the BasicEmitter it receives (individual to each Subscriber).
     * @return the new FlowableTransformer instance
     * 
     * @since 0.10.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> FlowableTransformer<T, R> mapFilter(BiConsumer<? super T, ? super BasicEmitter<R>> consumer) {
        ObjectHelper.requireNonNull(consumer, "consumer is null");
        return new FlowableMapFilter<T, R>(null, consumer);
    }
}
