package org.jetbrains.krpc.server

import kotlinx.coroutines.CoroutineScope
import org.jetbrains.krpc.RPC
import kotlin.reflect.KClass

/**
 * RPCServer is used to accept RPC messages, route them to a specific service and process given responses.
 * Server may contain multiple services.
 * [CoroutineScope] defines server lifetime.
 */
interface RPCServer : CoroutineScope {
    /**
     * Registers new service to the server. Server will route all designated messages to it.
     * Service of any type should be unique on the server, but RPCServer does not specify the actual retention policy.
     *
     * @param Service the exact type of the server to be registered.
     * For example for service with `MyService` interface and `MyServiceImpl` implementation,
     * type `MyService` should be specified explicitly.
     * @param service the actual implementation of the service that will handle the calls.
     * @param serviceKClass [KClass] of the [Service].
     */
    fun <Service : RPC> registerService(service: Service, serviceKClass: KClass<Service>)
}

/**
 * Registers new service to the server. Server will route all designated messages to it.
 * Service of any type should be unique on the server, but RPCServer does not specify the actual retention policy.
 *
 * @param Service the exact type of the server to be registered.
 * For example for service with `MyService` interface and `MyServiceImpl` implementation,
 * type `MyService` should be specified explicitly.
 * @param service the actual implementation of the service that will handle the calls.
 */
inline fun <reified Service : RPC> RPCServer.registerService(service: Service) {
    registerService(service, Service::class)
}
