package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.TypeInitializer;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MethodRegistryDefaultTest {

    private static final int BASIC_SIZE = 1, EXTENDED_SIZE = 2;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType basicInstrumentedType, extendedInstrumentedType;
    @Mock
    private Instrumentation.Target basicInstrumentationTarget, extendedInstrumentationTarget;
    @Mock
    private Instrumentation.Target.Factory instrumentationTargetFactory;
    @Mock
    private MethodList basicMethodList, extendedMethodList, croppedMethodList, singleSize, zeroSize;
    @Mock
    private MethodDescription unknownMethod, knownMethod, instrumentationAppendedMethod;
    @Mock
    private MethodRegistry.Compiled.Entry fallback;
    @Mock
    private MethodRegistry.LatentMethodMatcher latentMatchesKnownMethod;
    @Mock
    private MethodMatcher matchesKnownMethod;
    @Mock
    private Instrumentation simpleInstrumentation, otherInstrumentation, extendingInstrumentation;
    @Mock
    private MethodAttributeAppender.Factory simpleAttributeAppenderFactory, otherAttributeAppenderFactory;
    @Mock
    private MethodAttributeAppender simpleAttributeAppender, otherAttributeAppender;
    @Mock
    private ByteCodeAppender simpleByteCodeAppender, otherByteCodeAppender;
    @Mock
    private MethodLookupEngine methodLookupEngine;
    @Mock
    private MethodLookupEngine.Finding basicFinding, extendedFinding;
    @Mock
    private TypeInitializer typeInitializer;

    @Before
    public void setUp() throws Exception {
        when(latentMatchesKnownMethod.manifest(any(TypeDescription.class))).thenReturn(matchesKnownMethod);
        when(matchesKnownMethod.matches(knownMethod)).thenReturn(true);
        when(basicInstrumentedType.getDeclaredMethods()).thenReturn(basicMethodList);
        when(basicMethodList.size()).thenReturn(BASIC_SIZE);
        when(basicInstrumentationTarget.getTypeDescription()).thenReturn(basicInstrumentedType);
        when(extendedInstrumentationTarget.getTypeDescription()).thenReturn(extendedInstrumentedType);
        when(methodLookupEngine.process(basicInstrumentedType)).thenReturn(basicFinding);
        when(methodLookupEngine.process(extendedInstrumentedType)).thenReturn(extendedFinding);
        when(instrumentationTargetFactory.make(basicFinding)).thenReturn(basicInstrumentationTarget);
        when(instrumentationTargetFactory.make(extendedFinding)).thenReturn(extendedInstrumentationTarget);
        when(extendedInstrumentedType.getDeclaredMethods()).thenReturn(extendedMethodList);
        when(extendedMethodList.size()).thenReturn(EXTENDED_SIZE);
        when(extendedMethodList.subList(anyInt(), anyInt())).thenReturn(croppedMethodList);
        when(zeroSize.size()).thenReturn(0);
        when(singleSize.size()).thenReturn(1);
        when(croppedMethodList.filter(any(MethodMatcher.class))).thenAnswer(new Answer<MethodList>() {
            @Override
            public MethodList answer(InvocationOnMock invocation) throws Throwable {
                Field field = invocation.getArguments()[0].getClass().getDeclaredField("methodDescription");
                field.setAccessible(true);
                return field.get(invocation.getArguments()[0]) == instrumentationAppendedMethod ? singleSize : zeroSize;
            }
        });
        when(simpleInstrumentation.prepare(any(InstrumentedType.class))).thenReturn(basicInstrumentedType);
        when(simpleInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(simpleByteCodeAppender);
        when(simpleAttributeAppenderFactory.make(any(InstrumentedType.class))).thenReturn(simpleAttributeAppender);
        when(otherInstrumentation.prepare(any(InstrumentedType.class))).thenReturn(basicInstrumentedType);
        when(otherInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(otherByteCodeAppender);
        when(otherAttributeAppenderFactory.make(any(InstrumentedType.class))).thenReturn(otherAttributeAppender);
        when(extendingInstrumentation.prepare(any(InstrumentedType.class))).thenReturn(extendedInstrumentedType);
        when(extendingInstrumentation.appender(any(Instrumentation.Target.class))).thenReturn(simpleByteCodeAppender);
        when(basicInstrumentedType.detach()).thenReturn(basicInstrumentedType);
        when(extendedInstrumentedType.detach()).thenReturn(extendedInstrumentedType);
        when(basicInstrumentedType.getTypeInitializer()).thenReturn(typeInitializer);
        when(extendedInstrumentedType.getTypeInitializer()).thenReturn(typeInitializer);
    }

    @Test
    public void testFallbackReturnedForEmptyRegistry() throws Exception {
        assertThat(new MethodRegistry.Default()
                        .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback)
                        .target(unknownMethod),
                is(fallback)
        );
    }

    @Test
    public void testSingleEntryRegistry() throws Exception {
        MethodRegistry.Compiled compiled = new MethodRegistry.Default()
                .prepend(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback);
        assertThat(compiled.target(knownMethod).isDefineMethod(), is(true));
        assertThat(compiled.target(knownMethod).getAttributeAppender(), is(simpleAttributeAppender));
        assertThat(compiled.target(knownMethod).getByteCodeAppender(), is(simpleByteCodeAppender));
        assertThat(compiled.target(unknownMethod), is(fallback));
        verify(simpleInstrumentation).prepare(basicInstrumentedType);
        verify(simpleInstrumentation).appender(basicInstrumentationTarget);
        verifyNoMoreInteractions(simpleInstrumentation);
        verify(simpleAttributeAppenderFactory).make(basicInstrumentedType);
        verifyNoMoreInteractions(simpleAttributeAppenderFactory);
    }

    @Test
    public void testDoubleEntryRegistryOnlyPreparedOnce() throws Exception {
        MethodRegistry.Compiled compiled = new MethodRegistry.Default()
                .prepend(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                .prepend(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback);
        assertThat(compiled.target(knownMethod).isDefineMethod(), is(true));
        assertThat(compiled.target(knownMethod).getAttributeAppender(), is(simpleAttributeAppender));
        assertThat(compiled.target(knownMethod).getByteCodeAppender(), is(simpleByteCodeAppender));
        assertThat(compiled.target(unknownMethod), is(fallback));
        verify(simpleInstrumentation).prepare(basicInstrumentedType);
        verify(simpleInstrumentation).appender(basicInstrumentationTarget);
        verifyNoMoreInteractions(simpleInstrumentation);
        verify(simpleAttributeAppenderFactory, times(2)).make(basicInstrumentedType);
        verifyNoMoreInteractions(simpleAttributeAppenderFactory);
    }

    @Test
    public void testDoubleEntryRegistryReturnsLastPrepended() throws Exception {
        MethodRegistry.Compiled compiled = new MethodRegistry.Default()
                .prepend(latentMatchesKnownMethod, otherInstrumentation, otherAttributeAppenderFactory)
                .prepend(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback);
        assertThat(compiled.target(knownMethod).isDefineMethod(), is(true));
        assertThat(compiled.target(knownMethod).getAttributeAppender(), is(simpleAttributeAppender));
        assertThat(compiled.target(knownMethod).getByteCodeAppender(), is(simpleByteCodeAppender));
        assertThat(compiled.target(unknownMethod), is(fallback));
        verify(simpleInstrumentation).prepare(basicInstrumentedType);
        verify(simpleInstrumentation).appender(basicInstrumentationTarget);
        verify(simpleAttributeAppenderFactory).make(basicInstrumentedType);
        verifyNoMoreInteractions(simpleInstrumentation);
        verifyNoMoreInteractions(simpleAttributeAppenderFactory);
        verify(otherInstrumentation).prepare(basicInstrumentedType);
        verify(otherInstrumentation).appender(basicInstrumentationTarget);
        verify(otherAttributeAppenderFactory).make(basicInstrumentedType);
        verifyNoMoreInteractions(otherInstrumentation);
        verifyNoMoreInteractions(otherAttributeAppenderFactory);
    }

    @Test
    public void testDoubleEntryRegistryReturnsFirstAppended() throws Exception {
        MethodRegistry.Compiled compiled = new MethodRegistry.Default()
                .append(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                .append(latentMatchesKnownMethod, otherInstrumentation, otherAttributeAppenderFactory)
                .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback);
        assertThat(compiled.target(knownMethod).isDefineMethod(), is(true));
        assertThat(compiled.target(knownMethod).getAttributeAppender(), is(simpleAttributeAppender));
        assertThat(compiled.target(knownMethod).getByteCodeAppender(), is(simpleByteCodeAppender));
        assertThat(compiled.target(unknownMethod), is(fallback));
        verify(simpleInstrumentation).prepare(basicInstrumentedType);
        verify(simpleInstrumentation).appender(basicInstrumentationTarget);
        verify(simpleAttributeAppenderFactory).make(basicInstrumentedType);
        verifyNoMoreInteractions(simpleInstrumentation);
        verifyNoMoreInteractions(simpleAttributeAppenderFactory);
        verify(otherInstrumentation).prepare(basicInstrumentedType);
        verify(otherInstrumentation).appender(basicInstrumentationTarget);
        verify(otherAttributeAppenderFactory).make(basicInstrumentedType);
        verifyNoMoreInteractions(otherInstrumentation);
        verifyNoMoreInteractions(otherAttributeAppenderFactory);
    }

    @Test
    public void testAppendedMethodsAreHandledByAppendingInstrumentation() throws Exception {
        MethodRegistry.Compiled compiled = new MethodRegistry.Default()
                .append(latentMatchesKnownMethod, extendingInstrumentation, simpleAttributeAppenderFactory)
                .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback);
        assertThat(compiled.target(knownMethod).isDefineMethod(), is(true));
        assertThat(compiled.target(knownMethod).getAttributeAppender(), is(simpleAttributeAppender));
        assertThat(compiled.target(knownMethod).getByteCodeAppender(), is(simpleByteCodeAppender));
        assertThat(compiled.target(unknownMethod), is(fallback));
        verify(extendingInstrumentation).prepare(basicInstrumentedType);
        verify(extendingInstrumentation).appender(extendedInstrumentationTarget);
        verify(simpleAttributeAppenderFactory).make(extendedInstrumentedType);
        verifyNoMoreInteractions(simpleInstrumentation);
        verifyNoMoreInteractions(simpleAttributeAppenderFactory);
        verify(extendedMethodList).subList(BASIC_SIZE, EXTENDED_SIZE);
        assertThat(compiled.target(instrumentationAppendedMethod).isDefineMethod(), is(true));
        assertThat(compiled.target(instrumentationAppendedMethod).getByteCodeAppender(), is(simpleByteCodeAppender));
        assertThat(compiled.target(instrumentationAppendedMethod).getAttributeAppender(),
                is((MethodAttributeAppender) MethodAttributeAppender.NoOp.INSTANCE));
        verify(croppedMethodList, times(7) /* for 7 calls to compiled.target */).filter(any(MethodMatcher.class));
    }


    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodRegistry.Default().hashCode(), is(new MethodRegistry.Default().hashCode()));
        assertThat(new MethodRegistry.Default(), is(new MethodRegistry.Default()));
        assertThat(new MethodRegistry.Default().append(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory).hashCode(),
                not(is(new MethodRegistry.Default().hashCode())));
        assertThat(new MethodRegistry.Default().append(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory),
                not(is((MethodRegistry) new MethodRegistry.Default())));
    }

    @Test
    public void testCompiledHashCodeEquals() throws Exception {
        assertThat(new MethodRegistry.Default().compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback).hashCode(),
                is(new MethodRegistry.Default().compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback).hashCode()));
        assertThat(new MethodRegistry.Default().compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback),
                is(new MethodRegistry.Default().compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback)));
        assertThat(new MethodRegistry.Default().append(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                        .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback).hashCode(),
                not(is(new MethodRegistry.Default().hashCode()))
        );
        assertThat(new MethodRegistry.Default().append(latentMatchesKnownMethod, simpleInstrumentation, simpleAttributeAppenderFactory)
                        .compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback),
                not(is(new MethodRegistry.Default().compile(basicInstrumentedType, methodLookupEngine, instrumentationTargetFactory, fallback)))
        );
    }
}
