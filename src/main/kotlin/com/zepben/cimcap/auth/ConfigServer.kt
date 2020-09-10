// Copyright 2019 Zeppelin Bend Pty Ltd
// This file is part of cimcap-meas.
//
// cimcap-meas is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// cimcap-meas is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with cimcap-meas.  If not, see <https://www.gnu.org/licenses/>.


package com.zepben.cimcap.auth

import com.zepben.auth.AvailableRoute
import com.zepben.auth.routeFactory
import com.zepben.vertxutils.routing.RouteRegister
import com.zepben.vertxutils.routing.RouteVersionUtils.forVersion
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class ConfigServer(
    vertx: Vertx,
    private val port: Int,
    private val audience: String,
    private val domain: String,
    private val algorithm: String = "RS256"
) {
    private val httpServer = vertx.createHttpServer()
    private val router = Router.router(vertx)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val routeRegister = RouteRegister(router, true)

    init {
        routeRegister.add(forVersion(AvailableRoute.values(), 2) { routeFactory(it, audience, domain, algorithm) })
        httpServer.requestHandler(router)
            .exceptionHandler(::serverExceptionHandler)
    }

    suspend fun start() {
        httpServer.listenAwait(port)
    }

    suspend fun close() {
        httpServer.closeAwait()
        logger.info("Stopped AuthConfigServer")
    }

    private fun serverExceptionHandler(t: Throwable) {
        // Without this the vert.x HTTP server spews messages about "Connection reset by peer" and other things we have
        // absolutely no control over. If you are seeing some really funny behaviour from the connection you might want
        // to try commenting this check out to see if there is anything that you can do something about.
        if (t is IOException) return
        logger.error("Unhandled error from within HTTP server", t)
    }
}

