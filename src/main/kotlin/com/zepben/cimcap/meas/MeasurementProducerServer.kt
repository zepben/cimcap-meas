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

import com.zepben.cimbend.cim.iec61970.base.meas.AccumulatorValue
import com.zepben.cimbend.cim.iec61970.base.meas.AnalogValue
import com.zepben.cimbend.cim.iec61970.base.meas.DiscreteValue
import com.zepben.cimbend.measurement.MeasurementProtoToCim
import com.zepben.cimbend.measurement.MeasurementService
import com.zepben.protobuf.mp.*
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MeasurementProducerServer : MeasurementProducerGrpcKt.MeasurementProducerCoroutineImplBase() {

    var measurementService = MeasurementService()
        private set
    private var measToCim = MeasurementProtoToCim(measurementService)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val LIMIT : Int = 5000000

    override suspend fun createAccumulatorValue(request: CreateAccumulatorValueRequest): CreateAccumulatorValueResponse {
        try {
            logger.info("Received AccumulatorValue: mRID=${request.accumulatorValue.accumulatorMRID}, value=${request.accumulatorValue.value}, " +
                "timestamp=${request.accumulatorValue.mv.timeStamp}");
            if (measurementService.num() >= LIMIT)
            {
                val toRemove = measurementService.listOf(AccumulatorValue::class).first()
                measurementService.remove(toRemove);
            }
            measToCim.addFromPb(request.accumulatorValue);
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAccumulatorValueResponse.getDefaultInstance()
    }

    override suspend fun createAnalogValue(request: CreateAnalogValueRequest): CreateAnalogValueResponse {
        try {
            logger.info("Received AnalogValue: mRID=${request.analogValue.analogMRID}, value=${request.analogValue.value}, " +
                "timestamp=${request.analogValue.mv.timeStamp}");
            if (measurementService.num() >= LIMIT)
            {
                val toRemove = measurementService.listOf(AnalogValue::class).first()
                measurementService.remove(toRemove);
            }
            measToCim.addFromPb(request.analogValue)
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateAnalogValueResponse.getDefaultInstance()
    }

    override suspend fun createDiscreteValue(request: CreateDiscreteValueRequest): CreateDiscreteValueResponse {
        try {
            logger.info("Received DiscreteValue: mRID=${request.discreteValue.discreteMRID}, value=${request.discreteValue.value}, " +
                "timestamp=${request.discreteValue.mv.timeStamp}");
            if (measurementService.num() >= LIMIT)
            {
                val toRemove = measurementService.listOf(DiscreteValue::class).first()
                measurementService.remove(toRemove);
            }
            measToCim.addFromPb(request.discreteValue)
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription(e.message).asException()
        }
        return CreateDiscreteValueResponse.getDefaultInstance()
    }
}
