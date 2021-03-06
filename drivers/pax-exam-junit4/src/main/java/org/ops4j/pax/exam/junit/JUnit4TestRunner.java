/*
 * Copyright 2010 Toni Menzel.
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
package org.ops4j.pax.exam.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.pax.exam.ExamConfigurationException;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.ExxamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactorFactory;
import org.ops4j.pax.exam.spi.container.DefaultRaw;
import org.ops4j.pax.exam.spi.container.PaxExamRuntime;
import org.ops4j.pax.exam.spi.driversupport.DefaultExamReactor;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

import static org.ops4j.pax.exam.spi.container.DefaultRaw.*;

/**
 * This is the default Test Runner using Exxam plumbing API.
 * Its also the blueprint for custom, much more specific runners.
 * This will make a single probe bundling in all @Tests in this class.
 *
 * This uses the whole regression class as a single unit of tests with the following valid annotaions:
 * - @Configuration -> Configuration 1:N. Multiple configurations will result into multiple invokations of the same regression.
 * - @ProbeBuilder -> Customize the probe creation.
 * - @Test -> Single tests to be invoked. Note that in @Configuration you can specify the invokation strategy.
 */
public class JUnit4TestRunner extends BlockJUnit4ClassRunner
{

    private static Logger LOG = LoggerFactory.getLogger( JUnit4TestRunner.class );

    private StagedExamReactor m_reactor;
    private Map<FrameworkMethod, TestAddress> m_map;

    public JUnit4TestRunner( Class<?> klass )
        throws InitializationError
    {
        super( klass );


    }

    @Override
    public void run( RunNotifier notifier )
    {

        m_map = new HashMap<FrameworkMethod, TestAddress>();
        try
        {
            m_reactor = prepareReactor();

            super.run( notifier );
        } catch( Exception e )
        {
            throw new TestContainerException( "Problem interacting with reactor.", e );
        } finally
        {
            m_reactor.tearDown();
        }
    }

    private synchronized StagedExamReactor prepareReactor()
        throws Exception
    {
        ExxamReactor reactor = getReactor();
        Class testClass = getTestClass().getJavaClass();

        Object testClassInstance = testClass.newInstance();

        Method[] methods = testClass.getMethods();
        for( Method m : methods )
        {
            Configuration conf = m.getAnnotation( Configuration.class );
            if( conf != null )
            {
                // consider as option, so prepare that one:
                LOG.info( "Add Configuration " + m.getName() );
                reactor.addConfiguration( (Option[]) m.invoke( testClassInstance ) );
            }
        }

        // add the tests to run as well:
        Properties extraProperties = new Properties();

        // TODO remove ignore flagged methods:

        TestProbeBuilder probe = createProbe( extraProperties );
        // overwrite with possible user settings:
        probe = overwriteWithUserDefinition( testClass, testClassInstance, probe );

        probe.setAnchor( testClass );
        for( FrameworkMethod s : getChildren() )
        {
            LOG.debug( "Add Test " + s.getName() );
            TestAddress call = save( s, DefaultRaw.call( testClass, s.getName() ) );
            probe.addTest( call );
        }
        reactor.addProbe( probe.build() );
        // finally stage it
        return stage( reactor, testClass );
    }

    private TestProbeBuilder overwriteWithUserDefinition( Class testClass, Object instance, TestProbeBuilder probe )
        throws ExamConfigurationException
    {
        Method[] methods = testClass.getMethods();
        for( Method m : methods )
        {
            LOG.debug( "Trying.." + m.getName() );
            ProbeBuilder conf = m.getAnnotation( ProbeBuilder.class );
            if( conf != null )
            {
                // consider as option, so prepare that one:
                LOG.debug( "User defined probe hook found: " + m.getName() );
                TestProbeBuilder probeBuilder;
                try
                {
                    probeBuilder = (TestProbeBuilder) m.invoke( instance, probe );
                } catch( Exception e )
                {
                    throw new ExamConfigurationException( "Invoking custom probe hook " + m.getName() + " failed", e );
                }
                if( probeBuilder != null )
                {
                    return probe;
                }
                else
                {
                    throw new ExamConfigurationException( "Invoking custom probe hook " + m.getName() + " succeeded but returned null" );
                }

            }
        }
        LOG.debug( "No User defined probe hook found" );
        return probe;
    }

    private StagedExamReactor stage( ExxamReactor reactor, Class testClass )
        throws InstantiationException, IllegalAccessException
    {
        ExamReactorStrategy strategy = (ExamReactorStrategy) testClass.getAnnotation( ExamReactorStrategy.class );

        StagedExamReactorFactory fact;
        if( strategy != null )
        {
            fact = strategy.value()[ 0 ].newInstance();
        }
        else
        {
            // default:
            fact = new EagerSingleStagedReactorFactory();
        }
        return reactor.stage( fact );
    }

    private DefaultExamReactor getReactor()
    {
        return new DefaultExamReactor( PaxExamRuntime.getTestContainerFactory() );
    }

    private TestAddress save( FrameworkMethod fwMethod, TestAddress call )
    {
        LOG.info( "Assign " + fwMethod.getName() + " to Address: " + call.signature() );
        m_map.put( fwMethod, call );
        return call;
    }

    // overwrite call chain

    protected synchronized Statement methodInvoker( final FrameworkMethod method, final Object test )
    {
        return new Statement()
        {

            @Override
            public void evaluate()
                throws Throwable
            {
                // usually we just need the signature here.
                m_reactor.invoke( findMatchingCall( method, test ) );
                LOG.info( "Invokations still in list: " + m_map.keySet().size() );
                //
            }
        };

    }

    protected TestAddress findMatchingCall( FrameworkMethod method, Object test )
    {
        return m_map.remove( method );
    }

    @Override
    protected void validatePublicVoidNoArgMethods( Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors )
    {

    }
}
