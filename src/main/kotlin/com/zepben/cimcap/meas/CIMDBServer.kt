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


package com.zepben.cimcap.meas

import ch.qos.logback.classic.Level
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.zepben.auth.JWTAuthenticator
import com.zepben.auth.grpc.AuthInterceptor
import com.zepben.cimcap.auth.ConfigServer
import com.zepben.evolve.conn.grpc.GrpcServer
import com.zepben.evolve.conn.grpc.SslContextConfig
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement
import kotlin.system.exitProcess

const val write_network_scope = "write:ewb"

/**
 * @property domain The domain on which to fetch tokens from. Must expose its JWKS on <domain>/.well_known/jwks.json
 */
class CIMDBServer(
    port: Int = 50051,
    sslContextConfig: SslContextConfig? = null,
    audience: String? = null,
    domain: String? = null,
    databaseConnectionString: String = "jdbc:sqlite:measurements.db",
    private val getConnection: (String) -> Connection = DriverManager::getConnection,
    private val getStatement: (Connection) -> Statement = Connection::createStatement,
    private val getPreparedStatement: (Connection, String) -> PreparedStatement = Connection::prepareStatement,
) : GrpcServer(port, sslContextConfig, createAuthInterceptor(audience, domain)) {

    init {
        getConnection(databaseConnectionString).use { conn ->
            if (conn.metaData.usesLocalFiles()) {
                conn.createStatement().use { statement ->
                    statement.executeUpdate(
                        """CREATE TABLE IF NOT EXISTS accumulator_values (
                    timestamp   TIMESTAMP       NOT NULL,
                    write_time  TIMESTAMP       NOT NULL,
                    accumulator_mrid        TEXT            NOT NULL,
                    value       INTEGER         NOT NULL
                );"""
                    )

                    statement.executeUpdate(
                        """CREATE TABLE IF NOT EXISTS analog_values (
                    timestamp   TIMESTAMP         NOT NULL,
                    write_time  TIMESTAMP         NOT NULL,
                    analog_mrid        TEXT              NOT NULL,
                    value       DOUBLE PRECISION  NOT NULL
                );"""
                    )

                    statement.executeUpdate(
                        """CREATE TABLE IF NOT EXISTS discrete_values (
                    timestamp   TIMESTAMP       NOT NULL,
                    write_time  TIMESTAMP       NOT NULL,
                    discrete_mrid        TEXT            NOT NULL,
                    value       INTEGER         NOT NULL
                );"""
                    )
                }
            }
        }
    }

    private var measurementServicer: MeasurementProducerServer = MeasurementProducerServer(getConnection(databaseConnectionString))
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        serverBuilder.addService(measurementServicer)
    }

    override fun start() {
        super.start()
        println("Server started, listening on $port")
    }

    override fun stop() {
        measurementServicer.close()
        super.stop()
    }
}

private fun createAuthInterceptor(audience: String?, domain: String?) =
    if (audience != null && domain != null) {
        val jwtAuthenticator = JWTAuthenticator(audience, domain)
        val requiredScopes = mapOf(
            "zepben.protobuf.mp.MeasurementProducer" to write_network_scope,
        )

        AuthInterceptor(jwtAuthenticator, requiredScopes)
    } else {
        null
    }

class Args(parser: ArgParser) {

    val port by parser.storing("-p", "--port", help = "Port for gRPC server") { toInt() }.default(50051)
    val confPort by parser.storing("--conf-port", help = "Port for HTTP auth config server") { toInt() }.default(8080)
    val privateKeyFilePath by parser.storing("-k", "--key", help = "Private key").default(null)
    val certChainFilePath by parser.storing("-c", "--cert", help = "Certificate chain for private key").default(null)
    val trustCertCollectionFilePath by parser.storing("-a", "--cacert", help = "CA Certificate chain").default(null)
    val tokenAuth by parser.flagging("-t", "--token-auth", help = "Token authentication (Auth0 M2M).").default(false)
    val clientAuth by parser.flagging("--client-auth", help = "Require client authentication.").default(false)
    val dbConnStr by parser.storing("-d", "--db", help = "Database JDBC URL connection string.").default("jdbc:sqlite:measurements.db")
    val audience by parser.storing("--audience", help = "Auth0 Audience for this application").default("https://evolve-ingestor/")
    val domain by parser.storing("--domain", help = "Auth0 domain to use").default("zepben.au.auth0.com")
    val tokenLookup by parser.storing("--token-url", help = "Token fetch URL to use").default("https://zepben.au.auth0.com/oauth/token")
    val algorithm by parser.storing("--alg", help = "Auth0 Algorithm to use").default("RS256")

}

fun main(args: Array<String>) {
    // This is to stop the gRPC lib spamming debug messages. Need to figure out how to stop it in a cleaner way.
    val logger: Logger = LoggerFactory.getLogger("main")
    val root = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
    root.level = Level.INFO
    val ret = try {
        runBlocking {
            val vertx = Vertx.vertx()
            ArgParser(args).parseInto(::Args).run {
                val server = try {
                    CIMDBServer(
                        port,
                        SslContextConfig(
                            certChainFilePath,
                            privateKeyFilePath,
                            trustCertCollectionFilePath,
                            if (clientAuth) ClientAuth.REQUIRE else ClientAuth.OPTIONAL
                        ),
                        if (tokenAuth) audience else null,
                        if (tokenAuth) domain else null,
                        dbConnStr
                    )
                } catch (e: IllegalArgumentException) {
                    logger.error("Failed to create CIMDBServer. Error was: ${e.message}")
                    logger.debug("", e)
                    return@runBlocking -1
                }
                logger.info("Starting CIMDBServer")
                server.start()
                logger.info("CIMDBServer running on 0.0.0.0:${port}")

                if (tokenAuth) {
                    val confServer = ConfigServer(vertx, confPort, audience, tokenLookup, algorithm)
                    logger.info("Starting AuthConfig server")
                    try {
                        confServer.start()

                        Runtime.getRuntime().addShutdownHook(Thread {
                            runBlocking {
                                confServer.close()
                                vertx.close()
                            }
                        })
                    } catch (e: Exception) {
                        logger.error("Failed to start AuthConfig server: ${e.message}")
                        logger.debug("", e)
                        return@runBlocking -2
                    }
                    logger.info("AuthConfig HTTP server running on 0.0.0.0:${confPort}")

                }
                server.blockUntilShutdown()
            }
            0
        }
    } catch (e: Exception) {
        logger.error("Failed to launch server.", e)
        -3
    }
    logger.info("Shutdown commenced")
    exitProcess(ret)
}
