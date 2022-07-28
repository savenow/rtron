/*
 * Copyright 2019-2022 Chair of Geoinformatics, Technical University of Munich
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

package io.rtron.model.opendrive.additions.identifier

import arrow.core.Option

interface RoadLaneSectionIdentifierInterface {
    val laneSectionIndex: Int
}

data class LaneSectionIdentifier(override val laneSectionIndex: Int, val roadIdentifier: RoadIdentifier) :
    AbstractOpendriveIdentifier(), RoadLaneSectionIdentifierInterface, RoadIdentifierInterface by roadIdentifier {

    // Conversions
    override fun toString() = "Lane section: laneSectionIndex=$laneSectionIndex, roadId=${roadIdentifier.roadId}"
}

interface AdditionalLaneSectionIdentifier {
    var additionalId: Option<LaneSectionIdentifier>
}