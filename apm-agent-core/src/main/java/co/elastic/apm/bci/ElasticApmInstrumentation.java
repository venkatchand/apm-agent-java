/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 Elastic and contributors
 * %%
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
 * #L%
 */
package co.elastic.apm.bci;

import co.elastic.apm.impl.ElasticApmTracer;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.annotation.Nullable;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * An advice is responsible for instrumenting methods (see {@link #getMethodMatcher()}) in particular classes
 * (see {@link #getTypeMatcher()}).
 * <p>
 * The actual instrumentation of the matched methods is performed by static methods within this class,
 * which are annotated by {@link net.bytebuddy.asm.Advice.OnMethodEnter} or {@link net.bytebuddy.asm.Advice.OnMethodExit}.
 * </p>
 */
public abstract class ElasticApmInstrumentation {

    @Nullable
    @VisibleForAdvice
    public static ElasticApmTracer tracer;

    /**
     * Initializes the advice with the {@link ElasticApmTracer}
     * <p>
     * This enables tests to register a custom instance with a {@link co.elastic.apm.impl.ElasticApmTracerBuilder#configurationRegistry}
     * and {@link co.elastic.apm.impl.ElasticApmTracerBuilder#reporter} which is specific to a particular test or test class.
     * </p>
     *
     * @param tracer the tracer to use for this advice.
     */
    static void staticInit(ElasticApmTracer tracer) {
        // allow re-init with a different tracer
        ElasticApmInstrumentation.tracer = tracer;
    }

    public void init(ElasticApmTracer tracer) {
    }

    /**
     * Pre-select candidates solely based on the class name for the slower {@link #getTypeMatcher()},
     * at the expense of potential false negative matches.
     * <p>
     * Any matcher which does not only take the class name into account,
     * causes the class' bytecode to be parsed.
     * If the matcher needs information from other classes than the one currently being loaded,
     * like it's super class,
     * those classes have to be loaded from the file system,
     * unless they are cached or already loaded.
     * </p>
     */
    public ElementMatcher<? super NamedElement> getTypeMatcherPreFilter() {
        return any();
    }

    /**
     * The type matcher selects types which should be instrumented by this advice
     * <p>
     * To make type matching more efficient,
     * first apply the cheaper matchers like {@link ElementMatchers#nameStartsWith(String)} and {@link ElementMatchers#isInterface()}
     * which pre-select the types as narrow as possible.
     * Only then use more expensive matchers like {@link ElementMatchers#hasSuperType(ElementMatcher)}
     * </p>
     *
     * @return the type matcher
     */
    public abstract ElementMatcher<? super TypeDescription> getTypeMatcher();

    /**
     * The method matcher selects methods of types matching {@link #getTypeMatcher()},
     * which should be instrumented
     *
     * @return the method matcher
     */
    public abstract ElementMatcher<? super MethodDescription> getMethodMatcher();

    public Class<?> getAdviceClass() {
        return getClass();
    }


    /**
     * Return {@code true},
     * if this instrumentation should even be applied when
     * {@link co.elastic.apm.configuration.CoreConfiguration#instrument} is set to {@code false}.
     */
    public boolean includeWhenInstrumentationIsDisabled() {
        return false;
    }

    /**
     * Returns a name which groups several instrumentations into a logical group.
     * <p>
     * This name is used in {@link co.elastic.apm.configuration.CoreConfiguration#disabledInstrumentations} to exclude a logical group
     * of instrumentations.
     * </p>
     *
     * @return a name which groups several instrumentations into a logical group
     */
    public abstract Collection<String> getInstrumentationGroupNames();

}
