package org.ops4j.pax.exam.spi;

import org.ops4j.pax.exam.TestAddress;

/**
 * Separates logical regression invocations from underlying reactor strategy.
 * You get an instance from {@link org.ops4j.pax.exam.spi.ExxamReactor}
 */
public interface StagedExamReactor {

    /**
     * Invoke an actual regression. The reactor implementation will take care of (perhaps) instantiating a TestContainer or
     * reusing an existing one and passing the call.
     * You get the {@link TestAddress} from {@link org.ops4j.pax.exam.TestProbeBuilder#getTests()}.
     *
     * @param address reference to a concrete, single regression.
     * @throws Exception in case of a problem.
     */
    void invoke(TestAddress address) throws Exception;

    /**
     * When you are done with using your reactor make sure to call this method so underlying resources (like TestContainers
     * and connections) can be cleaned up.
     */
    void tearDown();

}
