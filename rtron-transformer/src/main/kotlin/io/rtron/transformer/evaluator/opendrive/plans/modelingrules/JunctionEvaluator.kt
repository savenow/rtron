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

package io.rtron.transformer.evaluator.opendrive.plans.modelingrules

import arrow.core.None
import io.rtron.io.messages.ContextMessageList
import io.rtron.io.messages.DefaultMessage
import io.rtron.io.messages.DefaultMessageList
import io.rtron.io.messages.Severity
import io.rtron.model.opendrive.OpendriveModel
import io.rtron.model.opendrive.additions.optics.everyJunction
import io.rtron.model.opendrive.junction.EJunctionType
import io.rtron.transformer.evaluator.opendrive.OpendriveEvaluatorParameters
import io.rtron.transformer.messages.opendrive.of

class JunctionEvaluator(val parameters: OpendriveEvaluatorParameters) {

    // Methods
    fun evaluateFatalViolations(opendriveModel: OpendriveModel): DefaultMessageList {
        val messageList = DefaultMessageList()
        return messageList
    }

    fun evaluateNonFatalViolations(opendriveModel: OpendriveModel): ContextMessageList<OpendriveModel> {
        val messageList = DefaultMessageList()
        var healedOpendriveModel = opendriveModel.copy()

        healedOpendriveModel = everyJunction.modify(healedOpendriveModel) { currentJunction ->

            // Junctions should not be used when only two roads meet.
            if (currentJunction.typeValidated == EJunctionType.DEFAULT && currentJunction.getNumberOfIncomingRoads() <= 2) {
                messageList += DefaultMessage.of("", "Junctions of type default should only be used when at least three roads are coming in (currently incoming road ids: ${currentJunction.getIncomingRoadIds()})", currentJunction.additionalId, Severity.WARNING, wasHealed = false)
            }

            // The @mainRoad, @orientation, @sStart and @sEnd attributes shall only be specified for virtual junctions.
            if (currentJunction.typeValidated != EJunctionType.VIRTUAL) {
                currentJunction.mainRoad.tap {
                    messageList += DefaultMessage.of("", "Attribute 'mainRoad' shall only be specified for virtual junctions", currentJunction.additionalId, Severity.FATAL_ERROR, wasHealed = true)
                    currentJunction.mainRoad = None
                }

                currentJunction.orientation.tap {
                    messageList += DefaultMessage.of("", "Attribute 'orientation' shall only be specified for virtual junctions", currentJunction.additionalId, Severity.FATAL_ERROR, wasHealed = true)
                    currentJunction.orientation = None
                }

                currentJunction.sStart.tap {
                    messageList += DefaultMessage.of("", "Attribute 'sStart' shall only be specified for virtual junctions", currentJunction.additionalId, Severity.FATAL_ERROR, wasHealed = true)
                    currentJunction.sStart = None
                }

                currentJunction.sEnd.tap {
                    messageList += DefaultMessage.of("", "Attribute 'sEnd' shall only be specified for virtual junctions", currentJunction.additionalId, Severity.FATAL_ERROR, wasHealed = true)
                    currentJunction.sEnd = None
                }
            }

            currentJunction
        }

        return ContextMessageList(healedOpendriveModel, messageList)
    }
}