/*
 * Copyright 2019-2020 Chair of Geoinformatics, Technical University of Munich
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

package io.rtron.transformer.roadspaces2citygml.module

import com.github.kittinunf.result.Result
import io.rtron.model.roadspaces.roadspace.objects.RoadspaceObject
import io.rtron.std.handleFailure
import io.rtron.transformer.roadspaces2citygml.geometry.GeometryTransformer
import io.rtron.transformer.roadspaces2citygml.geometry.LevelOfDetail
import io.rtron.transformer.roadspaces2citygml.geometry.populateGeometryOrImplicitGeometry
import io.rtron.transformer.roadspaces2citygml.parameter.Roadspaces2CitygmlConfiguration
import org.citygml4j.model.building.Building

/**
 * Builder for city objects of the CityGML Building module.
 */
class BuildingModuleBuilder(
    val configuration: Roadspaces2CitygmlConfiguration,
    private val identifierAdder: IdentifierAdder
) {
    // Properties and Initializers
    private val _reportLogger = configuration.getReportLogger()
    private val _attributesAdder = AttributesAdder(configuration.parameters)

    // Methods
    fun createBuildingFeature(roadspaceObject: RoadspaceObject): Result<Building, Exception> {
        val buildingFeature = Building()

        // geometry
        val geometryTransformer = GeometryTransformer.of(roadspaceObject, configuration.parameters)
        buildingFeature.populateGeometryOrImplicitGeometry(geometryTransformer, LevelOfDetail.ONE)
            .handleFailure { return it }

        // semantics
        identifierAdder.addIdentifier(roadspaceObject.id, roadspaceObject.name, buildingFeature)
        _attributesAdder.addAttributes(roadspaceObject, buildingFeature)

        return Result.success(buildingFeature)
    }
}
