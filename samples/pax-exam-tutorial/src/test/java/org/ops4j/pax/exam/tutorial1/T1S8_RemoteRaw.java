/*
 * Copyright 2009 Toni Menzel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.tutorial1;

import org.junit.Test;
import org.ops4j.pax.exam.OptionDescription;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.container.def.internal.PaxRunnerTestContainerFactory;
import org.ops4j.pax.exam.spi.ProbeCall;
import org.ops4j.pax.exam.spi.container.PaxExamRuntime;
import org.ops4j.pax.exam.spi.TestProbeBuilder;

import static org.ops4j.pax.exam.CoreOptions.options;

import static org.ops4j.pax.exam.container.remote.RBCRemoteTargetOptions.waitForRBCFor;
import static org.ops4j.pax.exam.spi.container.DefaultRaw.createProbe;
import static org.ops4j.pax.exam.spi.container.DefaultRaw.execute;

/**
 * This demo shows how to not necessarily use the junit user interface but fully
 * control the lifecycle yourself
 *
 * @author Toni Menzel
 * @since Jan 26, 2010
 */
public class T1S8_RemoteRaw
{

    @Test
    public void testPlan()
        throws Exception
    {
        TestContainerFactory factory = PaxExamRuntime.getTestContainerFactory( PaxRunnerTestContainerFactory.class );

        // we know there can be only one container
        OptionDescription testTarget = factory.parse(
            options(
                waitForRBCFor( 2000 )
                //location( "192.168.73.204", 8181 )
            )
        )[ 0 ];

        TestContainer testContainer = factory.createContainer( testTarget );
        try
        {

            TestProbeBuilder probe = createProbe().addTest( MyCode.class );
            testContainer.install( probe.getStream() );

            for( ProbeCall call : probe.getTests() )
            {
                execute( testContainer, call );
            }
        } finally
        {
            testContainer.stop();
        }
    }

//    @Test
//    public void testUsingReactor()
//        throws Exception
//    {
//        DefaultExamReactor reactor = new DefaultExamReactor();
//    	reactor.addConfiguration( options(
//                logProfile(),
//                rawPaxRunnerOption( "log", "debug" ),
//                mavenBundle().groupId( "org.apache.felix" ).artifactId( "org.apache.felix.dependencymanager" ).version( "3.0.0-SNAPSHOT" )
//         ));
//    	
//    	TestProbeBuilder probeBuilder = createProbe().addTest( MyCode.class );
//		reactor.addProbe( probeBuilder );
//    	
//    	// now can do:
//    	StagedExamReactor stagedReactor = reactor.stage();
//    	
//    	// invoking is fully decoupled from container status.
//    	
//    	// call all of them without knowing the calls: 
//    	//ProbeCall[] calls = probeBuilder.getTests();
//    	
//    	//
//    	stagedReactor.invoke( DefaultRaw.call ( MyCode.class,"testMe" ) );
//    }

}
