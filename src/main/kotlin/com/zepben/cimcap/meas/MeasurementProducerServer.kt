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

import com.google.common.collect.ImmutableList
import com.mchange.v2.c3p0.PooledDataSource
import com.zepben.cimbend.cim.iec61970.base.meas.AccumulatorValue
import com.zepben.cimbend.cim.iec61970.base.meas.AnalogValue
import com.zepben.cimbend.cim.iec61970.base.meas.DiscreteValue
import com.zepben.cimbend.measurement.MeasurementProtoToCim
import com.zepben.cimbend.measurement.MeasurementService
import com.zepben.cimbend.measurement.toCim
import com.zepben.protobuf.mp.*
import io.grpc.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

class MeasurementProducerServer(private val connectionPool: PooledDataSource) : MeasurementProducerGrpcKt.MeasurementProducerCoroutineImplBase() {

//    var measurementService = MeasurementService()
//        private set
//    private var measToCim = MeasurementProtoToCim(measurementService)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

//    private val LIMIT: Int = 5000000

    private fun addAccumulatorValue(accumulatorValue: com.zepben.protobuf.cim.iec61970.base.meas.AccumulatorValue) {
        logger.info(
            "Received AccumulatorValue: mRID=${accumulatorValue.accumulatorMRID}, value=${accumulatorValue.value}, " +
                "timestamp=${accumulatorValue.mv.timeStamp}"
        );
//        if (measurementService.num() >= LIMIT) {
//            val toRemove = measurementService.listOf(AccumulatorValue::class).first()
//            measurementService.remove(toRemove);
//        }
        val av = toCim(accumulatorValue)
//        measurementService.add(av)

        connectionPool.connection.use {
            insertAccumulatorValue(av, it);
        }
    }

    private fun insertAccumulatorValue(av: AccumulatorValue, connection: Connection){
        connection.prepareStatement("INSERT INTO accumulator_values(timestamp, write_time, accumulator_mrid, value) VALUES (?, ?, ?, ?)").use {
            it.setTimestamp(1, Timestamp.from(av.timeStamp))
            it.setTimestamp(2, Timestamp.from(Instant.now()))
            it.setString(3, av.accumulatorMRID)
            it.setInt(4, av.value.toInt())
            assert(it.executeUpdate() == 1)
            it.clearParameters()
        }

    }

    private fun addAnalogValue(analogValue: com.zepben.protobuf.cim.iec61970.base.meas.AnalogValue) {
        logger.info(
            "Received AnalogValue: mRID=${analogValue.analogMRID}, value=${analogValue.value}, " +
                "timestamp=${analogValue.mv.timeStamp}"
        );
//        if (measurementService.num() >= LIMIT) {
//            val toRemove = measurementService.listOf(AnalogValue::class).first()
//            measurementService.remove(toRemove);
//        }
        val av = toCim(analogValue)
//        measurementService.add(av)
        connectionPool.connection.use {
            insertAnalogValue(av, it)
        }
    }

    private fun insertAnalogValue(av: AnalogValue, connection: Connection) {
        connection.prepareStatement("INSERT INTO analog_values(timestamp, write_time, analog_mrid, value) VALUES (?, ?, ?, ?)").use {
            it.setTimestamp(1, Timestamp.from(av.timeStamp))
            it.setTimestamp(2, Timestamp.from(Instant.now()))
            it.setString(3, av.analogMRID)
            it.setDouble(4, av.value)
            assert(it.executeUpdate() == 1)
            it.clearParameters()
        }
    }

    private fun addDiscreteValue(discreteValue: com.zepben.protobuf.cim.iec61970.base.meas.DiscreteValue) {
        logger.info(
            "Received DiscreteValue: mRID=${discreteValue.discreteMRID}, value=${discreteValue.value}, " +
                "timestamp=${discreteValue.mv.timeStamp}"
        );
//        if (measurementService.num() >= LIMIT) {
//            val toRemove = measurementService.listOf(DiscreteValue::class).first()
//            measurementService.remove(toRemove);
//        }
        val dv = toCim(discreteValue)
//        measurementService.add(dv)

        connectionPool.connection.use {
            insertDiscreteValue(dv, it);
        }
    }

    private fun insertDiscreteValue(dv: DiscreteValue, connection: Connection){
        connection.prepareStatement("INSERT INTO discrete_values(timestamp, write_time, discrete_mrid, value) VALUES (?, ?, ?, ?)").use {
            it.setTimestamp(1, Timestamp.from(dv.timeStamp))
            it.setTimestamp(2, Timestamp.from(Instant.now()))
            it.setString(3, dv.discreteMRID)
            it.setInt(4, dv.value)
            assert(it.executeUpdate() == 1)
            it.clearParameters()
        }
    }

    @ExperimentalUnsignedTypes
    override suspend fun createAccumulatorValue(request: CreateAccumulatorValueRequest): CreateAccumulatorValueResponse {
        try {
            addAccumulatorValue(request.accumulatorValue)
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAccumulatorValueResponse.getDefaultInstance()
    }

    override suspend fun createAccumulatorValues(request: CreateAccumulatorValuesRequest): CreateAccumulatorValuesResponse {
        try {
            for (accumulatorValue in request.accumulatorValuesList) {
                addAccumulatorValue(accumulatorValue)
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAccumulatorValuesResponse.getDefaultInstance()
    }

    override suspend fun createAnalogValue(request: CreateAnalogValueRequest): CreateAnalogValueResponse {
        try {
            addAnalogValue(request.analogValue)
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAnalogValueResponse.getDefaultInstance()
    }

    override suspend fun createAnalogValues(request: CreateAnalogValuesRequest): CreateAnalogValuesResponse {
        try {
            for (analogValue in request.analogValuesList) {
                addAnalogValue(analogValue)
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAnalogValuesResponse.getDefaultInstance()
    }

    override suspend fun createDiscreteValue(request: CreateDiscreteValueRequest): CreateDiscreteValueResponse {
        try {
            addDiscreteValue(request.discreteValue)
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDiscreteValueResponse.getDefaultInstance()
    }

    override suspend fun createDiscreteValues(request: CreateDiscreteValuesRequest): CreateDiscreteValuesResponse {
        try {
            for (discreteValue in request.discreteValuesList) {
                addDiscreteValue(discreteValue)
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDiscreteValuesResponse.getDefaultInstance()
    }

    override fun createMeasurementValues(requests: Flow<CreateMeasurementValuesRequest>): Flow<CreateMeasurementValuesResponse> =
        flow {
            requests.collect { mv ->
                val response = CreateMeasurementValuesResponse.newBuilder()
                connectionPool.connection.use {
                    it.createStatement().use { statement ->
                        statement.executeUpdate("PRAGMA journal_mode = OFF")
                        statement.executeUpdate("PRAGMA synchronous = OFF")
                    }

                    if (mv.analogValuesCount > 0) {
                        logger.info("Adding ${mv.analogValuesList.size} analog values.");
                        for (value in mv.analogValuesList) {
                            try {
                                val av = toCim(value)
                                insertAnalogValue(av, it);
                            } catch (e: Exception) {
                                val error = ErrorDetail.newBuilder()
                                error.analogValue = value
                                error.error = e.message
                                response.addErrors(error)
                                logger.error(e.message, e)
                            }
                        }
                    }
                    if (mv.accumulatorValuesCount > 0) {
                        logger.info("Adding ${mv.accumulatorValuesList.size} accumulator values.");
                        for (value in mv.accumulatorValuesList) {
                            try {
                                val av = toCim(value)
                                insertAccumulatorValue(av, it)
                            } catch (e: Exception) {
                                val error = ErrorDetail.newBuilder()
                                error.accumulatorValue = value
                                error.error = e.message
                                response.addErrors(error)
                                logger.error(e.message, e)
                            }
                        }
                    }

                    if (mv.discreteValuesCount > 0) {
                        logger.info("Adding ${mv.discreteValuesList.size} discrete values.");
                        for (value in mv.discreteValuesList) {
                            try {
                                val dv = toCim(value)
                                insertDiscreteValue(dv, it)
                            } catch (e: Exception) {
                                val error = ErrorDetail.newBuilder()
                                error.discreteValue = value
                                error.error = "DB Error"
                                response.addErrors(error)
                                logger.error(e.message, e)
                            }
                        }
                    }
                }

                if (response.errorsCount > 0) {
                    logger.info("Response failed with ${response.errorsCount} errors.")
                    response.failed = true
                }

                emit(response.build())
            }
        }


    fun close() {
        connectionPool.close()
    }
}
