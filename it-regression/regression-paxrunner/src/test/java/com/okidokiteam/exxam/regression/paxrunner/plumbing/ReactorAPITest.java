/*
 * Copyright (C) 2010 Okidokiteam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okidokiteam.exxam.regression.paxrunner.plumbing;

import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.TestProbeProvider;
import org.ops4j.pax.exam.spi.ExxamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactorFactory;
import org.ops4j.pax.exam.spi.container.PaxExamRuntime;
import org.ops4j.pax.exam.spi.driversupport.DefaultExamReactor;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

import static org.ops4j.pax.exam.LibraryOptions.*;
import static org.ops4j.pax.exam.spi.container.DefaultRaw.*;

/**
 * Simple regression
 */
public class ReactorAPITest
{

    private TestContainerFactory getFactory()
    {
        return PaxExamRuntime.getTestContainerFactory();
    }

    @Test
    public void reactorRunAllConfinedTest()
        throws Exception
    {
        reactorRun( new AllConfinedStagedReactorFactory() );
    }

    @Test
    public void reactorRunEagerTest()
        throws Exception
    {
        reactorRun( new EagerSingleStagedReactorFactory() );
    }

    public void reactorRun( StagedExamReactorFactory strategy )
        throws Exception
    {
        TestContainerFactory factory = getFactory();
        Option[] options = new Option[]{ junitBundles(), easyMockBundles() };

        ExxamReactor reactor = new DefaultExamReactor( factory );

        TestProbeProvider probe = createProbe().addTest( Probe.class ).build();

        reactor.addProbe( probe );
        reactor.addConfiguration( options );

        StagedExamReactor stagedReactor = reactor.stage( strategy );
        try
        {
            for( TestAddress call : probe.getTests() )
            {
                stagedReactor.invoke( call );
            }

        } finally
        {
            stagedReactor.tearDown();
        }
    }

}
