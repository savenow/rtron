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

package io.rtron.transformer.roadspaces2citygml.transformer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.rtron.io.logging.Logger
import io.rtron.model.roadspaces.roadspace.objects.RoadspaceObject
import io.rtron.std.Optional
import io.rtron.std.mapAndHandleFailureOnOriginal
import io.rtron.std.unwrapValues
import io.rtron.transformer.roadspaces2citygml.module.BuildingModuleBuilder
import io.rtron.transformer.roadspaces2citygml.module.CityFurnitureModuleBuilder
import io.rtron.transformer.roadspaces2citygml.module.GenericsModuleBuilder
import io.rtron.transformer.roadspaces2citygml.module.IdentifierAdder
import io.rtron.transformer.roadspaces2citygml.module.VegetationModuleBuilder
import io.rtron.transformer.roadspaces2citygml.parameter.Roadspaces2CitygmlConfiguration
import io.rtron.transformer.roadspaces2citygml.router.RoadspaceObjectRouter
import org.citygml4j.model.core.AbstractCityObject
import org.citygml4j.model.core.CityModel

/**
 * Transforms [RoadspaceObject] classes (RoadSpaces model) to the [CityModel] (CityGML model).
 */
class RoadspaceObjectTransformer(
    private val configuration: Roadspaces2CitygmlConfiguration,
    private val identifierAdder: IdentifierAdder
) {

    // Properties and Initializers
    private val _reportLogger: Logger = configuration.getReportLogger()

    private val _genericsModuleBuilder = GenericsModuleBuilder(configuration, identifierAdder)
    private val _buildingModuleBuilder = BuildingModuleBuilder(configuration, identifierAdder)
    private val _cityFurnitureModuleBuilder = CityFurnitureModuleBuilder(configuration, identifierAdder)
    private val _vegetationModuleBuilder = VegetationModuleBuilder(configuration, identifierAdder)

    // Methods

    /**
     * Transforms a list of [roadspaceObjects] (RoadSpaces model) to the [AbstractCityObject] (CityGML model).
     */
    fun transformRoadspaceObjects(roadspaceObjects: List<RoadspaceObject>): List<AbstractCityObject> =
        roadspaceObjects.mapAndHandleFailureOnOriginal(
            { transformSingleRoadspaceObject(it) },
            { result, original -> _reportLogger.log(result, original.id.toString()) }
        ).unwrapValues()

    /**
     * Creates a city object (CityGML model) from the [RoadspaceObject] and it's geometry.
     * Contains the rules which determine the CityGML feature types from the [RoadspaceObject].
     *
     * @param roadspaceObject road space object from the RoadSpaces model
     * @return city object (CityGML model)
     */
    private fun transformSingleRoadspaceObject(roadspaceObject: RoadspaceObject): Result<Optional<AbstractCityObject>, Exception> =
        when (RoadspaceObjectRouter.route(roadspaceObject)) {
            RoadspaceObjectRouter.CitygmlTargetFeatureType.BUILDING_BUILDING -> _buildingModuleBuilder.createBuildingFeature(roadspaceObject).map { Optional(it) }
            RoadspaceObjectRouter.CitygmlTargetFeatureType.CITYFURNITURE_CITYFURNITURE -> _cityFurnitureModuleBuilder.createCityFurnitureFeature(roadspaceObject).map { Optional(it) }
            RoadspaceObjectRouter.CitygmlTargetFeatureType.GENERICS_GENERICOCCUPIEDSPACE -> _genericsModuleBuilder.createGenericOccupiedSpaceFeature(roadspaceObject).map { Optional(it) }
            RoadspaceObjectRouter.CitygmlTargetFeatureType.TRANSPORTATION_TRAFFICSPACE -> Result.success(Optional.empty())
            RoadspaceObjectRouter.CitygmlTargetFeatureType.TRANSPORTATION_AUXILIARYTRAFFICSPACE -> Result.success(Optional.empty())
            RoadspaceObjectRouter.CitygmlTargetFeatureType.TRANSPORTATION_MARKING -> Result.success(Optional.empty())
            RoadspaceObjectRouter.CitygmlTargetFeatureType.VEGETATION_SOLITARYVEGEATIONOBJECT -> _vegetationModuleBuilder.createSolitaryVegetationFeature(roadspaceObject).map { Optional(it) }
        }
}