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

import com.mchange.v2.c3p0.PooledDataSource
import com.zepben.cimbend.database.sqlite.extensions.setNullableString
import com.zepben.cimbend.network.NetworkService
import com.zepben.cimbend.network.model.NetworkProtoToCim
import com.zepben.cimbend.network.model.toCim
import com.zepben.protobuf.np.*
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NetworkProducerServer(private val connectionPool: PooledDataSource) :  NetworkProducerGrpcKt.NetworkProducerCoroutineImplBase() {

    var networkService = NetworkService()
        private set
    private var networkToCim = NetworkProtoToCim(networkService)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun createAccumulator(request: CreateAccumulatorRequest): CreateAccumulatorResponse {
        try {
            logger.info("Received Accumulator: accumulator.measurement.io.mRID=${request.accumulator.measurement.io.mrid}, " +
                "accumulator.measurement.io.name=${request.accumulator.measurement.io.name}, " +
                "accumulator.measurement.io.numDiagramObjects=${request.accumulator.measurement.io.numDiagramObjects}, " +
                "accumulator.measurement.io.description=${request.accumulator.measurement.io.description}, " +
                "accumulator.measurement.powerSystemResourceMRID=${request.accumulator.measurement.powerSystemResourceMRID}, " +
                "accumulator.measurement.remoteSourceMRID=${request.accumulator.measurement.remoteSourceMRID}, " +
                "accumulator.measurement.terminalMRID=${request.accumulator.measurement.terminalMRID}, " +
                "accumulator.measurement.phases=${request.accumulator.measurement.phases}, " +
                "accumulator.measurement.unitSymbol=${request.accumulator.measurement.unitSymbol}");
            val accumulator = toCim(request.accumulator, networkService)
            networkService.add(accumulator)


            connectionPool.connection.use {
                val preparedAccumulator =
                    it.prepareStatement("INSERT INTO accumulators(mrid, name, description, power_system_resource_mrid, remote_source_mrid, terminal_mrid, unit_symbol, phases) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                preparedAccumulator.setString(1, accumulator.mRID)
                preparedAccumulator.setNullableString(2, accumulator.name)
                preparedAccumulator.setNullableString(3, accumulator.description)
                preparedAccumulator.setNullableString(4, accumulator.powerSystemResourceMRID)
                preparedAccumulator.setNullableString(5, accumulator.remoteSource?.mRID)
                preparedAccumulator.setNullableString(6, accumulator.terminalMRID)
                preparedAccumulator.setNullableString(7, accumulator.unitSymbol.name)
                preparedAccumulator.setNullableString(8, accumulator.phases.name)
                assert(preparedAccumulator.executeUpdate() == 1)
                preparedAccumulator.clearParameters()
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAccumulatorResponse.getDefaultInstance()
    }

    override suspend fun createAnalog(request: CreateAnalogRequest): CreateAnalogResponse {
        try {
            logger.info("Received Analog: analog.measurement.io.mRID=${request.analog.measurement.io.mrid}, " +
                "analog.measurement.io.name=${request.analog.measurement.io.name}, " +
                "analog.measurement.io.numDiagramObjects=${request.analog.measurement.io.numDiagramObjects}, " +
                "analog.measurement.io.description=${request.analog.measurement.io.description}, " +
                "analog.measurement.powerSystemResourceMRID=${request.analog.measurement.powerSystemResourceMRID}, " +
                "analog.measurement.remoteSourceMRID=${request.analog.measurement.remoteSourceMRID}, " +
                "analog.measurement.terminalMRID=${request.analog.measurement.terminalMRID}, " +
                "analog.measurement.phases=${request.analog.measurement.phases}, " +
                "analog.measurement.unitSymbol=${request.analog.measurement.unitSymbol}, " +
                "analog.positiveFlowIn=${request.analog.positiveFlowIn}");
            val analog = toCim(request.analog, networkService)
            networkService.add(analog)

            connectionPool.connection.use {
                val preparedAnalog =
                    it.prepareStatement("INSERT INTO analogs(mrid, name, description, power_system_resource_mrid, remote_source_mrid, terminal_mrid, unit_symbol, phases, positive_flow_in) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                preparedAnalog.setString(1, analog.mRID)
                preparedAnalog.setNullableString(2, analog.name)
                preparedAnalog.setNullableString(3, analog.description)
                preparedAnalog.setNullableString(4, analog.powerSystemResourceMRID)
                preparedAnalog.setNullableString(5, analog.remoteSource?.mRID)
                preparedAnalog.setNullableString(6, analog.terminalMRID)
                preparedAnalog.setNullableString(7, analog.unitSymbol.name)
                preparedAnalog.setNullableString(8, analog.phases.name)
                preparedAnalog.setBoolean(9, analog.positiveFlowIn)
                assert(preparedAnalog.executeUpdate() == 1)
                preparedAnalog.clearParameters()
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAnalogResponse.getDefaultInstance()
    }

    override suspend fun createDiscrete(request: CreateDiscreteRequest): CreateDiscreteResponse {
        try {
            logger.info("Received Analog: discrete.measurement.io.mRID=${request.discrete.measurement.io.mrid}, " +
                "discrete.measurement.io.name=${request.discrete.measurement.io.name}, " +
                "discrete.measurement.io.numDiagramObjects=${request.discrete.measurement.io.numDiagramObjects}, " +
                "discrete.measurement.io.description=${request.discrete.measurement.io.description}, " +
                "discrete.measurement.powerSystemResourceMRID=${request.discrete.measurement.powerSystemResourceMRID}, " +
                "discrete.measurement.remoteSourceMRID=${request.discrete.measurement.remoteSourceMRID}, " +
                "discrete.measurement.terminalMRID=${request.discrete.measurement.terminalMRID}, " +
                "discrete.measurement.phases=${request.discrete.measurement.phases}, " +
                "discrete.measurement.unitSymbol=${request.discrete.measurement.unitSymbol}");
            val discrete = toCim(request.discrete, networkService)
            networkService.add(discrete)


            connectionPool.connection.use {
                val preparedDiscrete =
                    it.prepareStatement("INSERT INTO discretes(mrid, name, description, power_system_resource_mrid, remote_source_mrid, terminal_mrid, unit_symbol, phases) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                preparedDiscrete.setString(1, discrete.mRID)
                preparedDiscrete.setNullableString(2, discrete.name)
                preparedDiscrete.setNullableString(3, discrete.description)
                preparedDiscrete.setNullableString(4, discrete.powerSystemResourceMRID)
                preparedDiscrete.setNullableString(5, discrete.remoteSource?.mRID)
                preparedDiscrete.setNullableString(6, discrete.terminalMRID)
                preparedDiscrete.setNullableString(7, discrete.unitSymbol.name)
                preparedDiscrete.setNullableString(8, discrete.phases.name)
                assert(preparedDiscrete.executeUpdate() == 1)
                preparedDiscrete.clearParameters()
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDiscreteResponse.getDefaultInstance()
    }
}
