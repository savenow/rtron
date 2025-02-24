/*
 * Copyright 2019-2023 Chair of Geoinformatics, Technical University of Munich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rtron.model.opendrive.objects

import arrow.core.None
import arrow.core.Option
import io.rtron.model.opendrive.core.OpendriveElement

class RoadObjectsTunnel(
    var validity: List<RoadObjectsObjectLaneValidity> = emptyList(),

    var daylight: Option<Double> = None,
    var id: String = "",
    var length: Double = Double.NaN,
    var lighting: Option<Double> = None,
    var name: Option<String> = None,
    var s: Double = Double.NaN,
    var type: ETunnelType = ETunnelType.STANDARD
) : OpendriveElement()
