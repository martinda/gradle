/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.workers.internal;

import org.gradle.initialization.ClassLoaderRegistry;
import org.gradle.internal.Cast;
import org.gradle.internal.service.ServiceRegistry;

import javax.inject.Inject;

public class WorkerDaemonServer implements WorkerProtocol {
    private final ClassLoaderRegistry classLoaderRegistry;
    private final ServiceRegistry serviceRegistry;
    private Worker isolatedClassloaderWorker;

    @Inject
    public WorkerDaemonServer(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.classLoaderRegistry = serviceRegistry.get(ClassLoaderRegistry.class);
    }

    @Override
    public DefaultWorkResult execute(ActionExecutionSpec spec) {
        try {
            WrappedActionExecutionSpec daemonSpec = Cast.uncheckedCast(spec);
            Worker isolatedWorker = getIsolatedClassloaderWorker(daemonSpec.getClassLoaderStructure());
            return isolatedWorker.execute(daemonSpec);
        } catch (Throwable t) {
            return new DefaultWorkResult(true, t);
        }
    }

    private Worker getIsolatedClassloaderWorker(ClassLoaderStructure classLoaderStructure) {
        // We can reuse the classloader worker as the ClassLoaderStructure should not change in between invocations
        if (isolatedClassloaderWorker == null) {
            isolatedClassloaderWorker = new IsolatedClassloaderWorker(classLoaderStructure, classLoaderRegistry.getPluginsClassLoader(), serviceRegistry, true);
        }
        return isolatedClassloaderWorker;
    }

    @Override
    public String toString() {
        return "WorkerDaemonServer{}";
    }
}
