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

import com.zepben.cimbend.common.extensions.typeNameAndMRID
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

    override suspend fun createAccumulatorValue(request: CreateAccumulatorValueRequest): CreateAccumulatorValueResponse {
        // TODO: Implement
        return super.createAccumulatorValue(request)
    }

    override suspend fun createAnalogValue(request: CreateAnalogValueRequest): CreateAnalogValueResponse {
        // TODO: Implement
        return super.createAnalogValue(request)
    }

    override suspend fun createDiscreteValue(request: CreateDiscreteValueRequest): CreateDiscreteValueResponse {
        // TODO: Implement
        return super.createDiscreteValue(request)
    }
}
