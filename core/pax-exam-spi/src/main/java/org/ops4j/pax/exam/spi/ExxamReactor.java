package org.ops4j.pax.exam.spi;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.TestProbeProvider;

/**
 * Part of the Plumbing Level API.
 * The container creation control process needs a higher level control.
 * Instead of directly creating containers out of {@link org.ops4j.pax.exam.TestContainerFactory} it is recommended
 * to use {@link ExxamReactor} instead.
 * <p/>
 * You basically add some {@link Option}s and probes ({@link TestProbeBuilder}
 * and get a retrieve a {@link org.ops4j.pax.exam.spi.StagedExamReactor} that can be used to retrieve containers.
 * Now its up to the reactor and its configuration when to use and re-use the TestContainers.
 */
public interface ExxamReactor
{

    /**
     * Add the Configuration that contribute to desired container(s) you will get after calling {@link #stage(StagedExamReactorFactory)}.
     *
     * @param options a set of (user-end) options.
     */
    void addConfiguration( Option[] options );

    /**
     * Add the probe that contribute to desired container(s) you will get after calling {@link #stage(StagedExamReactorFactory)}.
     *
     * @param addTest provider that will (in the end) create a probe to be installed in the target container.
     */
    void addProbe( TestProbeProvider addTest );

    /**
     * Last thing you call on the reactor usually. This gives you a unmodifiable Reactor version.
     *
     * @param factory to be used to stage. (usually a strategy)
     *
     * @return new reactor that can be used to derive TestContainer instances.
     */
    StagedExamReactor stage( StagedExamReactorFactory factory );
}
