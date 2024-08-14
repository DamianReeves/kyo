package kyoTest

import kyo.*

class atomicsTest extends KyoTest:

    "AtomicInt" - {
        "should initialize to the provided value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.get
            yield assert(v == 5)
        }
        "should set the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                _   <- ref.set(10)
                v   <- ref.get
            yield assert(v == 10)
        }
        "should compare and set the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.cas(5, 10)
                r   <- ref.get
            yield
                assert(v == true)
                assert(r == 10)
        }
        "should increment and get the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.incrementAndGet
            yield assert(v == 6)
        }
        "should get and increment the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.getAndIncrement
            yield assert(v == 5)
        }
        "should decrement and get the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.decrementAndGet
            yield assert(v == 4)
        }
        "should get and decrement the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.getAndDecrement
            yield assert(v == 5)
        }
        "should add and get the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.addAndGet(5)
            yield assert(v == 10)
        }
        "should get and add the value" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v   <- ref.getAndAdd(5)
            yield assert(v == 5)
        }
        "lazySet" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                _   <- ref.lazySet(5)
                v   <- ref.get
            yield assert(v == 5)
        }
        "getAndSet" in IOs.run {
            for
                ref <- Atomics.initInt(5)
                v1  <- ref.getAndSet(6)
                v2  <- ref.get
            yield assert(v1 == 5 && v2 == 6)
        }
    }

    "AtomicLong" - {
        "should initialize to the provided value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.get
            yield assert(v == 5L)
        }
        "should set the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                _   <- ref.set(10L)
                v   <- ref.get
            yield assert(v == 10L)
        }
        "should compare and set the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.cas(5L, 10L)
                r   <- ref.get
            yield
                assert(v == true)
                assert(r == 10L)
        }
        "should increment and get the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.incrementAndGet
            yield assert(v == 6L)
        }
        "should get and increment the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.getAndIncrement
            yield assert(v == 5L)
        }
        "should decrement and get the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.decrementAndGet
            yield assert(v == 4L)
        }
        "should get and decrement the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.getAndDecrement
            yield assert(v == 5L)
        }
        "should add and get the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.addAndGet(5L)
            yield assert(v == 10L)
        }
        "should get and add the value" in IOs.run {
            for
                ref <- Atomics.initLong(5L)
                v   <- ref.getAndAdd(5L)
            yield assert(v == 5L)
        }
        "lazySet" in IOs.run {
            for
                ref <- Atomics.initLong(5)
                _   <- ref.lazySet(5)
                v   <- ref.get
            yield assert(v == 5)
        }
        "getAndSet" in IOs.run {
            for
                ref <- Atomics.initLong(5)
                v1  <- ref.getAndSet(6)
                v2  <- ref.get
            yield assert(v1 == 5 && v2 == 6)
        }
    }

    "AtomicBoolean" - {
        "should initialize to the provided value" in IOs.run {
            for
                ref <- Atomics.initBoolean(true)
                v   <- ref.get
            yield assert(v == true)
        }
        "should set the value" in IOs.run {
            for
                ref <- Atomics.initBoolean(true)
                _   <- ref.set(false)
                v   <- ref.get
            yield assert(v == false)
        }
        "should compare and set the value" in IOs.run {
            for
                ref <- Atomics.initBoolean(true)
                v   <- ref.cas(true, false)
                r   <- ref.get
            yield
                assert(v == true)
                assert(r == false)
        }
        "lazySet" in IOs.run {
            for
                ref <- Atomics.initBoolean(true)
                _   <- ref.lazySet(false)
                v   <- ref.get
            yield assert(!v)
        }
        "getAndSet" in IOs.run {
            for
                ref <- Atomics.initBoolean(true)
                v1  <- ref.getAndSet(false)
                v2  <- ref.get
            yield assert(v1 && !v2)
        }
    }

    "AtomicRef" - {
        "should initialize to the provided value" in IOs.run {
            for
                ref <- Atomics.initRef("initial")
                v   <- ref.get
            yield assert(v == "initial")
        }
        "should set the value" in IOs.run {
            for
                ref <- Atomics.initRef("initial")
                _   <- ref.set("new")
                v   <- ref.get
            yield assert(v == "new")
        }
        "should compare and set the value" in IOs.run {
            for
                ref <- Atomics.initRef("initial")
                v   <- ref.cas("initial", "new")
                r   <- ref.get
            yield
                assert(v == true)
                assert(r == "new")
        }
        "should fail compare and set the value" in IOs.run {
            for
                ref <- Atomics.initRef("initial")
                v   <- ref.cas("not-initial", "new")
                r   <- ref.get
            yield
                assert(v == false)
                assert(r == "initial")
        }
        "should get and set the value" in IOs.run {
            for
                ref <- Atomics.initRef("initial")
                v   <- ref.getAndSet("new")
                r   <- ref.get
            yield
                assert(v == "initial")
                assert(r == "new")
        }
        "should lazy set the value" in IOs.run {
            for
                ref <- Atomics.initRef("initial")
                _   <- ref.lazySet("new")
                v   <- ref.get
            yield assert(v == "new")
        }
    }
end atomicsTest
