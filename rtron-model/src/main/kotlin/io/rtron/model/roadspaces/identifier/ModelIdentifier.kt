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

package io.rtron.model.roadspaces.identifier

import arrow.core.Option
import io.rtron.io.files.FileIdentifier
import io.rtron.io.files.FileIdentifierInterface

/**
 * Model identifier interface required for class delegation.
 */
interface ModelIdentifierInterface {
    val modelName: Option<String>
    val modelDate: Option<String>
    val modelVendor: Option<String>
}

/**
 * Identifier of a model containing essential meta information.
 *
 * @param modelName name of the model
 * @param modelDate date of model creation
 * @param modelVendor organization or vendor of the model
 * @param sourceFileIdentifier identifier of the model's source file
 */
data class ModelIdentifier(
    override val modelName: Option<String>,
    override val modelDate: Option<String>,
    override val modelVendor: Option<String>,
    val sourceFileIdentifier: FileIdentifier
) : AbstractRoadspacesIdentifier(), ModelIdentifierInterface, FileIdentifierInterface by sourceFileIdentifier {

    override fun toIdentifierText(): String {
        return "ModelIdentifier(modelName=$modelName, modelDate=$modelDate, modelVendor=$modelVendor)"
    }

    override fun toStringMap(): Map<String, String> = emptyMap()
}
